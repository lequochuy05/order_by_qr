package com.qros.modules.menu.repository;

import com.qros.modules.menu.model.ItemOptionValue;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemOptionValueRepository extends JpaRepository<ItemOptionValue, Long> {

    List<ItemOptionValue> findByItemOptionIdOrderByDisplayOrderAscNameAsc(Long optionId);

    @EntityGraph(attributePaths = "itemOption")
    List<ItemOptionValue> findAllByIdIn(@Param("ids") java.util.Collection<Long> ids);
}
