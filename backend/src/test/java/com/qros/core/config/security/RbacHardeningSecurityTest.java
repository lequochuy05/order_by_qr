package com.qros.core.config.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qros.modules.analytics.controller.AnalyticsController;
import com.qros.modules.analytics.controller.ForecastController;
import com.qros.modules.analytics.service.AnalyticsService;
import com.qros.modules.inventory.controller.InventoryItemController;
import com.qros.modules.inventory.service.InventoryItemService;
import com.qros.modules.order.controller.OrderItemController;
import com.qros.modules.order.controller.StaffOrderController;
import com.qros.modules.order.service.OrderService;
import com.qros.modules.table.controller.TableSessionController;
import com.qros.modules.table.service.TableMergeService;
import com.qros.modules.table.service.TableSessionService;
import com.qros.modules.user.service.CurrentUserService;
import com.qros.shared.config.AppProperties;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.ratelimit.ClientAddressResolver;
import com.qros.shared.ratelimit.RateLimitProperties;
import com.qros.shared.ratelimit.RedisRateLimiter;
import com.qros.shared.security.JwtAuthFilter;
import com.qros.shared.security.JwtService;
import com.qros.shared.security.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {
            StaffOrderController.class,
            OrderItemController.class,
            InventoryItemController.class,
            AnalyticsController.class,
            ForecastController.class,
            TableSessionController.class
        })
@ContextConfiguration(classes = RbacHardeningSecurityTest.TestApplication.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({
    SecurityConfig.class,
    CorsConfig.class,
    JwtAuthFilter.class,
    StaffOrderController.class,
    OrderItemController.class,
    InventoryItemController.class,
    AnalyticsController.class,
    ForecastController.class,
    TableSessionController.class,
    RbacHardeningSecurityTest.PropertiesConfiguration.class
})
class RbacHardeningSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private InventoryItemService inventoryItemService;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private TableMergeService tableMergeService;

    @MockitoBean
    private TableSessionService tableSessionService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private RedisRateLimiter redisRateLimiter;

    @MockitoBean
    private ClientAddressResolver clientAddressResolver;

    @Test
    void staffAndChefCanCancelOrderItemWhileDeleteOrderRemainsAdminOnly() throws Exception {
        mockMvc.perform(delete(ApiRoutes.ORDER_ITEMS + "/55")
                        .with(user("staff@qros.vn").roles("STAFF")))
                .andExpect(status().isOk());
        mockMvc.perform(delete(ApiRoutes.ORDER_ITEMS + "/56")
                        .with(user("chef@qros.vn").roles("CHEF")))
                .andExpect(status().isOk());

        mockMvc.perform(delete(ApiRoutes.ORDERS + "/99")
                        .with(user("manager@qros.vn").roles("MANAGER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(ApiRoutes.ORDERS + "/99")
                        .with(user("staff@qros.vn").roles("STAFF")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(ApiRoutes.ORDERS + "/99")
                        .with(user("admin@qros.vn").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(orderService).deleteOrder(99L);
    }

    @Test
    void inventoryReadBlocksStaffAndCashierAndAllowsChefManagerAdminAndDeleteIsAdminOnly() throws Exception {
        mockMvc.perform(get(ApiRoutes.INVENTORY_ITEMS)
                        .with(user("staff@qros.vn").roles("STAFF")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ApiRoutes.INVENTORY_ITEMS)
                        .with(user("cashier@qros.vn").roles("CASHIER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(inventoryItemService);

        for (String role : java.util.List.of("CHEF", "MANAGER", "ADMIN")) {
            mockMvc.perform(get(ApiRoutes.INVENTORY_ITEMS)
                            .with(user("u@qros.vn").roles(role)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(delete(ApiRoutes.INVENTORY_ITEMS + "/10")
                        .with(user("manager@qros.vn").roles("MANAGER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(ApiRoutes.INVENTORY_ITEMS + "/10")
                        .with(user("admin@qros.vn").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(inventoryItemService).delete(10L);
    }

    @Test
    void managerCanAccessRevenueAndForecastEndpointsWhileStaffAndCashierCannot() throws Exception {
        for (String deniedRole : java.util.List.of("STAFF", "CASHIER", "CHEF")) {
            mockMvc.perform(get(ApiRoutes.ANALYTICS + "/revenue")
                            .with(user("d@qros.vn").roles(deniedRole)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get(ApiRoutes.FORECAST + "/popular-items")
                            .with(user("d@qros.vn").roles(deniedRole)))
                    .andExpect(status().isForbidden());
        }

        for (String allowedRole : java.util.List.of("MANAGER", "ADMIN")) {
            mockMvc.perform(get(ApiRoutes.ANALYTICS + "/revenue")
                            .with(user("m@qros.vn").roles(allowedRole)))
                    .andExpect(status().isOk());
            mockMvc.perform(get(ApiRoutes.FORECAST + "/popular-items")
                            .with(user("m@qros.vn").roles(allowedRole)))
                    .andExpect(status().isOk());
        }

        verify(analyticsService, org.mockito.Mockito.times(2)).getRevenueSeries(any(), any());
        verify(analyticsService, org.mockito.Mockito.times(2)).getPopularItemsForecast();
    }

    @Test
    void staffAndManagerCanCloseEmptyTableSessionWhileChefCannot() throws Exception {
        mockMvc.perform(post(ApiRoutes.TABLE_SESSIONS + "/100/close-empty")
                        .with(user("chef@qros.vn").roles("CHEF")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(ApiRoutes.TABLE_SESSIONS + "/100/close-empty")
                        .with(user("staff@qros.vn").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tableSessionService).closeEmptySession(eq(100L), eq("staff@qros.vn"));
    }

    @TestConfiguration
    @EnableConfigurationProperties({AppProperties.class, RateLimitProperties.class})
    static class PropertiesConfiguration {}

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {}
}
