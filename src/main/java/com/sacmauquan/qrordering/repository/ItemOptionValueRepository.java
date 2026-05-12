package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.ItemOptionValue;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ItemOptionValueRepository extends JpaRepository<ItemOptionValue, Long> {

    List<ItemOptionValue> findByItemOptionId(Long optionId);

    @Modifying
    @Transactional
    @Query("UPDATE ItemOptionValue iov SET iov.isDeleted = true WHERE iov.itemOption.id = :optionId")
    void softDeleteByItemOptionId(Long optionId);
}
