package com.lexho.user_service.admin;

import com.lexho.user_service.admin.service.AdminCustomerManagementService;
import com.lexho.user_service.customer.profile.entity.CustomerAccountDeleteRequestEntity;
import com.lexho.user_service.customer.profile.entity.CustomerEntity;
import com.lexho.user_service.customer.profile.repository.CustomerAccountDeleteRequestRepository;
import com.lexho.user_service.customer.profile.repository.CustomerRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCustomerManagementServiceTest {
    @Mock CustomerRepository customerRepository;
    @Mock LexhoAddressRepository addressRepository;
    @Mock CustomerAccountDeleteRequestRepository deleteRepository;
    private AdminCustomerManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminCustomerManagementService(customerRepository, addressRepository, deleteRepository);
    }

    @Test
    void listNormalizesSearchBoundsPageAndDefaultsInvalidSort() {
        CustomerEntity customer = customer(1L);
        when(customerRepository.search(isNull(), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer)));
        when(addressRepository.findByUserId(1L)).thenReturn(List.of());

        var result = service.list("  ", true, -2, 500, "invalid", "asc");

        assertEquals(1L, ((List<?>) result.get("items")).size());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).search(isNull(), eq(true), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(100, captor.getValue().getPageSize());
        assertEquals("updatedAt: ASC", captor.getValue().getSort().toString());
    }

    @Test
    void byIdAndStatusUpdateHandleFoundAndMissingCustomer() {
        CustomerEntity customer = customer(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(addressRepository.findByUserId(1L)).thenReturn(List.of());
        assertEquals("User", service.byId(1L).get("fullName"));
        assertFalse((Boolean) service.updateStatus(1L, false).get("active"));

        when(customerRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.byId(9L));
    }

    @Test
    void profileGovernanceAppliesOnlyProvidedFieldsAndTrimsNotes() {
        CustomerEntity customer = customer(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(addressRepository.findByUserId(1L)).thenReturn(List.of());

        var result = service.profileGovernance(1L, true, null, true, false, " note ");

        assertEquals(true, result.get("profileEditLocked"));
        assertEquals(false, result.get("emailVerified"));
        assertEquals(true, result.get("phoneVerified"));
        assertEquals("note", result.get("adminNotes"));
    }

    @Test
    void listDeleteRequestsChoosesFilteredOrUnfilteredRepository() {
        when(deleteRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        service.listDeleteRequests(null, 0, 10);
        verify(deleteRepository).findAll(any(Pageable.class));

        when(deleteRepository.findByResolved(eq(false), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        service.listDeleteRequests(false, 0, 10);
        verify(deleteRepository).findByResolved(eq(false), any(Pageable.class));
    }

    @Test
    void approveDeleteRequestDisablesAndLocksCustomer() {
        CustomerAccountDeleteRequestEntity request = deleteRequest(5L, 1L, false);
        CustomerEntity customer = customer(1L);
        when(deleteRepository.findById(5L)).thenReturn(Optional.of(request));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        var result = service.resolveDeleteRequest(5L, true, " approved ", 9L);

        assertEquals("APPROVED", result.get("resolutionStatus"));
        assertFalse(customer.getActive());
        assertTrue(customer.getProfileEditLocked());
        verify(deleteRepository).save(request);
        verify(customerRepository).save(customer);
    }

    @Test
    void resolvedRequestIsIdempotentAndRejectionDoesNotChangeCustomer() {
        CustomerAccountDeleteRequestEntity resolved = deleteRequest(5L, 1L, true);
        when(deleteRepository.findById(5L)).thenReturn(Optional.of(resolved));
        service.resolveDeleteRequest(5L, false, null, 9L);
        verify(deleteRepository, never()).save(any());
        verifyNoInteractions(customerRepository);

        CustomerAccountDeleteRequestEntity pending = deleteRequest(6L, 2L, false);
        when(deleteRepository.findById(6L)).thenReturn(Optional.of(pending));
        assertEquals("REJECTED", service.resolveDeleteRequest(6L, false, null, 9L).get("resolutionStatus"));
        verify(customerRepository, never()).findById(2L);
    }

    private CustomerEntity customer(Long id) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(id); customer.setFullName("User"); customer.setEmail("u@test.com");
        customer.setActive(true); customer.setProfileEditLocked(false); customer.setEmailVerified(false);
        customer.setPhoneVerified(false); customer.setProfileImageApproved(true);
        return customer;
    }

    private CustomerAccountDeleteRequestEntity deleteRequest(Long id, Long userId, boolean resolved) {
        CustomerAccountDeleteRequestEntity request = new CustomerAccountDeleteRequestEntity();
        ReflectionTestUtils.setField(request, "id", id);
        request.setUserId(userId); request.setResolved(resolved);
        return request;
    }
}
