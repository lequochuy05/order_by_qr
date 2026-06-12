package com.qros.modules.promotion.service;

import com.qros.modules.promotion.dto.request.PromotionRequest;
import com.qros.modules.promotion.dto.response.PromotionResponse;
import com.qros.modules.promotion.mapper.PromotionMapper;
import com.qros.modules.promotion.model.Promotion;
import com.qros.modules.promotion.repository.PromotionRepository;
import com.qros.modules.notification.service.NotificationService;
import com.qros.shared.cache.CacheNames;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.time.AppTime;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PROMOTIONS, key = "'all_desc'")
    public List<PromotionResponse> findAll() {
        List<Promotion> promotions = promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        return promotionMapper.toResponses(promotions);
    }

    @Transactional(readOnly = true)
    public PromotionResponse findById(@NonNull Long id) {
        return promotionMapper.toResponse(getEntityById(id));
    }

    @Transactional(readOnly = true)
    public Promotion getEntityById(@NonNull Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMOTION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Promotion> findActivePromotions() {
        LocalDateTime now = AppTime.now();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDay = now.getDayOfWeek();

        return promotionRepository.findAllActive(currentTime, currentDay);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> findActivePromotionResponses() {
        return promotionMapper.toResponses(findActivePromotions());
    }

    @Transactional
    @CacheEvict(value = CacheNames.PROMOTIONS, allEntries = true)
    public PromotionResponse create(@NonNull PromotionRequest request) {
        String normalizedName = normalizeName(request.name());

        if (promotionRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new BusinessException(ErrorCode.PROMOTION_NAME_EXISTS);
        }

        Promotion promotion = promotionMapper.toEntity(request, normalizedName);
        Promotion saved = promotionRepository.save(promotion);
        notificationService.notifyPromotionChange();
        return promotionMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PROMOTIONS, allEntries = true)
    public PromotionResponse update(@NonNull Long id, @NonNull PromotionRequest request) {
        Promotion promotion = getEntityById(id);
        String normalizedName = normalizeName(request.name());

        if (promotionRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new BusinessException(ErrorCode.PROMOTION_NAME_EXISTS);
        }

        promotionMapper.updateEntity(promotion, request, normalizedName);
        Promotion saved = promotionRepository.save(promotion);
        notificationService.notifyPromotionChange();
        return promotionMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PROMOTIONS, allEntries = true)
    public void delete(@NonNull Long id) {
        Promotion promotion = getEntityById(id);
        promotionRepository.delete(promotion);
        notificationService.notifyPromotionChange();
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return name.trim();
    }
}
