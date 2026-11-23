# Execution Tasks `007`: Nhập kho từ ảnh hóa đơn (Gemini Vision)

- **Spec:** [spec.md](./spec.md) · **Plan:** [plan.md](./plan.md)
- **DoD bắt buộc:**
  - Backend: `mvn -f backend/pom.xml spotless:apply && mvn -f backend/pom.xml --batch-mode verify`
  - Frontend: `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build`

---

## Nhóm 1 — Backend Multimodal Vision, Invoice OCR & Atomic Batch Stock-In
- [x] Mở rộng `AiChatGateway` và `GeminiChatGateway` với `chatWithImage` (tách `executeGeminiRequest`, hỗ trợ `inline_data`).
- [x] Thêm `buildInvoiceOcrPrompt` trong `AiPromptBuilder`, tạo `AiInvoiceOcrResponse` và `AiInvoiceOcrService` (validate bằng `ImageFileValidator`, fuzzy match, tự quy đổi `kg↔g`, `l↔ml`, không giữ DB transaction khi gọi Gemini).
- [x] Bổ sung `POST /api/v1/ai/inventory/invoice-ocr` trong `AiInventoryController`.
- [x] Tạo `BatchStockInRequest`, `BatchStockInResponse`, triển khai `InventoryItemService.batchStockIn` (`findAllByIdInForUpdate`, RBAC chặn `CHEF` sửa `unitCost`) và `POST /api/v1/inventory/items/batch-stock-in` trên `InventoryItemController` + `SecurityRoutes`.
- [x] Viết test `GeminiChatGatewayTest` (`chatWithImage`), `AiInvoiceOcrServiceTest`, `InventoryBatchStockInServiceTest`, cập nhật `CoreApiSecurityIntegrationTest`.

## Nhóm 2 — Frontend `SCR-18` Invoice OCR Review & Batch Import
- [x] Bổ sung `unitCost` & `batchStockIn` trong `entities/inventory/api/{inventoryService,inventoryMutations}.js` và `scanInvoice` trong `shared/api/aiService.js`.
- [x] Tạo pure helper `invoiceOcrHelpers.js` (kiểm tra file, quy đổi đơn vị, dựng review rows, tính lại khi đổi nguyên liệu, build payload theo quyền `isManager`).
- [x] Tạo `InvoiceOcrModal.jsx` (chọn file + camera mobile, xem trước, bảng chỉnh sửa rà soát 5 trạng thái UI, Light/Dark WCAG AA, song ngữ `VI`/`EN`) và gắn vào `InventoryToolbar.jsx` + `InventoryManagement.jsx`.
- [x] Viết `frontend/tests/unit/invoiceOcr.test.js` và đăng ký trong `frontend/tests/run-tests.js`.

## Nhóm 3 — Verification & Closure
- [x] Chạy `mvn -f backend/pom.xml spotless:apply && mvn -f backend/pom.xml --batch-mode verify` → pass 100%.
- [x] Chạy `npm --prefix frontend test && npm --prefix frontend run lint && npm --prefix frontend run build` → pass 100%.
- [x] Chuyển `spec.md`, `tasks.md`, `KNOWLEDGE_HUB.md` (`007-ai-invoice-stock-in`) sang `Implemented (Pending Sync-Back Review)`.
