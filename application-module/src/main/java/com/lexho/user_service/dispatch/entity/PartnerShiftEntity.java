package com.lexho.user_service.dispatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_shifts")
public class PartnerShiftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false, unique = true)
    private Long partnerId;

    @Column(name = "is_online", nullable = false)
    private boolean online;

    @Column(name = "shift_start")
    private LocalDateTime shiftStart;

    @Column(name = "shift_end")
    private LocalDateTime shiftEnd;

    @Column(name = "current_pincode", length = 20)
    private String currentPincode;

    @Column(name = "active_jobs_count", nullable = false)
    private int activeJobsCount;

    @Column(name = "max_active_jobs", nullable = false)
    private int maxActiveJobs = 3;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public LocalDateTime getShiftStart() {
        return shiftStart;
    }

    public void setShiftStart(LocalDateTime shiftStart) {
        this.shiftStart = shiftStart;
    }

    public LocalDateTime getShiftEnd() {
        return shiftEnd;
    }

    public void setShiftEnd(LocalDateTime shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    public String getCurrentPincode() {
        return currentPincode;
    }

    public void setCurrentPincode(String currentPincode) {
        this.currentPincode = currentPincode;
    }

    public int getActiveJobsCount() {
        return activeJobsCount;
    }

    public void setActiveJobsCount(int activeJobsCount) {
        this.activeJobsCount = activeJobsCount;
    }

    public int getMaxActiveJobs() {
        return maxActiveJobs;
    }

    public void setMaxActiveJobs(int maxActiveJobs) {
        this.maxActiveJobs = maxActiveJobs;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

