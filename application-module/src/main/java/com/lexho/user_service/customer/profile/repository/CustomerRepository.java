package com.lexho.user_service.customer.profile.repository;

import com.lexho.user_service.customer.profile.entity.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            select c from CustomerEntity c
            where (:search is null
               or lower(c.fullName) like lower(concat('%', :search, '%'))
               or lower(c.email) like lower(concat('%', :search, '%'))
               or c.phoneNumber like concat('%', :search, '%')
               or c.mobile like concat('%', :search, '%'))
              and (:active is null or c.active = :active)
            """)
    Page<CustomerEntity> search(@Param("search") String search,
                                @Param("active") Boolean active,
                                Pageable pageable);
}
