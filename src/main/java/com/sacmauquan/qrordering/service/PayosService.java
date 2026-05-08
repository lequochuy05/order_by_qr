package com.sacmauquan.qrordering.service;

import com.sacmauquan.qrordering.dto.PayosCreateRequest;
import com.sacmauquan.qrordering.dto.PayosCreateResponse;
import com.sacmauquan.qrordering.model.PaymentTransaction;

public interface PayosService {
    PayosCreateResponse createPaymentLink(PayosCreateRequest request);
    
    void cancelPaymentLink(Long transactionId, String reason);
    
    PaymentTransaction syncPaymentStatus(Long transactionId);
}
