---
trigger: glob
globs: "backend/src/main/java/com/qros/modules/ai/**"
---

# AI Module Rules (`com.qros.modules.ai.*`)

Mọi tính năng AI mới hoặc chỉnh sửa trong `com.qros.modules.ai` bắt buộc tuân thủ kiến trúc chuẩn gồm 4 thành phần cốt lõi dưới đây:

## 1. Luồng xử lý Bắt buộc cho Mọi Tính năng AI

1. **Kiểm tra hạn mức qua `AiQuotaService` (`service/AiQuotaService.java`):**
   - Bắt buộc gọi kiểm tra quota (rate-limit burst + daily trên Redis qua `RedisRateLimiter`) ngay tại Controller trước khi gọi AI service.
   - Tái sử dụng đúng bucket có sẵn: `checkPublicChat(httpRequest)` (public theo IP), `checkStaffAssistant(username)`, `checkMenuDescription(username)`, `checkExecutiveInsights(username)`, `checkOperationsAi(username)`. Nếu thêm nhóm năng lực mới, khai báo policy trong `AiQuotaProperties` và thêm method tương ứng trong `AiQuotaService`.
2. **Thu thập ngữ cảnh qua `*ContextProvider` (`service/*ContextProvider.java`):**
   - Mọi dữ liệu nghiệp vụ đưa vào prompt phải được nạp và chuẩn hóa thông qua `*ContextProvider` (`MenuContextProvider`, `SystemSettingsContextProvider`, `StaffContextProvider`, `ExecutiveInsightsContextProvider`, hoặc provider mới theo cùng quy ước đặt tên).
   - `*ContextProvider` chỉ gọi qua service/port của module sở hữu dữ liệu — **cấm** inject trực tiếp `*Repository` của module khác (ArchUnit).
3. **Dựng prompt tập trung tại `AiPromptBuilder` (`support/AiPromptBuilder.java`):**
   - **Cấm** viết hoặc ghép chuỗi prompt trực tiếp trong Controller/Service. Mọi system prompt / task prompt phải là method trong `AiPromptBuilder`.
   - Prompt phải khóa rõ ràng: (a) grounding 100% vào context được cấp (không bịa món, ID, giá, số liệu), (b) JSON schema đầu ra (không bọc markdown fence), và (c) phòng thủ prompt injection bằng thẻ `<untrusted_customer_data>...</untrusted_customer_data>` cho mọi dữ liệu từ khách hàng/ghi chú tự do.
4. **Gọi LLM duy nhất qua `AiChatGateway` (`gateway/AiChatGateway.java`):**
   - Mọi cuộc gọi model (text hoặc multimodal/OCR) phải đi qua `AiChatGateway` (`chat(...)`, `chatWithImage(...)` — triển khai mặc định `GeminiChatGateway`).
   - **Cấm** khởi tạo HTTP client hoặc gọi thẳng Gemini API từ service/controller.

## 2. An toàn Đầu ra, Quyền & Quan sát (Observability)

- **Parse JSON & Graceful Fallback:** Dùng `AiJsonExtractor.extractJsonObject(...)` kết hợp Jackson `ObjectMapper` để trích xuất JSON; luôn chuẩn bị fallback an toàn khi LLM lỗi hoặc trả JSON không hợp lệ. Mọi ID thực thể AI đề xuất (`menuItemId`, `comboId`, `inventoryItemId`, ...) phải được kiểm tra/hydrate lại từ snapshot thực tế trước khi trả về client.
- **Bảo mật & Gói cước:**
  - Khai báo route bằng `ApiRoutes.AI` (hoặc `ApiRoutes.PUBLIC + "/ai"` cho khách hàng) và đăng ký trong `SecurityRoutes` (`PUBLIC_POST`, `STAFF_AI_POST`, `INVENTORY_AI_POST`, hoặc `MANAGER_POST`).
  - Gắn đầy đủ `@PreAuthorize(...)` (với endpoint nội bộ) và `@RequiresPlanFeature(PlanFeature.*)` (`AI_CUSTOMER_ASSISTANT` hoặc `AI_OPERATIONS_SUITE`).
- **Metrics:** Ghi nhận counter `ai.requests.total` gắn tag `feature` qua `MeterRegistry`.
