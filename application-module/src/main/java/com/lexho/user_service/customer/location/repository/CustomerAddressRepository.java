package com.lexho.user_service.customer.location.repository;

import com.lexho.user_service.customer.location.entity.CustomerAddress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {
    Optional<CustomerAddress> findByIdAndCustomerIdAndIsDeletedFalse(UUID id, Long customerId);
    boolean existsByCustomerIdAndIsDeletedFalse(Long customerId);
    Optional<CustomerAddress> findFirstByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtAsc(Long customerId);

    @Query("""
            select a from CustomerAddress a
            where a.customerId=:customerId and a.isDeleted=false
            and (:search is null or :search='' or
                lower(coalesce(a.city,'')) like lower(concat('%', :search, '%')) or
                lower(coalesce(a.sector,'')) like lower(concat('%', :search, '%')) or
                lower(coalesce(a.landmark,'')) like lower(concat('%', :search, '%')) or
                lower(coalesce(a.pincode,'')) like lower(concat('%', :search, '%')) or
                lower(coalesce(a.buildingName,'')) like lower(concat('%', :search, '%')))
            order by a.isDefault desc, a.updatedAt desc
            """)
    Page<CustomerAddress> search(Long customerId, String search, Pageable pageable);

    @Modifying
    @Query("update CustomerAddress a set a.isDefault=false where a.customerId=:customerId and a.isDeleted=false")
    void clearDefault(Long customerId);

    List<CustomerAddress> findAllByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescUpdatedAtDesc(Long customerId);
}
