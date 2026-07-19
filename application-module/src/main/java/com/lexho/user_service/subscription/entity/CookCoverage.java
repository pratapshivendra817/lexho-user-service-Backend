package com.lexho.user_service.subscription.entity;

import com.lexho.user_service.subscription.enums.FamilySize;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cook_coverage", indexes = {
        @Index(name = "idx_cook_coverage_active_sort", columnList = "is_active,sort_order"),
        @Index(name = "idx_cook_coverage_service_active_sort", columnList = "service_id,is_active,sort_order")
})
public class CookCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id")
    private Long serviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "family_size", nullable = false, length = 20)
    private FamilySize familySize;

    @Column(nullable = false, length = 30)
    private String members;

    @Column(name = "family_size_label", nullable = false, length = 50)
    private String familySizeLabel;

    @Column(length = 150)
    private String reference;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "coverage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<CookCoverageItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public FamilySize getFamilySize() { return familySize; }
    public void setFamilySize(FamilySize familySize) { this.familySize = familySize; }
    public String getMembers() { return members; }
    public void setMembers(String members) { this.members = members; }
    public String getFamilySizeLabel() { return familySizeLabel; }
    public void setFamilySizeLabel(String familySizeLabel) { this.familySizeLabel = familySizeLabel; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<CookCoverageItem> getItems() { return items; }
    public void setItems(List<CookCoverageItem> items) {
        this.items.clear();
        if (items != null) {
            items.forEach(this::addItem);
        }
    }
    public void addItem(CookCoverageItem item) {
        item.setCoverage(this);
        this.items.add(item);
    }
}
