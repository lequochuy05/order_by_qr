package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.User;
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

    /**
     * Checks if a user exists with the given email, excluding a specific ID (used during updates).
     * 
     * @param email The email to check
     * @param id The ID to exclude from search
     * @return true if another user already uses this email
     */
    boolean existsByEmailAndIdNot(String email, Long id);

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
     * Checks if an email is registered, including users that have been soft-deleted.
     * Bypasses SQLRestriction using native SQL.
     * 
     * @param email The email to check
     * @return true if email was ever registered
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE email = :email", nativeQuery = true)
    boolean existsByEmailIncludingDeleted(@Param("email") String email);

    /**
     * Checks if a phone number is registered, including users that have been soft-deleted.
     * Bypasses SQLRestriction using native SQL.
     * 
     * @param phone The phone number to check
     * @return true if phone number was ever registered
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE phone = :phone", nativeQuery = true)
    boolean existsByPhoneIncludingDeleted(@Param("phone") String phone);

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
