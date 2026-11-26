package com.qros.modules.order.service;

import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.service.MenuCatalogLookupService;
import com.qros.modules.order.dto.request.CustomerCreateOrderRequest;
import com.qros.modules.order.dto.request.OrderComboRequest;
import com.qros.modules.order.dto.request.OrderItemRequest;
import com.qros.modules.order.dto.request.StaffCreateOrderRequest;
import com.qros.modules.order.dto.response.OrderPreviewResponse;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.dto.internal.VoucherPaymentResult;
import com.qros.modules.promotion.service.PromotionService;
import com.qros.modules.promotion.service.VoucherCheckoutService;
import com.qros.modules.settings.model.SystemSettings;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.table.service.TableSessionService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPricingService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final MenuCatalogLookupService menuCatalogLookupService;
    private final VoucherCheckoutService voucherCheckoutService;
    private final OrderValidator orderValidator;
    private final TableSessionService tableSessionService;
    private final PromotionService promotionService;
    private final SystemSettingsService systemSettingsService;

    public OrderPricingService(
            MenuCatalogLookupService menuCatalogLookupService,
            VoucherCheckoutService voucherCheckoutService,
            OrderValidator orderValidator,
            TableSessionService tableSessionService) {
        this(menuCatalogLookupService, voucherCheckoutService, orderValidator, tableSessionService, null, null);
    }

    public OrderPricingService(
            MenuCatalogLookupService menuCatalogLookupService,
            VoucherCheckoutService voucherCheckoutService,
            OrderValidator orderValidator,
            TableSessionService tableSessionService,
            PromotionService promotionService) {
        this(
                menuCatalogLookupService,
                voucherCheckoutService,
                orderValidator,
                tableSessionService,
                promotionService,
                null);
    }

    @Autowired
    public OrderPricingService(
            MenuCatalogLookupService menuCatalogLookupService,
            VoucherCheckoutService voucherCheckoutService,
            OrderValidator orderValidator,
            TableSessionService tableSessionService,
            PromotionService promotionService,
            SystemSettingsService systemSettingsService) {
        this.menuCatalogLookupService = menuCatalogLookupService;
        this.voucherCheckoutService = voucherCheckoutService;
        this.orderValidator = orderValidator;
        this.tableSessionService = tableSessionService;
        this.promotionService = promotionService;
        this.systemSettingsService = systemSettingsService;
    }

    public record PriceBreakdown(
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal taxableBase,
            BigDecimal serviceChargePercent,
            BigDecimal serviceChargeAmount,
            BigDecimal taxPercent,
            BigDecimal taxAmount,
            BigDecimal finalAmount) {}

    public PriceBreakdown calculatePriceBreakdown(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal safeSubtotal = safe(subtotal);
        BigDecimal safeDiscount = safe(discount).min(safeSubtotal).max(BigDecimal.ZERO);
        BigDecimal taxableBase = safeSubtotal.subtract(safeDiscount).max(BigDecimal.ZERO);

        BigDecimal serviceChargePercent = BigDecimal.ZERO;
        BigDecimal taxPercent = BigDecimal.ZERO;
        if (systemSettingsService != null) {
            SystemSettings settings = systemSettingsService.getSettingsEntity();
            if (settings != null) {
                serviceChargePercent = normalizeRate(settings.getServiceChargePercent());
                taxPercent = normalizeRate(settings.getTaxPercent());
            }
        }

        BigDecimal serviceChargeAmount = serviceChargePercent.compareTo(BigDecimal.ZERO) > 0
                ? taxableBase.multiply(serviceChargePercent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal taxBase = taxableBase.add(serviceChargeAmount);
        BigDecimal taxAmount = taxPercent.compareTo(BigDecimal.ZERO) > 0
                ? taxBase.multiply(taxPercent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal finalAmount = taxBase.add(taxAmount);

        return new PriceBreakdown(
                safeSubtotal,
                safeDiscount,
                taxableBase,
                serviceChargePercent,
                serviceChargeAmount,
                taxPercent,
                taxAmount,
                finalAmount);
    }

    public BigDecimal calculatePromotionDiscount(BigDecimal subtotal) {
        if (promotionService == null || subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return safe(promotionService.calculatePromotionDiscount(subtotal));
    }

    public BigDecimal resolveOrderPromotionDiscount(Order order, BigDecimal subtotal) {
        BigDecimal safeSubtotal = safe(subtotal);
        if (safeSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal liveDiscount = calculatePromotionDiscount(safeSubtotal);
        BigDecimal livePercent = BigDecimal.ZERO;
        if (promotionService != null) {
            livePercent = promotionService
                    .findBestActivePromotion()
                    .map(com.qros.modules.promotion.model.Promotion::getDiscountPercent)
                    .map(this::normalizeRate)
                    .orElse(BigDecimal.ZERO);
        }

        BigDecimal currentSnapshot =
                order != null ? normalizeRate(order.getAppliedPromotionPercent()) : BigDecimal.ZERO;
        BigDecimal effectivePercent = currentSnapshot.max(livePercent);
        if (order != null && effectivePercent.compareTo(currentSnapshot) > 0) {
            order.setAppliedPromotionPercent(effectivePercent);
        }

        BigDecimal snapshotDiscount = effectivePercent.compareTo(BigDecimal.ZERO) > 0
                ? safeSubtotal.multiply(effectivePercent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return liveDiscount.max(snapshotDiscount).min(safeSubtotal);
    }

    public void recalculateOrderTotals(Order order) {
        order.getOrderItems().forEach(this::recalculateLineTotal);

        BigDecimal subtotal = calculateSubtotal(order);
        BigDecimal promotionDiscount = resolveOrderPromotionDiscount(order, subtotal);
        BigDecimal voucherDiscount = BigDecimal.ZERO;

        if (order.getVoucherCode() != null && !order.getVoucherCode().isBlank()) {
            DiscountResult preview = voucherCheckoutService.previewVoucher(order.getVoucherCode(), subtotal);
            voucherDiscount = safe(preview.appliedDiscountAmount());
        }

        BigDecimal totalDiscount = promotionDiscount.add(voucherDiscount).min(subtotal);
        setOrderMoney(order, subtotal, totalDiscount);
    }

    public void applyVoucher(Order order, String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return;
        }

        BigDecimal subtotal = calculateSubtotal(order);
        BigDecimal promotionDiscount = resolveOrderPromotionDiscount(order, subtotal);
        VoucherPaymentResult voucherResult = voucherCheckoutService.resolveForPayment(voucherCode.trim(), subtotal);
        BigDecimal totalDiscount = promotionDiscount
                .add(safe(voucherResult.appliedDiscountAmount()))
                .min(subtotal);

        order.setVoucherCode(voucherResult.voucherCode());
        setOrderMoney(order, subtotal, totalDiscount);
    }

    public void setOrderMoney(Order order, BigDecimal subtotal, BigDecimal discount) {
        PriceBreakdown breakdown = calculatePriceBreakdown(subtotal, discount);

        order.setSubtotalAmount(breakdown.subtotalAmount());
        order.setDiscountAmount(breakdown.discountAmount());
        order.setServiceChargePercentSnapshot(breakdown.serviceChargePercent());
        order.setServiceChargeAmount(breakdown.serviceChargeAmount());
        order.setTaxPercentSnapshot(breakdown.taxPercent());
        order.setTaxAmount(breakdown.taxAmount());
        order.setFinalAmount(breakdown.finalAmount());

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
        return calculatePriceBreakdown(subtotal, discount).finalAmount();
    }

    public void recalculateLineTotal(OrderItem item) {
        item.setLineTotal(calculateLineTotal(item.getUnitPrice(), item.getQuantity()));
    }

    public BigDecimal calculateLineTotal(BigDecimal unitPrice, Integer quantity) {
        return safe(unitPrice).multiply(BigDecimal.valueOf(quantity == null ? 0 : quantity));
    }

    @Transactional(readOnly = true)
    public OrderPreviewResponse previewCustomerOrder(@NonNull CustomerCreateOrderRequest request) {
        tableSessionService.requireOpenSessionForRead(request.tableCode(), request.sessionToken());
        orderValidator.validateCustomerOperatingHours();

        return preview(request.items(), request.combos(), null, true);
    }

    @Transactional(readOnly = true)
    public OrderPreviewResponse previewStaffOrder(@NonNull StaffCreateOrderRequest request) {
        return preview(request.items(), request.combos(), request.voucherCode(), false);
    }

    private OrderPreviewResponse preview(
            List<OrderItemRequest> items, List<OrderComboRequest> combos, String voucherCode, boolean isCustomerOrder) {
        validateOrderContent(items, combos);
        orderValidator.validateSystemAcceptsOrders();

        BigDecimal subtotalItems = calculateItemSubtotal(items);
        BigDecimal subtotalCombos = calculateComboSubtotal(combos, isCustomerOrder);
        BigDecimal subtotal = subtotalItems.add(subtotalCombos);

        BigDecimal discountPromotion = calculatePromotionDiscount(subtotal);
        DiscountResult discountResult = voucherCheckoutService.previewVoucher(voucherCode, subtotal);
        BigDecimal voucherDiscount = safe(discountResult.appliedDiscountAmount());
        BigDecimal totalDiscount = discountPromotion.add(voucherDiscount).min(subtotal);
        PriceBreakdown breakdown = calculatePriceBreakdown(subtotal, totalDiscount);

        boolean isVoucherValid = discountResult.voucherId() != null;
        String voucherMessage = isVoucherValid
                ? "Voucher applied successfully"
                : (voucherCode != null && !voucherCode.isBlank() ? "Voucher is invalid or expired" : "");

        return new OrderPreviewResponse(
                subtotalItems,
                subtotalCombos,
                breakdown.subtotalAmount(),
                breakdown.discountAmount(),
                breakdown.serviceChargePercent(),
                breakdown.serviceChargeAmount(),
                breakdown.taxPercent(),
                breakdown.taxAmount(),
                breakdown.finalAmount(),
                isVoucherValid,
                voucherMessage,
                discountPromotion);
    }

    private void validateOrderContent(List<OrderItemRequest> items, List<OrderComboRequest> combos) {
        OrderOptionSupport.validateOrderContent(items, combos);
    }

    private BigDecimal calculateItemSubtotal(List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        Set<Long> menuItemIds = items.stream().map(OrderItemRequest::menuItemId).collect(Collectors.toSet());

        java.util.Map<Long, MenuItem> menuItemsMap = menuCatalogLookupService.findMenuItemsByIds(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItem::getId, item -> item));
        Map<Long, ItemOptionValue> selectedValuesById = loadSelectedOptionValues(items);

        for (OrderItemRequest itemRequest : items) {
            MenuItem menuItem = menuItemsMap.get(itemRequest.menuItemId());
            if (menuItem == null) {
                throw new BusinessException(
                        ErrorCode.MENU_ITEM_NOT_FOUND, "Menu item not found: " + itemRequest.menuItemId());
            }
            orderValidator.validateMenuItemOrderable(menuItem);

            List<Long> selectedOptionValueIds =
                    itemRequest.selectedOptionValueIds() == null ? List.of() : itemRequest.selectedOptionValueIds();

            OrderOptionSupport.validateSelectedOptions(menuItem, selectedOptionValueIds);

            List<ItemOptionValue> selectedValues =
                    OrderOptionSupport.resolveSelectedValues(selectedOptionValueIds, selectedValuesById);
            OrderOptionSupport.validateOptionValuesExist(selectedOptionValueIds, selectedValues);

            BigDecimal optionsPrice = OrderOptionSupport.calculateOptionsPrice(selectedValues);

            BigDecimal unitTotal = safe(menuItem.getPrice()).add(optionsPrice);
            subtotal = subtotal.add(calculateLineTotal(unitTotal, itemRequest.quantity()));
        }

        return subtotal;
    }

    private BigDecimal calculateComboSubtotal(List<OrderComboRequest> combos, boolean isCustomerOrder) {
        if (combos == null || combos.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        List<Long> comboIds = combos.stream().map(OrderComboRequest::comboId).collect(Collectors.toList());

        java.util.Map<Long, Combo> combosMap = menuCatalogLookupService.findCombosByIdsWithItems(comboIds).stream()
                .collect(Collectors.toMap(Combo::getId, c -> c));

        for (OrderComboRequest comboRequest : combos) {
            Combo combo = Optional.ofNullable(combosMap.get(comboRequest.comboId()))
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.COMBO_NOT_FOUND, "Combo not found: " + comboRequest.comboId()));
            if (isCustomerOrder) {
                orderValidator.validateComboOrderable(combo, true);
            } else {
                orderValidator.validateComboOrderable(combo);
            }

            subtotal = subtotal.add(calculateLineTotal(combo.getPrice(), comboRequest.quantity()));
        }

        return subtotal;
    }

    private Map<Long, ItemOptionValue> loadSelectedOptionValues(List<OrderItemRequest> items) {
        Set<Long> selectedOptionValueIds = OrderOptionSupport.extractSelectedOptionValueIds(items);
        if (selectedOptionValueIds.isEmpty()) {
            return Map.of();
        }

        return menuCatalogLookupService.findItemOptionValuesByIterableIds(selectedOptionValueIds).stream()
                .collect(Collectors.toMap(ItemOptionValue::getId, value -> value));
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return rate;
    }
}
