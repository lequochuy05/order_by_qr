package com.qros.modules.payment.gateway;

import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayResolver {

    private final Map<PaymentMethod, PaymentGateway> gateways = new EnumMap<>(PaymentMethod.class);

    public PaymentGatewayResolver(List<PaymentGateway> gatewayList) {
        for (PaymentGateway gateway : gatewayList) {
            gateways.put(gateway.getMethod(), gateway);
        }
    }

    public PaymentGateway resolve(PaymentMethod method) {
        PaymentGateway gateway = gateways.get(method);
        if (gateway == null) {
            throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                    "Unsupported payment method: " + method);
        }
        return gateway;
    }
}