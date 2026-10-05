package com.lexho.user_service.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.review.controller.AdminReviewController;
import com.lexho.user_service.review.controller.CustomerReviewController;
import com.lexho.user_service.review.controller.ProviderReviewController;
import com.lexho.user_service.review.dto.CustomerReviewResponse;
import com.lexho.user_service.review.dto.CustomerReviewSubmitResponse;
import com.lexho.user_service.review.dto.ModerationActionRequest;
import com.lexho.user_service.review.dto.ReviewDetailResponse;
import com.lexho.user_service.review.dto.ReviewPageResponse;
import com.lexho.user_service.review.dto.ReviewReportResponse;
import com.lexho.user_service.review.dto.ReviewResponse;
import com.lexho.user_service.review.dto.ReviewStatsResponse;
import com.lexho.user_service.review.dto.SubmitReviewRequest;
import com.lexho.user_service.review.enums.ReviewStatus;
import com.lexho.user_service.review.exception.ReviewAccessDeniedException;
import com.lexho.user_service.review.exception.ReviewExceptionHandler;
import com.lexho.user_service.review.exception.ReviewNotFoundException;
import com.lexho.user_service.review.exception.ReviewValidationException;
import com.lexho.user_service.review.service.ReviewManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewWebLayerTest {

    @Mock
    private ReviewManagementService reviewManagementService;

    private AdminReviewController adminReviewController;
    private CustomerReviewController customerReviewController;
    private ProviderReviewController providerReviewController;
    private ReviewExceptionHandler reviewExceptionHandler;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminReviewController = new AdminReviewController(reviewManagementService);
        customerReviewController = new CustomerReviewController(reviewManagementService);
        providerReviewController = new ProviderReviewController(reviewManagementService);
        reviewExceptionHandler = new ReviewExceptionHandler();

        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
        validatorFactoryBean.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(customerReviewController)
                .setControllerAdvice(reviewExceptionHandler)
                .setValidator(validatorFactoryBean)
                .build();
    }

    @Test
    void adminEndpointsDelegateToService() {
        ReviewResponse reviewResponse = reviewResponse(7L);
        ReviewDetailResponse detailResponse = new ReviewDetailResponse(reviewResponse, Map.of(), Map.of(), Map.of(),
                Map.of(), List.of(), List.of());
        ReviewPageResponse<ReviewResponse> pageResponse = new ReviewPageResponse<>(List.of(reviewResponse), 1, 1, 0);
        ReviewPageResponse<ReviewReportResponse> reportPage = new ReviewPageResponse<>(List.of(), 0, 0, 0);
        ReviewStatsResponse statsResponse = new ReviewStatsResponse(3, 4.5, 1, 1, List.of(), List.of());
        ModerationActionRequest actionRequest = new ModerationActionRequest();
        actionRequest.reason = "policy";

        when(reviewManagementService.getAllReviewsForAdmin(1, 5, "rating", "asc", 4, ReviewStatus.APPROVED, 11L, 22L, " query ",
                null, null, null))
                .thenReturn(pageResponse);
        when(reviewManagementService.getReviewDetailForAdmin(7L)).thenReturn(detailResponse);
        when(reviewManagementService.approveReview(7L, "policy")).thenReturn(reviewResponse);
        when(reviewManagementService.rejectReview(7L, "policy")).thenReturn(reviewResponse);
        when(reviewManagementService.hideReview(7L, "policy")).thenReturn(reviewResponse);
        when(reviewManagementService.unhideReview(7L, "policy")).thenReturn(reviewResponse);
        when(reviewManagementService.getReviewReportsForAdmin(2, 8, null)).thenReturn(reportPage);
        when(reviewManagementService.getReviewStats(null, null, null, null)).thenReturn(statsResponse);

        assertThat(adminReviewController.all(1, 5, "rating", "asc", 4, ReviewStatus.APPROVED, 11L, 22L, " query ",
                null, null, null).getBody())
                .isSameAs(pageResponse);
        assertThat(adminReviewController.byId(7L).getBody()).isSameAs(detailResponse);
        assertThat(adminReviewController.approve(7L, actionRequest).getBody()).isSameAs(reviewResponse);
        assertThat(adminReviewController.approvePost(7L, actionRequest).getBody()).isSameAs(reviewResponse);
        assertThat(adminReviewController.reject(7L, actionRequest).getBody()).isSameAs(reviewResponse);
        assertThat(adminReviewController.rejectPost(7L, actionRequest).getBody()).isSameAs(reviewResponse);
        assertThat(adminReviewController.hide(7L, actionRequest).getBody()).isSameAs(reviewResponse);
        assertThat(adminReviewController.unhide(7L, actionRequest).getBody()).isSameAs(reviewResponse);
        assertThat(adminReviewController.reported(2, 8, null).getBody()).isSameAs(reportPage);
        assertThat(adminReviewController.stats(null, null, null, null).getBody()).isSameAs(statsResponse);

        verify(reviewManagementService, times(2)).approveReview(7L, "policy");
        verify(reviewManagementService, times(2)).rejectReview(7L, "policy");
        verify(reviewManagementService).hideReview(7L, "policy");
        verify(reviewManagementService).unhideReview(7L, "policy");
    }

    @Test
    void bulkStatusProcessesEverySupportedActionAndSkipsNullIds() {
        AdminReviewController.BulkReviewStatusRequest request = new AdminReviewController.BulkReviewStatusRequest();
        request.reviewIds = Arrays.asList(1L, null, 2L);
        request.action = "approve";

        Map<String, Object> approveResult = adminReviewController.bulkStatus(request).getBody();

        assertThat(approveResult).containsEntry("success", true);
        assertThat(approveResult).containsEntry("processed", 2L);
        verify(reviewManagementService).approveReview(1L, null);
        verify(reviewManagementService).approveReview(2L, null);

        request.action = "reject";
        adminReviewController.bulkStatus(request);
        verify(reviewManagementService).rejectReview(1L, null);
        verify(reviewManagementService).rejectReview(2L, null);

        request.action = "hide";
        adminReviewController.bulkStatus(request);
        verify(reviewManagementService).hideReview(1L, null);
        verify(reviewManagementService).hideReview(2L, null);

        request.action = "unhide";
        adminReviewController.bulkStatus(request);
        verify(reviewManagementService, times(2)).unhideReview(any(Long.class), any());
    }

    @Test
    void bulkStatusRejectsUnsupportedAction() {
        AdminReviewController.BulkReviewStatusRequest request = new AdminReviewController.BulkReviewStatusRequest();
        request.reviewIds = List.of(1L);
        request.action = "archive";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminReviewController.bulkStatus(request));

        assertThat(exception).hasMessageContaining("Invalid action");
    }

    @Test
    void customerAndProviderControllersReturnWrappedResponses() {
        SubmitReviewRequest submitReviewRequest = new SubmitReviewRequest();
        submitReviewRequest.setBookingId(101L);
        submitReviewRequest.setRating(5);
        submitReviewRequest.setReviewComment("Great");

        CustomerReviewSubmitResponse submitResponse = new CustomerReviewSubmitResponse(
                9L, 101L, 5, "Great", LocalDateTime.now(), true);
        CustomerReviewResponse customerReviewResponse = new CustomerReviewResponse(9L, 5, "Great", LocalDateTime.now());
        ReviewPageResponse<ReviewResponse> providerPage = new ReviewPageResponse<>(List.of(reviewResponse(3L)), 1, 1, 0);

        when(reviewManagementService.submitCustomerReview(submitReviewRequest)).thenReturn(submitResponse);
        when(reviewManagementService.getCustomerReviewByBookingId(101L)).thenReturn(customerReviewResponse);
        when(reviewManagementService.getProviderReviews(0, 10)).thenReturn(providerPage);

        assertThat(customerReviewController.submit(submitReviewRequest).getData()).isEqualTo(submitResponse);
        assertThat(customerReviewController.byBooking(101L).getData()).isEqualTo(customerReviewResponse);
        assertThat(providerReviewController.providerReviews(0, 10).getBody()).isSameAs(providerPage);
    }

    @Test
    void customerSubmitRequestValidationFailureReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/customer/reviews")
                        .contentType("application/json")
                        .content(new ObjectMapper().writeValueAsString(Map.of("rating", 6))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RATING"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void reviewExceptionHandlerMapsBusinessAndValidationExceptions() throws Exception {
        assertEquals(HttpStatus.NOT_FOUND,
                reviewExceptionHandler.handleNotFound(new ReviewNotFoundException(44L)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                reviewExceptionHandler.handleForbidden(new ReviewAccessDeniedException("denied")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                reviewExceptionHandler.handleValidation(new ReviewValidationException("invalid")).getStatusCode());

        Method method = DummyValidatedHandler.class.getDeclaredMethod("submit", SubmitReviewRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SubmitReviewRequest(), "request");
        bindingResult.addError(new FieldError("request", "rating", "rating is required"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        assertThat(reviewExceptionHandler.handleRequestValidation(exception).getBody())
                .containsEntry("message", "rating is required")
                .containsEntry("code", "INVALID_RATING");
    }

    private ReviewResponse reviewResponse(Long id) {
        ReviewResponse reviewResponse = new ReviewResponse();
        reviewResponse.setId(id);
        reviewResponse.setRating(5);
        reviewResponse.setReviewComment("solid");
        return reviewResponse;
    }

    static class DummyValidatedHandler {
        @SuppressWarnings("unused")
        public void submit(SubmitReviewRequest request) {
            ReflectionTestUtils.invokeMethod(this, "submit", request);
        }
    }
}
