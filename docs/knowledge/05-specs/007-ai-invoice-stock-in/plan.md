# Technical Plan `007`: Nhập kho từ ảnh hóa đơn (Gemini Vision)

- **Spec tham chiếu:** [spec.md](./spec.md)
- **Constitution tham chiếu:** [CONSTITUTION.md](../CONSTITUTION.md)

---

## 1. Kiến trúc, API & Bảo mật

| Method | Endpoint | Auth / Role | Mục đích & Bảo vệ |
|---|---|---|---|
| `POST` | `/api/v1/ai/inventory/invoice-ocr` | `INVENTORY_AI_POST` + `@PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")` + `checkOperationsAi` (`15/1m`, `150/1d`) | Nhận `multipart/form-data`, kiểm tra định dạng/magic bytes qua `ImageFileValidator`, gọi Gemini Vision `inline_data` ngoài transaction DB, khớp nguyên liệu đang hoạt động & quy đổi `kg↔g` / `l↔ml` |
| `POST` | `/api/v1/inventory/items/batch-stock-in` | `INVENTORY_OPERATION_POST` + `@PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")` | Nhập kho nguyên tử tối đa 50 dòng bằng `findAllByIdInForUpdate` theo thứ tự ID, ghi `STOCK_IN` movement, chặn `updateUnitCost=true` với `CHEF` ở tầng service (`403 FORBIDDEN`) |

---

## 2. Danh sách File dự kiến thêm / sửa
- **Backend:**
  - `AiChatGateway.java`, `GeminiChatGateway.java` (hỗ trợ multimodal `chatWithImage`, tách helper `executeGeminiRequest` tránh trùng lặp)
  - `AiPromptBuilder.java`, `AiInvoiceOcrResponse.java`, `AiInvoiceOcrService.java`, `AiInventoryController.java`
  - `BatchStockInRequest.java`, `BatchStockInResponse.java`, `InventoryItemService.java`, `InventoryItemController.java`, `SecurityRoutes.java`
  - Tests: `GeminiChatGatewayTest.java`, `AiInvoiceOcrServiceTest.java`, `InventoryBatchStockInServiceTest.java`, `CoreApiSecurityIntegrationTest.java`
- **Frontend:**
  - `entities/inventory/api/{inventoryService,inventoryMutations}.js`, `shared/api/aiService.js`
  - `features/inventory-management/lib/invoiceOcrHelpers.js`, `ui/modals/InvoiceOcrModal.jsx`, `ui/components/InventoryToolbar.jsx`, `ui/InventoryManagement.jsx`
  - Tests: `frontend/tests/unit/invoiceOcr.test.js`, `frontend/tests/run-tests.js`
