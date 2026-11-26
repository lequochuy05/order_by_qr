package com.qros.modules.menu.service;

import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.ItemOptionValueRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Module-boundary lookup facade for catalog entities consumed by other modules.
 */
@Service
@RequiredArgsConstructor
public class MenuCatalogLookupService {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final ComboRepository comboRepository;
    private final ItemOptionValueRepository itemOptionValueRepository;

    public List<Category> findActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    public Optional<MenuItem> findMenuItemById(@NonNull Long id) {
        return menuItemRepository.findById(id);
    }

    public boolean menuItemExists(@NonNull Long id) {
        return menuItemRepository.existsById(id);
    }

    public List<MenuItem> findMenuItemsByIds(Collection<Long> ids) {
        return menuItemRepository.findAllByIdIn(ids);
    }

    public List<MenuItem> findAllPublicAvailableMenuItems() {
        return menuItemRepository.findAllPublicAvailableItems();
    }

    public List<MenuItem> findAllActiveMenuItems() {
        return menuItemRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc();
    }

    public List<MenuItem> findAvailableMenuItemsNewest(Pageable pageable) {
        return menuItemRepository.findByActiveTrueAndAvailableTrueOrderByIdDesc(pageable);
    }

    public List<MenuItem> findSimilarAvailableMenuItems(Long categoryId, Long excludedItemId, Pageable pageable) {
        return menuItemRepository.findSimilarAvailableItems(categoryId, excludedItemId, pageable);
    }

    public List<MenuItem> saveAllMenuItems(@NonNull Iterable<MenuItem> menuItems) {
        return menuItemRepository.saveAll(menuItems);
    }

    public List<Combo> findAllActiveCombosWithItems() {
        return comboRepository.findAllActiveWithItems();
    }

    public List<Combo> findCombosByIdsWithItems(List<Long> ids) {
        return comboRepository.findAllByIdInWithItems(ids);
    }

    public List<Combo> findCombosByMenuItemIdsWithItems(Collection<Long> menuItemIds) {
        return comboRepository.findAllByMenuItemIdsWithItems(menuItemIds);
    }

    public List<Combo> saveAllCombos(@NonNull Iterable<Combo> combos) {
        return comboRepository.saveAll(combos);
    }

    public List<ItemOptionValue> findItemOptionValuesByIds(Collection<Long> ids) {
        return itemOptionValueRepository.findAllByIdIn(ids);
    }

    public List<ItemOptionValue> findItemOptionValuesByIterableIds(@NonNull Iterable<Long> ids) {
        return itemOptionValueRepository.findAllById(ids);
    }
}
