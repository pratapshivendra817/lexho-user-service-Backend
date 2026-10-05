package com.lexho.user_service.dispatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.config.DispatchProperties;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerServiceSkillRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.repository.PincodeAdjacencyRepository;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchOrchestrationSkillFilterTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private LexhoAddressRepository lexhoAddressRepository;
    @Mock
    private ServiceableAreaRepository serviceableAreaRepository;
    @Mock
    private PartnerShiftRepository partnerShiftRepository;
    @Mock
    private CurrentPartnerLocationRepository currentPartnerLocationRepository;
    @Mock
    private PincodeAdjacencyRepository pincodeAdjacencyRepository;
    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private PartnerServiceRequestRepository partnerServiceRequestRepository;
    @Mock
    private PartnerServiceSkillRepository partnerServiceSkillRepository;
    @Mock
    private AdminServiceEntityRepository adminServiceEntityRepository;
    @Mock
    private BookingEventRepository bookingEventRepository;
    @Mock
    private DispatchScoringPolicy dispatchScoringPolicy;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TrackingLifecycleService trackingLifecycleService;

    private DispatchOrchestrationService dispatchOrchestrationService;

    @BeforeEach
    void setUp() {
        dispatchOrchestrationService = new DispatchOrchestrationService(
                orderRepository,
                lexhoAddressRepository,
                serviceableAreaRepository,
                partnerShiftRepository,
                currentPartnerLocationRepository,
                pincodeAdjacencyRepository,
                partnerRepository,
                partnerServiceRequestRepository,
                partnerServiceSkillRepository,
                adminServiceEntityRepository,
                bookingEventRepository,
                dispatchScoringPolicy,
                objectMapper,
                trackingLifecycleService,
                dispatchProperties(),
                new PartnerDispatchEligibilityService()
        );
    }

    @Test
    void cookRequestShouldDispatchOnlyToCookSkilledPartner() {
        baseData(101L, 1L);
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(1L, 101L)).thenReturn(true);
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(2L, 101L)).thenReturn(false);
        when(partnerServiceSkillRepository.existsByPartnerIdAndCategoryId(2L, 1L)).thenReturn(false);

        int created = dispatchOrchestrationService.dispatchOrder(9001L, 500L, 700L, 101L, 1, LocalDateTime.now());
        assertEquals(1, created);

        ArgumentCaptor<PartnerServiceRequestEntity> requestCaptor = ArgumentCaptor.forClass(PartnerServiceRequestEntity.class);
        verify(partnerServiceRequestRepository).save(requestCaptor.capture());
        assertEquals(1L, requestCaptor.getValue().getPartnerId());
        assertEquals(101L, requestCaptor.getValue().getServiceId());
    }

    @Test
    void cleaningRequestShouldNotDispatchToCookOnlyPartner() {
        baseData(201L, 2L);
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(1L, 201L)).thenReturn(false);
        when(partnerServiceSkillRepository.existsByPartnerIdAndCategoryId(1L, 2L)).thenReturn(false);
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(2L, 201L)).thenReturn(true);

        int created = dispatchOrchestrationService.dispatchOrder(9001L, 500L, 700L, 201L, 1, LocalDateTime.now());
        assertEquals(1, created);

        ArgumentCaptor<PartnerServiceRequestEntity> requestCaptor = ArgumentCaptor.forClass(PartnerServiceRequestEntity.class);
        verify(partnerServiceRequestRepository).save(requestCaptor.capture());
        assertEquals(2L, requestCaptor.getValue().getPartnerId());
        assertEquals(201L, requestCaptor.getValue().getServiceId());
    }

    @Test
    void cleaningRequestShouldDispatchToAllEligibleCleaningSkilledPartnersIncludingMultiSkill() {
        baseData(201L, 2L);
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(1L, 201L)).thenReturn(true);
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(2L, 201L)).thenReturn(true);

        int created = dispatchOrchestrationService.dispatchOrder(9001L, 500L, 700L, 201L, 1, LocalDateTime.now());
        assertEquals(2, created);
    }

    private BaseData baseData(Long subServiceId, Long categoryId) {
        Order order = new Order();
        order.setId(9001L);
        order.setStatus(OrderStatus.BOOKING_CREATED);
        order.setBookingType("SCHEDULED");
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        when(orderRepository.findByIdForUpdate(9001L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        LexhoAddress address = new LexhoAddress();
        address.setUserId(500L);
        address.setHouseNo("H1");
        address.setArea("Area");
        address.setCity("City");
        address.setState("State");
        address.setPincode("560001");
        address.setLatitude(java.math.BigDecimal.valueOf(12.9716));
        address.setLongitude(java.math.BigDecimal.valueOf(77.5946));
        when(lexhoAddressRepository.findByIdAndUserId(700L, 500L)).thenReturn(Optional.of(address));
        when(serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse("560001")).thenReturn(true);
        when(currentPartnerLocationRepository.findNearbyPartnerIds(anyDouble(), anyDouble(), eq(1.0d), eq(200))).thenReturn(List.of(1L, 2L));

        PartnerShiftEntity shiftRaj = new PartnerShiftEntity();
        shiftRaj.setPartnerId(1L);
        shiftRaj.setOnline(true);
        shiftRaj.setCurrentPincode("560001");
        shiftRaj.setActiveJobsCount(0);
        shiftRaj.setMaxActiveJobs(3);
        shiftRaj.setShiftStart(LocalDateTime.now().minusHours(1));
        shiftRaj.setShiftEnd(LocalDateTime.now().plusHours(1));

        PartnerShiftEntity shiftAman = new PartnerShiftEntity();
        shiftAman.setPartnerId(2L);
        shiftAman.setOnline(true);
        shiftAman.setCurrentPincode("560001");
        shiftAman.setActiveJobsCount(0);
        shiftAman.setMaxActiveJobs(3);
        shiftAman.setShiftStart(LocalDateTime.now().minusHours(1));
        shiftAman.setShiftEnd(LocalDateTime.now().plusHours(1));
        when(partnerShiftRepository.findByPartnerIdInAndDeletedFalse(List.of(1L, 2L))).thenReturn(List.of(shiftRaj, shiftAman));

        Partner raj = new Partner();
        raj.setId(1L);
        raj.setName("Raj");
        raj.setAvailable(true);
        raj.setVerificationStatus("VERIFIED");
        raj.setCategoryId(null);
        raj.setSubCategoryId(null);

        Partner aman = new Partner();
        aman.setId(2L);
        aman.setName("Aman");
        aman.setAvailable(true);
        aman.setVerificationStatus("VERIFIED");
        aman.setCategoryId(null);
        aman.setSubCategoryId(null);
        when(partnerRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(raj, aman));

        com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity rajLocation =
                new com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity();
        rajLocation.setPartnerId(1L);
        rajLocation.setLatitude(java.math.BigDecimal.valueOf(12.9717));
        rajLocation.setLongitude(java.math.BigDecimal.valueOf(77.5947));
        com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity amanLocation =
                new com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity();
        amanLocation.setPartnerId(2L);
        amanLocation.setLatitude(java.math.BigDecimal.valueOf(12.9718));
        amanLocation.setLongitude(java.math.BigDecimal.valueOf(77.5948));
        lenient().when(currentPartnerLocationRepository.findByPartnerId(1L)).thenReturn(Optional.of(rajLocation));
        lenient().when(currentPartnerLocationRepository.findByPartnerId(2L)).thenReturn(Optional.of(amanLocation));
        AdminCategoryEntity category = new AdminCategoryEntity();
        setId(category, categoryId);
        AdminServiceEntity service = new AdminServiceEntity();
        setId(service, subServiceId);
        service.setCategory(category);
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(subServiceId)).thenReturn(Optional.of(service));

        when(dispatchScoringPolicy.isWithinDispatchRadius(any(), anyDouble())).thenReturn(true);
        when(dispatchScoringPolicy.comparator("560001"))
                .thenReturn(Comparator.comparing(c -> c.partner().getId()));
        when(partnerServiceRequestRepository.findByCartId(9001L)).thenReturn(List.of());
        lenient().when(partnerServiceRequestRepository.existsByCartIdAndPartnerIdAndStatus(9001L, 1L, PartnerServiceRequestStatus.PENDING)).thenReturn(false);
        lenient().when(partnerServiceRequestRepository.existsByCartIdAndPartnerIdAndStatus(9001L, 2L, PartnerServiceRequestStatus.PENDING)).thenReturn(false);
        lenient().when(partnerShiftRepository.findByPartnerIdForUpdate(1L)).thenReturn(Optional.of(shiftRaj));
        lenient().when(partnerShiftRepository.findByPartnerIdForUpdate(2L)).thenReturn(Optional.of(shiftAman));
        lenient().when(partnerRepository.findById(1L)).thenReturn(Optional.of(raj));
        lenient().when(partnerRepository.findById(2L)).thenReturn(Optional.of(aman));

        return new BaseData(order, address, raj, aman);
    }

    private static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private record BaseData(Order order, LexhoAddress address, Partner raj, Partner aman) {}

    private static DispatchProperties dispatchProperties() {
        DispatchProperties properties = new DispatchProperties();
        properties.getPartner().setInitialSearchRadiusKm(1.0d);
        properties.getPartner().setMaxSearchRadiusKm(2.0d);
        properties.getBooking().setMaxEtaMinutes(15);
        properties.getBooking().setPreDispatchMinutes(30);
        return properties;
    }
}
