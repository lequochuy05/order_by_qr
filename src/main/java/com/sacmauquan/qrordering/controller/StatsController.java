// src/main/java/com/sacmauquan/qrordering/controller/StatsController.java
package com.sacmauquan.qrordering.controller;

import java.time.*;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.sacmauquan.qrordering.dto.*;
import com.sacmauquan.qrordering.service.StatsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
  private final StatsService statsService;

  private Instant startOfDay(LocalDate d){
    return d.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }
  private Instant endOfDay(LocalDate d){
    return d.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().minusMillis(1);
  }

  @GetMapping("/revenue")
  public List<RevenueDto> revenue(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return statsService.revenue(startOfDay(from), endOfDay(to));
  }

  @GetMapping("/employees")
  public List<EmpPerfDto> employeePerf(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return statsService.empPerformance(startOfDay(from), endOfDay(to));
  }

  @GetMapping("/orders")
  public List<OrderDetailDto> orders(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return statsService.orderDetails(startOfDay(from), endOfDay(to));
  }
}
