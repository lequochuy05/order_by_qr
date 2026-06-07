package com.qros.modules.order.service;

import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.ItemOptionValueRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.dto.OrderPreviewResponse;
import com.qros.modules.order.dto.OrderRequest;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.promotion.dto.VoucherValidateResponse;
import com.qros.modules.promotion.service.DiscountService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.util.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final MenuItemRepository menuItemRepository;
    private final ComboRepository comboRepository;
    private final ItemOptionValueRepository itemOptionValueRepository;
    private final DiscountService discountService;

    public void recalculateOrderTotals(Order order) {
        order.getOrderItems().forEach(this::recalculateLineTotal);
        BigDecimal subtotal = calculateSubtotal(order);
        BigDecimal discount = safe(order.getDiscountAmount());
        setOrderMoney(order, subtotal, discount);
    }

    public void setOrderMoney(Order order, BigDecimal subtotal, BigDecimal discount) {
        BigDecimal safeSubtotal = safe(subtotal);
        BigDecimal safeDiscount = safe(discount);
        BigDecimal finalAmount = calculateFinalTotal(safeSubtotal, safeDiscount);

        order.setSubtotalAmount(safeSubtotal);
        order.setDiscountAmount(safeDiscount);
        order.setFinalAmount(finalAmount);
        if (order.getPaidAmount() == null) {
            order.setPaidAmount(BigDecimal.ZERO);
        }
        if (order.getBusinessDate() == null) {
            order.setBusinessDate(AppTime.today());
        }
    }

    public BigDecimal calculateSubtotal(Order order) {
        return order.getOrderItems().stream()
                .map(item -> safe(item.getLineTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateFinalTotal(BigDecimal subtotal, BigDecimal discount) {
        return safe(subtotal).subtract(safe(discount)).max(BigDecimal.ZERO);
    }

    public void recalculateLineTotal(OrderItem item) {
        item.setLineTotal(calculateLineTotal(item.getUnitPrice(), item.getQuantity()));
    }

    public BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity) {
        return safe(unitPrice).multiply(BigDecimal.valueOf(quantity));
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public OrderPreviewResponse preview(@NonNull OrderRequest request) {
        BigDecimal subtotalItems = calculateItemSubtotal(request);
        BigDecimal subtotalCombos = calculateComboSubtotal(request);
        BigDecimal subtotal = subtotalItems.add(subtotalCombos);
        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean voucherValid = false;
        String voucherMessage = "";

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            VoucherValidateResponse voucher = discountService.validateCode(request.getVoucherCode(), subtotal);
            voucherValid = voucher.applicable();
            voucherMessage = voucher.status();
            discountAmount = voucher.discountValue();
        }

        return OrderPreviewResponse.builder()
                .subtotalItems(subtotalItems)
                .subtotalCombos(subtotalCombos)
                .subtotalAmount(subtotal)
                .discountAmount(discountAmount)
                .finalAmount(calculateFinalTotal(subtotal, discountAmount))
                .voucherValid(voucherValid)
                .voucherMessage(voucherMessage)
                .build();
    }

    private BigDecimal calculateItemSubtotal(OrderRequest request) {
        if (request.getItems() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            MenuItem menuItem = menuItemRepository.findById(Objects.requireNonNull(itemRequest.getMenuItemId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND,
                            "Menu item not found: " + itemRequest.getMenuItemId()));

            BigDecimal optionsPrice = BigDecimal.ZERO;
            if (itemRequest.getSelectedOptionValueIds() != null
                    && !itemRequest.getSelectedOptionValueIds().isEmpty()) {
                optionsPrice = itemOptionValueRepository
                        .findAllById(Objects.requireNonNull(itemRequest.getSelectedOptionValueIds())).stream()
                        .map(ItemOptionValue::getExtraPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            BigDecimal unitTotal = menuItem.getPrice().add(optionsPrice);
            subtotal = subtotal.add(unitTotal.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }
        return subtotal;
    }

    private BigDecimal calculateComboSubtotal(OrderRequest request) {
        if (request.getCombos() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderRequest.OrderComboRequest comboRequest : request.getCombos()) {
            Combo combo = comboRepository.findById(Objects.requireNonNull(comboRequest.getComboId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMBO_NOT_FOUND,
                            "Combo not found: " + comboRequest.getComboId()));
            subtotal = subtotal.add(combo.getPrice().multiply(BigDecimal.valueOf(comboRequest.getQuantity())));
        }
        return subtotal;
    }
}
