// src/utils/invoiceGenerator.js
/**
 * Hàm định dạng tiền tệ
 */
const fmtVND = (num) => {
    return Number(num || 0).toLocaleString('vi-VN') + ' đ';
};

/**
 * Tạo chuỗi HTML hóa đơn từ dữ liệu đơn hàng
 * @param {Object} data - Bao gồm { order, table, paidBy, paidAt }
 * @returns {string} - Mã HTML của hóa đơn
 */
export const generateInvoice = ({ order, table, paidBy, paidAt }) => {
    const items = order?.orderItems || [];

    // 1. GOM NHÓM MÓN ĂN & COMBO
    const comboMap = {};   // { [comboId]: { name, qty, price } }
    const normalItems = [];

    items.forEach(it => {
        if (it.combo) {
            const key = it.combo.id;
            if (!comboMap[key]) {
                comboMap[key] = {
                    name: it.combo.name,
                    qty: 0,
                    price: it.combo.price || it.unitPrice || 0
                };
            }
            comboMap[key].qty += (it.quantity || 1);
        } else {
            normalItems.push(it);
        }
    });

    // 2. TẠO CÁC DÒNG HTML CHO COMBO
    const rowsCombo = Object.values(comboMap).map(c => {
        const lineTotal = (c.price || 0) * (c.qty || 0);
        return `
            <tr>
                <td style="padding: 8px; border-bottom: 1px solid #eee;">
                    <strong>Combo ${c.name}</strong>
                </td>
                <td style="text-align: center; padding: 8px; border-bottom: 1px solid #eee;">${c.qty}</td>
                <td style="text-align: right; padding: 8px; border-bottom: 1px solid #eee;">${fmtVND(c.price)}</td>
                <td style="text-align: right; padding: 8px; border-bottom: 1px solid #eee;">${fmtVND(lineTotal)}</td>
            </tr>
        `;
    }).join('');

    // 3. TẠO CÁC DÒNG HTML CHO MÓN LẺ
    const rowsNormal = normalItems.map(it => {
        const name = it?.menuItem?.name ?? 'Món chưa đặt tên';
        const price = it.unitPrice ?? it.menuItem?.price ?? 0;
        const qty = it.quantity ?? 0;
        const notes = it.notes ? `<div style="color: #6b7280; font-style: italic; font-size: 12px;">(${it.notes})</div>` : '';
        const lineTotal = price * qty;
        
        return `
            <tr>
                <td style="padding: 8px; border-bottom: 1px solid #eee;">
                    ${name}
                    ${notes}
                </td>
                <td style="text-align: center; padding: 8px; border-bottom: 1px solid #eee;">${qty}</td>
                <td style="text-align: right; padding: 8px; border-bottom: 1px solid #eee;">${fmtVND(price)}</td>
                <td style="text-align: right; padding: 8px; border-bottom: 1px solid #eee;">${fmtVND(lineTotal)}</td>
            </tr>
        `;
    }).join('');

    const rows = rowsCombo + rowsNormal;

    // 4. TÍNH TOÁN TỔNG TIỀN
    // Ưu tiên lấy từ order snapshot nếu có (để khớp với lúc thanh toán)
    const originalTotal = order.originalTotal ?? order.totalAmount ?? 0;
    const discount = order.discountVoucher ?? 0;
    const finalTotal = order.totalAmount ?? 0;

    const timeStr = paidAt ? new Date(paidAt).toLocaleString('vi-VN') : new Date().toLocaleString('vi-VN');

    // 5. TRẢ VỀ HTML TEMPLATE
    return `
    <!doctype html>
    <html>
    <head>
        <meta charset="utf-8">
        <title>Hóa đơn - Bàn ${table?.tableNumber ?? 'Mang về'}</title>
        <style>
            * { box-sizing: border-box; }
            body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; color: #111827; padding: 20px; font-size: 14px; line-height: 1.5; }
            .bill { max-width: 480px; margin: 0 auto; background: #fff; padding: 20px; border: 1px solid #eee; }
            .brand { font-weight: 800; font-size: 22px; text-transform: uppercase; margin-bottom: 4px; }
            .muted { color: #6b7280; font-size: 13px; }
            .header { text-align: center; margin-bottom: 24px; }
            .divider { border-top: 1px dashed #ddd; margin: 12px 0; }
            table { width: 100%; border-collapse: collapse; }
            th { text-align: left; background: #f9fafb; padding: 8px; font-size: 12px; text-transform: uppercase; color: #6b7280; }
            .right { text-align: right; }
            .total-row td { font-weight: 700; font-size: 16px; border-top: 2px solid #333; padding-top: 12px; }
            .footer { text-align: center; margin-top: 24px; font-size: 13px; }
            @media print { 
                .no-print { display: none; } 
                body { padding: 0; background: #fff; } 
                .bill { border: none; max-width: 100%; width: 100%; }
            }
        </style>
    </head>
    <body>
        <div class="bill">
            <div class="header">
                <div class="brand">Sắc Màu Quán</div>
                <div class="muted">V328+9QR, Đại Minh, Đại Lộc, Quảng Nam</div>
                <div class="muted">Hotline: 0706163387</div>
            </div>

            <div style="display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 13px;">
                <div>
                    <div>Bàn: <strong>${table?.tableNumber ?? '??'}</strong></div>
                    <div>Thu ngân: ${paidBy || 'Staff'}</div>
                </div>
                <div style="text-align: right;">
                    <div>Đơn #: ${order.id}</div>
                    <div>${timeStr}</div>
                </div>
            </div>

            <div class="divider"></div>

            <table>
                <thead>
                    <tr>
                        <th style="width: 45%">Món</th>
                        <th style="width: 15%; text-align: center;">SL</th>
                        <th style="width: 20%; text-align: right;">Đơn giá</th>
                        <th style="width: 20%; text-align: right;">Thành tiền</th>
                    </tr>
                </thead>
                <tbody>
                    ${rows || '<tr><td colspan="4" style="text-align:center; padding: 20px;">Không có món nào</td></tr>'}
                </tbody>
            </table>

            <div class="divider"></div>

            <table>
                <tbody>
                    <tr>
                        <td class="right" colspan="3">Tạm tính:</td>
                        <td class="right" style="width: 25%">${fmtVND(originalTotal)}</td>
                    </tr>
                    ${discount > 0 ? `
                    <tr>
                        <td class="right" colspan="3">Giảm giá (Voucher):</td>
                        <td class="right">-${fmtVND(discount)}</td>
                    </tr>
                    ` : ''}
                    <tr class="total-row">
                        <td class="right" colspan="3">THANH TOÁN:</td>
                        <td class="right">${fmtVND(finalTotal)}</td>
                    </tr>
                </tbody>
            </table>

            <div class="footer">
                <div>Cảm ơn quý khách & Hẹn gặp lại!</div>
                <div class="muted">Wifi: SacMauQuan / Pass: 12345678</div>
            </div>

            <div class="no-print" style="text-align: center; margin-top: 30px;">
                <button onclick="window.print()" style="background: #f97316; color: white; border: none; padding: 10px 20px; border-radius: 8px; font-weight: bold; cursor: pointer;">
                    🖨️ In hóa đơn ngay
                </button>
            </div>
        </div>
        <script>
            // Tự động in khi mở popup
            window.onload = function() { setTimeout(function(){ window.print(); }, 500); }
        </script>
    </body>
    </html>
    `;
};

/**
 * Hàm gọi cửa sổ in
 */
export const printInvoice = (data) => {
    const html = generateInvoice(data);
    // Mở popup
    const win = window.open('', 'INVOICE_PRINT', 'width=480,height=640,scrollbars=yes');
    if(win) {
        win.document.write(html);
        win.document.close();
        // Tự động in khi popup load xong
        win.onload = function() {
            setTimeout(function() {
                win.print();
            }, 500);
        };
    } else {
        alert("Vui lòng cho phép mở cửa sổ popup để in hóa đơn.");
    }
};