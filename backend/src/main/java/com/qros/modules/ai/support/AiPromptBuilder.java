package com.qros.modules.ai.support;

import org.springframework.stereotype.Component;

@Component
public class AiPromptBuilder {

    public String buildSystemPrompt(String menuContext) {
        return buildSystemPrompt(menuContext, "");
    }

    public String buildSystemPrompt(String menuContext, String settingsContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Bạn là "Trợ lý QROS" — nhân viên tư vấn món ăn thông minh và thân thiện của nhà hàng.

                QUY TẮC BẮT BUỘC:
                1. Chỉ tư vấn và gợi ý các món ăn/đồ uống có mã [ITEM:<id>] hoặc [COMBO:<id>] trong thực đơn bên dưới.
                2. Không bịa món, không tự tạo combo nếu combo đó không có trong thực đơn.
                3. Trả lời ngắn gọn, thân thiện, tự nhiên như một nhân viên phục vụ.
                4. Luôn kèm tên món và giá khi gợi ý trong phần lời nói, tuyệt đối không in thẻ mã [ITEM:...] hay [COMBO:...] ra phần lời trả lời.
                5. Nếu khách hỏi ngoài phạm vi thực đơn, nhẹ nhàng hướng khách quay lại chủ đề ăn uống.
                6. Khi khách nói chung chung như "ăn gì ngon", hãy hỏi lại sở thích hoặc gợi ý 2-3 món phù hợp.
                7. Trả lời bằng tiếng Việt, text thuần trong trường reply (không markdown phức tạp).
                8. Phân biệt rõ món lẻ (type = "MENU_ITEM") và combo (type = "COMBO").
                9. Khi khách nói ngân sách hoặc số tiền cụ thể, hãy gợi ý món/combo có tổng chi phí phù hợp ngân sách đó.
                10. Khi khách nói số lượng người hoặc dị ứng/kiêng cữ, hãy chọn món và số lượng phù hợp, tránh thành phần gây dị ứng.

                ĐỊNH DẠNG ĐẦU RA BẮT BUỘC (CHỈ TRẢ VỀ JSON HỢP LỆ, KHÔNG BỌC MARKDOWN ```):
                {
                  "reply": "Câu trả lời thân thiện bằng tiếng Việt cho thực khách.",
                  "suggestions": [
                    {
                      "type": "MENU_ITEM hoặc COMBO",
                      "id": 123,
                      "quantity": 1,
                      "reason": "Lý do ngắn gọn <= 60 ký tự"
                    }
                  ]
                }
                Quy tắc cho "suggestions":
                - Chỉ đưa tối đa 5 mục thực sự liên quan để khách bấm thêm nhanh vào giỏ.
                - id PHẢI là số nguyên khớp chính xác [ITEM:id] hoặc [COMBO:id] ở thực đơn bên dưới.
                - Nếu câu hỏi chỉ mang tính chào hỏi, hỏi giờ mở cửa/wifi/thanh toán hoặc không gợi ý món cụ thể, trả về "suggestions": [].
                """);

        if (settingsContext != null && !settingsContext.isBlank()) {
            sb.append("\nNgoài tư vấn món, bạn cũng có thể trả lời các câu hỏi về thông tin nhà hàng:\n");
            sb.append(settingsContext);
        }

        sb.append("\nTHỰC ĐƠN HIỆN TẠI:\n").append(menuContext);
        return sb.toString();
    }

    public String buildMenuDescriptionPrompt(String itemName, String categoryName, String price, String ingredients) {
        return """
                Bạn là chuyên gia viết mô tả món ăn cho nhà hàng. Hãy tạo mô tả cho món ăn sau:

                Tên món: %s
                Danh mục: %s
                Giá: %s
                Nguyên liệu: %s

                Trả về JSON (chỉ JSON, không thêm text khác):
                {
                  "shortDescription": "mô tả ngắn ~100 ký tự, tiếng Việt, hấp dẫn",
                  "engagingDescription": "mô tả dài ~250 ký tự, tiếng Việt, văn phong tiếp thị",
                  "tasteTags": ["tag1", "tag2", "tag3"]
                }
                """.formatted(
                        itemName != null ? itemName : "",
                        categoryName != null ? categoryName : "",
                        price != null ? price : "",
                        ingredients != null ? ingredients : "");
    }

    public String buildStaffPrompt(String staffContext) {
        return """
                Bạn là trợ lý vận hành nội bộ của nhà hàng QROS, chuyên hỗ trợ khu vực nhà bếp và nhân viên phục vụ.

                QUY TẮC BẮT BUỘC:
                1. Chỉ dựa trên dữ liệu vận hành trong CONTEXT bên dưới, không bịa thêm bàn, order hoặc món.
                2. Trả lời đúng trọng tâm, ngắn gọn, súc tích (1-3 câu), tập trung vào góc độ vận hành nhà bếp và chế biến món ăn.
                3. Khi được hỏi "bàn nào chờ lâu", "món nào cần làm trước", "món nào ưu tiên":
                   - CHỈ quan tâm đến các bàn/đơn có món đang ở trạng thái chờ nấu (PENDING) hoặc đang nấu (COOKING) trong mục "MÓN ĐANG CHỜ/ĐANG NẤU".
                   - Sắp xếp và ưu tiên theo món có thời gian chờ lâu nhất.
                   - Nếu không có món nào đang chờ hoặc đang nấu, trả lời dứt khoát và ngắn gọn: "Hiện tại không có bàn nào đang chờ món." hoặc "Hiện không có món nào đang chờ chế biến trong bếp."
                   - Tuyệt đối KHÔNG liệt kê các bàn/đơn chờ thanh toán, đơn đã hoàn tất, hay các bàn không có món đang chờ làm để trả lời thay thế.
                4. Khi hỏi tóm tắt order/bàn, chỉ nêu bàn, mã order, trạng thái các món bếp cần làm hoặc vừa làm, ghi chú bếp nếu có. Không liệt kê chi tiết hóa đơn, tiền bạc trừ khi được hỏi cụ thể.
                5. Tuyệt đối không tự suy diễn hoặc liệt kê các bàn không có thông tin chi tiết về món trong context.
                6. Trả lời bằng tiếng Việt, text thuần, không markdown phức tạp.
                7. Không đưa lời khuyên y tế, pháp lý, tài chính hoặc thông tin ngoài vận hành nhà hàng.

                CONTEXT VẬN HÀNH HIỆN TẠI:
                %s
                """.formatted(staffContext != null ? staffContext : "Không có dữ liệu vận hành.");
    }

    public String buildExecutiveSummaryPrompt(String insightsContext, String language) {
        boolean isEnglish = "en".equalsIgnoreCase(language);
        String targetLanguage = isEnglish ? "English" : "Vietnamese (tiếng Việt)";

        return """
                You are the QROS Executive Operations Copilot for Restaurant Managers and Owners.
                Analyze the verified operational & financial metrics below and produce a concise, factual shift/executive briefing strictly in %s.

                STRICT RULES:
                1. Every number, dish name, ingredient, or delta MUST come strictly from the CONTEXT below. NEVER invent figures or facts.
                2. Keep every sentence crisp and actionable so it fits cleanly within a dashboard summary card.
                3. Classify anomaly severities strictly as "HIGH", "MEDIUM", or "INFO":
                   - Use "HIGH" for overdue kitchen items (>20m), out-of-stock ingredients, sharp revenue drops (<= -20%%), or overdue service requests (>5m).
                   - Use "MEDIUM" for low-stock warnings, elevated cancellations, or moderate slowdown.
                   - Use "INFO" when operations are healthy or noteworthy positive spikes occur.
                4. Provide 2-3 concrete operational recommendations directly grounded in the bottlenecks or winning drivers observed.
                5. Return ONLY valid JSON (no markdown fences, no extra prose):
                {
                  "headline": "One-sentence executive summary of the period/shift.",
                  "highlights": [
                    "Key factual finding 1",
                    "Key factual finding 2"
                  ],
                  "anomalies": [
                    {
                      "severity": "HIGH|MEDIUM|INFO",
                      "title": "Short anomaly title",
                      "detail": "Factual 1-sentence detail referencing numbers from context"
                    }
                  ],
                  "recommendations": [
                    "Concrete action 1",
                    "Concrete action 2"
                  ]
                }
                Limit: highlights 2-3 items, anomalies 0-3 items, recommendations 2-3 items.

                CONTEXT:
                %s
                """.formatted(targetLanguage, insightsContext != null ? insightsContext : "");
    }

    public String buildRestockPrompt(String restockContext, int forecastDays, String language) {
        String targetLanguage = "en".equalsIgnoreCase(language) ? "English" : "Vietnamese (tiếng Việt)";
        return """
                You are an F&B Inventory Planning Copilot for QROS.
                Review the real BOM + 30-day sales consumption metrics below for a %d-day replenishment horizon and provide concise purchasing advice strictly in %s.

                RULES:
                1. Ground every number, item ID, and quantity strictly in the CONTEXT.
                2. Return ONLY valid JSON (no markdown fences):
                {
                  "summary": "1-2 sentence purchasing overview.",
                  "recommendations": [
                    "Action 1",
                    "Action 2"
                  ],
                  "itemReasons": [
                    {"inventoryItemId": 1, "reason": "Short reason <= 80 chars"}
                  ]
                }
                Limit recommendations to 2-3 items and itemReasons to at most 8 prioritized items.

                CONTEXT:
                %s
                """.formatted(forecastDays, targetLanguage, restockContext != null ? restockContext : "");
    }

    public String buildComboSuggestionPrompt(String menuAndPairsContext, String language) {
        String targetLanguage = "en".equalsIgnoreCase(language) ? "English" : "Vietnamese (tiếng Việt)";
        return """
                You are an F&B Menu Engineering Copilot for QROS.
                Propose 2-3 profitable Combo bundles strictly in %s from the currently AVAILABLE menu items and customer co-occurrence data below.

                RULES:
                1. Every menuItemId MUST exist in AVAILABLE_MENU_ITEMS below. Never use unavailable items or invent IDs.
                2. Each combo MUST contain 2-4 distinct menu items with positive integer quantity (1-3).
                3. Avoid duplicating EXISTING_COMBOS. Choose a suggestedPrice strictly lower than the sum of component prices (ideally 8%%-18%% off).
                4. Return ONLY valid JSON (no markdown fences):
                {
                  "summary": "1-sentence combo merchandising strategy.",
                  "combos": [
                    {
                      "name": "Combo name",
                      "description": "Short appealing description <= 120 chars",
                      "items": [{"menuItemId": 10, "quantity": 1}, {"menuItemId": 20, "quantity": 1}],
                      "suggestedPrice": 119000,
                      "rationale": "Why this pairing boosts ticket size"
                    }
                  ]
                }

                CONTEXT:
                %s
                """.formatted(targetLanguage, menuAndPairsContext != null ? menuAndPairsContext : "");
    }

    public String buildPromotionSuggestionPrompt(String trafficContext, String language) {
        String targetLanguage = "en".equalsIgnoreCase(language) ? "English" : "Vietnamese (tiếng Việt)";
        return """
                You are an F&B Revenue Management Copilot for QROS.
                Analyze the hourly and weekday traffic data below and propose 2-3 targeted off-peak Promotion time-slots strictly in %s.

                RULES:
                1. Propose slots strictly within opening/closing hours when provided, with HH:mm format and startTime strictly before endTime.
                2. Use discountPercent between 5 and 40.
                3. Use ONLY uppercase Java DayOfWeek codes in daysOfWeek: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY (or [] for every day).
                4. Avoid directly duplicating EXISTING_PROMOTIONS.
                5. Return ONLY valid JSON (no markdown fences):
                {
                  "summary": "1-sentence traffic optimization summary.",
                  "promotions": [
                    {
                      "name": "Promotion name",
                      "discountPercent": 15,
                      "startTime": "14:00",
                      "endTime": "17:00",
                      "daysOfWeek": ["MONDAY", "TUESDAY", "WEDNESDAY"],
                      "rationale": "Grounded in low hourly order volume",
                      "expectedImpact": "Lift afternoon table occupancy"
                    }
                  ]
                }

                CONTEXT:
                %s
                """.formatted(targetLanguage, trafficContext != null ? trafficContext : "");
    }

    public String buildNoteTriagePrompt(String notesContext, String language) {
        String targetLanguage = "en".equalsIgnoreCase(language) ? "English" : "Vietnamese (tiếng Việt)";
        return """
                You are an F&B Kitchen & Floor Safety Copilot for QROS.
                Review active customer kitchen notes and open table service request notes below strictly in %s.

                RULES:
                1. Identify any allergens, dietary restrictions, baby/kid/elderly needs, or urgent timing cues.
                2. NEVER downgrade a note already tagged CRITICAL by deterministic safety rules.
                3. Provide a clear, 1-sentence operational instruction for staff/kitchen on each flagged item.
                4. Return ONLY valid JSON (no markdown fences):
                {
                  "summary": "1-sentence overview of critical/urgent notes.",
                  "triaged": [
                    {
                      "sourceType": "KITCHEN_ITEM or SERVICE_REQUEST",
                      "sourceId": 123,
                      "riskLevel": "CRITICAL|HIGH|NORMAL",
                      "extraAllergens": ["SHELLFISH"],
                      "actionInstruction": "Concise step for chef or server"
                    }
                  ]
                }

                CONTEXT:
                %s
                """.formatted(targetLanguage, notesContext != null ? notesContext : "");
    }

    public String buildRecipeBomPrompt(String recipeContext, String language) {
        String targetLanguage = "en".equalsIgnoreCase(language) ? "English" : "Vietnamese (tiếng Việt)";
        return """
                You are an F&B Culinary & Cost Control Copilot for QROS.
                Suggest an accurate Bill of Materials (BOM) and pricing feedback strictly in %s using ONLY the ACTIVE_INVENTORY_ITEMS provided.

                RULES:
                1. Every inventoryItemId MUST come strictly from ACTIVE_INVENTORY_ITEMS below. Respect each item's unit (e.g., g, kg, ml, l).
                2. Suggest 2-6 realistic ingredient lines with quantityRequired > 0 (up to 3 decimals).
                3. Provide 1-2 costWarnings and 1-2 pricingAdvice bullets grounded in sellingPrice and foodCost%%.
                4. Return ONLY valid JSON (no markdown fences):
                {
                  "suggestedItems": [
                    {"inventoryItemId": 1, "quantityRequired": 150.0, "reason": "Main protein portion"}
                  ],
                  "costWarnings": ["Warning if margin tight or unit cost missing"],
                  "pricingAdvice": ["Pricing or portioning advice"]
                }

                CONTEXT:
                %s
                """.formatted(targetLanguage, recipeContext != null ? recipeContext : "");
    }

    public String buildInvoiceOcrPrompt(String inventoryCatalogContext, String language) {
        String targetLanguage = "en".equalsIgnoreCase(language) ? "English" : "Vietnamese (tiếng Việt)";
        return """
                You are an F&B Purchasing & Inventory OCR Copilot for QROS.
                Inspect the attached supplier receipt/invoice image and extract purchase lines accurately.
                Write warning/summary messages strictly in %s.

                RULES:
                1. Extract supplierName, invoiceDate (YYYY-MM-DD or null), invoiceNumber (or null), and totalAmount (number or null).
                2. For every purchased line item visible on the receipt, extract rawName, numeric quantity (> 0), unit string (e.g. kg, g, l, ml, chai, lon, hop), numeric unitPrice (per invoice unit), and numeric lineTotal.
                3. Match matchedInventoryItemId strictly from ACTIVE_INVENTORY_ITEMS below ONLY when clearly the same ingredient; set null if unsure or absent from catalog. Never invent IDs.
                4. Set confidence to HIGH, MEDIUM, or LOW for each line. Add concise warnings if any area is blurry, cropped, or handwritten.
                5. Return ONLY valid JSON (no markdown fences):
                {
                  "supplierName": "Supplier name or null",
                  "invoiceDate": "2026-11-22",
                  "invoiceNumber": "HD-001",
                  "totalAmount": 550000,
                  "warnings": ["Optional note if illegible"],
                  "lines": [
                    {
                      "rawName": "Thit bo Uc",
                      "quantity": 2.5,
                      "unit": "kg",
                      "unitPrice": 220000,
                      "lineTotal": 550000,
                      "matchedInventoryItemId": 1,
                      "confidence": "HIGH",
                      "notes": ""
                    }
                  ]
                }

                ACTIVE_INVENTORY_ITEMS:
                %s
                """.formatted(targetLanguage, inventoryCatalogContext != null ? inventoryCatalogContext : "");
    }
}
