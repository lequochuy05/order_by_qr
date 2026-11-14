package com.qros.modules.analytics.controller;

import com.qros.modules.analytics.dto.response.PopularItemForecastResponse;
import com.qros.modules.analytics.dto.response.RevenueForecastResponse;
import com.qros.modules.analytics.service.AnalyticsService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.FORECAST)
@PreAuthorize("hasRole('ADMIN')")
public class ForecastController {

    private final AnalyticsService analyticsService;

    public ForecastController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/revenue")
    public ApiResponse<List<RevenueForecastResponse>> revenueForecast() {
        return ApiResponse.success(analyticsService.getRevenueForecast());
    }

    @GetMapping("/popular-items")
    public ApiResponse<List<PopularItemForecastResponse>> popularItemsForecast() {
        return ApiResponse.success(analyticsService.getPopularItemsForecast());
    }
}
