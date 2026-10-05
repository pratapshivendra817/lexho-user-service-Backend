package com.lexho.booking.service;

import com.lexho.booking.dto.BookingDetailResponse;
import com.lexho.booking.dto.BookingListItemResponse;
import com.lexho.booking.dto.BookingStatsResponse;
import com.lexho.booking.dto.BookingUpdateRequest;
import com.lexho.booking.dto.PageResponse;
import com.lexho.booking.entity.Booking;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.booking.exception.BookingNotFoundException;
import com.lexho.booking.exception.BookingOperationException;
import com.lexho.booking.exception.BookingValidationException;
import com.lexho.booking.mapper.BookingMapper;
import com.lexho.booking.repository.BookingRepository;
import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.common.enums.Role;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BookingManagementService {

    private static final Logger LOG = LoggerFactory.getLogger(BookingManagementService.class);
    private static final String CREATED_AT_KEY = "createdAt";
    private static final String UPDATED_AT_KEY = "updatedAt";

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookingMapper bookingMapper;

    public BookingManagementService(BookingRepository bookingRepository,
                                    UserRepository userRepository,
                                    CategoryRepository categoryRepository,
                                    BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.bookingMapper = bookingMapper;
    }

    @Transactional(readOnly = true)
    public BookingStatsResponse getStats() {
        long pending = bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.PENDING);
        long confirmed = bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.CONFIRMED);
        long completed = bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.COMPLETED);
        long cancelled = bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.CANCELLED);
        return new BookingStatsResponse(pending, confirmed, completed, cancelled);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingListItemResponse> getAllBookings(int page,
                                                                int size,
                                                                String sortBy,
                                                                String sortDir,
                                                                String search,
                                                                String status,
                                                                String serviceType) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC, normalizeSortBy(sortBy))
        );

        BookingStatus statusFilter = parseStatus(status);
        String normalizedSearch = normalize(search);
        String normalizedServiceType = normalize(serviceType);

        Specification<Booking> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("bookingStatus"), statusFilter));
            }

            if (normalizedSearch != null) {
                String like = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
                Predicate bookingIdLike = cb.like(cb.lower(root.get("bookingId")), like);
                Predicate customerNameLike = cb.like(
                        cb.lower(root.join("customer", JoinType.LEFT).get("fullName")),
                        like
                );
                Predicate providerNameLike = cb.like(
                        cb.lower(root.join("partner", JoinType.LEFT).get("fullName")),
                        like
                );
                Predicate serviceNameLike = cb.like(
                        cb.lower(root.join("service", JoinType.LEFT).get("name")),
                        like
                );
                predicates.add(cb.or(bookingIdLike, customerNameLike, providerNameLike, serviceNameLike));
            }

            if (normalizedServiceType != null && !"ALL".equalsIgnoreCase(normalizedServiceType)) {
                predicates.add(cb.equal(
                        cb.lower(root.join("service", JoinType.LEFT).get("name")),
                        normalizedServiceType.toLowerCase(Locale.ROOT)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Booking> bookings = bookingRepository.findAll(specification, pageable);
        List<BookingListItemResponse> items = bookings.getContent().stream()
                .map(bookingMapper::toListItem)
                .toList();
        return new PageResponse<>(items, bookings.getTotalElements(), bookings.getNumber(), bookings.getSize());
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingById(Long id) {
        Booking booking = findActiveBooking(id);
        return bookingMapper.toDetail(booking);
    }

    @Transactional
    public BookingDetailResponse updateBooking(Long id, BookingUpdateRequest request) {
        Booking booking = findActiveBooking(id);
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new BookingOperationException("Completed bookings cannot be edited");
        }

        User partner = userRepository.findByIdAndDeletedFalse(request.getProviderId())
                .orElseThrow(() -> new BookingValidationException("Partner not found: " + request.getProviderId()));
        if (partner.getRole() != Role.PARTNER) {
            throw new BookingValidationException("Selected partner is not a valid service partner");
        }

        booking.setBookingDate(request.getBookingDate());
        booking.setBookingTime(request.getBookingTime());
        booking.setPartner(partner);
        booking.setAmount(request.getAmount());
        booking.setNotes(normalize(request.getNotes()));

        Booking saved = bookingRepository.save(booking);
        LOG.info("Booking updated id={} bookingId={}", saved.getId(), saved.getBookingId());
        return bookingMapper.toDetail(saved);
    }

    @Transactional
    public BookingDetailResponse cancelBooking(Long id) {
        Booking booking = findActiveBooking(id);
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BookingOperationException("Booking is already cancelled");
        }
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new BookingOperationException("Completed bookings cannot be cancelled");
        }
        booking.setBookingStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        LOG.info("Booking cancelled id={} bookingId={}", saved.getId(), saved.getBookingId());
        return bookingMapper.toDetail(saved);
    }

    @Transactional
    public void softDeleteBooking(Long id) {
        Booking booking = findActiveBooking(id);
        booking.setDeleted(true);
        bookingRepository.save(booking);
        LOG.info("Booking soft deleted id={} bookingId={}", booking.getId(), booking.getBookingId());
    }

    @Transactional(readOnly = true)
    public List<Booking> getActiveBookingsForCustomer(Long customerId) {
        return bookingRepository.findByCustomerIdAndDeletedFalse(customerId);
    }

    @Transactional(readOnly = true)
    public ServiceCategory validateService(Long serviceId) {
        return categoryRepository.findById(serviceId)
                .orElseThrow(() -> new BookingValidationException("Service not found: " + serviceId));
    }

    private Booking findActiveBooking(Long id) {
        return bookingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
    }

    private BookingStatus parseStatus(String statusText) {
        String normalized = normalize(statusText);
        if (normalized == null) {
            return null;
        }
        try {
            return BookingStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BookingValidationException("Invalid booking status: " + statusText);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSortBy(String sortBy) {
        String normalized = normalize(sortBy);
        if (normalized == null) {
            return CREATED_AT_KEY;
        }
        return switch (normalized) {
            case "bookingDate", "amount", CREATED_AT_KEY, UPDATED_AT_KEY, "bookingStatus" -> normalized;
            default -> CREATED_AT_KEY;
        };
    }
}
