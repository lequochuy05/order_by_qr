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
 * Uses historical order patterns and time-of-day context to suggest menu items.
 */
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;

    /**
     * Suggests items that are frequently ordered alongside a specific item
     * (Cross-sell).
     * Falls back to popular items if no strong associations exist.
     * 
     * @param itemId Source menu item ID
     * @param limit  Maximum number of recommendations
     * @return List of suggested menu items
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations", key = "'cross_' + #itemId + '_' + #limit")
    public List<MenuItemResponse> getCrossSellRecommendations(Long itemId, int limit) {
        if (itemId == null)
            return getPopularItems(limit);

        List<Long> associatedIds = orderItemRepository.findTopAssociatedItems(itemId, PageRequest.of(0, limit * 2));

        if (associatedIds == null || associatedIds.isEmpty())
            return getPopularItems(limit);

        Map<Long, MenuItem> byId = menuItemRepository.findAllById(associatedIds).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .collect(Collectors.toMap(MenuItem::getId, item -> item, (a, b) -> a));

        return associatedIds.stream()
                .filter(byId::containsKey)
                .limit(limit)
                .map(id -> convertToResponse(byId.get(id)))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves similar items based on sales associations.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations", key = "'similar_' + #itemId + '_' + #limit")
    public List<MenuItemResponse> getRecommendations(Long itemId, int limit) {
        return getCrossSellRecommendations(itemId, limit);
    }

    /**
     * Identifies the most popular items across the entire menu based on historical
     * volume.
     * 
     * @param limit Maximum number of items
     * @return List of trending menu items
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "popularItems", key = "'pop_' + #limit")
    public List<MenuItemResponse> getPopularItems(int limit) {
        List<Long> topIds = orderItemRepository.findTopSellingItemIds(PageRequest.of(0, limit * 2));

        List<MenuItem> items;
        if (topIds == null || topIds.isEmpty()) {
            items = menuItemRepository.findAllByActiveTrue().stream()
                    .sorted(Comparator.comparing(MenuItem::getCreatedAt).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        } else {
            // Re-sort by original SQL ordering to preserve popularity ranking
            Map<Long, MenuItem> byId = menuItemRepository.findAllById(topIds).stream()
                    .filter(item -> Boolean.TRUE.equals(item.getActive()))
                    .collect(Collectors.toMap(MenuItem::getId, item -> item, (a, b) -> a));

            items = topIds.stream()
                    .filter(byId::containsKey)
                    .limit(limit)
                    .map(byId::get)
                    .collect(Collectors.toList());
        }

        return items.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Provides personalized recommendations by scoring items against current time
     * and historical sales popularity.
     * Implements a Weighted Scoring algorithm (Time: 60%, Popularity: 40%).
     * 
     * @param timeContext Current time description (e.g., "morning", "dinner")
     * @param limit       Maximum number of results
     * @return Ranked list of contextually appropriate menu items
     */
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getPersonalizedRecommendations(String timeContext, int limit) {
        List<MenuItem> activeMenu = menuItemRepository.findAllByActiveTrue();
        if (activeMenu.isEmpty())
            return List.of();

        Map<Long, Long> popularityMap = getPopularityMap(activeMenu);
        String tKey = normalize(timeContext);

        return activeMenu.stream()
                .map(item -> {
                    double score = calculateItemScore(item, tKey, popularityMap.getOrDefault(item.getId(), 0L));
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
    private double calculateItemScore(MenuItem item, String tKey, long soldCount) {
        if (item.getCategory() == null)
            return 0;

        String cateName = item.getCategory().getName().toLowerCase();

        // Weighted attributes: Time alignment + Sales popularity
        double timeScore = calculateTimeMatch(cateName, tKey) * 60;
        double popularityScore = Math.min(soldCount / 50.0, 1.0) * 40;

        return timeScore + popularityScore;
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
        if (containsAny(tKey, "chiều", "afternoon") &&
                containsAny(cateName, "Ăn Vặt", "Trà Sữa", "Nước Ép"))
            return 1.0;
        if (containsAny(tKey, "tối", "dinner") &&
                containsAny(cateName, "Ăn Vặt", "Giải Khát", "Mỳ cay", "Trà Sữa", "Trà - Soda", "Nước Ép"))
            return 1.0;
        return 0.1;
    }

    /**
     * Batch retrieves sales volume for a list of items to calculate popularity
     * scores efficiently.
     */
    private Map<Long, Long> getPopularityMap(List<MenuItem> items) {
        List<Long> ids = items.stream().map(MenuItem::getId).collect(Collectors.toList());
        List<Object[]> results = orderItemRepository.countTotalSoldBatch(ids);
        return results.stream().collect(Collectors.toMap(
                res -> ((Number) res[0]).longValue(),
                res -> ((Number) res[1]).longValue()));
    }

    /**
     * Maps a MenuItem entity to its Response DTO, including nested options and
     * values.
     */
    private MenuItemResponse convertToResponse(MenuItem item) {
        List<MenuItemResponse.ItemOptionResponse> options = new ArrayList<>();

        if (item.getItemOptions() != null) {
            for (var opt : item.getItemOptions()) {
                if (opt.isDeleted())
                    continue;

                List<MenuItemResponse.ItemOptionValueResponse> values = new ArrayList<>();
                if (opt.getOptionValues() != null) {
                    for (var val : opt.getOptionValues()) {
                        if (val.isDeleted())
                            continue;
                        values.add(MenuItemResponse.ItemOptionValueResponse.builder()
                                .id(val.getId())
                                .name(val.getName())
                                .extraPrice(val.getExtraPrice())
                                .build());
                    }
                }

                options.add(MenuItemResponse.ItemOptionResponse.builder()
                        .id(opt.getId())
                        .name(opt.getName())
                        .isRequired(opt.isRequired())
                        .maxSelection(opt.getMaxSelection())
                        .optionValues(values)
                        .build());
            }
        }

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
                .itemOptions(options)
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
