package com.qros.modules.order.service;

import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.ItemOptionValueRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.request.OrderComboRequest;
import com.qros.modules.order.dto.request.OrderItemRequest;
import com.qros.modules.order.dto.request.StaffCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final MenuItemRepository menuItemRepository;
    private final ComboRepository comboRepository;
    private final ItemOptionValueRepository itemOptionValueRepository;
    private final VoucherCheckoutService voucherCheckoutService;
    private final OrderValidator orderValidator;

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
                .filter(OrderItem::isBillable)
                .map(item -> safe(item.getLineTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateFinalTotal(BigDecimal subtotal, BigDecimal discount) {
        return safe(subtotal).subtract(safe(discount)).max(BigDecimal.ZERO);
    }

    public void recalculateLineTotal(OrderItem item) {
        item.setLineTotal(calculateLineTotal(item.getUnitPrice(), item.getQuantity()));
    }

    public BigDecimal calculateLineTotal(BigDecimal unitPrice, Integer quantity) {
        return safe(unitPrice).multiply(BigDecimal.valueOf(quantity == null ? 0 : quantity));
    }

    @Transactional(readOnly = true)
    public OrderPreviewResponse previewCustomerOrder(@NonNull CustomerCreateOrderRequest request) {
        return preview(
                request.items(),
                request.combos(),
                null);
    }

    @Transactional(readOnly = true)
    public OrderPreviewResponse previewStaffOrder(@NonNull StaffCreateOrderRequest request) {
        return preview(
                request.items(),
                request.combos(),
                request.voucherCode());
    }

    private OrderPreviewResponse preview(
            List<OrderItemRequest> items,
            List<OrderComboRequest> combos,
            String voucherCode) {
        validateOrderContent(items, combos);
        orderValidator.validateSystemAcceptsOrders();

        BigDecimal subtotalItems = calculateItemSubtotal(items);
        BigDecimal subtotalCombos = calculateComboSubtotal(combos);
        BigDecimal subtotal = subtotalItems.add(subtotalCombos);

        DiscountResult discountResult = voucherCheckoutService.previewVoucher(voucherCode, subtotal);

        boolean isVoucherValid = discountResult.voucherId() != null;
        String voucherMessage = isVoucherValid ? "Voucher áp dụng thành công" : (voucherCode != null ? "Voucher không hợp lệ hoặc đã hết hạn" : "");

        return new OrderPreviewResponse(
                subtotalItems,
                subtotalCombos,
                subtotal,
                discountResult.appliedDiscountAmount(),
                discountResult.finalAmount(),
                isVoucherValid,
                voucherMessage,
                BigDecimal.ZERO);
    }

    private void validateOrderContent(List<OrderItemRequest> items, List<OrderComboRequest> combos) {
        boolean hasItems = items != null && !items.isEmpty();
        boolean hasCombos = combos != null && !combos.isEmpty();

        if (!hasItems && !hasCombos) {
            throw new BusinessException(ErrorCode.ORDER_CONTENT_EMPTY);
        }
    }

    private BigDecimal calculateItemSubtotal(List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        Set<Long> menuItemIds = items.stream()
                .map(OrderItemRequest::menuItemId)
                .collect(Collectors.toSet());
        
        java.util.Map<Long, MenuItem> menuItemsMap = menuItemRepository.findAllByIdIn(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItem::getId, item -> item));

        for (OrderItemRequest itemRequest : items) {
            MenuItem menuItem = menuItemsMap.get(itemRequest.menuItemId());
            if (menuItem == null) {
                throw new BusinessException(
                        ErrorCode.MENU_ITEM_NOT_FOUND,
                        "Menu item not found: " + itemRequest.menuItemId());
            }
            orderValidator.validateMenuItemOrderable(menuItem);

            List<Long> selectedOptionValueIds = itemRequest.selectedOptionValueIds() == null
                    ? List.of()
                    : itemRequest.selectedOptionValueIds();

            validateSelectedOptions(menuItem, selectedOptionValueIds);

            BigDecimal optionsPrice = calculateOptionsPrice(selectedOptionValueIds);

            BigDecimal unitTotal = safe(menuItem.getPrice()).add(optionsPrice);
            subtotal = subtotal.add(calculateLineTotal(unitTotal, itemRequest.quantity()));
        }

        return subtotal;
    }

    private BigDecimal calculateComboSubtotal(List<OrderComboRequest> combos) {
        if (combos == null || combos.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        List<Long> comboIds = combos.stream()
                .map(OrderComboRequest::comboId)
                .collect(Collectors.toList());
                
        java.util.Map<Long, Combo> combosMap = comboRepository.findAllByIdInWithItems(comboIds).stream()
                .collect(Collectors.toMap(Combo::getId, c -> c));

        for (OrderComboRequest comboRequest : combos) {
            Combo combo = Optional.ofNullable(combosMap.get(comboRequest.comboId()))
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.COMBO_NOT_FOUND,
                            "Combo not found: " + comboRequest.comboId()));
            orderValidator.validateComboOrderable(combo);

            subtotal = subtotal.add(calculateLineTotal(combo.getPrice(), comboRequest.quantity()));
        }

        return subtotal;
    }

    private BigDecimal calculateOptionsPrice(List<Long> selectedOptionValueIds) {
        if (selectedOptionValueIds == null || selectedOptionValueIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<ItemOptionValue> selectedValues = itemOptionValueRepository.findAllById(selectedOptionValueIds);

        validateOptionValuesExist(selectedOptionValueIds, selectedValues);

        return selectedValues.stream()
                .map(ItemOptionValue::getExtraPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateSelectedOptions(MenuItem menuItem, List<Long> selectedValueIds) {
        Set<Long> selectedIds = selectedValueIds == null
                ? Set.of()
                : new HashSet<>(selectedValueIds);

        validateRequiredOptions(menuItem, selectedIds);
        validateMaxSelections(menuItem, selectedIds);
        validateOptionValuesBelongToMenuItem(menuItem, selectedIds);
    }

    private void validateRequiredOptions(MenuItem menuItem, Set<Long> selectedIds) {
        menuItem.getItemOptions().stream()
                .filter(option -> Boolean.TRUE.equals(option.getRequired()))
                .forEach(option -> {
                    boolean selected = option.getOptionValues().stream()
                            .anyMatch(value -> selectedIds.contains(value.getId()));

                    if (!selected) {
                        throw new BusinessException(
                                ErrorCode.INVALID_REQUEST,
                                "Required option selection missing: " + option.getName());
                    }
                });
    }

    private void validateMaxSelections(MenuItem menuItem, Set<Long> selectedIds) {
        menuItem.getItemOptions().forEach(option -> {
            int maxSelection = option.getMaxSelection() == null ? 1 : option.getMaxSelection();
            long selectedCount = option.getOptionValues().stream()
                    .filter(value -> selectedIds.contains(value.getId()))
                    .count();

            if (selectedCount > maxSelection) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "Too many option selections for: " + option.getName());
            }
        });
    }

    private void validateOptionValuesBelongToMenuItem(MenuItem menuItem, Set<Long> selectedIds) {
        if (selectedIds.isEmpty()) {
            return;
        }

        Set<Long> validValueIds = menuItem.getItemOptions().stream()
                .flatMap(option -> option.getOptionValues().stream())
                .map(ItemOptionValue::getId)
                .collect(Collectors.toSet());

        if (!validValueIds.containsAll(selectedIds)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Selected option contains invalid value for this menu item.");
        }
    }

    private void validateOptionValuesExist(List<Long> requestedIds, List<ItemOptionValue> selectedValues) {
        Set<Long> foundIds = selectedValues.stream()
                .map(ItemOptionValue::getId)
                .collect(Collectors.toSet());

        if (!foundIds.containsAll(requestedIds)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Selected option value does not exist.");
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
