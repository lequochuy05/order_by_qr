package com.qros.modules.payment.dto.internal;

import com.qros.modules.payment.model.PaymentTransaction;
import java.util.List;

public record CashSettlementResult(
        PaymentTransaction transaction, List<PaymentTransaction> cancelledOnlineTransactions) {}
