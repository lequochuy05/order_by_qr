package com.qros.modules.user.repository;

import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository - Repository interface for managing User entities.
 * Includes methods for authentication lookup, duplicate checks, and advanced
 * searching.
 */
public interface UserRepository extends JpaRepository<User, Long> {

        Optional<User> findByEmailIgnoreCase(String email);

        Optional<User> findByPhone(String phone);

        boolean existsByEmailIgnoreCase(String email);

        boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

        boolean existsByPhone(String phone);

        boolean existsByPhoneAndIdNot(String phone, Long id);

        @Query("SELECT u FROM User u WHERE " +
                        "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "u.phone LIKE CONCAT('%', :keyword, '%')")
        Page<User> searchUsers(
                        @Param("keyword") String keyword,
                        Pageable pageable);

        List<User> findByRoleAndStatus(UserRole role, UserStatus status);
}
