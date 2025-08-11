// /repository/StatsRepository.java
package com.sacmauquan.qrordering.repository;

import com.sacmauquan.qrordering.dto.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface StatsRepository extends Repository<com.sacmauquan.qrordering.model.Order, Long> {

  // ---- Doanh thu theo ngày
 @Query(value = """
      SELECT DATE(o.payment_time) AS bucket,
             SUM(o.total_amount)  AS revenue,
             COUNT(o.id)          AS orders
      FROM orders o
      WHERE o.status = 'PAID'
        AND o.payment_time BETWEEN :from AND :to
      GROUP BY DATE(o.payment_time)
      ORDER BY DATE(o.payment_time)
      """, nativeQuery = true)
  List<Object[]> revenueByDay(@Param("from") Instant from, @Param("to") Instant to);

  // ---- Hiệu suất nhân viên
  @Query(value = """
      SELECT u.id, u.full_name,
             COUNT(o.id) AS orders,
             SUM(o.total_amount) AS revenue
      FROM orders o
      JOIN users u ON u.id = o.paid_by
      WHERE o.status='PAID'
        AND o.payment_time BETWEEN :from AND :to
      GROUP BY u.id, u.full_name
      ORDER BY revenue DESC
      """, nativeQuery = true)
  List<Object[]> empPerformance(@Param("from") Instant from, @Param("to") Instant to);

  // ---- Chi tiết đơn hàng trong khoảng thời gian
  @Query(value = """
      SELECT o.id, o.payment_time, COALESCE(u.full_name, '—') AS emp, o.total_amount
      FROM orders o
      LEFT JOIN users u ON u.id = o.paid_by
      WHERE o.status='PAID'
        AND o.payment_time BETWEEN :from AND :to
      ORDER BY o.payment_time DESC
      """, nativeQuery = true)
  List<Object[]> orderDetails(@Param("from") Instant from, @Param("to") Instant to);
}
