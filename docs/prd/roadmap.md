# Product Roadmap: First Usable Scope & Phased Expansion

> **Mô-đun:** `roadmap.md`  
> **Giai đoạn:** Execution Roadmap & Phased Delivery  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  

---

## 1. Triết lý phân kỳ sản phẩm (Product Phasing Philosophy)

QROS tuân thủ triết lý phát triển linh hoạt (Agile / Evolutionary Delivery): **"Bắt đầu từ giải pháp khả dụng tối thiểu có giá trị cao nhất (MVP / First Usable Scope), làm vững chắc tầng nghiệp vụ lõi (Operational Hardening), sau đó mới mở rộng bằng Trí tuệ Nhân tạo và Quy mô Doanh nghiệp (Enterprise Scale)"**.

```
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │ QUY MÔ / GIÁ TRỊ                                                            │
  │                                                                 PHASE 4     │
  │                                                           ┌─────────────────┤
  │                                                           │ ENTERPRISE &    │
  │                                              PHASE 3      │ MULTI-BRANCH    │
  │                                        ┌──────────────────┼─────────────────┤
  │                                        │ AI INTELLIGENCE &│ Multi-tenant,   │
  │                           PHASE 2      │ ANALYTICS        │ e-Invoice VN,   │
  │                     ┌──────────────────┼──────────────────┤ Loyalty CRM     │
  │                     │ OPERATIONAL      │ Gemini AI Menu,  │                 │
  │        PHASE 1      │ HARDENING        │ AI Forecasting,  │                 │
  │  ┌──────────────────┼──────────────────┤ Warm Hospitality │                 │
  │  │ FIRST USABLE     │ Gộp bàn động     │ Design System    │                 │
  │  │ SCOPE (MVP)      │ KDS Batch Cook,  │                  │                 │
  │  │ Core QR Menu,    │ Trừ kho BOM,     │                  │                 │
  │  │ KDS cơ bản,      │ Voucher Engine,  │                  │                 │
  │  │ Cash/PayOS chốt  │ 2FA Admin        │                  │                 │
  └──┴──────────────────┴──────────────────┴──────────────────┴─────────────────┴───►
      Tháng 1 - 2            Tháng 3 - 4        Tháng 5 - 6        Tháng 7 - 12   THỜI GIAN
```

---

## 2. Chi tiết 4 Giai đoạn Phát triển (Roadmap Breakdown)

---

### Phase 1: First Usable Scope / MVP (Tháng 1 - 2)
*Mục tiêu:* Đưa vào vận hành thử nghiệm tại 1 nhà hàng thực tế; kiểm chứng luồng đặt món tại bàn không gián đoạn.

- **Kênh Khách hàng:**
  - Quét mã QR theo `tableCode` có gắn chữ ký bảo mật `sig`.
  - Hiển thị menu số theo danh mục món ăn và combo.
  - Giỏ hàng đơn giản, chọn số lượng, gửi đơn gọi món tức thời.
  - Giao diện thân thiện trên thiết bị di động (Mobile Web).
- **Kênh Sàn & Bếp:**
  - Màn hình KDS cơ bản hiển thị danh sách món đang chờ.
  - Chuyển trạng thái món đơn lẻ: `PENDING` -> `COOKING` -> `READY`.
  - Sơ đồ bàn hiển thị màu sắc cơ bản (Trống / Đang có khách).
- **Kênh Thu ngân & Quản trị:**
  - Hỗ trợ thanh toán tiền mặt với công cụ tính tiền thối.
  - Tích hợp thanh toán PayOS tạo mã QR VietQR tĩnh/động cơ bản.
  - CRUD Danh mục, Món ăn và hình ảnh món.
  - Phân quyền cơ bản: `ADMIN`, `STAFF`, `CHEF`.

---

### Phase 2: Operational Hardening & Real-Time Synchronization (Tháng 3 - 4)
*Mục tiêu:* Giải quyết toàn bộ các tình huống phức tạp trong giờ cao điểm; loại bỏ hoàn toàn sai lệch doanh thu và thất thoát kho.

- **Nghiệp vụ Bàn phức tạp (Migration V1.15):**
  - Triển khai cơ chế Gộp bàn động (`table_session_tables`) cho phép ghép nhiều bàn vật lý vào 1 phiên chung.
  - Chuyển bàn, tách bàn với cơ chế khóa bi quan `Pessimistic Lock` chống race condition.
- **Tối ưu hóa Bếp chuyên sâu (KDS Advanced):**
  - Chế độ nấu gom theo mẻ (Batch Cooking View) cho phép đầu bếp nấu hàng loạt món cùng loại.
  - Bộ đếm thời gian thực trên từng món; chuông cảnh báo âm thanh khi món chờ quá hạn 20 phút.
- **Tự động hóa Kho & Định lượng (BOM):**
  - Thiết lập công thức chế biến (Bill of Materials) cho từng món.
  - Tự động trừ kho nguyên liệu ngay khi món chuyển sang `READY`.
  - Cảnh báo nguyên liệu chạm ngưỡng an toàn và chặn đặt món tự động.
- **Bảo mật & Tài chính hoàn thiện:**
  - Bắt buộc xác thực 2FA OTP qua email cho quản trị viên `ROLE_ADMIN`.
  - Quản trị Voucher khuyến mãi: chiết khấu %/VND, giới hạn số lần, bill tối thiểu.
  - Chuẩn hóa in hóa đơn nhiệt (Thermal Receipt Printer) khổ 80mm/58mm.

---

