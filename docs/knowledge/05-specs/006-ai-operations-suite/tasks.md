# Execution Tasks `006`: AI Operations Suite

- **Spec:** [spec.md](./spec.md) · **Plan:** [plan.md](./plan.md)
- **DoD bắt buộc:**
  - Backend: `mvn -f backend/pom.xml spotless:apply && mvn -f backend/pom.xml --batch-mode verify`
  - Frontend: `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`

---

## Nhóm 1 — Flyway `V1.24`, Repository Queries, Quota & Security
- [x] Tạo `V1.24__Inventory_Unit_Cost.sql`, cập nhật `InventoryItem`, `InventoryItemRequest`, `InventoryItemResponse`, `InventoryItemMapper`.
- [x] Thêm queries đọc `RecipeItemRepository.findAllActiveWithMenuAndInventory`, `OrderItemRepository.findFrequentlyPairedMenuItems`, `AnalyticsQueryRepository.{hourlyOrderTraffic, dayOfWeekOrderTraffic}` + projections.
- [x] Bổ sung quota `operationsUser` / `operationsUserDaily` và cấu hình `SecurityRoutes` + `SecurityConfig` (`INVENTORY_AI_POST`, `STAFF_AI_POST`, `MANAGER_POST`).

## Nhóm 2 — Backend AI Services, Controllers & Tests
- [x] Tạo `NoteSafetyAnalyzer` và mở rộng `AiPromptBuilder`.
- [x] Triển khai `AiInventoryRestockService` + `AiInventoryController` (`/restock-suggestions`, `/recipe-assistant`) + `AiRecipeAssistantService`.
- [x] Triển khai `AiMerchandisingService` + `AiMerchandisingController` (`/combo-suggestions`, `/promotion-suggestions`).
- [x] Triển khai `AiNoteTriageService` + cập nhật `AiStaffController` (`/analyze-notes`).
- [x] Viết đầy đủ unit tests (`NoteSafetyAnalyzerTest`, `AiInventoryRestockServiceTest`, `AiMerchandisingServiceTest`, `AiNoteTriageServiceTest`, `AiRecipeAssistantServiceTest`) và cập nhật `AiQuotaServiceTest`, `SecurityConfigRegressionTest`, `CoreApiSecurityIntegrationTest`.

## Nhóm 3 — Frontend UI & Unit Tests
- [x] Bổ sung API clients (`aiService.js`), pure helper `noteSafety.js`, UI `<NoteSafetyBadges>`.
- [x] Inventory (`SCR-18`): trường `unitCost`, `AiRestockPanel`, `useStockAction` pre-fill, `RecipeModal` Food Cost bar + AI BOM panel + phân quyền lưu.
- [x] Merchandising (`SCR-17`, `SCR-20`): `AiComboSuggestionsPanel`, `AiPromotionSuggestionsPanel`, `aiMerchandisingDrafts.js`, tạo bản nháp 1 chạm trong `ComboModal` & `PromotionModal`.
- [x] Kitchen & Staff (`SCR-12`, `SCR-27`, `SCR-11`): `kitchenItems.js`, `KitchenCard`, `BatchCookingView`, `KitchenFilters` (`ALLERGEN`), `@shared/ui/AiNoteTriageCard`, `ServiceRequestQueue`, `ServiceRequestCard`, `OrderDetailModal`.
- [x] Tạo `noteSafety.test.js`, `aiOperations.test.js`, cập nhật `inventory.test.js`, `kitchenItems.test.js`, `run-tests.js`.

## Nhóm 4 — Verification & Pending Sync-Back Review
- [x] Chạy `mvn -f backend/pom.xml spotless:apply && mvn -f backend/pom.xml --batch-mode verify` → pass 100%.
- [x] Chạy `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build` → pass 100%.
- [x] Chuyển `spec.md` sang `Implemented (Pending Sync-Back Review)`, cập nhật `tasks.md` và `KNOWLEDGE_HUB.md`.
