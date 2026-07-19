package com.lexho.provider.controller;

import com.lexho.provider.dto.ProviderDetailsResponse;
import com.lexho.provider.dto.ProviderDocumentResponse;
import com.lexho.provider.dto.ProviderDocumentUploadResponse;
import com.lexho.provider.entity.ProviderDocument;
import com.lexho.provider.enums.ProviderDocumentType;
import com.lexho.provider.exception.ProviderExceptionHandler;
import com.lexho.provider.mapper.ProviderMapper;
import com.lexho.provider.service.ProviderOnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProviderOnboardingControllerTest {

    @Mock
    private ProviderOnboardingService service;
    @Mock
    private ProviderMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProviderOnboardingController(service, mapper))
                .setControllerAdvice(new ProviderExceptionHandler())
                .build();
    }

    @Test
    void onboardingAcceptsValidRequest() throws Exception {
        ProviderDetailsResponse response = new ProviderDetailsResponse();
        response.setId(10L);
        when(service.submitOnboarding(any())).thenReturn(response);

        String json = """
                {"fullName":"Asha","email":"asha@example.com","phoneNumber":"9876543210",
                 "city":"Delhi","state":"Delhi","fullAddress":"Sector 1","serviceCategoryId":1,
                 "serviceAreas":["Delhi"],"accountNumber":"12345678","ifscCode":"HDFC0001234"}
                """;
        mockMvc.perform(post("/api/partner/onboarding").contentType("application/json").content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void onboardingRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/partner/onboarding").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onboardingRejectsEmailPatternAndBoundaryValidationErrors() throws Exception {
        String json = """
                {"fullName":"Asha","email":"not-an-email","phoneNumber":"123",
                 "gender":"this-value-is-longer-than-twenty-characters",
                 "city":"Delhi","state":"Delhi","fullAddress":"Sector 1","serviceCategoryId":1,
                 "experienceYears":61,"serviceAreas":["Delhi"],"accountNumber":"12","ifscCode":"bad"}
                """;

        mockMvc.perform(post("/api/partner/onboarding").contentType("application/json").content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void onboardingRejectsBlankNestedServiceAreaAndEmptyBody() throws Exception {
        String blankServiceArea = """
                {"fullName":"Asha","email":"asha@example.com","phoneNumber":"9876543210",
                 "city":"Delhi","state":"Delhi","fullAddress":"Sector 1","serviceCategoryId":1,
                 "serviceAreas":[" "],"accountNumber":"12345678","ifscCode":"HDFC0001234"}
                """;

        mockMvc.perform(post("/api/partner/onboarding").contentType("application/json").content(blankServiceArea))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Service area cannot be blank"));

        mockMvc.perform(post("/api/partner/onboarding").contentType("application/json").content(""))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    @Test
    void uploadDocumentRejectsInvalidEnumBeforeServiceInvocation() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "pan.pdf", "application/pdf", "pdf".getBytes());

        mockMvc.perform(multipart("/api/partner/documents/upload").file(file)
                        .param("providerId", "5").param("documentType", "UNKNOWN"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }

    @Test
    void uploadDocumentDelegatesMultipartData() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "pan.pdf", "application/pdf", "pdf".getBytes());
        when(service.uploadDocument(5L, ProviderDocumentType.PAN, file))
                .thenReturn(new ProviderDocumentUploadResponse(8L, 5L, ProviderDocumentType.PAN, "/pan", "pan.pdf"));

        mockMvc.perform(multipart("/api/partner/documents/upload").file(file)
                        .param("providerId", "5").param("documentType", "PAN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.documentId").value(8));
    }

    @Test
    void getDocumentMapsEntityMetadata() throws Exception {
        ProviderDocument document = new ProviderDocument();
        ProviderDocumentResponse response = new ProviderDocumentResponse();
        response.setId(9L);
        when(service.getDocument(9L)).thenReturn(document);
        when(mapper.toDocument(document)).thenReturn(response);

        mockMvc.perform(get("/api/partner/documents/9"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(9));
        verify(mapper).toDocument(document);
    }
}
