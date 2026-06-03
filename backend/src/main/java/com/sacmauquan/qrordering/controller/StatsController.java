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
 * StatsController - Provides reporting and statistical data for the administrative dashboard.
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

  private final StatsService statsService;

  /**
   * Retrieves revenue reports within a specific date range.
   * 
   * @param from Start date
   * @param to End date
   * @return List of Revenue statistical objects
   */
  @GetMapping("/revenue")
  public ApiResponse<List<StatsResponse.Revenue>> getRevenue(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getRevenue(from, to));
  }

  /**
   * Retrieves employee performance statistics.
   * 
   * @param from Start date
   * @param to End date
   * @return List of employee performance objects
   */
  @GetMapping("/employees")
  public ApiResponse<List<StatsResponse.EmpPerformance>> getEmployeePerformance(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getEmployeePerformance(from, to));
  }

  /**
   * Retrieves detailed order list for reporting purposes.
   * 
   * @param from Start date
   * @param to End date
   * @return List of detailed order objects
   */
  @GetMapping("/orders")
  public ApiResponse<List<StatsResponse.OrderDetail>> getOrderDetails(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getOrderDetails(from, to));
  }

  /**
   * Retrieves the top-selling dishes in the given time range.
   * 
   * @param from Start date
   * @param to End date
   * @return List of TopDish objects
   */
  @GetMapping("/top-dishes")
  public ApiResponse<List<StatsResponse.TopDish>> getTopDishes(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getTopDishes(from, to));
  }

  /**
   * Retrieves dish trends and popularity evolution over time.
   * 
   * @param from Start date
   * @param to End date
   * @return List of DishTrend objects
   */
  @GetMapping("/dish-trend")
  public ApiResponse<List<StatsResponse.DishTrend>> getDishTrend(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getDishTrend(from, to));
  }

  /**
   * Retrieves recent revenue and a short-term forecast for the dashboard.
   */
  @GetMapping("/forecast/revenue")
  public ApiResponse<List<StatsResponse.RevenueForecast>> getRevenueForecast() {
    return ApiResponse.success(statsService.getRevenueForecast());
  }

  /**
   * Retrieves dishes expected to sell best in the next week.
   */
  @GetMapping("/forecast/popular-dishes")
  public ApiResponse<List<StatsResponse.PopularDishForecast>> getPopularDishesForecast() {
    return ApiResponse.success(statsService.getPopularDishesForecast());
  }

  /**
   * Composite dashboard endpoint — returns ALL stats in a single response.
   * Replaces 7 separate API calls with 1.
   * 
   * @param from Start date
   * @param to End date
   * @return DashboardSummary containing revenue, employees, orders, top dishes, trends, and forecasts
   */
  @GetMapping("/dashboard")
  public ApiResponse<StatsResponse.DashboardSummary> getDashboardSummary(
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @NonNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(statsService.getDashboardSummary(from, to));
  }
}
