package com.qros.core.config;

import com.qros.shared.subscription.RequiresPlanFeature;
import com.qros.shared.subscription.SubscriptionEntitlementPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SubscriptionFeatureInterceptor implements HandlerInterceptor {

    private final ObjectProvider<SubscriptionEntitlementPort> entitlementPortProvider;

    public SubscriptionFeatureInterceptor(ObjectProvider<SubscriptionEntitlementPort> entitlementPortProvider) {
        this.entitlementPortProvider = entitlementPortProvider;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresPlanFeature annotation = handlerMethod.getMethodAnnotation(RequiresPlanFeature.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequiresPlanFeature.class);
        }
        if (annotation == null) {
            return true;
        }

        SubscriptionEntitlementPort entitlementPort = entitlementPortProvider.getIfAvailable();
        if (entitlementPort != null) {
            entitlementPort.assertFeatureEnabled(annotation.value());
        }
        return true;
    }
}
