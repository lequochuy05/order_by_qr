// service/StatsService.java
package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service @RequiredArgsConstructor
public class StatsService {
  private final StatsRepository repo;

   public List<RevenueDto> revenue(Instant from, Instant to) {
    return repo.revenueByDay(from, to).stream()
        .map(r -> new RevenueDto(
            String.valueOf(r[0]),                                 // yyyy-MM-dd
            r[1] == null ? 0d : ((Number) r[1]).doubleValue(),
            r[2] == null ? 0L : ((Number) r[2]).longValue()
        )).toList();
  }

  public List<EmpPerfDto> empPerformance(Instant from, Instant to) {
    return repo.empPerformance(from, to).stream()
        .map(r -> new EmpPerfDto(
            ((Number) r[0]).longValue(),
            String.valueOf(r[1]),
            r[2] == null ? 0L : ((Number) r[2]).longValue(),
            r[3] == null ? 0d : ((Number) r[3]).doubleValue()
        )).toList();
  }

  public List<OrderDetailDto> orderDetails(Instant from, Instant to) {
    return repo.orderDetails(from, to).stream()
        .map(r -> new OrderDetailDto(
            ((Number) r[0]).longValue(),
            (java.sql.Timestamp) r[1] != null ? ((java.sql.Timestamp) r[1]).toInstant() : null,
            String.valueOf(r[2]),
            r[3] == null ? 0d : ((Number) r[3]).doubleValue()
        )).toList();
  }
}
