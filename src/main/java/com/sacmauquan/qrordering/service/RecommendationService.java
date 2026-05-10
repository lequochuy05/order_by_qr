package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.MenuItemResponse;
import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.sacmauquan.qrordering.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RecommendationService - Intelligent item recommendation engine.
 * Uses historical order patterns and environmental context (time/weather) to suggest menu items.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;

    /**
     * Suggests items that are frequently ordered alongside a specific item (Cross-sell).
     * Falls back to popular items if no strong associations exist.
     * 
     * @param itemId Source menu item ID
     * @param limit Maximum number of recommendations
     * @return List of suggested menu items
     */
    @Cacheable(value = "recommendations", key = "'cross_' + #itemId + '_' + #limit")
    public List<MenuItemResponse> getCrossSellRecommendations(Long itemId, int limit) {
        if (itemId == null)
            return getPopularItems(limit);

        List<Long> associatedIds = orderItemRepository.findTopAssociatedItems(itemId, limit * 2);

        if (associatedIds.isEmpty())
            return getPopularItems(limit);

        return menuItemRepository.findAllById(associatedIds).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .limit(limit)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves similar items based on sales associations.
     */
    @Cacheable(value = "recommendations", key = "'similar_' + #itemId + '_' + #limit")
    public List<MenuItemResponse> getRecommendations(Long itemId, int limit) {
        return getCrossSellRecommendations(itemId, limit);
    }

    /**
     * Identifies the most popular items across the entire menu based on historical volume.
     * 
     * @param limit Maximum number of items
     * @return List of trending menu items
     */
    @Cacheable(value = "popularItems", key = "'pop_' + #limit")
    public List<MenuItemResponse> getPopularItems(int limit) {
        List<Long> topIds = orderItemRepository.findTopSellingItemIds(limit * 2);

        List<MenuItem> items;
        if (topIds == null || topIds.isEmpty()) {
            items = menuItemRepository.findAllByActiveTrue().stream()
                    .sorted(Comparator.comparing(MenuItem::getCreatedAt).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            items = menuItemRepository.findAllById(topIds).stream()
                    .filter(item -> Boolean.TRUE.equals(item.getActive()))
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Provides personalized recommendations by scoring items against current time and weather conditions.
     * Implements a Weighted Scoring algorithm (Time: 40%, Weather: 30%, Popularity: 30%).
     * 
     * @param timeContext Current time description (e.g., "morning", "dinner")
     * @param weatherContext Current weather status (e.g., "hot", "rainy")
     * @param limit Maximum number of results
     * @return Ranked list of contextually appropriate menu items
     */
    public List<MenuItemResponse> getPersonalizedRecommendations(String timeContext, String weatherContext, int limit) {
        List<MenuItem> activeMenu = menuItemRepository.findAllByActiveTrue();
        if (activeMenu.isEmpty())
            return List.of();

        Map<Long, Long> popularityMap = getPopularityMap(activeMenu);
        String tKey = normalize(timeContext);
        String wKey = normalize(weatherContext);

        return activeMenu.stream()
                .map(item -> {
                    double score = calculateItemScore(item, tKey, wKey, popularityMap.getOrDefault(item.getId(), 0L));
                    return new AbstractMap.SimpleEntry<>(item, score);
                })
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<MenuItem, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> convertToResponse(entry.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * Core scoring logic for contextual recommendations.
     */
    private double calculateItemScore(MenuItem item, String tKey, String wKey, long soldCount) {
        if (item.getCategory() == null)
            return 0;

        String cateName = item.getCategory().getName().toLowerCase();

        // Weighted attributes: Time alignment + Weather suitability + Sales popularity
        double timeScore = calculateTimeMatch(cateName, tKey) * 40;
        double weatherScore = calculateWeatherMatch(cateName, wKey) * 30;
        double popularityScore = Math.min(soldCount / 50.0, 1.0) * 30;

        return timeScore + weatherScore + popularityScore;
    }

    /**
     * Matches category keywords against specific time periods.
     */
    private double calculateTimeMatch(String cateName, String tKey) {
        if (tKey.isBlank())
            return 0.5;
        if (containsAny(tKey, "sáng", "morning") &&
                containsAny(cateName, "Mỳ cay", "Trà", "Soda", "Giải Khát", "Trà Sữa"))
            return 1.0;
        if (containsAny(tKey, "trưa", "lunch") &&
                containsAny(cateName, "Ăn Vặt", "Trà Sữa", "Nước Ép"))
            return 1.0;
        if (containsAny(tKey, "tối", "dinner") &&
                containsAny(cateName, "Ăn Vặt", "Giải Khát", "Mỳ cay"))
            return 1.0;
        return 0.1;
    }

    /**
     * Matches category keywords against specific weather conditions.
     */
    private double calculateWeatherMatch(String cateName, String wKey) {
        if (wKey.isBlank())
            return 0.5;
        if (containsAny(wKey, "nóng", "hot") &&
                containsAny(cateName, "Trà", "Soda", "Nước Ép", "Giải Khát"))
            return 1.0;
        if (containsAny(wKey, "lạnh", "mưa", "cold") &&
                containsAny(cateName, "Mỳ cay", "Ăn Vặt"))
            return 1.0;
        return 0.1;
    }

    /**
     * Batch retrieves sales volume for a list of items to calculate popularity scores efficiently.
     */
    private Map<Long, Long> getPopularityMap(List<MenuItem> items) {
        List<Long> ids = items.stream().map(MenuItem::getId).collect(Collectors.toList());
        List<Object[]> results = orderItemRepository.countTotalSoldBatch(ids);
        return results.stream().collect(Collectors.toMap(
                res -> (Long) res[0],
                res -> (Long) res[1]));
    }

    /**
     * Maps a MenuItem entity to its Response DTO.
     */
    private MenuItemResponse convertToResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .img(item.getImg())
                .active(item.getActive())
                .category(item.getCategory() != null ? MenuItemResponse.CategorySummary.builder()
                        .id(item.getCategory().getId())
                        .name(item.getCategory().getName())
                        .build() : null)
                .build();
    }

    private String normalize(String input) {
        return input != null ? input.toLowerCase().trim() : "";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw.toLowerCase()))
                return true;
        }
        return false;
    }
}
