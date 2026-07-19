package com.lexho.user_service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.testconfig.WebSliceBootTestConfig;
import com.lexho.user_service.admin.controller.AdminServiceableAreaController;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaPageResponse;
import com.lexho.user_service.admin.serviceablearea.dto.ServiceableAreaResponse;
import com.lexho.user_service.admin.serviceablearea.service.ServiceableAreaService;
import com.lexho.user_service.config.JwtFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = WebSliceBootTestConfig.class,
        properties = "spring.cloud.compatibility-verifier.enabled=false"
)
@AutoConfigureMockMvc(addFilters = false)
@Import(AdminServiceableAreaController.class)
class AdminServiceableAreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServiceableAreaService serviceableAreaService;

    @MockBean
    private JwtFilter jwtFilter;

    @Test
    void listShouldReturnPagedResponse() throws Exception {
        ServiceableAreaResponse area = new ServiceableAreaResponse();
        area.setId(1L);
        area.setName("Andheri");
        area.setCity("Mumbai");
        area.setState("Maharashtra");
        area.setPincode("400001");
        area.setActive(true);

        ServiceableAreaPageResponse pageResponse = new ServiceableAreaPageResponse();
        pageResponse.setContent(List.of(area));
        pageResponse.setPage(0);
        pageResponse.setSize(10);
        pageResponse.setTotalElements(1);
        pageResponse.setTotalPages(1);
        pageResponse.setFirst(true);
        pageResponse.setLast(true);

        when(serviceableAreaService.list(0, 10, "id", "desc")).thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/serviceable-areas")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Andheri"));
    }

    @Test
    void createShouldReturnCreatedArea() throws Exception {
        ServiceableAreaResponse response = new ServiceableAreaResponse();
        response.setId(11L);
        response.setName("Powai");
        response.setCity("Mumbai");
        response.setState("Maharashtra");
        response.setPincode("400076");
        response.setActive(true);

        when(serviceableAreaService.create(any())).thenReturn(response);

        String payload = """
                {
                  "name": "Powai",
                  "city": "Mumbai",
                  "state": "Maharashtra",
                  "pincode": "400076"
                }
                """;

        mockMvc.perform(post("/api/admin/serviceable-areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.name").value("Powai"));
    }

    @Test
    void updateShouldReturnUpdatedArea() throws Exception {
        ServiceableAreaResponse response = new ServiceableAreaResponse();
        response.setId(5L);
        response.setName("Borivali");
        response.setCity("Mumbai");
        response.setState("Maharashtra");
        response.setPincode("400092");
        response.setActive(true);

        when(serviceableAreaService.update(eq(5L), any())).thenReturn(response);

        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("name", "Borivali");
            put("city", "Mumbai");
            put("state", "Maharashtra");
            put("pincode", "400092");
        }});

        mockMvc.perform(put("/api/admin/serviceable-areas/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Borivali"));
    }

    @Test
    void deleteShouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(delete("/api/admin/serviceable-areas/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Serviceable area deleted"));
    }

    @Test
    void statusShouldReturnUpdatedState() throws Exception {
        ServiceableAreaResponse response = new ServiceableAreaResponse();
        response.setId(8L);
        response.setName("Nerul");
        response.setCity("Navi Mumbai");
        response.setState("Maharashtra");
        response.setPincode("400706");
        response.setActive(false);
        when(serviceableAreaService.updateStatus(eq(8L), any())).thenReturn(response);

        String payload = """
                { "active": false }
                """;

        mockMvc.perform(patch("/api/admin/serviceable-areas/8/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));
    }
}
