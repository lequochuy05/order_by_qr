package com.qros.modules.analytics.repository;

import com.qros.modules.analytics.repository.projection.DashboardSummaryProjection;
import com.qros.modules.analytics.repository.projection.OrderDetailProjection;
import com.qros.modules.analytics.repository.projection.OrderFilterSummaryProjection;
import com.qros.modules.analytics.repository.projection.OrderSummaryProjection;
import com.qros.modules.analytics.repository.projection.RecentOrderProjection;
import com.qros.modules.analytics.repository.projection.RevenuePointProjection;
import com.qros.modules.analytics.repository.projection.SalesTrendPointProjection;
import com.qros.modules.analytics.repository.projection.TableSummaryProjection;
import com.qros.modules.analytics.repository.projection.TopSellingItemProjection;
import com.qros.modules.analytics.repository.projection.UserPerformanceProjection;
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

    @Query(
            value =
                    """
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
      """,
            nativeQuery = true)
    DashboardSummaryProjection dashboardSummary(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    @Query(
            value =
                    """
      SELECT TO_CHAR(summary.business_date, 'YYYY-MM-DD') AS date,
             summary.final_amount AS revenue,
             summary.total_orders AS orderCount
      FROM daily_revenue_summary summary
      WHERE summary.business_date >= :from
        AND summary.business_date < :toExclusive
        AND summary.total_orders > 0
      ORDER BY summary.business_date ASC
      """,
            nativeQuery = true)
    List<RevenuePointProjection> revenueByDay(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    @Query(
            value =
                    """
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
      """,
            nativeQuery = true)
    List<UserPerformanceProjection> userPerformance(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive, @Param("limit") int limit);

    @Query(
            value =
                    """
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
      """,
            countQuery =
                    """
      SELECT COUNT(fact.order_id)
      FROM order_reporting_fact fact
      WHERE fact.status = 'COMPLETED'
        AND fact.payment_status = 'PAID'
        AND fact.business_date >= :from
        AND fact.business_date < :toExclusive
      """,
            nativeQuery = true)
    Page<OrderDetailProjection> orderDetails(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive, Pageable pageable);

    @Query(
            value =
                    """
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
      """,
            nativeQuery = true)
    List<TopSellingItemProjection> topSellingItems(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive, @Param("limit") int limit);

    @Query(
            value =
                    """
      SELECT TO_CHAR(summary.business_date, 'YYYY-MM-DD') AS date,
             CAST(SUM(summary.quantity) AS BIGINT) AS quantitySold
      FROM daily_item_sales_summary summary
      WHERE summary.business_date >= :from
        AND summary.business_date < :toExclusive
      GROUP BY summary.business_date
      ORDER BY summary.business_date ASC
      """,
            nativeQuery = true)
    List<SalesTrendPointProjection> salesTrendByDay(
            @Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    @Query(
            value =
                    """
      SELECT COALESCE(SUM(summary.total_orders), 0) AS totalOrders,
             COALESCE(SUM(summary.completed_orders), 0) AS completedOrders
      FROM daily_order_activity_summary summary
      WHERE summary.activity_date >= :from
        AND summary.activity_date < :toExclusive
      """,
            nativeQuery = true)
    OrderSummaryProjection orderSummary(@Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);

    @Query(
            value =
                    """
      SELECT COUNT(t.id) AS totalTables,
             COUNT(t.id) FILTER (WHERE t.status <> 'AVAILABLE') AS occupiedTables
      FROM tables t
      WHERE t.is_deleted = false
      """,
            nativeQuery = true)
    TableSummaryProjection tableSummary();

    @Query(
            value =
                    """
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
      """,
            nativeQuery = true)
    List<RecentOrderProjection> recentOrders(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("limit") int limit);

    @Query(
            value =
                    """
      SELECT COUNT(fact.order_id) AS totalOrders,
             COALESCE(SUM(fact.final_amount), 0) AS totalRevenue
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
                LIKE LOWER(CONCAT('%', CAST(:tableNumber AS VARCHAR), '%'))
        )
      """,
            nativeQuery = true)
    OrderFilterSummaryProjection orderFilterSummary(
            @Param("status") String status,
            @Param("from") LocalDate from,
            @Param("toExclusive") LocalDate toExclusive,
            @Param("orderId") Long orderId,
            @Param("tableNumber") String tableNumber);
}
