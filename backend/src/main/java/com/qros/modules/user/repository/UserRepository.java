package com.qros.modules.user.repository;

import com.qros.modules.user.model.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;

import java.util.*;

/**
 * UserRepository - Repository interface for managing User entities.
 * Includes methods for authentication lookup, duplicate checks, and advanced searching.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique email address.
     * 
     * @param email The target email
     * @return Optional containing the found user
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their unique phone number.
     * 
     * @param phone The target phone number
     * @return Optional containing the found user
     */
    Optional<User> findByPhone(String phone);

    /**
     * Checks if a user exists with the given email.
     * 
     * @param email The email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks if a user exists with the given email, excluding a specific ID (used during updates).
     * 
     * @param email The email to check
     * @param id The ID to exclude from search
     * @return true if another user already uses this email
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Checks if a user exists with the given phone number.
     * 
     * @param phone The phone number to check
     * @return true if exists, false otherwise
     */
    boolean existsByPhone(String phone);

    /**
     * Checks if a user exists with the given phone, excluding a specific ID (used during updates).
     * 
     * @param phone The phone number to check
     * @param id The ID to exclude from search
     * @return true if another user already uses this phone number
     */
    boolean existsByPhoneAndIdNot(String phone, Long id);
    /**
     * Searches for users across multiple fields (name, email, phone) with pagination support.
     * 
     * @param keyword The search term
     * @param pageable Pagination and sorting information
     * @return Paged result of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "u.phone LIKE CONCAT('%', :keyword, '%')")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * Filters users based on their security role and operational status.
     * 
     * @param role Target role
     * @param status Target status
     * @return List of matching users
     */
    List<User> findByRoleAndStatus(User.Role role, User.UserStatus status);
}
