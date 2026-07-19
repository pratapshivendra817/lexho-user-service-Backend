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

    public ProviderListItemResponse toListItem(Provider partner) {
        ProviderListItemResponse response = new ProviderListItemResponse();
        response.setId(partner.getId());
        response.setProviderCode(partner.getProviderCode());
        response.setFullName(partner.getFullName());
        response.setEmail(partner.getEmail());
        response.setPhoneNumber(partner.getPhoneNumber());
        response.setCity(partner.getCity());
        response.setServiceType(partner.getServiceCategory().getName());
        response.setExperienceYears(partner.getExperienceYears());
        response.setKycStatus(partner.getKycStatus());
        response.setOnboardingStatus(partner.getOnboardingStatus());
        response.setCreatedAt(partner.getCreatedAt());
        return response;
    }

    public ProviderDetailsResponse toDetails(Provider partner, List<ProviderDocument> documents) {
        ProviderDetailsResponse response = new ProviderDetailsResponse();
        response.setId(partner.getId());
        response.setProviderCode(partner.getProviderCode());
        response.setFullName(partner.getFullName());
        response.setEmail(partner.getEmail());
        response.setPhoneNumber(partner.getPhoneNumber());
        response.setGender(partner.getGender());
        response.setDateOfBirth(partner.getDateOfBirth());
        response.setCurrentCity(partner.getCurrentCity());
        response.setCurrentState(partner.getCurrentState());
        response.setCurrentFullAddress(partner.getCurrentFullAddress());
        response.setCity(partner.getCity());
        response.setState(partner.getState());
        response.setFullAddress(partner.getFullAddress());
        response.setServiceCategoryId(partner.getServiceCategory().getId());
        response.setServiceCategoryName(partner.getServiceCategory().getName());
        response.setExperienceYears(partner.getExperienceYears());
        response.setServiceAreas(new ArrayList<>(partner.getServiceAreas()));
        response.setAccountNumberMasked(maskAccount(partner.getAccountNumber()));
        response.setIfscCode(partner.getIfscCode());
        response.setKycStatus(partner.getKycStatus());
        response.setKycFailedReason(partner.getKycFailedReason());
        response.setOnboardingStatus(partner.getOnboardingStatus());
        response.setRejectionReason(partner.getRejectionReason());
        response.setBlockedReason(partner.getBlockedReason());
        response.setOnline(partner.isOnline());
        response.setDocuments(documents.stream().map(this::toDocument).toList());
        response.setCreatedAt(partner.getCreatedAt());
        response.setUpdatedAt(partner.getUpdatedAt());
        return response;
    }

    public ProviderDocumentResponse toDocument(ProviderDocument document) {
        ProviderDocumentResponse response = new ProviderDocumentResponse();
        response.setId(document.getId());
        response.setDocumentType(document.getDocumentType());
        response.setFileImage(document.getFileImage());
        response.setFileName(document.getFileName());
        response.setFileType(document.getFileType());
        response.setContentType(document.getFileType());
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
