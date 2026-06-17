package com.qros.modules.menu.repository;

import com.qros.modules.menu.model.ComboItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ComboItemRepository extends JpaRepository<ComboItem, Long> {

    @Modifying
    @Transactional
    @Query(
            """
        UPDATE ComboItem ci
        SET ci.isDeleted = true
        WHERE ci.combo.id = :comboId
        AND ci.isDeleted = false
    """)
    void softDeleteByComboId(@Param("comboId") Long comboId);

    @Query(
            """
        SELECT COUNT(ci) > 0
        FROM ComboItem ci
        WHERE ci.menuItem.id = :menuItemId
        AND ci.combo.active = true
        AND ci.isDeleted = false
        AND ci.menuItem.isDeleted = false
        AND ci.combo.isDeleted = false
    """)
    boolean existsInActiveCombo(@Param("menuItemId") Long menuItemId);
}
