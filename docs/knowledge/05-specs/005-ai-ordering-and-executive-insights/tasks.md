# Execution Tasks `005`: Actionable Customer AI Ordering & AI Executive Insights

- **Spec:** [spec.md](./spec.md) · **Plan:** [plan.md](./plan.md)
- **DoD bắt buộc (khớp CI):**
  - Backend: `mvn -f backend/pom.xml spotless:apply && mvn -f backend/pom.xml --batch-mode verify`
  - Frontend: `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`

---

## Nhóm 1 — Backend: Customer Actionable AI Ordering (`F1.5`, `BR-29`..`30`)
- [x] Tạo `AiSuggestedItem.java` và mở rộng `AiChatResponse.java` kèm constructor 1 tham số giữ tương thích ngược.
- [x] Cập nhật `MenuContextProvider.java`: lọc `findAllPublicAvailableItems` + `findAllActiveWithItems`, gắn `[ITEM:<id>]`/`[COMBO:<id>]`, và thêm `loadAvailableCatalog()`.
- [x] Cập nhật `AiPromptBuilder.buildSystemPrompt` yêu cầu JSON cấu trúc `{ reply, suggestions[] }` bám sát ID trong thực đơn.
- [x] Cập nhật `AiAssistantService.java`: parse JSON an toàn bằng `tools.jackson.databind.ObjectMapper`, đối chiếu live DB, giữ đơn giá/tên/tùy chọn chuẩn, chống trùng & giới hạn tối đa 5 thẻ, fallback an toàn sang văn bản thuần khi lỗi JSON.
- [x] Viết `AiAssistantServiceTest.java` kiểm chứng parse JSON, fenced JSON, lọc ID ảo/hết hàng/trùng, giới hạn 5 thẻ, và fallback plain-text.

## Nhóm 2 — Backend: AI Executive Summary (`F6.6`, `BR-31`)
- [x] Thêm `findByCreatedAtRange` vào `TableServiceRequestRepository.java`.
- [x] Bổ sung quota `insightsUser` / `insightsUserDaily` vào `AiQuotaProperties.java`, `AiQuotaService.java`, và `application.yml`.
- [x] Tạo `AiExecutiveSummaryRequest.java`, `AiExecutiveSummaryResponse.java`, `ExecutiveInsightsContextProvider.java`, và `AiPromptBuilder.buildExecutiveSummaryPrompt`.
- [x] Tạo `AiExecutiveInsightsService.java` (validate khoảng ngày ≤ 90 ngày với thông báo lỗi tiếng Anh, parse JSON Gemini, và fallback xác định theo quy tắc bằng `VI`/`EN` khi Gemini gián đoạn).
- [x] Tạo `AiInsightsController.java` (`POST /api/v1/ai/insights/summary`, `@PreAuthorize("hasRole('MANAGER')")`) và khai báo `SecurityRoutes.MANAGER_POST`.
- [x] Viết `AiExecutiveInsightsServiceTest.java`, cập nhật `AiQuotaServiceTest.java`, `SecurityConfigRegressionTest.java`, và `CoreApiSecurityIntegrationTest.java`.

## Nhóm 3 — Frontend: Chat Cards (`SCR-05`) & Executive Card (`SCR-10`)
- [x] Tạo pure helper `features/ai-assistant/lib/chatSuggestions.js`, cập nhật `aiCopy.js`, `useAiChat.js`, tạo `AiSuggestionList.jsx`, cập nhật `ChatMessages.jsx`, `AiChatWindow.jsx`, `AiChatAssistant.jsx` (hỗ trợ Light/Dark).
- [x] Nối `orderBridge` từ `CustomerMenu.jsx` → `OrderingPage.jsx` → `AiChatAssistant.jsx` (tôn trọng khóa đặt món, mở `ItemOptionsModal` cho món có tùy chọn, giữ sạch FSD).
- [x] Thêm `getAiExecutiveSummary` trong `analyticsService.js`, tạo pure helper `executiveInsights.js`, tạo `AiExecutiveSummaryCard.jsx` (5 trạng thái UI, Light/Dark WCAG AA, `VI`/`EN`), và tích hợp vào `useDashboard.js` + `DashboardOverview.jsx`.
- [x] Cập nhật `frontend/tests/unit/aiAssistant.test.js`, tạo `frontend/tests/unit/executiveInsights.test.js` và đăng ký vào `frontend/tests/run-tests.js`.

## Nhóm 4 — Verification & Pending Sync-Back Review
- [x] Chạy `mvn -f backend/pom.xml spotless:apply && mvn -f backend/pom.xml --batch-mode verify` → pass 100% (`280 / 280`).
- [x] Chạy `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build` → pass 100% (`128 / 128`, 0 lint errors, Vite build passed).
- [x] Cập nhật `spec.md` sang `Implemented (Pending Sync-Back Review)` và tick hoàn tất checklist thực thi.
- [x] *(Chỉ tick sau khi người dùng duyệt nghiệm thu)* Chuyển `spec.md` sang `Archived` trong `docs/knowledge/KNOWLEDGE_HUB.md`.
