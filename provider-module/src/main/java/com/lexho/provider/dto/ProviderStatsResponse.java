package com.lexho.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProviderStatsResponse {
    private long pending;
    private long approved;
    private long rejected;
    private long blocked;
}

