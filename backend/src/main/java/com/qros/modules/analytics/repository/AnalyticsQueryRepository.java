package com.qros.modules.analytics.repository;

import com.qros.modules.analytics.repository.projection.DashboardSummaryProjection;
import com.qros.modules.analytics.repository.projection.OrderDetailProjection;
import com.qros.modules.analytics.repository.projection.OrderSummaryProjection;
import com.qros.modules.analytics.repository.projection.RecentOrderProjection;
import com.qros.modules.analytics.repository.projection.RevenuePointProjection;
import com.qros.modules.analytics.repository.projection.SalesTrendPointProjection;
import com.qros.modules.analytics.repository.projection.TableSummaryProjection;
import com.qros.modules.analytics.repository.projection.TopSellingItemProjection;
import com.qros.modules.analytics.repository.projection.UserPerformanceProjection;
import com.qros.modules.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsQueryRepository extends Repository<Order, Long> {

  @Query(value = """
      SELECT COALESCE(SUM(o.final_amount), 0) AS totalRevenue,
             COUNT(o.id) AS totalOrders,
             CAST(COALESCE((
                 SELECT SUM(oi.quantity)
                 FROM order_item oi
                 JOIN orders oo ON oo.id = oi.order_id
                 WHERE oo.status = 'COMPLETED'
                   AND oo.is_deleted = false
                   AND oi.is_deleted = false
                   AND oo.business_date >= :from
                   AND oo.business_date < :toExclusive
             ), 0) AS BIGINT) AS totalItemsSold,
             CASE
                 WHEN COUNT(o.id) = 0 THEN 0
                 ELSE COALESCE(SUM(o.final_amount), 0) / COUNT(o.id)
             END AS averageOrderValue
      FROM orders o
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND o.business_date >= :from
        AND o.business_date < :toExclusive
      """, nativeQuery = true)
  DashboardSummaryProjection dashboardSummary(
      @Param("from") LocalDate from,
      @Param("toExclusive") LocalDate toExclusive);

  @Query(value = """
      SELECT TO_CHAR(o.business_date, 'YYYY-MM-DD') AS date,
             COALESCE(SUM(o.final_amount), 0) AS revenue,
             COUNT(o.id) AS orderCount
      FROM orders o
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND o.business_date >= :from
        AND o.business_date < :toExclusive
      GROUP BY o.business_date
      ORDER BY o.business_date ASC
      """, nativeQuery = true)
  List<RevenuePointProjection> revenueByDay(
      @Param("from") LocalDate from,
      @Param("toExclusive") LocalDate toExclusive);

  @Query(value = """
      SELECT u.id AS userId,
             u.full_name AS fullName,
             u.avatar_url AS avatarUrl,
             COUNT(o.id) AS orderCount,
             COALESCE(SUM(o.final_amount), 0) AS revenue
      FROM orders o
      JOIN users u ON u.id = o.paid_by AND u.is_deleted = false
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND o.business_date >= :from
        AND o.business_date < :toExclusive
      GROUP BY u.id, u.full_name, u.avatar_url
      ORDER BY revenue DESC
      LIMIT :limit
      """, nativeQuery = true)
  List<UserPerformanceProjection> userPerformance(
      @Param("from") LocalDate from,
      @Param("toExclusive") LocalDate toExclusive,
      @Param("limit") int limit);

  @Query(value = """
      SELECT o.id AS orderId,
             o.payment_time AS paymentTime,
             COALESCE(u.full_name, '—') AS userName,
             o.final_amount AS finalAmount,
             dt.table_number AS tableNumber
      FROM orders o
      LEFT JOIN users u ON u.id = o.paid_by AND u.is_deleted = false
      LEFT JOIN tables dt ON dt.id = o.table_id AND dt.is_deleted = false
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND o.business_date >= :from
        AND o.business_date < :toExclusive
      ORDER BY o.payment_time DESC
      """, countQuery = """
      SELECT COUNT(o.id)
      FROM orders o
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND o.business_date >= :from
        AND o.business_date < :toExclusive
      """, nativeQuery = true)
  Page<OrderDetailProjection> orderDetails(
      @Param("from") LocalDate from,
      @Param("toExclusive") LocalDate toExclusive,
      Pageable pageable);

  @Query(value = """
      SELECT mi.id AS menuItemId,
             oi.item_name_snapshot AS itemName,
             c.name AS categoryName,
             mi.img AS imageUrl,
             CAST(SUM(oi.quantity) AS BIGINT) AS quantitySold,
             COALESCE(SUM(COALESCE(oi.line_total, oi.quantity * oi.unit_price)), 0) AS revenue
      FROM order_item oi
      JOIN orders o ON o.id = oi.order_id
      LEFT JOIN menu_item mi ON mi.id = oi.menu_item_id AND mi.is_deleted = false
      LEFT JOIN category c ON c.id = mi.cate_id AND c.is_deleted = false
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND oi.is_deleted = false
        AND o.business_date >= :from
        AND o.business_date < :toExclusive
      GROUP BY mi.id, oi.item_name_snapshot, c.name, mi.img
      ORDER BY quantitySold DESC, revenue DESC
      LIMIT :limit
      """, nativeQuery = true)
  List<TopSellingItemProjection> topSellingItems(
      @Param("from") LocalDate from,
      @Param("toExclusive") LocalDate toExclusive,
      @Param("limit") int limit);

  @Query(value = """
      SELECT TO_CHAR(o.business_date, 'YYYY-MM-DD') AS date,
             CAST(SUM(oi.quantity) AS BIGINT) AS quantitySold
      FROM order_item oi
      JOIN orders o ON o.id = oi.order_id
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND oi.is_deleted = false
        AND o.business_date >= :from
        AND o.business_date < :toExclusive
      GROUP BY o.business_date
      ORDER BY o.business_date ASC
      """, nativeQuery = true)
  List<SalesTrendPointProjection> salesTrendByDay(
      @Param("from") LocalDate from,
      @Param("toExclusive") LocalDate toExclusive);

  @Query(value = """
      SELECT COUNT(o.id) AS totalOrders,
             COUNT(o.id) FILTER (WHERE o.status = 'COMPLETED') AS completedOrders
      FROM orders o
      WHERE o.is_deleted = false
        AND o.created_at >= :from
        AND o.created_at < :toExclusive
      """, nativeQuery = true)
  OrderSummaryProjection orderSummary(
      @Param("from") LocalDateTime from,
      @Param("toExclusive") LocalDateTime toExclusive);

  @Query(value = """
      SELECT COUNT(t.id) AS totalTables,
             COUNT(t.id) FILTER (WHERE t.status <> 'AVAILABLE') AS occupiedTables
      FROM tables t
      WHERE t.is_deleted = false
      """, nativeQuery = true)
  TableSummaryProjection tableSummary();

  @Query(value = """
      SELECT o.id AS orderId,
             o.status AS status,
             o.final_amount AS finalAmount,
             o.created_at AS createdAt,
             o.payment_time AS paymentTime,
             dt.table_number AS tableNumber
      FROM orders o
      LEFT JOIN tables dt ON dt.id = o.table_id AND dt.is_deleted = false
      WHERE o.is_deleted = false
        AND o.created_at >= :from
        AND o.created_at < :toExclusive
      ORDER BY o.created_at DESC
      LIMIT :limit
      """, nativeQuery = true)
  List<RecentOrderProjection> recentOrders(
      @Param("from") LocalDateTime from,
      @Param("toExclusive") LocalDateTime toExclusive,
      @Param("limit") int limit);
}
