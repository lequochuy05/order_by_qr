import { fmtVND } from '@shared/lib/formatters.js';
import {
  getOrderDiscountAmount,
  getOrderFinalAmount,
  getOrderSubtotalAmount,
} from './orderMoney.js';
import useSettingsStore from '@shared/model/settingsStore.js';

/**
 * Tạo chuỗi HTML hóa đơn chuyên nghiệp
 * @param {Object} data - { order, table, paidBy, paidAt }
 */
export const generateInvoice = ({ order, table, paidBy, paidAt }) => {
  if (!order) return '';

  // Fetch restaurant info from Zustand store (cached)
  const { settings } = useSettingsStore.getState();
  const storeName = settings.restaurantName || 'Nhà hàng';
  const storeAddress = settings.restaurantAddress || '';
  const storePhone = settings.restaurantPhone || '';
  const wifiSsid = settings.wifiName || settings.wifiSsid || '';
  const wifiPassword = settings.wifiPassword || '';
  const billTitle = settings.billTitle || 'HÓA ĐƠN THANH TOÁN';
  const billFooterMessage = settings.billFooterMessage || 'CẢM ƠN QUÝ KHÁCH & HẸN GẶP LẠI!';
  const paperWidth = settings.billPaperSize === '58' ? '58mm' : '80mm';

  const items = order.orderItems || [];
  const subtotalAmount = getOrderSubtotalAmount(order);
  const discountAmount = getOrderDiscountAmount(order);
  const finalAmount = getOrderFinalAmount(order);

  // 1. Phân loại Combo và Món lẻ
  const comboMap = {};
  const normalItems = [];

  items.forEach((it) => {
    if (it.combo) {
      const key = it.combo.id;
      if (!comboMap[key]) {
        comboMap[key] = {
          name: it.itemNameSnapshot || it.combo?.name || 'Combo',
          qty: 0,
          price: it.unitPrice || it.combo.price || 0,
          originalPrice: it.combo.price || 0,
          lineTotal: 0,
        };
      }
      comboMap[key].qty += it.quantity || 1;
      comboMap[key].lineTotal += Number(
        it.lineTotal ?? (it.unitPrice || it.combo.price || 0) * (it.quantity || 1),
      );
    } else {
      normalItems.push(it);
    }
  });

  // 2. Render dòng Combo
  const rowsCombo = Object.values(comboMap)
    .map((c) => {
      return `
      <tr>
        <td style="padding: 10px 0;">
          <div style="font-weight: bold; color: #000;">[COMBO] ${c.name}</div>
        </td>
        <td style="text-align: center; padding: 10px 0;">${c.qty}</td>
        <td style="text-align: right; padding: 10px 0;">${fmtVND(c.price)}</td>
        <td style="text-align: right; padding: 10px 0; font-weight: bold;">${fmtVND(c.lineTotal)}</td>
      </tr>
    `;
    })
    .join('');

  // 3. Render dòng Món lẻ (Kèm Options)
  const rowsNormal = normalItems
    .map((it) => {
      const name = it.itemNameSnapshot || it.menuItem?.name || 'Món không tên';
      const price = it.unitPrice || 0;
      const qty = it.quantity || 0;
      const lineTotal = it.lineTotal ?? price * qty;

      // Xử lý Options (Topping/Lựa chọn)
      const optionsHtml = (it.options || it.orderItemOptions || [])
        .map((opt) => {
          const extra = opt.extraPrice > 0 ? ` (+${fmtVND(opt.extraPrice)})` : '';
          return `<div style="font-size: 11px; color: #4b5563; margin-left: 8px;">• ${opt.optionName}: ${opt.optionValueName}${extra}</div>`;
        })
        .join('');

      const noteHtml = it.notes
        ? `<div style="font-size: 11px; color: #ef4444; font-style: italic; margin-left: 8px;">* Ghi chú: ${it.notes}</div>`
        : '';

      return `
      <tr>
        <td style="padding: 10px 0; border-bottom: 1px dashed #eee;">
          <div style="font-weight: 600; color: #1f2937;">${name}</div>
          ${optionsHtml}
          ${noteHtml}
        </td>
        <td style="text-align: center; padding: 10px 0; border-bottom: 1px dashed #eee;">${qty}</td>
        <td style="text-align: right; padding: 10px 0; border-bottom: 1px dashed #eee;">${fmtVND(price)}</td>
        <td style="text-align: right; padding: 10px 0; border-bottom: 1px dashed #eee; font-weight: bold;">${fmtVND(lineTotal)}</td>
      </tr>
    `;
    })
    .join('');

  const timeStr = paidAt
    ? new Date(paidAt).toLocaleString('vi-VN')
    : new Date().toLocaleString('vi-VN');

  // Wifi footer section (only if wifi configured)
  const wifiHtml =
    settings.showWifiOnBill !== false && wifiSsid
      ? `<div style="font-size: 11px; margin-top: 5px;">Wifi: ${wifiSsid}${wifiPassword ? ` / Pass: ${wifiPassword}` : ''}</div>`
      : '';

  // 4. Template HTML hoàn chỉnh
  return `
  <!DOCTYPE html>
  <html lang="vi">
  <head>
    <meta charset="UTF-8">
    <style>
      @page { size: ${paperWidth} auto; margin: 0; }
      body { 
        font-family: 'Courier New', Courier, monospace; 
        width: ${paperWidth}; margin: 0 auto; padding: 5mm;
        color: #000; background: #fff; line-height: 1.4; font-size: 13px;
      }
      .container { width: 100%; }
      .header { text-align: center; margin-bottom: 10px; }
      .store-name { font-size: 20px; font-weight: bold; text-transform: uppercase; margin-bottom: 2px; }
      .info { font-size: 12px; margin-bottom: 2px; }
      .divider { border-top: 1px dashed #000; margin: 8px 0; }
      .bold { font-weight: bold; }
      table { width: 100%; border-collapse: collapse; }
      th { text-align: left; font-size: 11px; border-bottom: 1px solid #000; padding: 5px 0; }
      .footer { text-align: center; margin-top: 15px; font-size: 12px; }
      .qr-placeholder { margin-top: 10px; border: 1px solid #eee; width: 80px; height: 80px; margin: 10px auto; }
      @media print {
        .no-print { display: none; }
      }
    </style>
  </head>
  <body>
    <div class="container">
      <div class="header">
        <div class="store-name">${storeName}</div>
        ${storeAddress ? `<div class="info">Đ/C: ${storeAddress}</div>` : ''}
        ${storePhone ? `<div class="info">SĐT: ${storePhone}</div>` : ''}
        <div style="font-size: 16px; font-weight: bold; margin-top: 10px;">${billTitle}</div>
      </div>

      <div class="info">Mã đơn: <span class="bold">#${order.id}</span></div>
      <div class="info">Bàn: <span class="bold">${table?.tableNumber || 'Mang về'}</span></div>
      <div class="info">Thời gian: ${timeStr}</div>
      <div class="info">Thu ngân: ${paidBy || 'Hệ thống'}</div>
      <div class="info">Hình thức thanh toán: <span class="bold">${order.paymentMethod === 'PAYOS' ? 'Chuyển khoản (PayOS)' : 'Tiền mặt'}</span></div>

      <div class="divider"></div>

      <table>
        <thead>
          <tr>
            <th style="width: 45%;">Tên món</th>
            <th style="width: 10%; text-align: center;">SL</th>
            <th style="width: 20%; text-align: right;">Đơn giá</th>
            <th style="width: 25%; text-align: right;">T.Tiền</th>
          </tr>
        </thead>
        <tbody>
          ${rowsCombo}
          ${rowsNormal}
        </tbody>
      </table>

      <div class="divider"></div>

      <table>
        <tr>
          <td style="padding: 3px 0;">Tổng cộng món:</td>
          <td style="text-align: right;">${fmtVND(subtotalAmount)}</td>
        </tr>
        ${
          discountAmount > 0
            ? `
        <tr>
          <td style="padding: 3px 0;">Giảm giá (Voucher):</td>
          <td style="text-align: right;">-${fmtVND(discountAmount)}</td>
        </tr>
        `
            : ''
        }
        <tr style="font-size: 16px; font-weight: bold;">
          <td style="padding: 10px 0;">TỔNG THANH TOÁN:</td>
          <td style="text-align: right; padding: 10px 0;">${fmtVND(finalAmount)}</td>
        </tr>
      </table>

      <div class="divider"></div>
      
      <div class="footer">
        <div style="font-weight: bold;">${billFooterMessage}</div>
        ${wifiHtml}
      </div>
    </div>

    <div class="no-print" style="position: fixed; bottom: 20px; left: 0; right: 0; text-align: center;">
        <button onclick="window.print()" style="background: #f97316; color: white; border: none; padding: 12px 24px; border-radius: 50px; font-weight: bold; cursor: pointer; box-shadow: 0 4px 12px rgba(249, 115, 22, 0.3);">
          🖨️ IN HÓA ĐƠN
        </button>
    </div>
  </body>
  </html>
  `;
};

/**
 * Thực hiện mở cửa sổ in
 */
export const printInvoice = (data) => {
  const html = generateInvoice(data);
  const win = window.open('', 'PRINT_INVOICE', 'width=450,height=600');

  if (win) {
    win.document.write(html);
    win.document.close();
  } else {
    alert('Vui lòng cho phép popup để in hóa đơn!');
  }
};
