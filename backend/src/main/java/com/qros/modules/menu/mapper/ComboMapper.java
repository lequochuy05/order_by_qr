package com.qros.modules.menu.mapper;

import com.qros.modules.menu.dto.response.ComboItemResponse;
import com.qros.modules.menu.dto.response.ComboResponse;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ComboItem;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ComboMapper {

    private static final Comparator<ComboItem> COMBO_ITEM_COMPARATOR = Comparator.comparing((ComboItem item) ->
                    item.getMenuItem() == null || item.getMenuItem().getDisplayOrder() == null
                            ? 0
                            : item.getMenuItem().getDisplayOrder())
            .thenComparing((ComboItem item) ->
                    item.getMenuItem() == null || item.getMenuItem().getName() == null
                            ? ""
                            : item.getMenuItem().getName());

    public ComboResponse toResponse(Combo combo) {
        if (combo == null) {
            return null;
        }

        List<ComboItemResponse> items = combo.getItems() == null
                ? Collections.emptyList()
                : combo.getItems().stream()
                        .filter(item -> !item.isDeleted())
                        .sorted(COMBO_ITEM_COMPARATOR)
                        .map(this::toItemResponse)
                        .toList();

        return new ComboResponse(
                combo.getId(),
                combo.getName(),
                combo.getDescription(),
                combo.getPrice(),
                combo.getActive(),
                isComboAvailable(combo),
                combo.getDisplayOrder(),
                combo.getCreatedAt(),
                combo.getUpdatedAt(),
                combo.getVersion(),
                items);
    }

    public ComboResponse toSummaryResponse(Combo combo) {
        if (combo == null) {
            return null;
        }

        return new ComboResponse(
                combo.getId(),
                combo.getName(),
                combo.getDescription(),
                combo.getPrice(),
                combo.getActive(),
                combo.getAvailable(),
                combo.getDisplayOrder(),
                combo.getCreatedAt(),
                combo.getUpdatedAt(),
                combo.getVersion(),
                Collections.emptyList());
    }

    public ComboResponse withAvailability(ComboResponse combo, boolean available) {
        if (combo == null) {
            return null;
        }

        return new ComboResponse(
                combo.id(),
                combo.name(),
                combo.description(),
                combo.price(),
                combo.active(),
                available,
                combo.displayOrder(),
                combo.createdAt(),
                combo.updatedAt(),
                combo.version(),
                combo.items());
    }

    private boolean isComboAvailable(Combo combo) {
        if (!Boolean.TRUE.equals(combo.getAvailable())) {
            return false;
        }
        if (combo.getItems() == null || combo.getItems().isEmpty()) {
            return true;
        }
        return combo.getItems().stream()
                .filter(item -> !item.isDeleted())
                .allMatch(item -> item.getMenuItem() != null
                        && !item.getMenuItem().isDeleted()
                        && Boolean.TRUE.equals(item.getMenuItem().getActive())
                        && Boolean.TRUE.equals(item.getMenuItem().getAvailable()));
    }

    public ComboItemResponse toItemResponse(ComboItem item) {
        if (item == null) {
            return null;
        }

        return new ComboItemResponse(
                item.getId(),
                item.getMenuItem() != null ? item.getMenuItem().getId() : null,
                item.getMenuItem() != null ? item.getMenuItem().getName() : null,
                item.getMenuItem() != null ? item.getMenuItem().getImg() : null,
                item.getQuantity());
    }
}
