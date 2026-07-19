package com.lexho.user_service.cookfamilysize.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CookFamilySizeAdminResponse {
    private Long familySizeId;
    private String familySize;
    private String familySizeLabel;
    private Boolean active;
    private Integer displayOrder;
}
