package com.sacmauquan.qrordering.service.impl;
import org.springframework.lang.NonNull;
import java.util.Objects;

import com.sacmauquan.qrordering.model.ItemOption;
import com.sacmauquan.qrordering.model.ItemOptionValue;
import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.sacmauquan.qrordering.service.ImageManagerService;
import com.sacmauquan.qrordering.service.MenuItemService;
import com.sacmauquan.qrordering.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final NotificationService notificationService;
    private final ImageManagerService imageManager;

    @Override
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    @Override
    public List<MenuItem> getItemsByCategory(@NonNull Integer categoryId) {
        return menuItemRepository.findByCategoryId(Objects.requireNonNull(categoryId));
    }

    @Override
    public Optional<MenuItem> getItemById(@NonNull Long id) {
        return menuItemRepository.findById(Objects.requireNonNull(id));
    }

    @Override
    @Transactional
    public MenuItem createItem(@NonNull MenuItem item) {
        if (menuItemRepository.existsByNameIgnoreCase(item.getName())) {
            throw new IllegalArgumentException("Tên món đã tồn tại");
        }

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
        notificationService.notifyMenuChange("create", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Optional<MenuItem> updateItem(@NonNull Long id, @NonNull MenuItem updated) {
        return menuItemRepository.findById(Objects.requireNonNull(id)).map(item -> {
            if (!item.getName().equalsIgnoreCase(updated.getName())) {
                if (menuItemRepository.existsByNameIgnoreCase(updated.getName())) {
                    throw new IllegalArgumentException("Tên món đã tồn tại");
                }
            }

            item.setName(updated.getName());
            item.setPrice(updated.getPrice());
            item.setCategory(updated.getCategory());

            if (updated.getImg() != null && !updated.getImg().isBlank()) {
                item.setImg(updated.getImg());
            }

            if (updated.getItemOptions() != null) {
                syncOptions(item, updated.getItemOptions());
            }

            MenuItem saved = menuItemRepository.saveAndFlush(item);
            notificationService.notifyMenuChange("update", saved.getId());
            return saved;
        });
    }

    private void syncOptions(MenuItem item, java.util.Collection<ItemOption> incomingOptions) {
        Map<Long, ItemOption> existingMap = item.getItemOptions().stream()
                .filter(o -> o.getId() != null)
                .collect(Collectors.toMap(ItemOption::getId, o -> o));

        Set<Long> processedIds = new HashSet<>();
        List<ItemOption> toAdd = new ArrayList<>();

        for (ItemOption incoming : incomingOptions) {
            if (incoming.getId() != null && existingMap.containsKey(incoming.getId())) {
                ItemOption existing = existingMap.get(incoming.getId());
                processedIds.add(existing.getId());

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
                    existing.setRequired(incoming.isRequired());
                    existing.setMaxSelection(incoming.getMaxSelection());
                    syncValues(existing, incoming.getOptionValues());
                }
            } else {
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

        item.getItemOptions().removeIf(o -> o.getId() != null && !processedIds.contains(o.getId()));
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

                if (!existing.getName().equals(incoming.getName()) || 
                    Double.compare(existing.getExtraPrice(), incoming.getExtraPrice()) != 0) {
                    existingOpt.getOptionValues().remove(existing);
                    incoming.setId(null);
                    incoming.setItemOption(existingOpt);
                    toAdd.add(incoming);
                }
            } else {
                incoming.setId(null);
                incoming.setItemOption(existingOpt);
                toAdd.add(incoming);
            }
        }

        existingOpt.getOptionValues().removeIf(v -> v.getId() != null && !processedIds.contains(v.getId()));
        existingOpt.getOptionValues().addAll(toAdd);
    }

    @Override
    @Transactional
    public void deleteItem(@NonNull Long id) {
        MenuItem item = menuItemRepository.findById(Objects.requireNonNull(id))
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
        notificationService.notifyMenuChange("delete", id);
    }

    @Override
    @Transactional
    public Map<String, Object> uploadImage(@NonNull Long id, @NonNull MultipartFile file) {
        MenuItem item = menuItemRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy món"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng");
        }

        try {
            String newUrl = imageManager.replace(file, item.getImg(), "order_by_qr/menu_items");
            item.setImg(newUrl);
            menuItemRepository.save(item);
            notificationService.notifyMenuChange("upload image", id);
            return Map.of("img", newUrl);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi upload: " + e.getMessage());
        }
    }
}
