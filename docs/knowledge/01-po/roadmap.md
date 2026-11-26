# Product Roadmap: First Usable Scope & Phased Expansion

> **Mô-đun:** `roadmap.md`  
> **Giai đoạn:** Execution Roadmap & Phased Delivery (`01-po`)  
> **Dự án:** QROS (Smart QR Order & Restaurant Operating System)  
> **Nhóm thực hiện:** Alpha Team

---

## 1. Triết lý phân kỳ sản phẩm (Product Phasing Philosophy)

QROS phát triển theo hướng tiếp cận tăng trưởng giá trị từng bước (Evolutionary Delivery): **"Ưu tiên ra mắt nhanh phạm vi khả dụng tối thiểu (MVP) giải quyết nút thắt gọi món – bếp – thanh toán, tiếp theo là hoàn thiện toàn bộ nghiệp vụ cao điểm (gộp bàn, nấu gom, trừ kho, bảo mật), sau đó nâng tầm bằng Trí tuệ Nhân tạo và trải nghiệm đa nền tảng, và cuối cùng mở rộng sang quy mô chuỗi doanh nghiệp."**

```text
  QUY MÔ GIÁ TRỊ ▲
                 │                                                           ┌─────────────────────┐
                 │                                                           │ PHASE 4: ENTERPRISE │
                 │                                        ┌──────────────────┤ Đa chi nhánh,       │
                 │                                        │ PHASE 3: AI &    │ Hóa đơn điện tử VN, │
                 │                     ┌──────────────────┤ TRẢI NGHIỆM CAO  │ Loyalty CRM,        │
                 │                     │ PHASE 2: VẬN HÀNH│ Trợ lý AI 3 luồng│ Đặt hàng NCC tự động│
                 │  ┌──────────────────┤ HOÀN CHỈNH       │ Dự báo doanh thu,│                     │
                 │  │ PHASE 1: MVP     │ Gộp bàn nhóm đông│ Song ngữ VI/EN,  │                     │
                 │  │ QR Menu tại bàn, │ KDS nấu gom mẻ,  │ Light/Dark Mode, │                     │
                 │  │ KDS 3 trạng thái,│ Trừ kho BOM,     │ Tự thanh toán QR,│                     │
                 │  │ Tiền mặt & VietQR│ Voucher, 2FA,    │ App di động      │                     │
                 │  │ CRUD Thực đơn    │ Đơn mang về      │                  │                     │
                 └──┴──────────────────┴──────────────────┴──────────────────┴─────────────────────┴──► THỜI GIAN
                        Tháng 1 – 2         Tháng 3 – 4         Tháng 5 – 6         Tháng 7 – 12
```

---

## 2. Chi tiết 4 Giai đoạn Phát triển Sản phẩm

### Phase 1: First Usable Scope / MVP *(Tháng 1 – 2)*
*Mục tiêu:* Vận hành trơn tru luồng cốt lõi tại một nhà hàng thực tế — khách quét QR gọi món, bếp nhận ngay, thu ngân chốt bill nhanh.

- **Thực khách (`PER-01`):** Quét mã QR định danh bàn có bảo mật, xem thực đơn theo danh mục/combo, tùy chọn topping/ghi chú, gửi đơn trực tiếp vào bếp trên điện thoại.
- **Sàn & Bếp (`PER-02`, `PER-03`):** Sơ đồ bàn hiển thị trạng thái cơ bản; màn hình KDS nhận đơn tức thời theo 3 bước (*Chờ chế biến → Đang nấu → Hoàn tất*).
- **Thu ngân & Quản trị (`PER-04`, `PER-05`, `PER-06`):** Thanh toán tiền mặt (tự tính tiền thối) và tạo mã VietQR cơ bản; quản trị danh mục, món ăn, hình ảnh và phân quyền vai trò cơ bản.

### Phase 2: Operational Hardening *(Tháng 3 – 4)*
*Mục tiêu:* Giải quyết triệt để các tình huống phức tạp giờ cao điểm, triệt tiêu sai lệch hóa đơn và thất thoát nguyên liệu.

- **Nghiệp vụ sàn nâng cao:** Gộp nhiều bàn vật lý vào một phiên hóa đơn duy nhất, hủy gộp/chuyển bàn an toàn; gọi nhân viên tại bàn theo lý do nhanh (chống spam) kết hợp hàng đợi yêu cầu bàn thời gian thực cho phục vụ (tránh nhận trùng); tách luồng đơn mua mang về (Takeaway) riêng biệt.
- **Bếp chuyên sâu:** Chế độ **nấu gom theo mẻ (Batch Cooking)**, đồng hồ đếm thời gian chờ, cảnh báo màu sắc & âm thanh khi món quá 20 phút, báo hết món kèm lý do.
- **Tự động hóa kho (BOM):** Khai báo nguyên liệu, công thức định lượng từng món, tự động giữ/trừ tồn kho theo đơn, phiếu nhập/điều chỉnh kho và cảnh báo nguyên liệu chạm ngưỡng an toàn.
- **Tài chính & Bảo mật hoàn chỉnh:** Tự động chốt thanh toán VietQR không cần chụp ảnh màn hình, quản lý voucher & khuyến mãi theo ngày/khung giờ, in hóa đơn, lịch sử đối soát theo ca, phân quyền đủ 6 vai trò và bắt buộc xác thực 2 lớp (2FA OTP) cho chủ quán.

