package com.lexho.user_service.customer;

import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.admin.dto.PagedDataResponse;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerResponse;
import com.lexho.user_service.customer.banner.dto.AdminCustomerBannerUpsertRequest;
import com.lexho.user_service.customer.banner.dto.CustomerBannerResponse;
import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import com.lexho.user_service.customer.banner.entity.CustomerBannerEntity;
import com.lexho.user_service.customer.banner.repository.CustomerBannerRepository;
import com.lexho.user_service.customer.banner.service.AdminCustomerBannerService;
import com.lexho.user_service.customer.dto.CookFamilySizeResponse;
import com.lexho.user_service.customer.entity.CustomerCartItemEntity;
import com.lexho.user_service.customer.location.dto.AddressResponse;
import com.lexho.user_service.customer.location.entity.AddressType;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import com.lexho.user_service.customer.location.mapper.CustomerAddressMapper;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.util.MediaUrlResolver;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerBannerAndMapperSupportTest {

    @Test
    void adminCustomerBannerServiceCoversCrudListAndValidationBranches() {
        CustomerBannerRepository repository = mock(CustomerBannerRepository.class);
        MediaUrlResolver mediaUrlResolver = mock(MediaUrlResolver.class);
        AdminCustomerBannerService service = new AdminCustomerBannerService(repository, mediaUrlResolver);

        AdminCustomerBannerUpsertRequest internalRequest = request();
        when(mediaUrlResolver.toStorablePath("/banner.png")).thenReturn("stored/banner.png");
        when(mediaUrlResolver.toPublicUrl("stored/banner.png")).thenReturn("https://cdn/banner.png");
        when(repository.save(any(CustomerBannerEntity.class))).thenAnswer(invocation -> {
            CustomerBannerEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(9L);
            }
            entity.prePersist();
            return entity;
        });

        DataResponse<AdminCustomerBannerResponse> created = service.create(internalRequest);
        assertThat(created.getData().getImageUrl()).isEqualTo("https://cdn/banner.png");

        CustomerBannerEntity existing = new CustomerBannerEntity();
        existing.setId(5L);
        existing.setTitle("old");
        existing.setImageUrl("stored/banner.png");
        existing.setRedirectType(BannerRedirectType.CATEGORY);
        existing.setPriority(2);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));

        DataResponse<AdminCustomerBannerResponse> updated = service.update(5L, internalRequest);
        assertThat(updated.getData().getTitle()).isEqualTo("Customer banner");
        assertThat(service.byId(5L).getData().getId()).isEqualTo(5L);

        when(repository.searchForAdmin(any(), any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(existing)));
        PagedDataResponse<AdminCustomerBannerResponse> listed = service.list(0, 500, null, true);
        assertThat(listed.getData()).hasSize(1);
        assertThat(listed.getTotal()).isEqualTo(1);

        DataResponse<AdminCustomerBannerResponse> statusUpdated = service.updateStatus(5L, false);
        assertThat(statusUpdated.getData().isActive()).isFalse();

        AdminMessageResponse deleted = service.delete(5L);
        assertThat(deleted.getMessage()).isEqualTo("Banner deleted successfully");
        verify(repository).delete(existing);

        AdminCustomerBannerUpsertRequest external = request();
        external.setRedirectType(BannerRedirectType.EXTERNAL);
        external.setRedirectId(null);
        external.setRedirectValue(null);
        external.setRedirectUrl(" https://example.com ");
        service.create(external);

        AdminCustomerBannerUpsertRequest withRedirectValue = request();
        withRedirectValue.setRedirectId(null);
        withRedirectValue.setRedirectValue("77");
        service.create(withRedirectValue);

        AdminCustomerBannerUpsertRequest badNumeric = request();
        badNumeric.setRedirectId(null);
        badNumeric.setRedirectValue("abc");
        assertThrows(BadRequestException.class, () -> service.create(badNumeric));

        AdminCustomerBannerUpsertRequest missingImage = request();
        missingImage.setImageUrl(" ");
        missingImage.setBannerImageUrl(" ");
        assertThrows(BadRequestException.class, () -> service.create(missingImage));

        AdminCustomerBannerUpsertRequest missingRedirectUrl = request();
        missingRedirectUrl.setRedirectType(BannerRedirectType.EXTERNAL);
        missingRedirectUrl.setRedirectId(null);
        missingRedirectUrl.setRedirectValue(null);
        missingRedirectUrl.setRedirectUrl(" ");
        assertThrows(BadRequestException.class, () -> service.create(missingRedirectUrl));

        when(repository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.byId(404L));
    }

    @Test
    void bannerDtosEntitiesMapperAndSmallCustomerModelsAreCovered() {
        CustomerBannerEntity entity = new CustomerBannerEntity();
        entity.setCategoryId(2L);
        entity.setTitle("Banner");
        entity.setImageUrl("img");
        entity.setRedirectType(BannerRedirectType.CATEGORY);
        entity.prePersist();
        LocalDateTime createdAt = entity.getCreatedAt();
        assertThat(entity.getPriority()).isEqualTo(100);
        entity.preUpdate();
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(createdAt);

        AdminCustomerBannerResponse adminResponse = AdminCustomerBannerResponse.builder()
                .id(1L).categoryId(2L).title("Banner").imageUrl("img")
                .redirectType(BannerRedirectType.CATEGORY).redirectId(3L).redirectUrl("url")
                .active(true).priority(4).createdAt(createdAt).updatedAt(createdAt).build();
        CustomerBannerResponse customerBannerResponse = CustomerBannerResponse.builder()
                .id(1L).imageUrl("img").redirectType(BannerRedirectType.EXTERNAL).redirectUrl("url").redirectId(4L).build();
        assertThat(adminResponse.getTitle()).isEqualTo("Banner");
        assertThat(customerBannerResponse.getRedirectType()).isEqualTo(BannerRedirectType.EXTERNAL);

        AdminCustomerBannerUpsertRequest upsertRequest = request();
        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(upsertRequest)).isEmpty();
        upsertRequest.setTitle("");
        assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(upsertRequest))
                .extracting(v -> v.getPropertyPath().toString()).contains("title");

        CustomerAddress address = new CustomerAddress();
        address.setType(AddressType.HOME);
        address.setAddressLine("Street");
        address.setCity("Delhi");
        address.setState("Delhi");
        address.setCountry("India");
        address.setPincode("110001");
        address.setLatitude(BigDecimal.valueOf(12.3));
        address.setLongitude(BigDecimal.valueOf(45.6));
        address.setDefault(true);
        CustomerAddressMapper mapper = Mappers.getMapper(CustomerAddressMapper.class);
        AddressResponse mapped = mapper.toResponse(address);
        assertThat(mapped.latitude).isEqualTo(12.3);
        assertThat(mapped.longitude).isEqualTo(45.6);
        assertThat(mapped.isDefault).isTrue();

        CookFamilySizeResponse familySizeResponse = new CookFamilySizeResponse(1L, "SMALL", "Small");
        assertThat(familySizeResponse.getFamilySizeId()).isEqualTo(1L);
        familySizeResponse.setFamilySize("LARGE");
        assertThat(familySizeResponse.getFamilySize()).isEqualTo("LARGE");

        CustomerCartItemEntity cartItemEntity = new CustomerCartItemEntity();
        cartItemEntity.setId(8L);
        cartItemEntity.setUserId(9L);
        cartItemEntity.setServiceId(10L);
        cartItemEntity.setFamilySizeId(2L);
        cartItemEntity.setFamilySize("MEDIUM");
        cartItemEntity.setFamilySizeLabel("Medium");
        cartItemEntity.setQuantity(3);
        assertThat(cartItemEntity.getAddonSignature()).isEqualTo("NONE");
        assertThat(cartItemEntity.getQuantity()).isEqualTo(3);
    }

    private AdminCustomerBannerUpsertRequest request() {
        AdminCustomerBannerUpsertRequest request = new AdminCustomerBannerUpsertRequest();
        request.setCategoryId(2L);
        request.setTitle("Customer banner");
        request.setImageUrl("/banner.png");
        request.setRedirectType(BannerRedirectType.CATEGORY);
        request.setRedirectId(5L);
        request.setActive(true);
        request.setPriority(1);
        return request;
    }
}
