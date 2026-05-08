package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.model.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

// StatsRepository - Cung cấp dữ liệu thống kê cho Dashboard.
public interface StatsRepository extends Repository<Order, Long> {

  // Projection Interface

  interface RevenueStats {
    String getBucket();

    BigDecimal getRevenue();

    Long getOrders();
  }

  interface EmpPerformanceStats {
    Long getId();

    String getFullName();

    Long getOrders();

    BigDecimal getRevenue();
  }

  interface OrderDetailStats {
    Long getId();

    Instant getPaymentTime();

    String getEmpName();

    BigDecimal getTotalAmount();
  }

  interface TopDishStats {
    Long getId();

    String getName();

    String getCategory();

    String getImg();

    Long getTotalQty();

    BigDecimal getTotalRevenue();
  }

  interface DishTrendStats {
    String getBucket();

    Long getTotalQty();
  }

  // Doanh thu theo ngày
  @Query(value = """
      SELECT DATE(o.payment_time) AS bucket,
             SUM(o.total_amount)  AS revenue,
             COUNT(o.id)          AS orders
      FROM orders o
      WHERE o.status = 'COMPLETED'
        AND o.payment_time BETWEEN :from AND :to
      GROUP BY DATE(o.payment_time)
      ORDER BY DATE(o.payment_time)
      """, nativeQuery = true)
  List<RevenueStats> revenueByDay(@Param("from") Instant from, @Param("to") Instant to);

  // Hiệu suất nhân viên
  @Query(value = """
      SELECT u.id AS id, u.full_name AS fullName,
             COUNT(o.id) AS orders,
             SUM(o.total_amount) AS revenue
      FROM orders o
      JOIN users u ON u.id = o.paid_by
      WHERE o.status = 'COMPLETED'
        AND o.payment_time BETWEEN :from AND :to
      GROUP BY u.id, u.full_name
      ORDER BY revenue DESC
      """, nativeQuery = true)
  List<EmpPerformanceStats> empPerformance(@Param("from") Instant from, @Param("to") Instant to);

  // Chi tiết đơn hàng trong khoảng thời gian
  @Query(value = """
      SELECT o.id AS id, o.payment_time AS paymentTime,
             COALESCE(u.full_name, '—') AS empName, o.total_amount AS totalAmount
      FROM orders o
      LEFT JOIN users u ON u.id = o.paid_by
      WHERE o.status = 'COMPLETED'
        AND o.payment_time BETWEEN :from AND :to
      ORDER BY o.payment_time DESC
      """, nativeQuery = true)
  List<OrderDetailStats> orderDetails(@Param("from") Instant from, @Param("to") Instant to);

  // Top món bán chạy nhất
  @Query(value = """
      SELECT mi.id AS id, mi.name AS name, c.name AS category, mi.img AS img,
             SUM(oi.quantity) AS totalQty,
             SUM(oi.quantity * oi.unit_price) AS totalRevenue
      FROM order_item oi
      JOIN orders o ON o.id = oi.order_id
      JOIN menu_item mi ON mi.id = oi.menu_item_id
      LEFT JOIN category c ON c.id = mi.cate_id
      WHERE o.status = 'COMPLETED'
        AND o.payment_time BETWEEN :from AND :to
      GROUP BY mi.id, mi.name, c.name, mi.img
      ORDER BY totalQty DESC
      """, nativeQuery = true)
  List<TopDishStats> topDishes(@Param("from") Instant from, @Param("to") Instant to);

  // Xu hướng bán món theo ngày
  @Query(value = """
      SELECT DATE(o.payment_time) AS bucket,
             SUM(oi.quantity) AS totalQty
      FROM order_item oi
      JOIN orders o ON o.id = oi.order_id
      WHERE o.status = 'COMPLETED'
        AND o.payment_time BETWEEN :from AND :to
      GROUP BY DATE(o.payment_time)
      ORDER BY DATE(o.payment_time)
      """, nativeQuery = true)
  List<DishTrendStats> dishTrendByDay(@Param("from") Instant from, @Param("to") Instant to);
}
