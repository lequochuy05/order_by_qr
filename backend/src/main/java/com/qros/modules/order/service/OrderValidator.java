package com.qros.modules.order.service;

import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderValidator {

    private final SystemSettingsService systemSettingsService;

    public void validateSystemAcceptsOrders() {
        SystemSettings settings = systemSettingsService.getOrCreateSettings();

        if (Boolean.TRUE.equals(settings.getMaintenanceMode())) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE,
                    "Ordering is disabled while the restaurant is in maintenance mode");
        }

        if (!Boolean.TRUE.equals(settings.getOrderingEnabled())) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE,
                    "Ordering is currently disabled");
        }
    }

    public void validateTableAcceptsOrders(@NonNull DiningTable table) {
        validateSystemAcceptsOrders();

        if (table.getStatus() == TableStatus.INACTIVE) {
            throw new BusinessException(
                    ErrorCode.TABLE_NOT_FOUND,
                    "Table is inactive and cannot accept orders");
        }
    }

    public void validateMenuItemOrderable(@NonNull MenuItem menuItem) {
        validateMenuItemOrderable(menuItem, "Menu item is not available for ordering");
    }

    public void validateComboOrderable(@NonNull Combo combo) {
        if (!Boolean.TRUE.equals(combo.getActive()) || !Boolean.TRUE.equals(combo.getAvailable())) {
            throw new BusinessException(
                    ErrorCode.COMBO_NOT_FOUND,
                    "Combo is not available for ordering");
        }

        if (combo.getItems() != null) {
            combo.getItems().forEach(comboItem -> {
                if (comboItem.getMenuItem() != null) {
                    validateMenuItemOrderable(
                            comboItem.getMenuItem(),
                            "Combo contains an unavailable menu item");
                }
            });
        }
    }

    private void validateMenuItemOrderable(MenuItem menuItem, String message) {
        boolean categoryActive = menuItem.getCategory() != null
                && Boolean.TRUE.equals(menuItem.getCategory().getActive());

        if (!Boolean.TRUE.equals(menuItem.getActive())
                || !Boolean.TRUE.equals(menuItem.getAvailable())
                || !categoryActive) {
            throw new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND, message);
        }
    }
}
