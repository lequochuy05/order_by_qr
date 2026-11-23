# Technical Plan `005`: Actionable Customer AI Ordering & AI Executive Insights

- **Spec tham chiếu:** [spec.md](./spec.md)
- **Constitution tham chiếu:** [CONSTITUTION.md](../CONSTITUTION.md)

---

## 1. Ảnh hưởng Kiến trúc & Hợp đồng giao tiếp

### Database / Flyway
- **Không phát sinh migration mới** *(giữ nguyên `V1`–`V1.23`, không sửa migration đã áp dụng)*.
- `TableServiceRequestRepository`: bổ sung truy vấn đọc `findByCreatedAtRange(LocalDateTime from, LocalDateTime toExclusive)` để tổng hợp thống kê yêu cầu bàn theo khung thời gian cho báo cáo điều hành.

### REST API / Security / Rate Limit
| Method | Endpoint | Auth / Role | Mục đích & Bảo vệ |
|---|---|---|---|
| `POST` | `/api/v1/public/ai/chat` | Public (`PUBLIC_POST`) + `RateLimitingFilter` (`public:ai`) + `AiQuotaService.checkPublicChat` | Nâng cấp response `AiChatResponse(String reply, List<AiSuggestedItem> suggestions)` giữ tương thích ngược constructor 1 tham số; kiểm tra live catalog DB trước khi trả `suggestions` |
| `POST` | `/api/v1/ai/insights/summary` | Protected (`MANAGER_POST` + `@PreAuthorize("hasRole('MANAGER')")` — cho phép `MANAGER` & `ADMIN`, chặn `STAFF`/`CASHIER`/`CHEF`/anonymous) + `AiQuotaService.checkExecutiveInsights` (`10/1m`, `80/1d`) | Nhận `{ from, to, language }`, tổng hợp số liệu từ `AnalyticsQueryRepository`, `KitchenService`, `TableServiceRequestRepository`, `InventoryItemRepository`, gọi `AiChatGateway` và fallback xác định khi AI gián đoạn |

---

## 2. Danh sách File dự kiến thêm / sửa

### Backend (`backend/`)
- `modules/ai/dto/response/AiSuggestedItem.java` *(new)*
- `modules/ai/dto/response/AiChatResponse.java` *(modify)*
- `modules/ai/service/MenuContextProvider.java` *(modify — lọc `available = true`, gắn `[ITEM:<id>]`/`[COMBO:<id>]`, thêm `loadAvailableCatalog()`)*
- `modules/ai/support/AiPromptBuilder.java` *(modify — prompt JSON cho customer ordering & executive summary)*
- `modules/ai/service/AiAssistantService.java` *(modify — parse JSON Jackson 3 an toàn, đối chiếu DB, fallback text-only)*
- `modules/ai/config/AiQuotaProperties.java` *(modify — thêm `insightsUser`, `insightsUserDaily`)*
- `modules/ai/service/AiQuotaService.java` *(modify — thêm `checkExecutiveInsights`)*
- `modules/ai/dto/request/AiExecutiveSummaryRequest.java` *(new)*
- `modules/ai/dto/response/AiExecutiveSummaryResponse.java` *(new)*
- `modules/ai/service/ExecutiveInsightsContextProvider.java` *(new)*
- `modules/ai/service/AiExecutiveInsightsService.java` *(new)*
- `modules/ai/controller/AiInsightsController.java` *(new)*
- `modules/table/repository/TableServiceRequestRepository.java` *(modify — thêm `findByCreatedAtRange`)*
- `core/config/security/SecurityRoutes.java` *(modify — thêm `ApiRoutes.AI + "/insights/**"` vào `MANAGER_POST`)*
- `resources/application.yml` *(modify — thêm `ai.quota.insights-user*`)*
- Tests: `AiAssistantServiceTest.java` *(new)*, `AiExecutiveInsightsServiceTest.java` *(new)*, `AiQuotaServiceTest.java` *(modify)*, `SecurityConfigRegressionTest.java` *(modify)*, `CoreApiSecurityIntegrationTest.java` *(modify)*

### Frontend (`frontend/`)
- `features/ai-assistant/lib/chatSuggestions.js` *(new — pure helper, không import `@` alias để chạy trực tiếp `node:test`)*
- `features/ai-assistant/lib/aiCopy.js` *(modify)*
- `features/ai-assistant/model/useAiChat.js` *(modify)*
- `features/ai-assistant/ui/AiSuggestionList.jsx` *(new)*
- `features/ai-assistant/ui/ChatMessages.jsx` *(modify — hỗ trợ Light/Dark & render `AiSuggestionList`)*
- `features/ai-assistant/ui/AiChatWindow.jsx` *(modify — hỗ trợ Light/Dark & truyền `orderBridge`)*
- `features/ai-assistant/ui/AiChatAssistant.jsx` *(modify)*
- `features/customer-menu/ui/CustomerMenu.jsx` *(modify — cung cấp `orderBridge` cho `renderAiAssistant`)*
- `pages/customer/OrderingPage.jsx` *(modify — nối `orderBridge` vào `AiChatAssistant` giữ sạch ranh giới FSD)*
- `entities/analytics/api/analyticsService.js` *(modify — thêm `getAiExecutiveSummary`)*
- `features/dashboard-overview/lib/executiveInsights.js` *(new — pure helper, không import `@` alias)*
- `features/dashboard-overview/ui/AiExecutiveSummaryCard.jsx` *(new)*
- `features/dashboard-overview/model/useDashboard.js` *(modify — expose `businessToday`)*
- `features/dashboard-overview/ui/DashboardOverview.jsx` *(modify — gắn `AiExecutiveSummaryCard`)*
- Tests: `frontend/tests/unit/aiAssistant.test.js` *(modify)*, `frontend/tests/unit/executiveInsights.test.js` *(new)*, `frontend/tests/run-tests.js` *(modify)*
