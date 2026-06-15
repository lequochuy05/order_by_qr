package com.qros.modules.menu.repository;

import com.qros.modules.menu.model.ItemOptionValue;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ItemOptionValueRepository extends JpaRepository<ItemOptionValue, Long> {

    List<ItemOptionValue> findByItemOptionIdOrderByDisplayOrderAscNameAsc(Long optionId);


}