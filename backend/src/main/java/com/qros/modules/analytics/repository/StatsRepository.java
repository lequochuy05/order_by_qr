package com.qros.modules.analytics.repository;

import com.qros.modules.order.model.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

/**
 * StatsRepository - Provides high-performance analytical and statistical data for the dashboard.
 * Uses native SQL queries and projection interfaces for efficient reporting.
 */
public interface StatsRepository extends Repository<Order, Long> {

  /**
   * Projection interface for revenue statistics grouped by time buckets.
   */
  interface RevenueStats {
    String getBucket();
    BigDecimal getRevenue();
    Long getOrders();
  }

  /**
   * Projection interface for evaluating employee performance.
   */
  interface EmpPerformanceStats {
    Long getId();
    String getFullName();
    String getAvatarUrl();
    Long getOrders();
    BigDecimal getRevenue();
  }

  /**
   * Projection interface for detailed order summary in reports.
   */
  interface OrderDetailStats {
    Long getId();
    LocalDateTime getPaymentTime();
    String getEmpName();
    BigDecimal getFinalAmount();
    BigDecimal getTotalAmount();
    String getTableNumber();
  }

  /**
   * Projection interface for identifying top-selling menu items.
   */
  interface TopDishStats {
    Long getId();
    String getName();
    String getCategory();
    String getImg();
    Long getTotalQty();
    BigDecimal getTotalRevenue();
  }

  /**
   * Projection interface for tracking item sales trends over time.
   */
  interface DishTrendStats {
    String getBucket();
    Long getTotalQty();
  }

  /**
   * Calculates total revenue and order count grouped by day within a time range.
   * 
   * @param from Start timestamp
   * @param to End timestamp
   * @return List of daily revenue statistics
   */
  @Query(value = """
      SELECT o.business_date AS bucket,
             SUM(o.final_amount) AS revenue,
             COUNT(o.id)          AS orders
      FROM orders o
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND o.business_date BETWEEN CAST(:from AS date) AND CAST(:to AS date)
      GROUP BY o.business_date
      ORDER BY o.business_date
      """, nativeQuery = true)
  List<RevenueStats> revenueByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  /**
   * Aggregates completed orders and total revenue handled by each employee.
   * 
   * @param from Start timestamp
   * @param to End timestamp
   * @return List of employee performance metrics
   */
  @Query(value = """
      SELECT u.id AS id, u.full_name AS fullName, u.avatar_url AS avatarUrl,
             COUNT(o.id) AS orders,
             SUM(o.final_amount) AS revenue
      FROM orders o
      JOIN users u ON u.id = o.paid_by
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND o.business_date BETWEEN CAST(:from AS date) AND CAST(:to AS date)
      GROUP BY u.id, u.full_name, u.avatar_url
      ORDER BY revenue DESC
      """, nativeQuery = true)
  List<EmpPerformanceStats> empPerformance(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  /**
   * Retrieves a list of completed orders with basic details for reporting.
   * 
   * @param from Start timestamp
   * @param to End timestamp
   * @return List of order details
   */
  @Query(value = """
      SELECT o.id AS id, o.payment_time AS paymentTime,
             COALESCE(u.full_name, '—') AS empName,
             o.final_amount AS finalAmount,
             o.final_amount AS totalAmount,
             dt.table_number AS tableNumber
      FROM orders o
      LEFT JOIN users u ON u.id = o.paid_by
      LEFT JOIN tables dt ON dt.id = o.table_id
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND o.business_date BETWEEN CAST(:from AS date) AND CAST(:to AS date)
      ORDER BY o.payment_time DESC
      """, nativeQuery = true)
  List<OrderDetailStats> orderDetails(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  /**
   * Identifies the most popular menu items based on quantity sold and total revenue.
   * 
   * @param from Start timestamp
   * @param to End timestamp
   * @return List of top-selling dishes
   */
  @Query(value = """
      SELECT mi.id AS id, oi.item_name_snapshot AS name, c.name AS category, mi.img AS img,
             SUM(oi.quantity) AS totalQty,
             SUM(COALESCE(oi.line_total, oi.quantity * oi.unit_price)) AS totalRevenue
      FROM order_item oi
      JOIN orders o ON o.id = oi.order_id
      LEFT JOIN menu_item mi ON mi.id = oi.menu_item_id AND mi.is_deleted = false
      LEFT JOIN category c ON c.id = mi.cate_id
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND oi.is_deleted = false
        AND o.business_date BETWEEN CAST(:from AS date) AND CAST(:to AS date)
      GROUP BY mi.id, oi.item_name_snapshot, c.name, mi.img
      ORDER BY totalQty DESC
      """, nativeQuery = true)
  List<TopDishStats> topDishes(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  /**
   * Tracks daily sales trends of individual dishes across the menu.
   * 
   * @param from Start timestamp
   * @param to End timestamp
   * @return List of quantity sold per day
   */
  @Query(value = """
      SELECT o.business_date AS bucket,
             SUM(oi.quantity) AS totalQty
      FROM order_item oi
      JOIN orders o ON o.id = oi.order_id
      WHERE o.status = 'COMPLETED'
        AND o.is_deleted = false
        AND oi.is_deleted = false
        AND o.business_date BETWEEN CAST(:from AS date) AND CAST(:to AS date)
      GROUP BY o.business_date
      ORDER BY o.business_date
      """, nativeQuery = true)
  List<DishTrendStats> dishTrendByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