### Phase 3: AI Intelligence, Experience Polish & Multi-Platform *(Tháng 5 – 6)*
*Mục tiêu:* Tăng giá trị đơn hàng trung bình (AOV), hỗ trợ ra quyết định kinh doanh và mang lại trải nghiệm thị giác đồng nhất trên mọi thiết bị.

- **Trợ lý AI đa vai trò:**
  - *Cho thực khách:* Chatbot tư vấn món theo khẩu vị, dị ứng, ngân sách ngay trên menu.
  - *Cho nhân viên & quản lý:* Tự động soạn mô tả món ăn hấp dẫn, gợi ý combo bán kèm và hỏi đáp nhanh thông tin vận hành.
  - *Cho chủ quán:* Dự báo doanh thu và nhu cầu nguyên liệu cho các ngày tiếp theo.
- **Tự thanh toán linh hoạt & Báo cáo chuyên sâu:** Cho phép chủ quán chủ động bật/tắt chế độ khách tự thanh toán VietQR tại bàn; hoàn thiện báo cáo doanh thu, món bán chạy và hiệu suất nhân sự.
- **Nâng tầm trải nghiệm & Đa nền tảng:** Chuẩn hóa toàn bộ hệ thống thiết kế (*Design System*, ánh sáng/tối dễ chịu cho ca làm dài), hỗ trợ **song ngữ Việt – Anh**, cảnh báo ngoại tuyến mượt mà và mở rộng đóng gói sang ứng dụng di động cho nhân sự.

### Phase 4: Enterprise Scale & Ecosystem Expansion *(Tháng 7 – 12 — Định hướng tương lai)*
*Mục tiêu:* Mở rộng từ điểm bán đơn lẻ sang phục vụ chuỗi nhà hàng nhiều chi nhánh.

- **Quản trị đa chi nhánh (Multi-Branch):** Điều hành tập trung nhiều cơ sở, phân quyền theo chi nhánh và điều chuyển kho liên chi nhánh.
- **Hóa đơn điện tử (e-Invoice):** Kết nối các nhà cung cấp hóa đơn điện tử chuẩn quy định thuế tại Việt Nam.
- **Chăm sóc khách hàng thân thiết (Loyalty CRM):** Tích điểm thành viên, phân hạng thẻ và ưu đãi cá nhân hóa.
- **Tự động hóa đặt hàng nhà cung cấp:** Gợi ý và tạo đơn đặt mua nguyên liệu tự động khi tồn kho chạm mức tối thiểu.

---

## 3. Tiêu chí Chấp thuận Phát hành (Product Release Gates)

Trước khi đưa từng giai đoạn vào vận hành thực tế tại nhà hàng, sản phẩm phải đạt đủ các tiêu chuẩn nghiệm thu ở góc độ người dùng và kinh doanh:

| Tiêu chuẩn nghiệm thu | Mục tiêu sản phẩm | Cách kiểm chứng |
|---|---|---|
| **Tốc độ gọi món tại bàn** | Khách quét QR tới khi gửi xong đơn **< 90 giây** | Kiểm thử thực địa (Usability Test) với thực khách mới |
| **Độ trễ đồng bộ Bàn – Bếp** | Đơn hiển thị tại bếp và trạng thái bàn cập nhật **< 1 giây** | Kiểm thử tải giờ cao điểm mô phỏng 30–50 bàn đồng thời |
| **Độ chính xác vận hành** | **0%** đơn bị trùng lặp hay lệch tổng tiền khi gộp bàn / nhiều người bấm cùng lúc | Kịch bản kiểm thử đồng thời & nghiệm thu nghiệp vụ |
| **An toàn phân quyền & Tài chính** | **100%** thao tác vượt quyền bị chặn; 2FA chủ quán hoạt động chặt chẽ | Kiểm thử phân quyền đủ 6 vai trò |
| **Khả năng sử dụng nhanh (Zero-Training)** | Nhân sự mới thành thạo Sơ đồ bàn & Bếp KDS sau **5–10 phút** hướng dẫn | Đánh giá trực quan giao diện đa thiết bị (Mobile / Tablet / Desktop) |

---

## 4. Quản trị Rủi ro Triển khai Sản phẩm (Product Risk Mitigation)

1. **Rủi ro gián đoạn kết nối mạng tại nhà hàng:** Hiển thị trạng thái mất mạng rõ ràng, tránh để khách bấm gửi lặp gây trùng đơn, và hỗ trợ nhân sự chuyển nhanh sang 4G/5G dự phòng.
2. **Rủi ro gián đoạn dịch vụ bên ngoài (Cổng thanh toán QR / AI):** Đảm bảo hoạt động cốt lõi (gọi món, nấu bếp, thu tiền mặt) **không bao giờ bị tê liệt** khi dịch vụ AI quá tải hoặc cổng ngân hàng bảo trì.
3. **Rủi ro nhân sự quen cách làm cũ ngại chuyển đổi:** Thiết kế giao diện tối giản, ít chữ, phân biệt rõ bằng màu sắc trạng thái và cho phép nhân viên đặt món hộ để chuyển tiếp êm ái trong tuần đầu triển khai.
