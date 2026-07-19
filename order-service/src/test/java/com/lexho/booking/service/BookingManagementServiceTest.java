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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingManagementServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingManagementService service;

    private Booking booking;

    @BeforeEach
    void setUp() {
        booking = new Booking();
        booking.setId(1L);
        booking.setBookingId("BK1291");
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setDeleted(false);
    }

    @Test
    void getStatsReturnsAllStatusCounts() {
        when(bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.PENDING)).thenReturn(5L);
        when(bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.CONFIRMED)).thenReturn(8L);
        when(bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.COMPLETED)).thenReturn(13L);
        when(bookingRepository.countByBookingStatusAndDeletedFalse(BookingStatus.CANCELLED)).thenReturn(2L);

        BookingStatsResponse stats = service.getStats();

        assertEquals(5L, stats.getPending());
        assertEquals(8L, stats.getConfirmed());
        assertEquals(13L, stats.getCompleted());
        assertEquals(2L, stats.getCancelled());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllBookingsClampsPagingUsesAllowedSortAndMapsResults() {
        BookingListItemResponse mapped = new BookingListItemResponse();
        mapped.setId(1L);
        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingMapper.toListItem(booking)).thenReturn(mapped);

        PageResponse<BookingListItemResponse> result = service.getAllBookings(
                -4, 250, "amount", "asc", "  BK12  ", "confirmed", "Cleaning");

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(bookingRepository).findAll(any(Specification.class), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
        Sort.Order order = pageable.getValue().getSort().getOrderFor("amount");
        assertTrue(order != null && order.isAscending());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllBookingsUsesDefaultsForInvalidSortBlankFiltersAndMinimumSize() {
        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<BookingListItemResponse> result =
                service.getAllBookings(2, 0, "unknown", "DESC", " ", null, "ALL");

        assertTrue(result.getContent().isEmpty());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(bookingRepository).findAll(any(Specification.class), pageable.capture());
        assertEquals(1, pageable.getValue().getPageSize());
        assertTrue(pageable.getValue().getSort().getOrderFor("createdAt").isDescending());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getAllBookingsSpecificationBuildsEveryConfiguredPredicate() {
        ArgumentCaptor<Specification<Booking>> specification = ArgumentCaptor.forClass(Specification.class);
        when(bookingRepository.findAll(specification.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.getAllBookings(0, 10, "createdAt", "desc", " Alice ", "PENDING", "Cleaning");

        Root<Booking> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Path deleted = org.mockito.Mockito.mock(Path.class);
        Path status = org.mockito.Mockito.mock(Path.class);
        Path bookingId = org.mockito.Mockito.mock(Path.class);
        Join customer = org.mockito.Mockito.mock(Join.class);
        Join partner = org.mockito.Mockito.mock(Join.class);
        Join serviceJoin = org.mockito.Mockito.mock(Join.class);
        Path customerName = org.mockito.Mockito.mock(Path.class);
        Path partnerName = org.mockito.Mockito.mock(Path.class);
        Path serviceName = org.mockito.Mockito.mock(Path.class);
        Expression lowerBookingId = org.mockito.Mockito.mock(Expression.class);
        Expression lowerCustomer = org.mockito.Mockito.mock(Expression.class);
        Expression lowerPartner = org.mockito.Mockito.mock(Expression.class);
        Expression lowerService = org.mockito.Mockito.mock(Expression.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);
        when(root.get("deleted")).thenReturn(deleted);
        when(root.get("bookingStatus")).thenReturn(status);
        when(root.get("bookingId")).thenReturn(bookingId);
        when(root.join("customer", jakarta.persistence.criteria.JoinType.LEFT)).thenReturn(customer);
        when(root.join("partner", jakarta.persistence.criteria.JoinType.LEFT)).thenReturn(partner);
        when(root.join("service", jakarta.persistence.criteria.JoinType.LEFT)).thenReturn(serviceJoin);
        when(customer.get("fullName")).thenReturn(customerName);
        when(partner.get("fullName")).thenReturn(partnerName);
        when(serviceJoin.get("name")).thenReturn(serviceName);
        when(cb.lower(bookingId)).thenReturn(lowerBookingId);
        when(cb.lower(customerName)).thenReturn(lowerCustomer);
        when(cb.lower(partnerName)).thenReturn(lowerPartner);
        when(cb.lower(serviceName)).thenReturn(lowerService);
        when(cb.isFalse(deleted)).thenReturn(predicate);
        when(cb.equal(status, BookingStatus.PENDING)).thenReturn(predicate);
        when(cb.like(any(Expression.class), any(String.class))).thenReturn(predicate);
        when(cb.or(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class)))
                .thenReturn(predicate);
        when(cb.equal(lowerService, "cleaning")).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        Predicate result = specification.getValue().toPredicate(root, query, cb);

        assertSame(predicate, result);
        verify(cb).equal(status, BookingStatus.PENDING);
        verify(cb).or(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class));
        verify(cb).equal(lowerService, "cleaning");
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getAllBookingsSpecificationUsesOnlyDeletedPredicateWithoutFilters() {
        ArgumentCaptor<Specification<Booking>> specification = ArgumentCaptor.forClass(Specification.class);
        when(bookingRepository.findAll(specification.capture(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.getAllBookings(0, 10, null, null, null, null, "ALL");

        Root<Booking> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder cb = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Path deleted = org.mockito.Mockito.mock(Path.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);
        when(root.get("deleted")).thenReturn(deleted);
        when(cb.isFalse(deleted)).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        assertSame(predicate, specification.getValue().toPredicate(root, query, cb));
        verify(cb, never()).or(any(Predicate[].class));
        verify(root, never()).join(any(String.class), any(jakarta.persistence.criteria.JoinType.class));
    }

    @Test
    void getAllBookingsRejectsUnknownStatusBeforeRepositoryCall() {
        BookingValidationException exception = assertThrows(BookingValidationException.class,
                () -> service.getAllBookings(0, 10, null, null, null, "unknown", null));

        assertEquals("Invalid booking status: unknown", exception.getMessage());
        verify(bookingRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getBookingByIdMapsActiveBooking() {
        BookingDetailResponse mapped = new BookingDetailResponse();
        mapped.setId(1L);
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDetail(booking)).thenReturn(mapped);

        assertSame(mapped, service.getBookingById(1L));
    }

    @Test
    void retrievalAndMutationThrowWhenBookingDoesNotExist() {
        when(bookingRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () -> service.getBookingById(99L));
        assertThrows(BookingNotFoundException.class, () -> service.cancelBooking(99L));
        assertThrows(BookingNotFoundException.class, () -> service.softDeleteBooking(99L));
    }

    @Test
    void updateBookingUpdatesAllMutableFieldsAndNormalizesBlankNotes() {
        User partner = partner(Role.PARTNER);
        BookingUpdateRequest request = request(11L);
        request.setNotes("   ");
        BookingDetailResponse mapped = new BookingDetailResponse();
        mapped.setId(1L);
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(partner));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toDetail(booking)).thenReturn(mapped);

        BookingDetailResponse result = service.updateBooking(1L, request);

        assertSame(mapped, result);
        assertEquals(request.getBookingDate(), booking.getBookingDate());
        assertEquals(request.getBookingTime(), booking.getBookingTime());
        assertEquals(request.getAmount(), booking.getAmount());
        assertSame(partner, booking.getPartner());
        assertNull(booking.getNotes());
    }

    @Test
    void updateBookingRejectsCompletedBooking() {
        booking.setBookingStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));

        assertThrows(BookingOperationException.class, () -> service.updateBooking(1L, request(11L)));
        verify(userRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    void updateBookingRejectsMissingPartner() {
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.empty());

        BookingValidationException exception = assertThrows(BookingValidationException.class,
                () -> service.updateBooking(1L, request(11L)));

        assertEquals("Partner not found: 11", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateBookingRejectsUserWithoutPartnerRole() {
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(partner(Role.CUSTOMER)));

        assertThrows(BookingValidationException.class, () -> service.updateBooking(1L, request(11L)));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBookingPersistsCancelledStateAndMapsResponse() {
        BookingDetailResponse mapped = new BookingDetailResponse();
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toDetail(booking)).thenReturn(mapped);

        assertSame(mapped, service.cancelBooking(1L));
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
    }

    @Test
    void cancelBookingRejectsCancelledAndCompletedStates() {
        booking.setBookingStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));
        assertThrows(BookingOperationException.class, () -> service.cancelBooking(1L));

        booking.setBookingStatus(BookingStatus.COMPLETED);
        assertThrows(BookingOperationException.class, () -> service.cancelBooking(1L));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void softDeleteBookingMarksDeletedAndPersists() {
        when(bookingRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(booking));

        service.softDeleteBooking(1L);

        assertTrue(booking.isDeleted());
        verify(bookingRepository).save(booking);
    }

    @Test
    void getActiveBookingsForCustomerDelegatesToRepository() {
        when(bookingRepository.findByCustomerIdAndDeletedFalse(7L)).thenReturn(List.of(booking));

        List<Booking> result = service.getActiveBookingsForCustomer(7L);

        assertEquals(List.of(booking), result);
    }

    @Test
    void validateServiceReturnsCategoryOrThrows() {
        ServiceCategory category = new ServiceCategory();
        category.setId(4L);
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(5L)).thenReturn(Optional.empty());

        assertSame(category, service.validateService(4L));
        BookingValidationException exception =
                assertThrows(BookingValidationException.class, () -> service.validateService(5L));
        assertEquals("Service not found: 5", exception.getMessage());
    }

    private BookingUpdateRequest request(Long providerId) {
        BookingUpdateRequest request = new BookingUpdateRequest();
        request.setBookingDate(LocalDate.of(2026, 7, 1));
        request.setBookingTime(LocalTime.of(10, 30));
        request.setProviderId(providerId);
        request.setAmount(BigDecimal.valueOf(799));
        request.setNotes("Morning slot");
        return request;
    }

    private User partner(Role role) {
        User user = new User();
        user.setRole(role);
        user.setDeleted(false);
        user.setFullName("Partner");
        return user;
    }
}
