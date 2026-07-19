package com.lexho.user_service.lexhopartner.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "app.partner.settlement")
public class PartnerSettlementProperties {

    private BigDecimal commissionPercent = new BigDecimal("15.00");
    private BigDecimal minWithdrawalAmount = new BigDecimal("100.00");

    public BigDecimal getCommissionPercent() {
        return commissionPercent;
    }

    public void setCommissionPercent(BigDecimal commissionPercent) {
        this.commissionPercent = commissionPercent;
    }

    public BigDecimal getMinWithdrawalAmount() {
        return minWithdrawalAmount;
    }

    public void setMinWithdrawalAmount(BigDecimal minWithdrawalAmount) {
        this.minWithdrawalAmount = minWithdrawalAmount;
    }
}
