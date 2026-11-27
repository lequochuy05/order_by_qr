package com.qros.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.subscription.PlanFeature;
import com.qros.shared.subscription.RequiresPlanFeature;
import com.qros.shared.subscription.SubscriptionEntitlementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class SubscriptionFeatureInterceptorTest {

    private SubscriptionEntitlementPort entitlementPort;
    private SubscriptionFeatureInterceptor interceptor;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        entitlementPort = mock(SubscriptionEntitlementPort.class);
        ObjectProvider<SubscriptionEntitlementPort> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(entitlementPort);
        interceptor = new SubscriptionFeatureInterceptor(provider);
    }

    @Test
    void enforcesMethodLevelAndClassLevelAnnotationsAndSkipsUnannotatedHandlers() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        HandlerMethod openHandler = new HandlerMethod(new OpenController(), OpenController.class.getMethod("ping"));
        assertThat(interceptor.preHandle(req, res, openHandler)).isTrue();
        verifyNoInteractions(entitlementPort);

        HandlerMethod methodAnnotated =
                new HandlerMethod(new OpenController(), OpenController.class.getMethod("writeBom"));
        doThrow(new BusinessException(ErrorCode.PRO_PLAN_REQUIRED))
                .when(entitlementPort)
                .assertFeatureEnabled(PlanFeature.INVENTORY_BOM);

        assertThatThrownBy(() -> interceptor.preHandle(req, res, methodAnnotated))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRO_PLAN_REQUIRED);

        HandlerMethod classAnnotated =
                new HandlerMethod(new ProAiController(), ProAiController.class.getMethod("analyze"));
        assertThat(interceptor.preHandle(req, res, classAnnotated)).isTrue();
        verify(entitlementPort).assertFeatureEnabled(PlanFeature.AI_OPERATIONS_SUITE);
    }

    static class OpenController {
        public void ping() {}

        @RequiresPlanFeature(PlanFeature.INVENTORY_BOM)
        public void writeBom() {}
    }

    @RequiresPlanFeature(PlanFeature.AI_OPERATIONS_SUITE)
    static class ProAiController {
        public void analyze() {}
    }
}
