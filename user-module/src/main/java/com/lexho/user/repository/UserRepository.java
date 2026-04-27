package com.lexho.user.repository;

import com.lexho.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Query("SELECT u FROM User u WHERE u.phoneNumber = :mobile AND u.deleted = false")
    Optional<User> findByMobile(@Param("mobile") String mobile);

    Optional<User> findByIdAndDeletedFalse(Long id);

    boolean existsByPhoneNumberAndDeletedFalse(String phoneNumber);

    boolean existsByPhoneNumberAndDeletedFalseAndIdNot(String phoneNumber, Long id);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByEmailIgnoreCaseAndDeletedFalseAndIdNot(String email, Long id);
}
