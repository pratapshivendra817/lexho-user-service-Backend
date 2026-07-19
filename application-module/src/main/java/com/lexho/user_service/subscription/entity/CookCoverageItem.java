package com.lexho.user_service.subscription.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cook_coverage_item", indexes = {
        @Index(name = "idx_cook_coverage_item_coverage_sort", columnList = "coverage_id,sort_order")
})
public class CookCoverageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coverage_id", nullable = false)
    private CookCoverage coverage;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 50)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    public Long getId() { return id; }
    public CookCoverage getCoverage() { return coverage; }
    public void setCoverage(CookCoverage coverage) { this.coverage = coverage; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
