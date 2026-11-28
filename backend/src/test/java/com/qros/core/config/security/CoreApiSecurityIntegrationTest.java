package com.qros.core.config.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qros.modules.ai.controller.AiInsightsController;
import com.qros.modules.ai.controller.AiInventoryController;
import com.qros.modules.ai.controller.AiMerchandisingController;
import com.qros.modules.ai.controller.AiStaffController;
import com.qros.modules.ai.service.AiExecutiveInsightsService;
import com.qros.modules.ai.service.AiInventoryRestockService;
import com.qros.modules.ai.service.AiInvoiceOcrService;
import com.qros.modules.ai.service.AiMerchandisingService;
import com.qros.modules.ai.service.AiNoteTriageService;
import com.qros.modules.ai.service.AiQuotaService;
import com.qros.modules.ai.service.AiRecipeAssistantService;
import com.qros.modules.ai.service.AiStaffService;
import com.qros.modules.inventory.controller.InventoryItemController;
import com.qros.modules.inventory.service.InventoryItemService;
import com.qros.modules.settings.controller.PublicSettingsController;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.subscription.controller.SubscriptionController;
import com.qros.modules.subscription.service.SubscriptionService;
import com.qros.modules.table.controller.PublicTableServiceRequestController;
import com.qros.modules.table.controller.TableServiceRequestController;
import com.qros.modules.table.service.TableServiceRequestService;
import com.qros.modules.user.controller.CurrentUserController;
import com.qros.modules.user.service.CurrentUserService;
import com.qros.modules.user.service.UserAvatarService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {
            PublicSettingsController.class,
            CurrentUserController.class,
            PublicTableServiceRequestController.class,
            TableServiceRequestController.class,
            AiInsightsController.class,
            AiStaffController.class,
            AiInventoryController.class,
            AiMerchandisingController.class,
            InventoryItemController.class,
            SubscriptionController.class
        },
        properties = "app.cors.allowed-origins=http://localhost:5173")
