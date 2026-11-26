package com.qros.modules.analytics.repository;

import com.qros.modules.analytics.dto.projection.DashboardSummaryProjection;
import com.qros.modules.analytics.dto.projection.DayOfWeekTrafficProjection;
import com.qros.modules.analytics.dto.projection.HourlyTrafficProjection;
import com.qros.modules.analytics.dto.projection.MenuItemDemandProjection;
import com.qros.modules.analytics.dto.projection.OrderDetailProjection;
import com.qros.modules.analytics.dto.projection.OrderFilterSummaryProjection;
import com.qros.modules.analytics.dto.projection.OrderSummaryProjection;
import com.qros.modules.analytics.dto.projection.RecentOrderProjection;
import com.qros.modules.analytics.dto.projection.RevenuePointProjection;
import com.qros.modules.analytics.dto.projection.SalesTrendPointProjection;
import com.qros.modules.analytics.dto.projection.TableSummaryProjection;
import com.qros.modules.analytics.dto.projection.TodayRevenueSummaryProjection;
import com.qros.modules.analytics.dto.projection.TopSellingItemProjection;
import com.qros.modules.analytics.dto.projection.UserPerformanceProjection;
import com.qros.modules.order.model.Order;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AnalyticsQueryRepository extends Repository<Order, Long> {

    @Query(value = """
      SELECT COALESCE(SUM(summary.final_amount), 0) AS totalRevenue,
             CAST(COALESCE(SUM(summary.total_orders), 0) AS BIGINT) AS totalOrders,
             CAST(COALESCE((
                 SELECT SUM(item_summary.quantity)
                 FROM daily_item_sales_summary item_summary
                 WHERE item_summary.business_date >= :from
                   AND item_summary.business_date < :toExclusive
             ), 0) AS BIGINT) AS totalItemsSold,
             CASE
                 WHEN COALESCE(SUM(summary.total_orders), 0) = 0 THEN 0
                 ELSE COALESCE(SUM(summary.final_amount), 0)
                      / COALESCE(SUM(summary.total_orders), 0)
             END AS averageOrderValue
      FROM daily_revenue_summary summary
      WHERE summary.business_date >= :from
        AND summary.business_date < :toExclusive
      """, nativeQuery = true)
    DashboardSummaryProjection dashboardSummary(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    @Query(value = """
      SELECT TO_CHAR(summary.business_date, 'YYYY-MM-DD') AS date,
             summary.final_amount AS revenue,
             summary.total_orders AS orderCount
      FROM daily_revenue_summary summary
      WHERE summary.business_date >= :from
        AND summary.business_date < :toExclusive
        AND summary.total_orders > 0
      ORDER BY summary.business_date ASC
      """, nativeQuery = true)
    List<RevenuePointProjection> revenueByDay(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    @Query(value = """
      SELECT summary.user_id AS userId,
             (ARRAY_AGG(summary.full_name_snapshot ORDER BY summary.business_date DESC))[1] AS fullName,
             (ARRAY_AGG(summary.avatar_url_snapshot ORDER BY summary.business_date DESC))[1] AS avatarUrl,
             CAST(SUM(summary.order_count) AS BIGINT) AS orderCount,
             COALESCE(SUM(summary.revenue), 0) AS revenue
      FROM daily_staff_sales_summary summary
      WHERE summary.business_date >= :from
        AND summary.business_date < :toExclusive
      GROUP BY summary.staff_key, summary.user_id
      ORDER BY revenue DESC
      LIMIT :limit
      """, nativeQuery = true)
    List<UserPerformanceProjection> userPerformance(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive, @Param("limit") int limit);

    @Query(value = """
      SELECT fact.order_id AS orderId,
             fact.payment_time AS paymentTime,
             COALESCE(fact.paid_by_name_snapshot, '—') AS userName,
             fact.final_amount AS finalAmount,
             fact.table_number_snapshot AS tableNumber
      FROM order_reporting_fact fact
      WHERE fact.status = 'COMPLETED'
        AND fact.payment_status = 'PAID'
        AND fact.business_date >= :from
        AND fact.business_date < :toExclusive
      ORDER BY fact.payment_time DESC
      """, countQuery = """
      SELECT COUNT(fact.order_id)
      FROM order_reporting_fact fact
      WHERE fact.status = 'COMPLETED'
        AND fact.payment_status = 'PAID'
        AND fact.business_date >= :from
        AND fact.business_date < :toExclusive
      """, nativeQuery = true)
    Page<OrderDetailProjection> orderDetails(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive, Pageable pageable);

    @Query(value = """
      SELECT MAX(summary.menu_item_id) AS menuItemId,
             (ARRAY_AGG(summary.item_name_snapshot ORDER BY summary.business_date DESC))[1] AS itemName,
             (ARRAY_AGG(summary.category_name_snapshot ORDER BY summary.business_date DESC))[1] AS categoryName,
             (ARRAY_AGG(summary.image_url_snapshot ORDER BY summary.business_date DESC))[1] AS imageUrl,
             CAST(SUM(summary.quantity) AS BIGINT) AS quantitySold,
             COALESCE(SUM(summary.revenue), 0) AS revenue
      FROM daily_item_sales_summary summary
      WHERE summary.business_date >= :from
        AND summary.business_date < :toExclusive
      GROUP BY summary.item_key
      ORDER BY quantitySold DESC, revenue DESC
      LIMIT :limit
      """, nativeQuery = true)
    List<TopSellingItemProjection> topSellingItems(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive, @Param("limit") int limit);

    @Query(value = """
      SELECT TO_CHAR(summary.business_date, 'YYYY-MM-DD') AS date,
             CAST(SUM(summary.quantity) AS BIGINT) AS quantitySold
      FROM daily_item_sales_summary summary
      WHERE summary.business_date >= :from
        AND summary.business_date < :toExclusive
      GROUP BY summary.business_date
      ORDER BY summary.business_date ASC
      """, nativeQuery = true)
    List<SalesTrendPointProjection> salesTrendByDay(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    @Query(value = """
      SELECT COALESCE(SUM(summary.total_orders), 0) AS totalOrders,
             COALESCE(SUM(summary.completed_orders), 0) AS completedOrders
      FROM daily_order_activity_summary summary
      WHERE summary.activity_date >= :from
        AND summary.activity_date < :toExclusive
      """, nativeQuery = true)
    OrderSummaryProjection orderSummary(@Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    @Query(value = """
      SELECT COUNT(t.id) AS totalTables,
             COUNT(t.id) FILTER (WHERE t.status <> 'AVAILABLE') AS occupiedTables
      FROM tables t
      WHERE t.is_deleted = false
      """, nativeQuery = true)
    TableSummaryProjection tableSummary();

    @Query(value = """
      SELECT COALESCE(SUM(summary.final_amount), 0) AS todayRevenue,
             CASE
                 WHEN COALESCE(SUM(summary.total_orders), 0) = 0 THEN 0
                 ELSE COALESCE(SUM(summary.final_amount), 0)
                      / COALESCE(SUM(summary.total_orders), 0)
             END AS todayAvgOrderValue
      FROM daily_revenue_summary summary
      WHERE summary.business_date = :today
      """, nativeQuery = true)
    TodayRevenueSummaryProjection todayRevenueSummary(@Param("today") LocalDate today);

    @Query(value = """
      SELECT fact.order_id AS orderId,
             fact.status AS status,
             fact.final_amount AS finalAmount,
             fact.created_at AS createdAt,
             fact.payment_time AS paymentTime,
             fact.table_number_snapshot AS tableNumber
      FROM order_reporting_fact fact
      WHERE fact.created_at >= :from
        AND fact.created_at < :toExclusive
      ORDER BY fact.created_at DESC
      LIMIT :limit
      """, nativeQuery = true)
    List<RecentOrderProjection> recentOrders(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("limit") int limit);

    @Query(value = """
      SELECT COUNT(fact.order_id) AS totalOrders,
             COALESCE(SUM(fact.final_amount) FILTER (WHERE fact.status = 'COMPLETED' AND fact.payment_status = 'PAID'), 0) AS totalRevenue
      FROM order_reporting_fact fact
      WHERE (CAST(:status AS VARCHAR) IS NULL OR fact.status = CAST(:status AS VARCHAR))
        AND (CAST(:from AS DATE) IS NULL OR fact.created_date >= CAST(:from AS DATE))
        AND (
            CAST(:toExclusive AS DATE) IS NULL
            OR fact.created_date < CAST(:toExclusive AS DATE)
        )
        AND (CAST(:orderId AS BIGINT) IS NULL OR fact.order_id = CAST(:orderId AS BIGINT))
        AND (
            CAST(:tableNumber AS VARCHAR) IS NULL
            OR LOWER(COALESCE(fact.table_number_snapshot, ''))
                LIKE LOWER(CONCAT('%', CAST(:tableNumber AS VARCHAR), '%')) ESCAPE '\\'
        )
      """, nativeQuery = true)
    OrderFilterSummaryProjection orderFilterSummary(
            @Param("status") String status,
            @Param("from") LocalDate from,
            @Param("toExclusive") LocalDate toExclusive,
            @Param("orderId") Long orderId,
            @Param("tableNumber") String tableNumber);

    @Query(value = """
      SELECT expanded.menu_item_id AS menuItemId,
             CAST(SUM(expanded.qty) AS BIGINT) AS quantitySold
      FROM (
          SELECT oi.menu_item_id AS menu_item_id,
                 oi.quantity AS qty
          FROM order_item oi
          JOIN orders o ON o.id = oi.order_id
          WHERE o.is_deleted = false
            AND oi.is_deleted = false
            AND o.status = 'COMPLETED'
            AND o.payment_status = 'PAID'
            AND oi.status <> 'CANCELLED'
            AND oi.menu_item_id IS NOT NULL
            AND o.business_date >= :from
            AND o.business_date < :toExclusive
          UNION ALL
          SELECT ci.menu_item_id AS menu_item_id,
                 oi.quantity * ci.quantity AS qty
          FROM order_item oi
          JOIN orders o ON o.id = oi.order_id
          JOIN combo_items ci ON ci.combo_id = oi.combo_id AND ci.is_deleted = false
          WHERE o.is_deleted = false
            AND oi.is_deleted = false
            AND o.status = 'COMPLETED'
            AND o.payment_status = 'PAID'
            AND oi.status <> 'CANCELLED'
            AND oi.combo_id IS NOT NULL
            AND o.business_date >= :from
            AND o.business_date < :toExclusive
      ) expanded
      GROUP BY expanded.menu_item_id
      """, nativeQuery = true)
    List<MenuItemDemandProjection> menuItemDemandIncludingCombos(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    @Query(value = """
      SELECT CAST(EXTRACT(HOUR FROM fact.created_at) AS INTEGER) AS hourOfDay,
             CAST(COUNT(fact.order_id) AS BIGINT) AS orderCount,
             COALESCE(SUM(fact.final_amount), 0) AS revenue
      FROM order_reporting_fact fact
      WHERE fact.status = 'COMPLETED'
        AND fact.payment_status = 'PAID'
        AND fact.business_date >= :from
        AND fact.business_date < :toExclusive
        AND fact.created_at IS NOT NULL
      GROUP BY CAST(EXTRACT(HOUR FROM fact.created_at) AS INTEGER)
      ORDER BY hourOfDay ASC
      """, nativeQuery = true)
    List<HourlyTrafficProjection> hourlyOrderTraffic(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    @Query(value = """
      SELECT CAST(EXTRACT(ISODOW FROM fact.created_at) AS INTEGER) AS isoDayOfWeek,
             CAST(COUNT(fact.order_id) AS BIGINT) AS orderCount,
             COALESCE(SUM(fact.final_amount), 0) AS revenue
      FROM order_reporting_fact fact
      WHERE fact.status = 'COMPLETED'
        AND fact.payment_status = 'PAID'
        AND fact.business_date >= :from
        AND fact.business_date < :toExclusive
        AND fact.created_at IS NOT NULL
      GROUP BY CAST(EXTRACT(ISODOW FROM fact.created_at) AS INTEGER)
      ORDER BY isoDayOfWeek ASC
      """, nativeQuery = true)
    List<DayOfWeekTrafficProjection> dayOfWeekOrderTraffic(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);
}
