package com.lexho.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public class IssueTokenRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String role;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof IssueTokenRequest that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role);
    }

    @Override
    public String toString() {
        return "IssueTokenRequest{"
                + "userId=" + userId
                + ", role='" + role + '\''
                + '}';
    }
}
