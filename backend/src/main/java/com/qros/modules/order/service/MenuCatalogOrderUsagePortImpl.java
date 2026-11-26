package com.qros.modules.order.service;

import com.qros.modules.menu.service.MenuCatalogOrderUsagePort;
import com.qros.modules.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MenuCatalogOrderUsagePortImpl implements MenuCatalogOrderUsagePort {

    private final OrderItemRepository orderItemRepository;

    @Override
    public boolean isMenuItemUsedInActiveOrders(Long menuItemId) {
        return menuItemId != null && orderItemRepository.existsByMenuItemIdInActiveOrders(menuItemId);
    }

    @Override
    public boolean isComboUsedInActiveOrders(Long comboId) {
        return comboId != null && orderItemRepository.existsByComboIdInActiveOrders(comboId);
    }
}