### Phase 3: AI Intelligence, Predictive Analytics & Design Polish (Tháng 5 - 6)
*Mục tiêu:* Tăng giá trị đơn hàng trung bình (AOV), tối ưu hóa nhân sự và nâng tầm trải nghiệm thị giác.

- **Trí tuệ nhân tạo (Generative AI - Gemini Engine):**
  - Tự động viết mô tả món ăn chuyên nghiệp, chuẩn SEO ẩm thực chỉ bằng 1 nút bấm.
  - Gợi ý gói Combo và món ăn kèm thông minh (Smart Cross-selling) trên giỏ hàng của khách.
  - AI Assistant hỏi đáp số liệu vận hành cho nhân viên và quản lý.
- **Phân tích dự báo doanh thu (Predictive Analytics):**
  - Dự báo doanh thu và lượng khách các ngày trong tuần bằng mô hình học máy dựa trên lịch sử bán hàng và ngày lễ/cuối tuần.
  - Báo cáo phân tích hiệu suất nhân viên (số lượng order xử lý, thời gian phục vụ trung bình).
  - Báo cáo cơ cấu món bán chạy nhất và tỷ suất lợi nhuận trên từng món ăn.
- **Thiết kế chuẩn hóa "Warm Hospitality POS":**
  - Hệ thống Design Token chuẩn hóa (Tailwind CSS 4), đồng nhất dark/light mode.
  - Tối ưu hóa khả năng truy cập (Accessibility - a11y) và phản hồi xúc giác/âm thanh khi thao tác POS.

---

### Phase 4: Enterprise Scale, Multi-Branch & Ecosystem Expansion (Tháng 7 - 12)
*Mục tiêu:* Mở rộng từ mô hình đơn lẻ sang phục vụ chuỗi nhà hàng quy mô lớn với đầy đủ tích hợp hành lang pháp lý.

- **Mô hình chuỗi đa chi nhánh (Multi-Tenant Architecture):**
  - Quản lý tập trung nhiều chi nhánh trên cùng một tài khoản tổng công ty.
  - Phân quyền điều hành theo từng chi nhánh; điều chuyển kho liên chi nhánh.
- **Tích hợp Hóa đơn Điện tử (e-Invoice):**
  - Tích hợp chuẩn hóa với các nhà cung cấp hóa đơn điện tử tại Việt Nam (Viettel Sinvoice, VNPT, MISA meInvoice) theo Nghị định 123 và Thông tư 78.
- **Hệ thống Khách hàng Thân thiết (Loyalty & CRM):**
  - Tích điểm thành viên qua số điện thoại; phân hạng thẻ thành viên (Bạc, Vàng, Kim Cương).
  - Tự động gửi voucher sinh nhật và ưu đãi cá nhân hóa qua Zalo ZNS / SMS.
- **Tự động hóa chuỗi cung ứng (Automated Procurement):**
  - Tự động tạo đơn đặt hàng nguyên vật liệu gửi đến nhà cung cấp khi kho đạt ngưỡng tối thiểu.

---

## 3. Tiêu chí Phát hành & Cổng Kiểm soát Chất lượng (Release Quality Gates)

Trước khi chuyển giao từng Phase lên môi trường Production, sản phẩm phải vượt qua các cổng kiểm soát nghiêm ngặt:

| Tiêu chuẩn chất lượng | Chỉ số chấp thuận (Quality Gate Target) | Phương thức đo lường |
|---|---|---|
| **Độ trễ API (API Latency)** | P95 < 200ms; P99 < 500ms | Prometheus & Spring Boot Actuator |
| **Độ trễ Real-time WebSocket** | Message truyền từ Client đến KDS < 400ms | WebSocket Network Monitor |
| **Độ tin cậy giao dịch (Concurrency)** | 0% xung đột dữ liệu (Zero Race Condition) khi 10 nhân viên cùng thao tác gộp bàn | Load testing k6 (100 virtual users đồng thời) |
| **Độ phủ kiểm thử tự động** | Unit & Integration Test Coverage > 75% | JUnit 5 + Mockito + Testcontainers |
| **Bảo mật & Tuân thủ** | 0 lỗ hổng nghiêm trọng (Critical/High Vulnerabilities); 2FA OTP hoạt động 100% | SAST Audit + SonarQube |
| **Tính khả dụng hệ thống (Uptime)** | ≥ 99.9% trong khung giờ hoạt động (07:00 - 23:30 hàng ngày) | UptimeRobot / Cloud Alerting |

---

## 4. Kế hoạch phòng ngừa rủi ro tiến độ (Roadmap Risk Mitigation)

1. **Rủi ro quá tải WebSocket khi nhà hàng đạt sức chứa cực đại:**
   - *Giải pháp:* Cấu hình Redis Pub/Sub làm message broker trung gian phía sau STOMP, kết hợp cơ chế fallback tự động sang polling thông minh (Smart Polling 5s) nếu kết nối socket bị ngắt quãng.
2. **Rủi ro phụ thuộc vào API bên ngoài (PayOS hoặc Gemini AI):**
   - *Giải pháp:* Áp dụng mẫu thiết kế Circuit Breaker (Resilience4j). Nếu PayOS hoặc Gemini gặp sự cố, hệ thống tự động ngắt kết nối an toàn, kích hoạt luồng thanh toán tiền mặt thủ công và giao diện soạn thảo mô tả món truyền thống mà không làm sập ứng dụng.
3. **Rủi ro người dùng tại chỗ từ chối thay đổi (Change Resistance):**
   - *Giải pháp:* Thiết kế giao diện theo hướng "Zero-Training": nhân viên mới có thể sử dụng màn hình Table Map và Bếp KDS chỉ sau 5 phút hướng dẫn trực tiếp.
