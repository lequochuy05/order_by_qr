package com.sacmauquan.qrordering.repository;

import java.util.List;

import com.sacmauquan.qrordering.model.Combo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<Combo> findByActiveTrue();
}
