package com.qros.service;

import org.springframework.context.ApplicationEventPublisher;
import com.qros.modules.promotion.dto.internal.DiscountResult;
import com.qros.modules.promotion.mapper.VoucherMapper;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.model.enums.VoucherType;
import com.qros.modules.promotion.repository.VoucherRepository;
import com.qros.modules.promotion.service.DiscountCalculator;
import com.qros.modules.promotion.service.VoucherService;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    VoucherRepository voucherRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    private DiscountCalculator discountCalculator;
    private VoucherService voucherService;

    @BeforeEach
    void setUp() {
        discountCalculator = new DiscountCalculator();
        voucherService = new VoucherService(
                voucherRepository,
                new VoucherMapper(),
                eventPublisher);
    }

    @Test
    void incrementUsageThrowsWhenAtomicIncrementFails() {
        when(voucherRepository.incrementUsedCountAtomically(1L)).thenReturn(0);

        assertThatThrownBy(() -> voucherService.incrementUsage(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED);
    }

    @Test
    void fixedDiscountIsCappedAtSubtotal() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("SAVE100")
                .type(VoucherType.FIXED_AMOUNT)
                .discountAmount(BigDecimal.valueOf(100_000))
                .build();

        DiscountResult result = discountCalculator.calculate(voucher, BigDecimal.valueOf(50_000));

        assertThat(result.appliedDiscountAmount()).isEqualByComparingTo("50000");
        assertThat(result.finalAmount()).isEqualByComparingTo("0");
    }
}
