# Technical Plan `006`: AI Operations Suite

- **Spec tham chiếu:** [spec.md](./spec.md)
- **Constitution tham chiếu:** [CONSTITUTION.md](../CONSTITUTION.md)

---

## 1. Ảnh hưởng Kiến trúc & Hợp đồng giao tiếp

### Database / Flyway
- Thêm migration mới `backend/src/main/resources/db/migration/V1.24__Inventory_Unit_Cost.sql` *(cấm sửa `V1`–`V1.23`)*:
  - Bổ sung cột `unit_cost NUMERIC(15, 2) NOT NULL DEFAULT 0` trên `inventory_items`.

### REST API / Security / Quota
| Method | Endpoint | Auth / Role | Mục đích & Bảo vệ |
|---|---|---|---|
| `POST` | `/api/v1/ai/inventory/restock-suggestions` | `INVENTORY_AI_POST` + `@PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")` + `checkOperationsAi` (`15/1m`, `150/1d`) | Dự toán nhu cầu nhập kho từ BOM + lịch sử bán 30 ngày + fallback |
| `POST` | `/api/v1/ai/inventory/recipe-assistant` | `INVENTORY_AI_POST` + `@PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")` + `checkOperationsAi` | Kiểm tra COGS % trực tiếp + AI gợi ý định mức từ nguyên liệu đang hoạt động + fallback |
| `POST` | `/api/v1/ai/merchandising/combo-suggestions` | `MANAGER_POST` + `@PreAuthorize("hasRole('MANAGER')")` + `checkOperationsAi` | Đề xuất combo từ cặp món đặt chung + xác thực món còn bán + fallback |
| `POST` | `/api/v1/ai/merchandising/promotion-suggestions` | `MANAGER_POST` + `@PreAuthorize("hasRole('MANAGER')")` + `checkOperationsAi` | Đề xuất khung giờ thấp điểm & ngày áp dụng + fallback |
| `POST` | `/api/v1/ai/staff/analyze-notes` | `STAFF_AI_POST` + `@PreAuthorize("hasAnyRole('MANAGER','STAFF','CHEF')")` + `checkStaffAssistant` | Phân loại & ưu tiên ghi chú bếp + yêu cầu bàn; không hạ cấp `CRITICAL` từ `NoteSafetyAnalyzer` |

---

## 2. Danh sách File dự kiến thêm / sửa
- **Backend:** `V1.24__Inventory_Unit_Cost.sql`, `InventoryItem*` (`unitCost`), `RecipeItemRepository`, `OrderItemRepository`, `AnalyticsQueryRepository` + `HourlyTrafficProjection` / `DayOfWeekTrafficProjection`, `AiQuotaProperties`/`AiQuotaService`/`application.yml`, `SecurityRoutes`/`SecurityConfig`, `NoteSafetyAnalyzer`, `AiPromptBuilder`, `AiInventoryRestockService`, `AiMerchandisingService`, `AiNoteTriageService`, `AiRecipeAssistantService`, `AiInventoryController`, `AiMerchandisingController`, `AiStaffController` + DTOs & Unit/Security Tests.
- **Frontend:** `aiService.js`, `noteSafety.js`, `NoteSafetyBadges.jsx`, `InventoryModal.jsx` + `AiRestockPanel.jsx` + `RecipeModal.jsx` + helpers, `AiComboSuggestionsPanel.jsx` + `ComboModal.jsx`, `AiPromotionSuggestionsPanel.jsx` + `PromotionModal.jsx`, `kitchenItems.js` + `KitchenCard.jsx` + `BatchCookingView.jsx` + `KitchenFilters.jsx` + `AiNoteTriageCard.jsx` + `KitchenBoard.jsx`, `ServiceRequestCard.jsx` + `ServiceRequestQueue.jsx`, `OrderDetailModal.jsx` + Unit Tests.
