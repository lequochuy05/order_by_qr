// src/main/java/com/sacmauquan/qrordering/repository/VoucherRepository.java
package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);
    Optional<Voucher> findByCodeIgnoreCase(String code);
    Optional<Voucher> findByCodeAndActiveTrue(String code);
    Optional<Voucher> findByCodeIgnoreCaseAndActiveTrue(String code);

    List<Voucher> findAllByOrderByIdDesc();
    boolean existsByCodeIgnoreCase(String code);
}
