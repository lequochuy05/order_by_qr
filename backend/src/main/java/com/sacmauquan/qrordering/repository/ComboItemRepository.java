package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.ComboItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ComboItemRepository extends JpaRepository<ComboItem, Long> {
    @Modifying
    @Transactional
    @Query("UPDATE ComboItem ci SET ci.isDeleted = true WHERE ci.combo.id = :comboId")
    void softDeleteByComboId(Long comboId);

    @Query("SELECT ci FROM ComboItem ci JOIN FETCH ci.menuItem WHERE ci.combo.id = :comboId")
    List<ComboItem> findByComboIdWithMenuItem(Long comboId);
}
