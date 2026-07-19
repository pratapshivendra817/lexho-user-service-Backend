package com.lexho.user_service.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CookFamilySizeResponse {
    private Long familySizeId;
    private String familySize;
    private String familySizeLabel;
}
