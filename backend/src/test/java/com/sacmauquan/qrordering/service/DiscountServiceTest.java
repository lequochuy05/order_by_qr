package com.qros.test;

import com.qros.modules.menu.service.CategoryService;
import com.qros.modules.menu.service.MenuItemService;
import com.qros.modules.menu.service.ComboService;
import com.qros.modules.order.service.OrderService;
import com.qros.modules.order.service.impl.OrderServiceImpl;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.modules.payment.service.PayosService;
import com.qros.modules.payment.service.impl.PayosServiceImpl;
import com.qros.modules.promotion.service.DiscountService;
import com.qros.modules.notification.service.NotificationService;
import com.qros.modules.recomendation.service.RecommendationService;
import com.qros.modules.user.service.UserService;
import com.qros.modules.table.service.DiningTableService;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.OrderItemOption;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.table.model.DiningTable;
import com.qros.modules.user.model.User;
import com.qros.modules.payment.model.PaymentTransaction;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.order.dto.OrderRequest;
import com.qros.modules.order.dto.OrderResponse;
import com.qros.modules.payment.dto.PayosCreateRequest;
import com.qros.modules.payment.dto.PayosCreateResponse;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.modules.order.state.OrderState;
import com.qros.modules.order.state.OrderStateFactory;
import com.qros.shared.response.ApiResponse;
import com.qros.shared.entity.BaseEntity;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.repository.VoucherRepository;
import com.qros.shared.util.AppTime;

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
                .validFrom(AppTime.now().minusDays(1))
                .validTo(AppTime.now().plusDays(1))
                .build();

        when(voucherRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(voucher));
        when(voucherRepository.incrementUsedCountAtomically(1L)).thenReturn(0);

        assertThatThrownBy(() -> discountService.applyVoucher("SAVE10", BigDecimal.valueOf(100)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
