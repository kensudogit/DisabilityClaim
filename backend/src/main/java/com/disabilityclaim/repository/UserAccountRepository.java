package com.disabilityclaim.repository;

import com.disabilityclaim.domain.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsername(String username);

    @Query("SELECT u FROM UserAccount u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<UserAccount> findByUsernameWithRoles(@Param("username") String username);
}
