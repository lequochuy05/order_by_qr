package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/personalized")
    public ResponseEntity<List<MenuItem>> getPersonalizedRecommendations(
            @RequestParam(defaultValue = "Morning") String timeContext,
            @RequestParam(defaultValue = "Clear") String weatherContext,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(recommendationService.getPersonalizedRecommendations(timeContext, weatherContext, limit));
    }

    @GetMapping("/cross-sell/{itemId}")
    public ResponseEntity<List<MenuItem>> getCrossSellRecommendations(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "3") int limit) {
        return ResponseEntity.ok(recommendationService.getCrossSellRecommendations(itemId, limit));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<MenuItem>> getPopularItems(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(recommendationService.getPopularItems(limit));
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<MenuItem>> getRecommendations(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(recommendationService.getRecommendations(itemId, limit));
    }
}
