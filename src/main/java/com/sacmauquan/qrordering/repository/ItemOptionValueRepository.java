package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.ItemOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemOptionValueRepository extends JpaRepository<ItemOptionValue, Long> {
}
