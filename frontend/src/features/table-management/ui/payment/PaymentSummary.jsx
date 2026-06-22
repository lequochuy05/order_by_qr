const PaymentSummary = ({ subtotalAmount, discountAmount, finalAmount }) => (
  <div className="space-y-3">
    <label className="text-xs font-bold uppercase text-gray-400">Chi tiết hóa đơn</label>
    <div className="space-y-3 rounded-2xl border bg-gray-50 p-5">
      <div className="flex justify-between text-sm">
        <span className="text-gray-500">Tạm tính:</span>
        <span className="font-semibold text-gray-700">{subtotalAmount.toLocaleString()}đ</span>
      </div>
      {discountAmount > 0 && (
        <div className="flex justify-between text-sm">
          <span className="font-medium text-green-600">Giảm giá voucher:</span>
          <span className="font-bold text-green-600">-{discountAmount.toLocaleString()}đ</span>
        </div>
      )}
      <div className="mt-1 flex items-center justify-between border-t border-dashed pt-3">
        <span className="font-bold text-gray-800">Cần thanh toán:</span>
        <span className="text-2xl font-black text-orange-600">{finalAmount.toLocaleString()}đ</span>
      </div>
    </div>
  </div>
);

export default PaymentSummary;
