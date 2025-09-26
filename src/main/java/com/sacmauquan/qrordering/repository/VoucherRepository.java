package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);
    Optional<Voucher> findByCodeAndActiveTrue(String code);
}
