package com.lexho.provider.controller;

import com.lexho.provider.dto.ProviderActionResponse;
import com.lexho.provider.dto.ProviderDetailsResponse;
import com.lexho.provider.dto.ProviderDocumentResponse;
import com.lexho.provider.dto.ProviderPageResponse;
import com.lexho.provider.dto.ProviderStatsResponse;
import com.lexho.provider.entity.Provider;
import com.lexho.provider.entity.ProviderDocument;
import com.lexho.provider.enums.OnboardingStatus;
import com.lexho.provider.exception.ProviderExceptionHandler;
import com.lexho.provider.service.ProviderOnboardingService;
import com.lexho.provider.service.storage.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminProviderOnboardingControllerTest {

    @Mock
    private ProviderOnboardingService service;
    @Mock
    private DocumentStorageService storageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminProviderOnboardingController(service, storageService))
                .setControllerAdvice(new ProviderExceptionHandler())
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void statsAndListReturnServiceResults() throws Exception {
        when(service.getStats()).thenReturn(new ProviderStatsResponse(1, 2, 3, 4));
        when(service.getProviders(1, 20, "fullName", "asc", "PENDING", "Cook", "Delhi", "Asha"))
                .thenReturn(new ProviderPageResponse<>(List.of(), 0, 0, 1));

        mockMvc.perform(get("/api/admin/partners/stats"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.approved").value(2));
        mockMvc.perform(get("/api/admin/partners").param("page", "1").param("size", "20")
                        .param("sortBy", "fullName").param("sortDir", "asc").param("status", "PENDING")
                        .param("serviceType", "Cook").param("city", "Delhi").param("search", "Asha"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pageNumber").value(1));
    }

    @Test
    void detailsEnrichesDocumentUrlsAndHandlesEmptyDocuments() throws Exception {
        ProviderDocumentResponse document = new ProviderDocumentResponse();
        document.setId(8L);
        ProviderDetailsResponse response = new ProviderDetailsResponse();
        response.setId(7L);
        response.setDocuments(List.of(document));
        when(service.getProviderDetails(7L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/partners/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].previewUrl").value("http://localhost/api/admin/partners/7/documents/8/preview"))
                .andExpect(jsonPath("$.documents[0].downloadUrl").value("http://localhost/api/admin/partners/7/documents/8/download"));

        response.setDocuments(List.of());
        mockMvc.perform(get("/api/admin/partners/7")).andExpect(status().isOk());
    }

    @Test
    void bankDetailsReturnsSensitiveDataForAuthenticatedAndAnonymousActors() throws Exception {
        Provider provider = provider(5L);
        when(service.getProviderForAdmin(5L)).thenReturn(provider);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", "n/a"));

        mockMvc.perform(get("/api/admin/partners/5/bank-details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("12345678"));
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/admin/partners/5/bank-details")).andExpect(status().isOk());
    }

    @Test
    void directLifecycleEndpointsDelegateToService() throws Exception {
        when(service.approveProvider(1L)).thenReturn(action(1L, OnboardingStatus.APPROVED));
        when(service.rejectProvider(2L, "bad docs")).thenReturn(action(2L, OnboardingStatus.REJECTED));
        when(service.blockProvider(3L, "risk")).thenReturn(action(3L, OnboardingStatus.BLOCKED));
        when(service.unblockProvider(4L)).thenReturn(action(4L, OnboardingStatus.APPROVED));

        mockMvc.perform(put("/api/admin/partners/1/approve")).andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/partners/2/reject").contentType("application/json")
                .content("{\"reason\":\"bad docs\"}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/partners/3/block").contentType("application/json")
                .content("{\"reason\":\"risk\"}")).andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/partners/4/unblock")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/partners/4")).andExpect(status().isNoContent());

        verify(service).softDeleteProvider(4L);
    }

    @Test
    void verificationCompatibilityEndpointCoversEveryStatusAndDefaultReason() throws Exception {
        when(service.approveProvider(1L)).thenReturn(action(1L, OnboardingStatus.APPROVED));
        when(service.rejectProvider(2L, "Rejected by admin")).thenReturn(action(2L, OnboardingStatus.REJECTED));
        when(service.blockProvider(3L, "custom block")).thenReturn(action(3L, OnboardingStatus.BLOCKED));
        when(service.unblockProvider(4L)).thenReturn(action(4L, OnboardingStatus.APPROVED));

        mockMvc.perform(patch("/api/admin/partners/1/verification").contentType("application/json")
                .content("{\"status\":\" verified \"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/partners/2/verification").contentType("application/json")
                .content("{\"status\":\"REJECTED\",\"reason\":\" \"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/partners/3/verification").contentType("application/json")
                .content("{\"status\":\"BLOCKED\",\"reason\":\" custom block \"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/partners/4/verification").contentType("application/json")
                .content("{\"status\":\"UNBLOCKED\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/admin/partners/5/verification").contentType("application/json")
                .content("{\"status\":\"UNKNOWN\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void reasonValidationRejectsBlankPayload() throws Exception {
        mockMvc.perform(put("/api/admin/partners/2/reject").contentType("application/json")
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Reason is required"));
    }

    @Test
    void previewAndDownloadSetDispositionAndMediaTypeFallback() throws Exception {
        ProviderDocument document = new ProviderDocument();
        document.setFileImage("stored/doc.pdf");
        document.setFileName("doc.pdf");
        document.setFileType("application/pdf");
        when(service.getProviderDocument(1L, 2L)).thenReturn(document);
        when(storageService.load("stored/doc.pdf")).thenReturn(new ByteArrayResource("pdf".getBytes()));

        mockMvc.perform(get("/api/admin/partners/1/documents/2/preview"))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"doc.pdf\""));
        document.setFileType("not-a-media-type");
        mockMvc.perform(get("/api/admin/partners/1/documents/2/download"))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"doc.pdf\""));
    }

    private ProviderActionResponse action(Long id, OnboardingStatus status) {
        return new ProviderActionResponse(id, "PRV" + id, status, "done");
    }

    private Provider provider(Long id) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setProviderCode("PRV" + id);
        provider.setAccountNumber("12345678");
        provider.setIfscCode("HDFC0001234");
        return provider;
    }
}
