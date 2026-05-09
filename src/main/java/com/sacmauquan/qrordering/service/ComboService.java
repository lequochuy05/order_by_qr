package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.ComboItemRequest;
import com.sacmauquan.qrordering.dto.ComboRequest;
import com.sacmauquan.qrordering.model.Combo;
import com.sacmauquan.qrordering.model.ComboItem;
import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.ComboItemRepository;
import com.sacmauquan.qrordering.repository.ComboRepository;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository comboRepo;
    private final ComboItemRepository comboItemRepo;
    private final MenuItemRepository menuItemRepo;
    private final NotificationService notificationService;

    @Cacheable(value = "combos", key = "'all_active'")
    public List<Combo> getAllActive() {
        return comboRepo.findAllActiveWithItems();
    }

    public List<Combo> getAll() {
        return comboRepo.findAll();
    }

    public Combo getById(@NonNull Long id) {
        return comboRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy combo"));
    }

    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public Combo create(ComboRequest req) {
        if (comboRepo.existsByNameIgnoreCase(req.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên combo đã tồn tại");
        }

        Combo combo = Combo.builder()
                .name(req.getName())
                .price(req.getPrice())
                .active(req.getActive() != null ? req.getActive() : true)
                .items(new LinkedHashSet<>())
                .build();

        if (req.getItems() != null && !req.getItems().isEmpty()) {
            Set<Long> menuIds = req.getItems().stream()
                    .map(ComboItemRequest::getMenuItemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            Map<Long, MenuItem> menuMap = menuItemRepo.findAllById(menuIds).stream()
                    .collect(Collectors.toMap(MenuItem::getId, m -> m));

            for (ComboItemRequest itemReq : req.getItems()) {
                MenuItem menuItem = menuMap.get(itemReq.getMenuItemId());
                if (menuItem == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Món không tồn tại ID: " + itemReq.getMenuItemId());
                }

                combo.getItems().add(ComboItem.builder()
                        .combo(combo)
                        .menuItem(menuItem)
                        .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1)
                        .build());
            }
        }

        Combo saved = comboRepo.save(Objects.requireNonNull(combo));
        notificationService.notifyComboChange("created", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public Combo update(@NonNull Long id, ComboRequest req) {
        Combo combo = getById(id);
        if (comboRepo.existsByNameIgnoreCaseAndIdNot(req.getName(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên combo đã tồn tại");
        }
        
        combo.setName(req.getName());
        combo.setPrice(req.getPrice());
        combo.setActive(req.getActive() != null ? req.getActive() : combo.getActive());
        
        comboItemRepo.softDeleteByComboId(id);
        combo.getItems().clear();

        if (req.getItems() != null && !req.getItems().isEmpty()) {
            // Senior Optimization: Batch fetch MenuItems
            Set<Long> menuIds = req.getItems().stream()
                    .map(ComboItemRequest::getMenuItemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            Map<Long, MenuItem> menuMap = menuItemRepo.findAllById(menuIds).stream()
                    .collect(Collectors.toMap(MenuItem::getId, m -> m));

            for (ComboItemRequest itemReq : req.getItems()) {
                MenuItem menuItem = menuMap.get(itemReq.getMenuItemId());
                if (menuItem == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Món không tồn tại ID: " + itemReq.getMenuItemId());
                }
                
                combo.getItems().add(ComboItem.builder()
                        .combo(combo)
                        .menuItem(menuItem)
                        .quantity(itemReq.getQuantity() != null ? itemReq.getQuantity() : 1)
                        .build());
            }
        }
        
        Combo saved = comboRepo.save(Objects.requireNonNull(combo));
        notificationService.notifyComboChange("updated", saved.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public void delete(@NonNull Long id) {
        Combo combo = getById(id);

        // Xóa mềm items và combo
        comboItemRepo.softDeleteByComboId(id);
        comboRepo.delete(Objects.requireNonNull(combo));

        notificationService.notifyComboChange("deleted", id);
    }

    @Transactional
    @CacheEvict(value = "combos", allEntries = true)
    public Combo toggleActive(@NonNull Long id) {
        Combo combo = getById(id);
        combo.setActive(!Boolean.TRUE.equals(combo.getActive()));

        notificationService.notifyComboChange("status_updated", id);
        return comboRepo.save(Objects.requireNonNull(combo));
    }
}
