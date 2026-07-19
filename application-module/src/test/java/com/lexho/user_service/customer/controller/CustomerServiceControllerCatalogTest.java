package com.lexho.user_service.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.cartconfig.dto.CartConfigResponse;
import com.lexho.user_service.customer.dto.CustomerAddToCartRequest;
import com.lexho.user_service.customer.dto.CustomerCartItemResponse;
import com.lexho.user_service.customer.dto.CustomerCartResponse;
import com.lexho.user_service.customer.dto.CustomerPopularServiceResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogPageResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCardResponse;
import com.lexho.user_service.customer.dto.CustomerUpdateCartItemRequest;
import com.lexho.user_service.customer.service.CustomerCartService;
import com.lexho.user_service.customer.service.CustomerCartSlotService;
import com.lexho.user_service.customer.service.CustomerCatalogService;
import com.lexho.user_service.customer.service.CustomerPopularServiceService;
import com.lexho.user_service.customer.service.CustomerServiceCatalogService;
import com.lexho.user_service.exception.GlobalExceptionHandler;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerServiceControllerCatalogTest {

    private static final LocalValidatorFactoryBean VALIDATOR = validator();

    private final CustomerCatalogService customerCatalogService = mock(CustomerCatalogService.class);
    private final CustomerCartService customerCartService = mock(CustomerCartService.class);
    private final CustomerCartSlotService customerCartSlotService = mock(CustomerCartSlotService.class);
    private final CustomerPopularServiceService customerPopularServiceService = mock(CustomerPopularServiceService.class);
    private final CustomerServiceCatalogService customerServiceCatalogService = mock(CustomerServiceCatalogService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new CustomerServiceController(
                    customerCatalogService,
                    customerCartService,
                    customerCartSlotService,
                    customerPopularServiceService,
                    customerServiceCatalogService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(VALIDATOR)
            .build();

    @AfterAll
    static void closeValidator() {
        VALIDATOR.close();
    }

    @Test
    void catalogAndPopularServiceEndpointsReturnExpectedPayloads() throws Exception {
        CustomerServiceCardResponse card = new CustomerServiceCardResponse();
        card.setServiceId(1L);
        card.setServiceName("Cleaning");
        when(customerCatalogService.getServicesByCategory(5L, 0, 20)).thenReturn(List.of(card));

        CustomerPopularServiceResponse popular = new CustomerPopularServiceResponse();
        popular.setId(2L);
        popular.setName("Cook");
        when(customerPopularServiceService.getPopularServices(5L, 0, 20)).thenReturn(List.of(popular));
        when(customerPopularServiceService.getPopularServicesByCategoryKey("cleaning")).thenReturn(List.of(popular));
        when(customerPopularServiceService.getPopularCookServices()).thenReturn(List.of(popular));
        when(customerPopularServiceService.getPopularCleaningServices()).thenReturn(List.of(popular));

        CustomerServiceCatalogResponse detail = new CustomerServiceCatalogResponse();
        detail.setId(3L);
        detail.setServiceName("Deep Cleaning");
        CustomerServiceCatalogPageResponse page = new CustomerServiceCatalogPageResponse(List.of(detail), 1, 0, 20);
        when(customerServiceCatalogService.listServices(0, 20, "deep")).thenReturn(page);
        when(customerServiceCatalogService.getServiceById(3L)).thenReturn(detail);

        mockMvc.perform(get("/api/customer/services").param("categoryId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Services fetched"))
                .andExpect(jsonPath("$.data[0].serviceName").value("Cleaning"));

        mockMvc.perform(get("/api/customer/services/popular").param("categoryId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Cook"));

        mockMvc.perform(get("/api/customer/services/popular/cleaning"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/customer/services/popular/cook"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/customer/services/popular/cleaning"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customer/services/catalog").param("search", "deep"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/customer/services/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serviceName").value("Deep Cleaning"));
    }

    @Test
    void cartEndpointsDelegateAndValidationErrorsAreHandled() throws Exception {
        CustomerCartItemResponse item = CustomerCartItemResponse.builder().itemId(7L).serviceId(8L).quantity(1).build();
        CustomerCartResponse cart = CustomerCartResponse.builder().cartId(10L).items(List.of(item)).build();
        CartConfigResponse config = new CartConfigResponse();
        config.setMaxCartItems(5);

        when(customerCartService.addToCart(eq(77L), any())).thenReturn(item);
        when(customerCartService.getCart(77L)).thenReturn(cart);
        when(customerCartService.config()).thenReturn(config);
        when(customerCartService.cookFamilySizes()).thenReturn(List.of());
        when(customerCartService.updateQuantity(eq(77L), any())).thenReturn(cart);
        when(customerCartSlotService.slotsForDate(LocalDate.of(2026, 6, 19))).thenReturn(Map.of("available", true));

        CustomerAddToCartRequest addRequest = new CustomerAddToCartRequest();
        addRequest.setServiceId(8L);
        addRequest.setQuantity(1);

        mockMvc.perform(post("/api/customer/cart")
                        .requestAttr("userId", 77L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId").value(7));

        mockMvc.perform(get("/api/customer/cart").requestAttr("userId", 77L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cartId").value(10));

        mockMvc.perform(get("/api/customer/cart/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxCartItems").value(5));

        mockMvc.perform(get("/api/customer/cook-family-sizes"))
                .andExpect(status().isOk());

        CustomerUpdateCartItemRequest updateRequest = new CustomerUpdateCartItemRequest();
        updateRequest.setItemId(7L);
        updateRequest.setQuantity(2);
        mockMvc.perform(put("/api/customer/cart/item")
                        .requestAttr("userId", 77L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart item updated"));

        mockMvc.perform(delete("/api/customer/cart/item/7").requestAttr("userId", 77L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart item removed"));
        verify(customerCartService).removeItem(77L, 7L);

        mockMvc.perform(delete("/api/customer/cart/clear").requestAttr("userId", 77L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared"));
        verify(customerCartService).clearCart(77L);

        mockMvc.perform(patch("/api/customer/cart/meta")
                        .requestAttr("userId", 77L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"x\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customer/cart/slots").param("date", "2026-06-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));

        mockMvc.perform(post("/api/customer/cart")
                        .requestAttr("userId", 77L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":8,\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resourceNotFoundFromCatalogIsMappedTo404() throws Exception {
        when(customerServiceCatalogService.getServiceById(99L))
                .thenThrow(new ResourceNotFoundException("Service detail not found"));

        mockMvc.perform(get("/api/customer/services/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Service detail not found"));
    }

    private static LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }
}
