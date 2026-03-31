package com.sacmauquan.qrordering.service;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.NoSuchElementException;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.model.ItemOption;
import com.sacmauquan.qrordering.model.ItemOptionValue;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageManagerService imageManager;

    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public List<MenuItem> getItemsByCategory(Integer categoryId) {
        return menuItemRepository.findByCategoryId(categoryId);
    }

    public Optional<MenuItem> getItemById(Long id) {
        return menuItemRepository.findById(id);
    }

    @Transactional
    public MenuItem createItem(MenuItem item) {
        if (menuItemRepository.existsByNameIgnoreCase(item.getName())) {
            throw new IllegalArgumentException("Tên món đã tồn tại");
        }

        // Ensure bi-directional relationships
        if (item.getItemOptions() != null) {
            for (ItemOption opt : item.getItemOptions()) {
                opt.setMenuItem(item);
                if (opt.getOptionValues() != null) {
                    for (ItemOptionValue val : opt.getOptionValues()) {
                        val.setItemOption(opt);
                    }
                }
            }
        }

        MenuItem saved = menuItemRepository.saveAndFlush(item);
        notifyChange("create", saved.getId());
        return saved;
    }

    @Transactional
    public Optional<MenuItem> updateItem(Long id, MenuItem updated) {
        return menuItemRepository.findById(id).map(item -> {
            if (!item.getName().equalsIgnoreCase(updated.getName())) {
                if (menuItemRepository.existsByNameIgnoreCase(updated.getName())) {
                    throw new IllegalArgumentException("Tên món đã tồn tại");
                }
            }

            // CHỈ LẤY CÁC TRƯỜNG CÓ TRONG MODEL CỦA BẠN
            item.setName(updated.getName());
            item.setPrice(updated.getPrice());
            item.setCategory(updated.getCategory());

            if (updated.getImg() != null && !updated.getImg().isBlank()) {
                item.setImg(updated.getImg());
            }

            // Sync Options Optimized
            if (updated.getItemOptions() != null) {
                syncOptions(item, updated.getItemOptions());
            }

            MenuItem saved = menuItemRepository.saveAndFlush(item);
            notifyChange("update", saved.getId());
            return saved;
        });
    }

    private void syncOptions(MenuItem item, java.util.Collection<ItemOption> incomingOptions) {
        // Map existing by ID
        Map<Long, ItemOption> existingMap = item.getItemOptions().stream()
                .filter(o -> o.getId() != null)
                .collect(Collectors.toMap(ItemOption::getId, o -> o));

        Set<Long> processedIds = new HashSet<>();
        List<ItemOption> toAdd = new ArrayList<>();

        for (ItemOption incoming : incomingOptions) {
            if (incoming.getId() != null && existingMap.containsKey(incoming.getId())) {
                ItemOption existing = existingMap.get(incoming.getId());
                processedIds.add(existing.getId());

                // Critical change: Name -> Soft Delete old, Create new
                if (!existing.getName().equals(incoming.getName())) {
                    item.getItemOptions().remove(existing);
                    incoming.setId(null);
                    incoming.setMenuItem(item);
                    if (incoming.getOptionValues() != null) {
                        incoming.getOptionValues().forEach(v -> {
                            v.setId(null);
                            v.setItemOption(incoming);
                        });
                    }
                    toAdd.add(incoming);
                } else {
                    // Non-critical: Update in place
                    existing.setRequired(incoming.isRequired());
                    existing.setMaxSelection(incoming.getMaxSelection());
                    syncValues(existing, incoming.getOptionValues());
                }
            } else {
                // New Option
                incoming.setId(null);
                incoming.setMenuItem(item);
                if (incoming.getOptionValues() != null) {
                    incoming.getOptionValues().forEach(v -> {
                        v.setId(null);
                        v.setItemOption(incoming);
                    });
                }
                toAdd.add(incoming);
            }
        }

        // Remove orphans
        item.getItemOptions().removeIf(o -> o.getId() != null && !processedIds.contains(o.getId()));
        // Add new
        item.getItemOptions().addAll(toAdd);
    }

    private void syncValues(ItemOption existingOpt, java.util.Collection<ItemOptionValue> incomingValues) {
        if (incomingValues == null) return;
        Map<Long, ItemOptionValue> existingMap = existingOpt.getOptionValues().stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(ItemOptionValue::getId, v -> v));

        Set<Long> processedIds = new HashSet<>();
        List<ItemOptionValue> toAdd = new ArrayList<>();

        for (ItemOptionValue incoming : incomingValues) {
            if (incoming.getId() != null && existingMap.containsKey(incoming.getId())) {
                ItemOptionValue existing = existingMap.get(incoming.getId());
                processedIds.add(existing.getId());

                // Critical change: Name or Price -> Replace
                if (!existing.getName().equals(incoming.getName()) || 
                    Double.compare(existing.getExtraPrice(), incoming.getExtraPrice()) != 0) {
                    existingOpt.getOptionValues().remove(existing);
                    incoming.setId(null);
                    incoming.setItemOption(existingOpt);
                    toAdd.add(incoming);
                }
                // No non-critical fields for ItemOptionValue currently, so no in-place update needed.
            } else {
                // New Value
                incoming.setId(null);
                incoming.setItemOption(existingOpt);
                toAdd.add(incoming);
            }
        }

        // Remove orphans
        existingOpt.getOptionValues().removeIf(v -> v.getId() != null && !processedIds.contains(v.getId()));
        // Add new
        existingOpt.getOptionValues().addAll(toAdd);
    }

    @Transactional
    public void deleteItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy món ăn"));

        if (item.getImg() != null && !item.getImg().isBlank()) {
            try {
                imageManager.delete(item.getImg());
            } catch (Exception e) {
                System.err.println("Lỗi xóa ảnh: " + e.getMessage());
            }
        }

        menuItemRepository.delete(item);
        menuItemRepository.flush();
        notifyChange("delete", id);
    }

    @Transactional
    public Map<String, Object> uploadImage(Long id, MultipartFile file) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy món"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng");
        }

        try {
            String newUrl = imageManager.replace(file, item.getImg(), "order_by_qr/menu_items");
            item.setImg(newUrl);
            menuItemRepository.save(item);
            notifyChange("upload image", id);
            return Map.of("img", newUrl);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi upload: " + e.getMessage());
        }
    }

    private void notifyChange(String type, Object id) {
        eventPublisher.publishEvent(new com.sacmauquan.qrordering.event.WebSocketEvent(
                "/topic/menu",
                "UPDATED",
                "[WS] Menu thay đổi (" + type + " ID: " + id + ")"));
    }
}