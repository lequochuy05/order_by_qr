package com.sacmauquan.qrordering.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.sacmauquan.qrordering.model.Voucher;
import com.sacmauquan.qrordering.repository.VoucherRepository;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {
    @Mock
    VoucherRepository voucherRepository;
    @Mock
    NotificationService notificationService;

    @InjectMocks
    DiscountService discountService;

    @Test
    void applyVoucherThrowsWhenAtomicUsageIncrementFails() {
        Voucher voucher = Voucher.builder()
                .id(1L)
                .code("SAVE10")
                .type(Voucher.VoucherType.FIXED_AMOUNT)
                .discountAmount(BigDecimal.TEN)
                .active(true)
                .usageLimit(1)
                .usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .build();

        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));
        when(voucherRepository.incrementUsedCountAtomically(1L)).thenReturn(0);

        assertThatThrownBy(() -> discountService.applyVoucher("SAVE10", BigDecimal.valueOf(100)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
