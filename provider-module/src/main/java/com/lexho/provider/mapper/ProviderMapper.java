package com.lexho.provider.mapper;

import com.lexho.provider.dto.ProviderDetailsResponse;
import com.lexho.provider.dto.ProviderDocumentResponse;
import com.lexho.provider.dto.ProviderListItemResponse;
import com.lexho.provider.entity.Provider;
import com.lexho.provider.entity.ProviderDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProviderMapper {

    public ProviderListItemResponse toListItem(Provider provider) {
        ProviderListItemResponse response = new ProviderListItemResponse();
        response.setId(provider.getId());
        response.setProviderCode(provider.getProviderCode());
        response.setFullName(provider.getFullName());
        response.setEmail(provider.getEmail());
        response.setPhoneNumber(provider.getPhoneNumber());
        response.setCity(provider.getCity());
        response.setServiceType(provider.getServiceCategory().getName());
        response.setExperienceYears(provider.getExperienceYears());
        response.setKycStatus(provider.getKycStatus());
        response.setOnboardingStatus(provider.getOnboardingStatus());
        response.setCreatedAt(provider.getCreatedAt());
        return response;
    }

    public ProviderDetailsResponse toDetails(Provider provider, List<ProviderDocument> documents) {
        ProviderDetailsResponse response = new ProviderDetailsResponse();
        response.setId(provider.getId());
        response.setProviderCode(provider.getProviderCode());
        response.setFullName(provider.getFullName());
        response.setEmail(provider.getEmail());
        response.setPhoneNumber(provider.getPhoneNumber());
        response.setGender(provider.getGender());
        response.setDateOfBirth(provider.getDateOfBirth());
        response.setCurrentCity(provider.getCurrentCity());
        response.setCurrentState(provider.getCurrentState());
        response.setCurrentFullAddress(provider.getCurrentFullAddress());
        response.setCity(provider.getCity());
        response.setState(provider.getState());
        response.setFullAddress(provider.getFullAddress());
        response.setServiceCategoryId(provider.getServiceCategory().getId());
        response.setServiceCategoryName(provider.getServiceCategory().getName());
        response.setExperienceYears(provider.getExperienceYears());
        response.setServiceAreas(new ArrayList<>(provider.getServiceAreas()));
        response.setAccountNumberMasked(maskAccount(provider.getAccountNumber()));
        response.setIfscCode(provider.getIfscCode());
        response.setKycStatus(provider.getKycStatus());
        response.setKycFailedReason(provider.getKycFailedReason());
        response.setOnboardingStatus(provider.getOnboardingStatus());
        response.setRejectionReason(provider.getRejectionReason());
        response.setBlockedReason(provider.getBlockedReason());
        response.setDocuments(documents.stream().map(this::toDocument).toList());
        response.setCreatedAt(provider.getCreatedAt());
        response.setUpdatedAt(provider.getUpdatedAt());
        return response;
    }

    public ProviderDocumentResponse toDocument(ProviderDocument document) {
        ProviderDocumentResponse response = new ProviderDocumentResponse();
        response.setId(document.getId());
        response.setDocumentType(document.getDocumentType());
        response.setFileImage(document.getFileImage());
        response.setFileName(document.getFileName());
        response.setFileType(document.getFileType());
        response.setUploadedAt(document.getUploadedAt());
        return response;
    }

    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        String suffix = accountNumber.substring(accountNumber.length() - 4);
        return "XXXXXX" + suffix;
    }
}

