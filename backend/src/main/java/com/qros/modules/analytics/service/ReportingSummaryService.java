package com.qros.modules.analytics.service;

import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.shared.util.AppTime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReportingSummaryService {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void recordCompletedOrder(Order order) {
        if (order == null || order.getBusinessDate() == null) {
            return;
        }
        upsertRevenue(order);
        order.getOrderItems().forEach(item -> upsertItem(order.getBusinessDate(), item));
    }

    private void upsertRevenue(Order order) {
        entityManager.createNativeQuery("""
                INSERT INTO daily_revenue_summary
                    (business_date, total_orders, subtotal_amount, discount_amount, final_amount, paid_amount, updated_at)
                VALUES (:businessDate, 1, :subtotal, :discount, :finalAmount, :paid, :updatedAt)
                ON CONFLICT (business_date) DO UPDATE SET
                    total_orders = daily_revenue_summary.total_orders + 1,
                    subtotal_amount = daily_revenue_summary.subtotal_amount + EXCLUDED.subtotal_amount,
                    discount_amount = daily_revenue_summary.discount_amount + EXCLUDED.discount_amount,
                    final_amount = daily_revenue_summary.final_amount + EXCLUDED.final_amount,
                    paid_amount = daily_revenue_summary.paid_amount + EXCLUDED.paid_amount,
                    updated_at = EXCLUDED.updated_at
                """)
                .setParameter("businessDate", order.getBusinessDate())
                .setParameter("subtotal", safe(order.getSubtotalAmount()))
                .setParameter("discount", safe(order.getDiscountAmount()))
                .setParameter("finalAmount", safe(order.getFinalAmount()))
                .setParameter("paid", safe(order.getPaidAmount()))
                .setParameter("updatedAt", AppTime.now())
                .executeUpdate();
    }

    private void upsertItem(LocalDate businessDate, OrderItem item) {
        if (item == null || item.getItemType() == null || item.getItemNameSnapshot() == null) {
            return;
        }
        String itemKey = item.getItemType().name() + ":" + (item.getMenuItem() != null
                ? item.getMenuItem().getId()
                : item.getItemNameSnapshot());

        entityManager.createNativeQuery("""
                INSERT INTO daily_item_sales_summary
                    (business_date, item_key, menu_item_id, item_name_snapshot, item_type, quantity, revenue, updated_at)
                VALUES (:businessDate, :itemKey, :menuItemId, :name, :type, :quantity, :revenue, :updatedAt)
                ON CONFLICT (business_date, item_key) DO UPDATE SET
                    quantity = daily_item_sales_summary.quantity + EXCLUDED.quantity,
                    revenue = daily_item_sales_summary.revenue + EXCLUDED.revenue,
                    item_name_snapshot = EXCLUDED.item_name_snapshot,
                    updated_at = EXCLUDED.updated_at
                """)
                .setParameter("businessDate", businessDate)
                .setParameter("itemKey", itemKey)
                .setParameter("menuItemId", item.getMenuItem() != null ? item.getMenuItem().getId() : null)
                .setParameter("name", item.getItemNameSnapshot())
                .setParameter("type", item.getItemType().name())
                .setParameter("quantity", item.getQuantity())
                .setParameter("revenue", safe(item.getLineTotal()))
                .setParameter("updatedAt", AppTime.now())
                .executeUpdate();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
