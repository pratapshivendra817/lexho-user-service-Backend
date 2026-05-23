package com.lexho.user_service.customer.location.mapper;

import com.lexho.user_service.customer.location.dto.AddressResponse;
import com.lexho.user_service.customer.location.entity.CustomerAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerAddressMapper {
    @Mapping(target = "isDefault", source = "default")
    @Mapping(target = "latitude", expression = "java(address.getLatitude() == null ? null : address.getLatitude().doubleValue())")
    @Mapping(target = "longitude", expression = "java(address.getLongitude() == null ? null : address.getLongitude().doubleValue())")
    AddressResponse toResponse(CustomerAddress address);
}
