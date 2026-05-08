package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.ItemOptionValue;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ItemOptionValueRepository extends JpaRepository<ItemOptionValue, Long> {

    // Lấy danh sách các giá trị thuộc về một Option nào đó
    List<ItemOptionValue> findByItemOptionId(Long optionId);

    // Xóa mềm toàn bộ các giá trị khi Option cha bị xóa
    @Modifying
    @Transactional
    @Query("UPDATE ItemOptionValue iov SET iov.isDeleted = true WHERE iov.itemOption.id = :optionId")
    void softDeleteByItemOptionId(Long optionId);
}
