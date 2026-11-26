package com.qros.modules.order.service;

import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.order.dto.request.OrderComboRequest;
import com.qros.modules.order.dto.request.OrderItemRequest;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class OrderOptionSupport {

    private OrderOptionSupport() {}

    static void validateOrderContent(List<OrderItemRequest> items, List<OrderComboRequest> combos) {
        boolean hasItems = items != null && !items.isEmpty();
        boolean hasCombos = combos != null && !combos.isEmpty();

        if (!hasItems && !hasCombos) {
            throw new BusinessException(ErrorCode.ORDER_CONTENT_EMPTY);
        }

        if (hasCombos
                && combos.stream()
                        .anyMatch(combo -> combo == null || combo.quantity() == null || combo.quantity() < 1)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Combo quantity must be at least 1");
        }
    }

    static Set<Long> extractSelectedOptionValueIds(List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return Set.of();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .flatMap(item -> item.selectedOptionValueIds() == null
                        ? Stream.<Long>empty()
                        : item.selectedOptionValueIds().stream())
                .collect(Collectors.toSet());
    }

    static List<ItemOptionValue> resolveSelectedValues(
            List<Long> selectedOptionValueIds, Map<Long, ItemOptionValue> selectedValuesById) {
        if (selectedOptionValueIds == null || selectedOptionValueIds.isEmpty()) {
            return List.of();
        }

        return selectedOptionValueIds.stream()
                .map(selectedValuesById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    static void validateSelectedOptions(MenuItem menuItem, List<Long> selectedValueIds) {
        if (selectedValueIds != null && !selectedValueIds.isEmpty()) {
            if (selectedValueIds.stream().anyMatch(Objects::isNull)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Selected option value ID cannot be null");
            }
            Set<Long> uniqueIds = new HashSet<>(selectedValueIds);
            if (uniqueIds.size() != selectedValueIds.size()) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST, "Duplicate option value selections are not allowed");
            }
            validateRequiredOptions(menuItem, uniqueIds);
            validateMaxSelections(menuItem, uniqueIds);
            validateOptionValuesBelongToMenuItem(menuItem, uniqueIds);
            return;
        }

        Set<Long> selectedIds = Set.of();
        validateRequiredOptions(menuItem, selectedIds);
        validateMaxSelections(menuItem, selectedIds);
        validateOptionValuesBelongToMenuItem(menuItem, selectedIds);
    }

    static void validateOptionValuesExist(List<Long> requestedIds, List<ItemOptionValue> selectedValues) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return;
        }

        Set<Long> foundIds = selectedValues.stream().map(ItemOptionValue::getId).collect(Collectors.toSet());

        if (!foundIds.containsAll(requestedIds)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Selected option value does not exist.");
        }
    }

    static BigDecimal calculateOptionsPrice(List<ItemOptionValue> selectedValues) {
        if (selectedValues == null || selectedValues.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return selectedValues.stream()
                .map(ItemOptionValue::getExtraPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void validateRequiredOptions(MenuItem menuItem, Set<Long> selectedIds) {
        menuItem.getItemOptions().stream()
                .filter(option -> Boolean.TRUE.equals(option.getRequired()))
                .forEach(option -> {
                    boolean selected =
                            option.getOptionValues().stream().anyMatch(value -> selectedIds.contains(value.getId()));

                    if (!selected) {
                        throw new BusinessException(
                                ErrorCode.INVALID_REQUEST, "Required option selection missing: " + option.getName());
                    }
                });
    }

    private static void validateMaxSelections(MenuItem menuItem, Set<Long> selectedIds) {
        menuItem.getItemOptions().forEach(option -> {
            int maxSelection = option.getMaxSelection() == null ? 1 : option.getMaxSelection();
            long selectedCount = option.getOptionValues().stream()
                    .filter(value -> selectedIds.contains(value.getId()))
                    .count();

            if (selectedCount > maxSelection) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST, "Too many option selections for: " + option.getName());
            }
        });
    }

    private static void validateOptionValuesBelongToMenuItem(MenuItem menuItem, Set<Long> selectedIds) {
        if (selectedIds.isEmpty()) {
            return;
        }

        Set<Long> validValueIds = menuItem.getItemOptions().stream()
                .flatMap(option -> option.getOptionValues().stream())
                .map(ItemOptionValue::getId)
                .collect(Collectors.toSet());

        if (!validValueIds.containsAll(selectedIds)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST, "Selected option contains invalid value for this menu item.");
        }
    }
}
