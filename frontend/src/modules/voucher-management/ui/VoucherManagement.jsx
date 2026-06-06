import React, { useState, useEffect, useCallback } from 'react';
import { Loader2, Ticket, Pencil, Trash2 } from 'lucide-react';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { voucherService } from '@modules/voucher-management/api/voucherService.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import VoucherModal from './VoucherModal.jsx';
import StatusModal from '@shared/ui/StatusModal.jsx';
import ConfirmModal from '@shared/ui/ConfirmModal.jsx';
import { playNotificationSound } from '@modules/notifications/lib/notificationSound.js';
import { fmtVND, fmtDate } from '@shared/lib/formatters.js';

const VoucherManager = () => {
  const [vouchers, setVouchers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVoucher, setEditingVoucher] = useState(null);

  // === 1. State cho Status Modal ===
  const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();
  const { confirmModal, confirm, closeConfirm } = useConfirmModal();

  const getStatusInfo = (v) => {
    const now = new Date();
    const to = v.validTo ? new Date(v.validTo) : null;
    const isExpired = to && now > to;
    const isExhausted = v.usageLimit > 0 && v.usedCount >= v.usageLimit;

    if (!v.active) return { text: "Ngừng", color: "bg-gray-100 text-gray-500" };
    if (isExpired || isExhausted) return { text: "Hết hạn", color: "bg-red-100 text-red-600" };
    return { text: "Đang dùng", color: "bg-green-100 text-green-700" };
  };

  const fetchData = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true);
    try {
      const data = await voucherService.getAll();
      setVouchers(data);
    } catch (err) { console.error(err); }
    finally { setLoading(false); }
  }, []);

  useWebSocket('/topic/vouchers', (message) => {
    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      playNotificationSound();
      fetchData();
    }
  });

  useEffect(() => { fetchData(true); }, [fetchData]);

  const handleSubmit = async (data) => {
    const safeValidFrom = data.validFrom && !data.validFrom.includes('T') ? `${data.validFrom}T00:00:00` : data.validFrom;
    const safeValidTo = data.validTo && !data.validTo.includes('T') ? `${data.validTo}T23:59:59` : data.validTo;

    const payload = {
      ...data,
      code: data.code.trim().toUpperCase(),
      type: data.discountPercent ? 'PERCENTAGE' : 'FIXED_AMOUNT',
      discountPercent: data.discountPercent ? parseFloat(data.discountPercent) : null,
      discountAmount: data.discountAmount ? parseFloat(data.discountAmount) : null,
      usageLimit: parseInt(data.usageLimit) || 0,
      validFrom: safeValidFrom,
      validTo: safeValidTo
    };

    try {
      if (editingVoucher?.id) {
        await voucherService.update(editingVoucher.id, payload);
        showSuccess(`Đã cập nhật voucher`);
      } else {
        await voucherService.create(payload);
        showSuccess(`Đã thêm voucher`);
      }
      setIsModalOpen(false);
    } catch (err) {
      const errorMsg = err.response?.data?.detail || err.message || '';
      if (errorMsg.includes("Voucher code already exists")) {
        showError("Mã voucher này đã tồn tại trên hệ thống");
      } else {
        showError(err);
      }
    }
  };

  const handleDelete = async (id) => {
    const confirmed = await confirm('Xóa voucher', 'Bạn chắc chắn muốn xóa voucher này?');
    if (!confirmed) return;
    try {
      await voucherService.delete(id);
      showSuccess("Đã xóa voucher thành công");
    } catch (err) {
      showError(err);
    }
  };

  const filteredVouchers = vouchers.filter(v => v.code.toLowerCase().includes(searchTerm.toLowerCase()));

  return (
    <div className="p-6 space-y-6">
      <ManagementHeader
        searchPlaceholder="Tìm mã voucher..."
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        onAddClick={() => { setEditingVoucher(null); setIsModalOpen(true); }}
        addButtonText="Thêm Voucher"
      />

      {loading ? (
        <div className="flex justify-center p-20"><Loader2 className="animate-spin text-orange-500" size={40} /></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
          {filteredVouchers.map(v => {
            const status = getStatusInfo(v);
            return (
              <div key={v.id} className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-all">
                <div className="flex justify-between items-start mb-4">
                  <div className="bg-orange-50 p-3 rounded-2xl text-orange-500"><Ticket size={24} /></div>
                  <span className={`text-[10px] font-black px-3 py-1 rounded-full uppercase ${status.color}`}>
                    {status.text}
                  </span>
                </div>

                <h3 className="text-xl font-black text-gray-800 mb-1 tracking-wider uppercase">{v.code}</h3>
                <div className="text-2xl font-black text-orange-600 mb-4">
                  {v.discountPercent ? `${v.discountPercent}%` : fmtVND(v.discountAmount)}
                </div>
                {/* ... Thông tin chi tiết ... */}
                <div className="space-y-2 text-xs text-gray-500 mb-6">
                  <div className="flex justify-between"><span>Đã dùng:</span> <span className="font-bold text-gray-700">{v.usedCount}</span></div>
                  <div className="flex justify-between"><span>Giới hạn:</span> <span className="font-bold text-gray-700">{v.usageLimit === 0 ? '∞' : v.usageLimit}</span></div>
                  <div className="flex justify-between"><span>Hạn dùng:</span> <span className="font-bold text-gray-700">{fmtDate(v.validTo) || 'Vô hạn'}</span></div>
                </div>

                <div className="flex gap-2">
                  <button onClick={() => { setEditingVoucher(v); setIsModalOpen(true); }} className="flex-1 py-2.5 bg-blue-50 text-blue-600 rounded-xl hover:bg-blue-600 hover:text-white transition-all flex justify-center"><Pencil size={18} /></button>
                  <button onClick={() => handleDelete(v.id)} className="flex-1 py-2.5 bg-red-50 text-red-600 rounded-xl hover:bg-red-600 hover:text-white transition-all flex justify-center"><Trash2 size={18} /></button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {!loading && filteredVouchers.length === 0 && (
        <div className="text-center py-20 text-gray-400 italic bg-white rounded-3xl border border-dashed">
          <Ticket size={40} className="mx-auto mb-4 opacity-30" />
          Không tìm thấy voucher nào.
        </div>
      )}

      {/* Modal Nhập liệu */}
      <VoucherModal
        key={isModalOpen ? (editingVoucher?.id || 'new') : 'closed'}
        isOpen={isModalOpen} onClose={() => setIsModalOpen(false)}
        initialData={editingVoucher} onSubmit={handleSubmit}
      />

      <StatusModal
        isOpen={statusModal.isOpen}
        onClose={closeStatusModal}
        type={statusModal.type}
        title={statusModal.title}
        message={statusModal.message}
      />
      <ConfirmModal
        isOpen={confirmModal.isOpen}
        onClose={closeConfirm}
        onConfirm={confirmModal.onConfirm}
        title={confirmModal.title}
        message={confirmModal.message}
      />
    </div>
  );
};

export default VoucherManager;
