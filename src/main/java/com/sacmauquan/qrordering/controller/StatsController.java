package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.dto.ApiResponse;
import com.sacmauquan.qrordering.dto.StatsResponse;
import com.sacmauquan.qrordering.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * StatsController - Cung cấp dữ liệu báo cáo và thống kê Dashboard.
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

  private final StatsService statsService;

  /**
   * Lấy báo cáo doanh thu theo khoảng thời gian.
   */
  @GetMapping("/revenue")
  public ApiResponse<List<StatsResponse.Revenue>> getRevenue(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getRevenue(from, to));
  }

  /**
   * Thống kê hiệu suất làm việc của nhân viên.
   */
  @GetMapping("/employees")
  public ApiResponse<List<StatsResponse.EmpPerformance>> getEmployeePerformance(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getEmployeePerformance(from, to));
  }

  /**
   * Lấy danh sách chi tiết các đơn hàng phục vụ báo cáo.
   */
  @GetMapping("/orders")
  public ApiResponse<List<StatsResponse.OrderDetail>> getOrderDetails(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getOrderDetails(from, to));
  }

  /**
   * Danh sách các món ăn bán chạy nhất.
   */
  @GetMapping("/top-dishes")
  public ApiResponse<List<StatsResponse.TopDish>> getTopDishes(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getTopDishes(from, to));
  }

  /**
   * Biểu đồ xu hướng món ăn theo thời gian.
   */
  @GetMapping("/dish-trend")
  public ApiResponse<List<StatsResponse.DishTrend>> getDishTrend(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getDishTrend(from, to));
  }
}
