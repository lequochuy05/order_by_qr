package com.qros.modules.promotion.service;

import com.qros.modules.promotion.model.Promotion;
import com.qros.modules.promotion.repository.PromotionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Module-boundary lookup facade for promotion configurations consumed by other modules.
 */
@Service
@RequiredArgsConstructor
public class PromotionLookupService {

    private final PromotionRepository promotionRepository;

    public List<Promotion> findAllWithDays() {
        return promotionRepository.findAllWithDays();
    }
}
