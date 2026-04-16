package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.model.MenuItem;
import com.sacmauquan.qrordering.repository.MenuItemRepository;
import com.sacmauquan.qrordering.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final AIService aiService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * Recommends items frequently bought with the given item.
     */
    public List<MenuItem> getRecommendations(Long itemId, int limit) {
        if (itemId == null) return List.of();
        List<Long> associatedIds = orderItemRepository.findTopAssociatedItems(itemId, limit);
        if (associatedIds == null || associatedIds.isEmpty()) return List.of();
        return menuItemRepository.findAllById(associatedIds);
    }

    /**
     * Gets personalized recommendations based on external context using Gemini.
     */
    public List<MenuItem> getPersonalizedRecommendations(String timeContext, String weatherContext, int limit) {
        try {
            List<MenuItem> allMenu = menuItemRepository.findAll();
            // Minimize data sent to AI: only ID and Name
            List<Map<String, Object>> menuData = allMenu.stream()
                    .map(item -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", item.getId());
                        map.put("name", item.getName());
                        return map;
                    })
                    .collect(Collectors.toList());

            String menuJson = objectMapper.writeValueAsString(menuData);
            String aiResponse = aiService.getMenuRecommendations(menuJson, timeContext, weatherContext);

            List<Long> recommendedIds = parseIdsFromAi(aiResponse);
            if (!recommendedIds.isEmpty()) {
                return menuItemRepository.findAllById(recommendedIds).stream().limit(limit).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error getting personalized recommendations from AI", e);
        }
        return getPopularItems(limit);
    }

    /**
     * Gets cross-sell recommendations when an item is added to cart.
     */
    public List<MenuItem> getCrossSellRecommendations(Long currentItemId, int limit) {
        try {
            MenuItem currentItem = menuItemRepository.findById(currentItemId).orElse(null);
            if (currentItem == null) return getPopularItems(limit);

            List<MenuItem> allMenu = menuItemRepository.findAll();
            List<Map<String, Object>> menuData = allMenu.stream()
                    .filter(item -> !item.getId().equals(currentItemId))
                    .map(item -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", item.getId());
                        map.put("name", item.getName());
                        return map;
                    })
                    .collect(Collectors.toList());

            String menuJson = objectMapper.writeValueAsString(menuData);
            String aiResponse = aiService.getCrossSellRecommendations(currentItem.getName(), menuJson);

            List<Long> recommendedIds = parseIdsFromAi(aiResponse);
            if (!recommendedIds.isEmpty()) {
                return menuItemRepository.findAllById(recommendedIds).stream().limit(limit).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error getting cross-sell recommendations from AI", e);
        }
        return getRecommendations(currentItemId, limit); // Fallback to frequency-based
    }

    private List<Long> parseIdsFromAi(String aiResponse) {
        try {
            return objectMapper.readValue(aiResponse, new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse AI response as IDs: {}", aiResponse);
            return List.of();
        }
    }

    /**
     * Recommends top selling items as a fallback.
     */
    public List<MenuItem> getPopularItems(int limit) {
        return menuItemRepository.findAll().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
