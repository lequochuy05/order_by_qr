package com.qros.modules.promotion.mapper;

import com.qros.modules.promotion.dto.request.PromotionRequest;
import com.qros.modules.promotion.dto.response.PromotionResponse;
import com.qros.modules.promotion.model.Promotion;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PromotionMapper {

    public Promotion toEntity(PromotionRequest request, String normalizedName) {
        return Promotion.builder()
                .name(normalizedName)
                .discountPercent(request.discountPercent())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .daysOfWeek(toSafeDays(request.daysOfWeek()))
                .active(request.active() != null ? request.active() : true)
                .build();
    }

    public void updateEntity(Promotion promotion, PromotionRequest request, String normalizedName) {
        promotion.setName(normalizedName);
        promotion.setDiscountPercent(request.discountPercent());
        promotion.setStartTime(request.startTime());
        promotion.setEndTime(request.endTime());
        promotion.setActive(request.active() != null ? request.active() : promotion.getActive());

        promotion.getDaysOfWeek().clear();
        promotion.getDaysOfWeek().addAll(toSafeDays(request.daysOfWeek()));
    }

    public PromotionResponse toResponse(Promotion promotion) {
        return new PromotionResponse(
                promotion.getId(),
                promotion.getName(),
                promotion.getDiscountPercent(),
                promotion.getStartTime(),
                promotion.getEndTime(),
                toSafeDays(promotion.getDaysOfWeek()),
                promotion.getActive());
    }

    public List<PromotionResponse> toResponses(List<Promotion> promotions) {
        return promotions.stream()
                .map(this::toResponse)
                .toList();
    }

    private Set<DayOfWeek> toSafeDays(Set<DayOfWeek> daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return new LinkedHashSet<>();
        }

        return new LinkedHashSet<>(daysOfWeek);
    }
}