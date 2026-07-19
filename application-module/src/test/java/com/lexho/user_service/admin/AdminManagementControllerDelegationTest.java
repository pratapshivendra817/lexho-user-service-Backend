package com.lexho.user_service.admin;

import com.lexho.user_service.admin.controller.AdminManagementController;
import com.lexho.user_service.admin.dto.AdminCategoryStatusRequest;
import com.lexho.user_service.admin.dto.AdminCategoryUpsertRequest;
import com.lexho.user_service.admin.dto.AdminServiceDetailBulkRequest;
import com.lexho.user_service.admin.dto.AdminServiceDetailUpdateRequest;
import com.lexho.user_service.admin.dto.AdminServiceUpsertRequest;
import com.lexho.user_service.admin.dto.AdminSupportFaqCategoryUpsertRequest;
import com.lexho.user_service.admin.dto.AdminSupportFaqUpsertRequest;
import com.lexho.user_service.admin.dto.AdminSupportTicketReplyRequest;
import com.lexho.user_service.admin.dto.AdminSupportTicketStatusRequest;
import com.lexho.user_service.admin.service.AdminCatalogService;
import com.lexho.user_service.admin.service.AdminFileUploadService;
import com.lexho.user_service.admin.service.AdminManagementService;
import com.lexho.user_service.admin.service.AdminSupportFaqService;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerUpsertRequest;
import com.lexho.user_service.customer.banner.service.AdminCustomerBannerService;
import com.lexho.user_service.service.ApiRateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminManagementControllerDelegationTest {

    @Mock
    private AdminManagementService managementService;
    @Mock
    private AdminCatalogService catalogService;
    @Mock
    private AdminFileUploadService fileUploadService;
    @Mock
    private AdminCustomerBannerService bannerService;
    @Mock
    private AdminSupportFaqService faqService;
    @Mock
    private ApiRateLimiterService rateLimiterService;

    private AdminManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminManagementController(
                managementService,
                catalogService,
                fileUploadService,
                bannerService,
                faqService,
                rateLimiterService);
    }

    @Test
    void delegatesEveryCategoryAndServiceEndpoint() {
        AdminCategoryUpsertRequest category = mock(AdminCategoryUpsertRequest.class);
        AdminCategoryStatusRequest categoryStatus = mock(AdminCategoryStatusRequest.class);
        AdminServiceUpsertRequest service = mock(AdminServiceUpsertRequest.class);
        AdminServiceDetailBulkRequest detailBulk = mock(AdminServiceDetailBulkRequest.class);
        AdminServiceDetailUpdateRequest detailUpdate = mock(AdminServiceDetailUpdateRequest.class);
        when(categoryStatus.getActive()).thenReturn(true);

        controller.createCategory(category);
        controller.updateCategory(1L, category);
        controller.deleteCategory(1L);
        controller.categories(2, 25, "clean");
        controller.updateCategoryStatus(1L, categoryStatus);
        controller.createService(service);
        controller.updateService(2L, service);
        controller.services(3, 20, "cook", "HOME", "ACTIVE", "name");
        controller.serviceById(2L);
        controller.toggleServiceStatus(2L);
        controller.deleteService(2L);
        controller.addServiceDetails(2L, detailBulk);
        controller.updateServiceDetail(2L, 3L, detailUpdate);
        controller.deleteServiceDetail(2L, 3L);
        controller.mediaSnapshot(15);
        controller.mediaLatest(16);

        verify(catalogService).createCategory(category);
        verify(catalogService).updateCategory(1L, category);
        verify(catalogService).deleteCategory(1L);
        verify(catalogService).listCategories(2, 25, "clean");
        verify(catalogService).updateCategoryStatus(1L, true);
        verify(catalogService).createService(service);
        verify(catalogService).updateService(2L, service);
        verify(catalogService).listServices(3, 20, "cook", "HOME", "ACTIVE", "name");
        verify(catalogService).getServiceById(2L);
        verify(catalogService).toggleServiceStatus(2L);
        verify(catalogService).deleteService(2L);
        verify(catalogService).addServiceDetails(2L, detailBulk);
        verify(catalogService).updateServiceDetail(2L, 3L, detailUpdate);
        verify(catalogService).deleteServiceDetail(2L, 3L);
        verify(catalogService).mediaDebugSnapshot(15);
        verify(catalogService).mediaDebugLatestActive(16);
    }

    @Test
    void delegatesBannerManagementAndBothUploadEndpoints() {
        AdminCustomerBannerUpsertRequest request = mock(AdminCustomerBannerUpsertRequest.class);
        MockMultipartFile file = new MockMultipartFile("file", "banner.jpg", "image/jpeg", new byte[]{1});
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setScheme("https");
        servletRequest.setServerName("admin.lexho.test");
        servletRequest.setServerPort(443);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        when(fileUploadService.uploadServiceImage(file)).thenReturn("/uploads/banner.jpg");

        controller.createCustomerBanner(request);
        controller.updateCustomerBanner(5L, request);
        controller.customerBannerById(5L);
        controller.customerBanners(1, 10, 9L, true);
        controller.customerBannerStatus(5L, false);
        controller.deleteCustomerBanner(5L);
        var image = controller.uploadImage(file);
        var bannerImage = controller.uploadBannerImageForCustomerBanners(file);

        verify(bannerService).create(request);
        verify(bannerService).update(5L, request);
        verify(bannerService).byId(5L);
        verify(bannerService).list(1, 10, 9L, true);
        verify(bannerService).updateStatus(5L, false);
        verify(bannerService).delete(5L);
        assertThat(image.toString()).contains("https://admin.lexho.test/uploads/banner.jpg");
        assertThat(bannerImage.toString()).contains("https://admin.lexho.test/uploads/banner.jpg");
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void delegatesOperationsSupportTicketsAndEnforcesPayoutRateLimit() {
        AdminSupportTicketStatusRequest statusRequest = mock(AdminSupportTicketStatusRequest.class);
        AdminSupportTicketReplyRequest replyRequest = mock(AdminSupportTicketReplyRequest.class);
        when(statusRequest.getStatus()).thenReturn("RESOLVED");
        when(replyRequest.getMessage()).thenReturn("Completed");

        controller.payouts(2, 50, "partner", "PENDING");
        controller.activityLogs(3, 20, "login");
        controller.supportTickets(1, 25, "refund", "OPEN", "PAYMENT", "HIGH");
        controller.supportTicketDetail(77L);
        controller.supportTicketStatus(77L, statusRequest);
        controller.supportTicketReply(77L, replyRequest);

        verify(rateLimiterService).check(
                "admin_payouts",
                "global",
                60,
                Duration.ofMinutes(1),
                "Too many payout queries. Please retry shortly");
        verify(managementService).payouts(2, 50, "partner", "PENDING");
        verify(managementService).activityLogs(3, 20, "login");
        verify(managementService).supportTickets(1, 25, "refund", "OPEN", "PAYMENT", "HIGH");
        verify(managementService).supportTicketDetail(77L);
        verify(managementService).updateSupportTicketStatus(77L, "RESOLVED");
        verify(managementService).replySupportTicket(77L, "Completed");
    }

    @Test
    void delegatesEveryFaqEndpoint() {
        AdminSupportFaqCategoryUpsertRequest category = mock(AdminSupportFaqCategoryUpsertRequest.class);
        AdminSupportFaqUpsertRequest faq = mock(AdminSupportFaqUpsertRequest.class);

        controller.faqCategories();
        controller.createFaqCategory(category);
        controller.updateFaqCategory("booking", category);
        controller.deleteFaqCategory("booking");
        controller.faqs("booking");
        controller.createFaq("booking", faq);
        controller.updateFaq("booking", "faq-1", faq);
        controller.deleteFaq("booking", "faq-1");

        verify(faqService).listCategories();
        verify(faqService).createCategory(category);
        verify(faqService).updateCategory("booking", category);
        verify(faqService).deleteCategory("booking");
        verify(faqService).listFaqs("booking");
        verify(faqService).createFaq("booking", faq);
        verify(faqService).updateFaq("booking", "faq-1", faq);
        verify(faqService).deleteFaq("booking", "faq-1");
    }
}
