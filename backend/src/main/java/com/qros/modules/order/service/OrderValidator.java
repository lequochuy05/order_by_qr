package com.qros.modules.order.service;

import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.table.model.enums.TableStatus;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderValidator {

    private final SystemSettingsService systemSettingsService;

    public void validateSystemAcceptsOrders() {
        SystemSettings settings = systemSettingsService.getSettingsEntity();

        if (Boolean.TRUE.equals(settings.getMaintenanceMode())) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE, "Ordering is disabled while the restaurant is in maintenance mode");
        }

        if (!Boolean.TRUE.equals(settings.getOrderingEnabled())) {
            throw new BusinessException(ErrorCode.ORDER_INVALID_STATE, "Ordering is currently disabled");
        }
    }

    public void validateCustomerOperatingHours() {
        SystemSettings settings = systemSettingsService.getSettingsEntity();
        if (settings == null) {
            return;
        }
        LocalTime open = settings.getOpeningTime();
        LocalTime close = settings.getClosingTime();
        if (open == null || close == null || open.equals(close)) {
            return;
        }

        LocalTime now = AppTime.now().toLocalTime();
        boolean withinHours = open.isBefore(close)
                ? (!now.isBefore(open) && !now.isAfter(close))
                : (!now.isBefore(open) || !now.isAfter(close));

        if (!withinHours) {
            throw new BusinessException(
                    ErrorCode.ORDER_INVALID_STATE, "Restaurant is currently outside operating hours");
        }
    }

    public void validateTableAcceptsOrders(@NonNull DiningTable table) {
        validateSystemAcceptsOrders();

        if (table.getStatus() == TableStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.TABLE_NOT_FOUND, "Table is inactive and cannot accept orders");
        }
    }

    public void validateMenuItemOrderable(@NonNull MenuItem menuItem) {
        validateMenuItemOrderable(menuItem, "Menu item is not available for ordering");
    }

    public void validateComboOrderable(@NonNull Combo combo) {
        validateComboOrderable(combo, false);
    }

    public void validateComboOrderable(@NonNull Combo combo, boolean isCustomerOrder) {
        if (isCustomerOrder
                && Boolean.FALSE.equals(
                        systemSettingsService.getSettingsEntity().getShowCombos())) {
            throw new BusinessException(ErrorCode.COMBO_NOT_FOUND, "Combos are currently disabled");
        }
        if (!Boolean.TRUE.equals(combo.getActive()) || !Boolean.TRUE.equals(combo.getAvailable())) {
            throw new BusinessException(ErrorCode.COMBO_NOT_FOUND, "Combo is not available for ordering");
        }

        if (combo.getItems() != null) {
            combo.getItems().forEach(comboItem -> {
                if (comboItem.getMenuItem() != null) {
                    validateMenuItemOrderable(comboItem.getMenuItem(), "Combo contains an unavailable menu item");
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
