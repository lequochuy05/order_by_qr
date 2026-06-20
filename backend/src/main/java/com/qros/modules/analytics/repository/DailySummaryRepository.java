package com.qros.modules.analytics.repository;

import com.qros.modules.order.model.Order;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DailySummaryRepository extends Repository<Order, Long> {

    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
            INSERT INTO daily_revenue_summary (
                business_date,
                total_orders,
                subtotal_amount,
                discount_amount,
                final_amount,
                paid_amount,
                updated_at
            )
            VALUES (:businessDate, 0, 0, 0, 0, 0, CURRENT_TIMESTAMP)
            ON CONFLICT (business_date) DO NOTHING
            """,
            nativeQuery = true)
    void ensureRevenueSummaryRow(@Param("businessDate") LocalDate businessDate);

    @Query(
            value =
                    """
            SELECT business_date
            FROM daily_revenue_summary
            WHERE business_date = :businessDate
            FOR UPDATE
            """,
            nativeQuery = true)
    LocalDate lockRevenueSummaryRow(@Param("businessDate") LocalDate businessDate);

    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
            UPDATE daily_revenue_summary summary
            SET total_orders = source.total_orders,
                subtotal_amount = source.subtotal_amount,
                discount_amount = source.discount_amount,
                final_amount = source.final_amount,
                paid_amount = source.paid_amount,
                updated_at = CURRENT_TIMESTAMP
            FROM (
                SELECT COUNT(*) AS total_orders,
                       COALESCE(SUM(fact.subtotal_amount), 0) AS subtotal_amount,
                       COALESCE(SUM(fact.discount_amount), 0) AS discount_amount,
                       COALESCE(SUM(fact.final_amount), 0) AS final_amount,
                       COALESCE(SUM(fact.paid_amount), 0) AS paid_amount
                FROM order_reporting_fact fact
                WHERE fact.business_date = :businessDate
                  AND fact.status = 'COMPLETED'
                  AND fact.payment_status = 'PAID'
            ) source
            WHERE summary.business_date = :businessDate
            """,
            nativeQuery = true)
    void refreshRevenueSummary(@Param("businessDate") LocalDate businessDate);

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM daily_staff_sales_summary WHERE business_date = :businessDate", nativeQuery = true)
    void deleteStaffSalesSummary(@Param("businessDate") LocalDate businessDate);

    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
            INSERT INTO daily_staff_sales_summary (
                business_date,
                staff_key,
                user_id,
                full_name_snapshot,
                avatar_url_snapshot,
                order_count,
                revenue,
                updated_at
            )
            SELECT :businessDate,
                   'USER:' || fact.paid_by,
                   fact.paid_by,
                   MAX(COALESCE(fact.paid_by_name_snapshot, 'Staff')),
                   MAX(fact.paid_by_avatar_url_snapshot),
                   COUNT(*),
                   COALESCE(SUM(fact.final_amount), 0),
                   CURRENT_TIMESTAMP
            FROM order_reporting_fact fact
            WHERE fact.business_date = :businessDate
              AND fact.status = 'COMPLETED'
              AND fact.payment_status = 'PAID'
              AND fact.paid_by IS NOT NULL
            GROUP BY fact.paid_by
            """,
            nativeQuery = true)
    void refreshStaffSalesSummary(@Param("businessDate") LocalDate businessDate);

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM daily_item_sales_summary WHERE business_date = :businessDate", nativeQuery = true)
    void deleteItemSalesSummary(@Param("businessDate") LocalDate businessDate);

    @Modifying(flushAutomatically = true)
    @Query(
            value =
                    """
            WITH item_sales AS (
                SELECT CASE
                           WHEN oi.item_type = 'MENU_ITEM' AND oi.menu_item_id IS NOT NULL
                               THEN 'MENU_ITEM:' || oi.menu_item_id
                           WHEN oi.item_type = 'COMBO' AND oi.combo_id IS NOT NULL
                               THEN 'COMBO:' || oi.combo_id
                           ELSE oi.item_type || ':SNAPSHOT:' || MD5(COALESCE(oi.item_name_snapshot, ''))
                       END AS item_key,
                       CASE WHEN oi.item_type = 'MENU_ITEM' THEN oi.menu_item_id END AS menu_item_id,
                       CASE WHEN oi.item_type = 'COMBO' THEN oi.combo_id END AS combo_id,
                       MAX(oi.item_name_snapshot) AS item_name_snapshot,
                       MAX(CASE
                               WHEN oi.item_type = 'MENU_ITEM'
                                   THEN COALESCE(category.name, 'Uncategorized')
                               ELSE 'Combo'
                           END) AS category_name_snapshot,
                       MAX(CASE WHEN oi.item_type = 'MENU_ITEM' THEN menu_item.img END) AS image_url_snapshot,
                       oi.item_type,
                       SUM(oi.quantity)::BIGINT AS quantity,
                       COALESCE(SUM(COALESCE(oi.line_total, oi.quantity * oi.unit_price)), 0) AS revenue
                FROM order_item oi
                JOIN order_reporting_fact fact ON fact.order_id = oi.order_id
                LEFT JOIN menu_item ON menu_item.id = oi.menu_item_id
                LEFT JOIN category ON category.id = menu_item.cate_id
                WHERE fact.business_date = :businessDate
                  AND fact.status = 'COMPLETED'
                  AND fact.payment_status = 'PAID'
                  AND oi.is_deleted = false
                  AND oi.status <> 'CANCELLED'
                GROUP BY CASE
                             WHEN oi.item_type = 'MENU_ITEM' AND oi.menu_item_id IS NOT NULL
                                 THEN 'MENU_ITEM:' || oi.menu_item_id
                             WHEN oi.item_type = 'COMBO' AND oi.combo_id IS NOT NULL
                                 THEN 'COMBO:' || oi.combo_id
                             ELSE oi.item_type || ':SNAPSHOT:' || MD5(COALESCE(oi.item_name_snapshot, ''))
                         END,
                         CASE WHEN oi.item_type = 'MENU_ITEM' THEN oi.menu_item_id END,
                         CASE WHEN oi.item_type = 'COMBO' THEN oi.combo_id END,
                         oi.item_type
            )
            INSERT INTO daily_item_sales_summary (
                business_date,
                item_key,
                menu_item_id,
                combo_id,
                item_name_snapshot,
                category_name_snapshot,
                image_url_snapshot,
                item_type,
                quantity,
                revenue,
                updated_at
            )
            SELECT :businessDate,
                   item_key,
                   menu_item_id,
                   combo_id,
                   item_name_snapshot,
                   category_name_snapshot,
                   image_url_snapshot,
                   item_type,
                   quantity,
                   revenue,
                   CURRENT_TIMESTAMP
            FROM item_sales
            """,
            nativeQuery = true)
    void refreshItemSalesSummary(@Param("businessDate") LocalDate businessDate);
}
