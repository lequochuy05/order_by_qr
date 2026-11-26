package com.qros.modules.menu.service;

import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.MenuItem;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * Port for querying and synchronizing stock-backed menu item and combo availability.
 */
public interface MenuInventoryAvailabilityPort {

    boolean hasSufficientInventoryForCombo(@NonNull Combo combo);

    Map<Long, Boolean> getMenuItemAvailability(List<Long> menuItemIds);

    Map<Long, Boolean> getComboAvailabilityByIds(List<Long> comboIds);

    void syncCombosForMenuItem(@NonNull MenuItem menuItem);

    void syncCombosForMenuItems(Collection<MenuItem> menuItems);

    boolean areAllComboItemsOrderable(Combo combo);
}
