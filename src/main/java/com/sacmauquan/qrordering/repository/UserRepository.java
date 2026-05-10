package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;

import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm kiếm định danh
    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    // Kiểm tra trùng lặp
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);
    
    // Kiểm tra tồn tại bao gồm cả bản ghi đã xóa (Native SQL để bypass SQLRestriction)
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE email = :email", nativeQuery = true)
    boolean existsByEmailIncludingDeleted(@Param("email") String email);

    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE phone = :phone", nativeQuery = true)
    boolean existsByPhoneIncludingDeleted(@Param("phone") String phone);

    // Tìm kiếm phân trang
    // Tìm theo Tên, Email hoặc Số điện thoại
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "u.phone LIKE CONCAT('%', :keyword, '%')")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            Pageable pageable);

    // 4. Lọc theo vai trò và trạng thái
    List<User> findByRoleAndStatus(User.Role role, User.UserStatus status);
}
