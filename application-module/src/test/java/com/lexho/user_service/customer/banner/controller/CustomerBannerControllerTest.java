package com.lexho.user_service.customer.banner.controller;

import com.lexho.user_service.TestWebSliceApp;
import com.lexho.user_service.config.JwtFilter;
import com.lexho.user_service.customer.banner.dto.CustomerBannerResponse;
import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import com.lexho.user_service.customer.banner.service.CustomerBannerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = TestWebSliceApp.class,
        properties = "spring.cloud.compatibility-verifier.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
@Import(CustomerBannerController.class)
class CustomerBannerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerBannerService customerBannerService;
    @MockBean
    private JwtFilter jwtFilter;

    @Test
    void shouldReturnBannersForCategory() throws Exception {
        CustomerBannerResponse response = CustomerBannerResponse.builder()
                .id(1L)
                .imageUrl("http://localhost:8082/images/banners/a.jpg")
                .redirectType(BannerRedirectType.SERVICE)
                .redirectId(12L)
                .redirectUrl(null)
                .build();

        when(customerBannerService.getBanners(eq(2L))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/customer/banners").param("categoryId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].imageUrl").value("http://localhost:8082/images/banners/a.jpg"))
                .andExpect(jsonPath("$.data[0].redirectType").value("SERVICE"))
                .andExpect(jsonPath("$.data[0].redirectId").value(12));
    }
}
