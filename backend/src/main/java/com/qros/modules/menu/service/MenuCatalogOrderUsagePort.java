package com.qros.modules.menu.service;

public interface MenuCatalogOrderUsagePort {

    boolean isMenuItemUsedInActiveOrders(Long menuItemId);

    boolean isComboUsedInActiveOrders(Long comboId);
}