@ContextConfiguration(classes = CoreApiSecurityIntegrationTest.TestApplication.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({
    SecurityConfig.class,
    CorsConfig.class,
    JwtAuthFilter.class,
    PublicSettingsController.class,
    CurrentUserController.class,
    PublicTableServiceRequestController.class,
    TableServiceRequestController.class,
    AiInsightsController.class,
    AiStaffController.class,
    AiInventoryController.class,
    AiMerchandisingController.class,
    InventoryItemController.class,
    SubscriptionController.class,
    CoreApiSecurityIntegrationTest.PropertiesConfiguration.class
})
class CoreApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemSettingsService settingsService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private UserAvatarService userAvatarService;

    @MockitoBean
    private TableServiceRequestService tableServiceRequestService;

    @MockitoBean
    private AiExecutiveInsightsService aiExecutiveInsightsService;

    @MockitoBean
    private AiStaffService aiStaffService;

    @MockitoBean
    private AiNoteTriageService aiNoteTriageService;

    @MockitoBean
    private AiInventoryRestockService aiInventoryRestockService;

    @MockitoBean
    private AiRecipeAssistantService aiRecipeAssistantService;

    @MockitoBean
    private AiInvoiceOcrService aiInvoiceOcrService;

    @MockitoBean
    private AiMerchandisingService aiMerchandisingService;

    @MockitoBean
    private InventoryItemService inventoryItemService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private AiQuotaService aiQuotaService;

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
    void servesPublicSettingsThroughTheConfiguredSecurityChain() throws Exception {
        mockMvc.perform(get(ApiRoutes.PUBLIC_SETTINGS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200));

        verify(settingsService).getPublicSettings();
    }

    @Test
    void rejectsAnonymousRequestsToTheCurrentUserEndpoint() throws Exception {
        mockMvc.perform(get(ApiRoutes.USERS + "/me")).andExpect(status().isUnauthorized());

        verifyNoInteractions(currentUserService);
    }

    @Test
    void permitsAuthenticatedRequestsAndPassesPrincipalToTheService() throws Exception {
        mockMvc.perform(get(ApiRoutes.USERS + "/me")
                        .with(user("staff@example.test").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200));

        verify(currentUserService).getCurrentProfile("staff@example.test");
    }

    @Test
    void serviceRequestQueueAllowsStaffHierarchyAndBlocksChefAndAnonymous() throws Exception {
        mockMvc.perform(get(ApiRoutes.TABLE_SERVICE_REQUESTS)).andExpect(status().isUnauthorized());

        mockMvc.perform(get(ApiRoutes.TABLE_SERVICE_REQUESTS)
                        .with(user("chef@example.test").roles("CHEF")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(ApiRoutes.TABLE_SERVICE_REQUESTS + "/1/claim")
                        .with(user("chef@example.test").roles("CHEF")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(tableServiceRequestService);

        for (String role : java.util.List.of("STAFF", "CASHIER", "MANAGER", "ADMIN")) {
            mockMvc.perform(get(ApiRoutes.TABLE_SERVICE_REQUESTS)
                            .with(user("u@example.test").roles(role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        mockMvc.perform(patch(ApiRoutes.TABLE_SERVICE_REQUESTS + "/1/claim")
                        .with(user("staff@example.test").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tableServiceRequestService).claimRequest(1L, "staff@example.test");
    }

    @Test
    void permitsAnonymousCustomerServiceRequestCreationThroughSecurityChain() throws Exception {
        mockMvc.perform(post(ApiRoutes.PUBLIC + "/tables/BAN_05/service-requests")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionToken":"tok-1","reason":"WATER_ICE","clientRequestId":"cid-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tableServiceRequestService).createCustomerRequest(eq("BAN_05"), any());
    }

    @Test
    void executiveInsightsSummaryRestrictsAccessToManagerAndAdmin() throws Exception {
        mockMvc.perform(post(ApiRoutes.AI + "/insights/summary")).andExpect(status().isUnauthorized());

        for (String deniedRole : java.util.List.of("STAFF", "CASHIER", "CHEF")) {
            mockMvc.perform(post(ApiRoutes.AI + "/insights/summary")
                            .with(user("denied@example.test").roles(deniedRole)))
                    .andExpect(status().isForbidden());
        }

        verifyNoInteractions(aiQuotaService, aiExecutiveInsightsService);

        for (String allowedRole : java.util.List.of("MANAGER", "ADMIN")) {
            mockMvc.perform(post(ApiRoutes.AI + "/insights/summary")
                            .with(user("lead@example.test").roles(allowedRole))
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("""
                                    {"language":"vi"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        verify(aiQuotaService, org.mockito.Mockito.times(2)).checkExecutiveInsights("lead@example.test");
    }

    @Test
    void noteTriageAllowsStaffChefCashierManagerAndAdminButBlocksAnonymous() throws Exception {
        mockMvc.perform(post(ApiRoutes.AI + "/staff/analyze-notes")).andExpect(status().isUnauthorized());
        verifyNoInteractions(aiQuotaService, aiNoteTriageService);

        org.mockito.Mockito.when(aiNoteTriageService.triage(any(), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    invocation.<Runnable>getArgument(1).run();
                    return null;
                });

        for (String role : java.util.List.of("STAFF", "CASHIER", "CHEF", "MANAGER", "ADMIN")) {
            mockMvc.perform(post(ApiRoutes.AI + "/staff/analyze-notes")
                            .with(user("floor@example.test").roles(role))
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("""
                                    {"language":"vi"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        verify(aiQuotaService, org.mockito.Mockito.times(5)).checkStaffAssistant("floor@example.test");
    }

    @Test
    void inventoryAiEndpointsAllowChefManagerAdminAndRejectStaffAndCashier() throws Exception {
        mockMvc.perform(post(ApiRoutes.AI + "/inventory/restock-suggestions")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ApiRoutes.AI + "/inventory/recipe-assistant")).andExpect(status().isUnauthorized());

        for (String deniedRole : java.util.List.of("STAFF", "CASHIER")) {
            mockMvc.perform(post(ApiRoutes.AI + "/inventory/restock-suggestions")
                            .with(user("denied@example.test").roles(deniedRole)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(ApiRoutes.AI + "/inventory/recipe-assistant")
                            .with(user("denied@example.test").roles(deniedRole))
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("""
                                    {"menuItemId":1}
                                    """))
                    .andExpect(status().isForbidden());
        }

        verifyNoInteractions(aiQuotaService, aiInventoryRestockService, aiRecipeAssistantService);

        for (String allowedRole : java.util.List.of("CHEF", "MANAGER", "ADMIN")) {
            mockMvc.perform(post(ApiRoutes.AI + "/inventory/restock-suggestions")
                            .with(user("chef@example.test").roles(allowedRole)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
            mockMvc.perform(post(ApiRoutes.AI + "/inventory/recipe-assistant")
                            .with(user("chef@example.test").roles(allowedRole))
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("""
                                    {"menuItemId":1,"language":"vi"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        verify(aiQuotaService, org.mockito.Mockito.times(6)).checkOperationsAi("chef@example.test");
    }

    @Test
    void merchandisingAiEndpointsRestrictAccessToManagerAndAdmin() throws Exception {
        mockMvc.perform(post(ApiRoutes.AI + "/merchandising/combo-suggestions")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ApiRoutes.AI + "/merchandising/promotion-suggestions"))
                .andExpect(status().isUnauthorized());

        for (String deniedRole : java.util.List.of("STAFF", "CASHIER", "CHEF")) {
            mockMvc.perform(post(ApiRoutes.AI + "/merchandising/combo-suggestions")
                            .with(user("denied@example.test").roles(deniedRole)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post(ApiRoutes.AI + "/merchandising/promotion-suggestions")
                            .with(user("denied@example.test").roles(deniedRole)))
                    .andExpect(status().isForbidden());
        }

        verifyNoInteractions(aiQuotaService, aiMerchandisingService);

        for (String allowedRole : java.util.List.of("MANAGER", "ADMIN")) {
            mockMvc.perform(post(ApiRoutes.AI + "/merchandising/combo-suggestions")
                            .with(user("manager@example.test").roles(allowedRole)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
            mockMvc.perform(post(ApiRoutes.AI + "/merchandising/promotion-suggestions")
                            .with(user("manager@example.test").roles(allowedRole)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        verify(aiQuotaService, org.mockito.Mockito.times(4)).checkOperationsAi("manager@example.test");
    }

    @Test
    void invoiceOcrRestrictsAccessToChefManagerAndAdminAndChecksQuota() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "file", "invoice.jpg", org.springframework.http.MediaType.IMAGE_JPEG_VALUE, new byte[] {
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
                });

        mockMvc.perform(multipart(ApiRoutes.AI + "/inventory/invoice-ocr").file(image))
                .andExpect(status().isUnauthorized());

        for (String deniedRole : java.util.List.of("STAFF", "CASHIER")) {
            mockMvc.perform(multipart(ApiRoutes.AI + "/inventory/invoice-ocr")
                            .file(image)
                            .param("language", "vi")
                            .with(user("denied@example.test").roles(deniedRole)))
                    .andExpect(status().isForbidden());
        }

        verifyNoInteractions(aiInvoiceOcrService);

        org.mockito.Mockito.when(aiInvoiceOcrService.parseInvoice(any(), any(), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    invocation.<Runnable>getArgument(2).run();
                    return null;
                });

        mockMvc.perform(multipart(ApiRoutes.AI + "/inventory/invoice-ocr")
                        .file(image)
                        .param("language", "vi")
                        .with(user("chef@example.test").roles("CHEF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(multipart(ApiRoutes.AI + "/inventory/invoice-ocr")
                        .file(image)
                        .param("language", "en")
                        .with(user("manager@example.test").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(multipart(ApiRoutes.AI + "/inventory/invoice-ocr")
                        .file(image)
                        .with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(aiQuotaService).checkOperationsAi("chef@example.test");
        verify(aiQuotaService).checkOperationsAi("manager@example.test");
        verify(aiQuotaService).checkOperationsAi("admin@example.test");
        verify(aiInvoiceOcrService, org.mockito.Mockito.times(3)).parseInvoice(any(), any(), any(Runnable.class));
    }

    @Test
    void batchStockInRestrictsAccessAndPassesCostPermissionByRole() throws Exception {
        String payload = """
                {
                  "supplierName": "NCC A",
                  "invoiceRef": "HD-001",
                  "items": [
                    {
                      "inventoryItemId": 1,
                      "quantity": 2.5,
                      "unitCost": 180000,
                      "updateUnitCost": true
                    }
                  ]
                }
                """;

        mockMvc.perform(post(ApiRoutes.INVENTORY_ITEMS + "/batch-stock-in")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        for (String deniedRole : java.util.List.of("STAFF", "CASHIER")) {
            mockMvc.perform(post(ApiRoutes.INVENTORY_ITEMS + "/batch-stock-in")
                            .with(user("denied@example.test").roles(deniedRole))
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
        }

        verifyNoInteractions(inventoryItemService);

        mockMvc.perform(post(ApiRoutes.INVENTORY_ITEMS + "/batch-stock-in")
                        .with(user("chef@example.test").roles("CHEF"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post(ApiRoutes.INVENTORY_ITEMS + "/batch-stock-in")
                        .with(user("manager@example.test").roles("MANAGER"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post(ApiRoutes.INVENTORY_ITEMS + "/batch-stock-in")
                        .with(user("admin@example.test").roles("ADMIN"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inventoryItemService).batchStockIn(any(), eq(false));
        verify(inventoryItemService, org.mockito.Mockito.times(2)).batchStockIn(any(), eq(true));
    }

    @Test
    void subscriptionEndpointsEnforceSelfManagerAndAdminRbac() throws Exception {
        mockMvc.perform(get(ApiRoutes.SUBSCRIPTION + "/current")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ApiRoutes.SUBSCRIPTION + "/plans")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(ApiRoutes.SUBSCRIPTION + "/invoices")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ApiRoutes.SUBSCRIPTION + "/trial")).andExpect(status().isUnauthorized());

        mockMvc.perform(get(ApiRoutes.SUBSCRIPTION + "/current")
                        .with(user("staff@example.test").roles("STAFF")))
                .andExpect(status().isOk());

        mockMvc.perform(get(ApiRoutes.SUBSCRIPTION + "/plans")
                        .with(user("chef@example.test").roles("CHEF")))
                .andExpect(status().isOk());

        for (String deniedInvoiceRole : java.util.List.of("STAFF", "CHEF", "CASHIER")) {
            mockMvc.perform(get(ApiRoutes.SUBSCRIPTION + "/invoices")
                            .with(user("u@example.test").roles(deniedInvoiceRole)))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(get(ApiRoutes.SUBSCRIPTION + "/invoices")
                        .with(user("manager@example.test").roles("MANAGER")))
                .andExpect(status().isOk());

        for (String deniedAdminRole : java.util.List.of("STAFF", "CHEF", "CASHIER", "MANAGER")) {
            mockMvc.perform(post(ApiRoutes.SUBSCRIPTION + "/trial")
                            .with(user("u@example.test").roles(deniedAdminRole)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post(ApiRoutes.SUBSCRIPTION + "/checkout")
                            .with(user("u@example.test").roles(deniedAdminRole))
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("""
                                    {"billingCycle":"MONTHLY"}
                                    """))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post(ApiRoutes.SUBSCRIPTION + "/invoices/1/sync")
                            .with(user("u@example.test").roles(deniedAdminRole)))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post(ApiRoutes.SUBSCRIPTION + "/invoices/1/cancel")
                            .with(user("u@example.test").roles(deniedAdminRole)))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(post(ApiRoutes.SUBSCRIPTION + "/trial")
                        .with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(post(ApiRoutes.SUBSCRIPTION + "/checkout")
                        .with(user("admin@example.test").roles("ADMIN"))
                        .header("Idempotency-Key", "sub-key-1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"billingCycle":"MONTHLY","returnPath":"/admin/billing"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post(ApiRoutes.SUBSCRIPTION + "/invoices/1/sync")
                        .with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(post(ApiRoutes.SUBSCRIPTION + "/invoices/1/cancel")
                        .with(user("admin@example.test").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(subscriptionService).activateTrial("admin@example.test");
        verify(subscriptionService).createCheckout(any(), eq("sub-key-1"), eq("admin@example.test"));
        verify(subscriptionService).syncInvoiceStatus(1L);
        verify(subscriptionService).cancelInvoice(1L);
    }

    @Test
    void corsPreflightAllowsIdempotencyKeyHeaderForSubscriptionCheckout() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options(
                                ApiRoutes.SUBSCRIPTION + "/checkout")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type,idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string(
                                "Access-Control-Allow-Headers",
                                org.hamcrest.Matchers.containsStringIgnoringCase("Idempotency-Key")));
    }

    @TestConfiguration
    @EnableConfigurationProperties({AppProperties.class, RateLimitProperties.class})
    static class PropertiesConfiguration {}

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {}
}
