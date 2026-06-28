import React, { useRef, useState } from 'react';
import { X, CheckCircle, Trash2, Edit3, Save, UtensilsCrossed, Loader2 } from 'lucide-react';
import { orderService } from '@entities/order/api/orderService.js';
import { useAuth } from '@features/auth';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

const OrderDetailModal = ({ isOpen, onClose, table, order, onOrderUpdate }) => {
  const [items, setItems] = useState(order?.orderItems || []);
  const [prevOrder, setPrevOrder] = useState(order);
  const [editingId, setEditingId] = useState(null);
  const [editVal, setEditVal] = useState({ quantity: 1, notes: '' });
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);
  const [preparingItemIds, setPreparingItemIds] = useState(() => new Set());
  const preparingItemIdsRef = useRef(new Set());
  const preparedRequestQueueRef = useRef(Promise.resolve());

  const { user } = useAuth();
  const isManager = user?.role === 'MANAGER';
  const isChef = user?.role === 'CHEF';
  const canMarkPrepared = isManager || isChef;

  // Adjust state when order prop changes (React recommended pattern)
  if (order !== prevOrder) {
    setPrevOrder(order);
    setItems(order?.orderItems || []);
  }

  if (!isOpen || !table) return null;

  const handlePrepared = (itemId) => {
    if (preparingItemIdsRef.current.has(itemId)) return;

    preparingItemIdsRef.current.add(itemId);
    setPreparingItemIds(new Set(preparingItemIdsRef.current));

    preparedRequestQueueRef.current = preparedRequestQueueRef.current
      .then(async () => {
        try {
          await orderService.markItemPrepared(itemId, user?.userId);
          setItems((prev) =>
            prev.map((i) => (i.id === itemId ? { ...i, prepared: true, status: 'FINISHED' } : i)),
          );
          showSuccessToast('Đã hoàn tất chuẩn bị món');
          await onOrderUpdate();
        } catch (error) {
          showErrorToast(error);
        }
      })
      .finally(() => {
        preparingItemIdsRef.current.delete(itemId);
        setPreparingItemIds(new Set(preparingItemIdsRef.current));
      });
  };

  const cancelDelete = () => setConfirmDeleteId(null);

  const handleDelete = async (itemId) => {
    try {
      await orderService.deleteOrderItem(itemId);
      setItems((prev) =>
        prev.map((i) => (i.id === itemId ? { ...i, prepared: true, status: 'CANCELLED' } : i)),
      );
      setConfirmDeleteId(null);
      showSuccessToast('Đã hủy món thành công');
      await onOrderUpdate();
    } catch (error) {
      setConfirmDeleteId(null);
      showErrorToast(error);
    }
  };

  const startEdit = (item) => {
    setEditingId(item.id);
    setEditVal({ quantity: item.quantity, notes: item.notes || '' });
  };

  const saveEdit = async (itemId) => {
    try {
      await orderService.updateOrderItem(itemId, editVal);
      setItems((prev) => prev.map((i) => (i.id === itemId ? { ...i, ...editVal } : i)));
      setEditingId(null);
      showSuccessToast('Cập nhật món thành công');
      await onOrderUpdate();
    } catch (error) {
      showErrorToast(error);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-2 animate-in fade-in duration-200 sm:p-4">
      <div className="flex h-[calc(100dvh-1rem)] w-[calc(100vw-1rem)] max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl sm:h-[80vh] sm:w-full">
        <div className="px-6 py-4 border-b flex justify-between items-center bg-gray-50">
          <div>
            <h3 className="font-bold text-lg text-gray-800">
              Bàn {table.tableNumber} - {items.length > 0 ? 'Chi tiết đơn' : 'Trạng thái'}
            </h3>
            <span
              className={`text-xs px-2 py-0.5 rounded border ${items.length > 0 ? 'bg-green-100 text-green-700 border-green-200' : 'bg-gray-100 text-gray-600 border-gray-200'}`}
            >
              {items.length > 0 ? `Mã đơn: #${order?.id}` : 'Bàn trống'}
            </span>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-200 rounded-full transition-colors text-gray-500"
          >
            <X size={24} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6 space-y-4 custom-scrollbar bg-slate-50/50">
          {/* Empty State */}
          {items.length === 0 && (
            <div className="flex flex-col items-center justify-center h-full text-gray-400 space-y-4">
              <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center">
                <UtensilsCrossed size={40} className="opacity-50" />
              </div>
              <div className="text-center">
                <p className="text-lg font-medium text-gray-600">Bàn này đang trống</p>
                <p className="text-sm">Chưa có món ăn nào được gọi.</p>
              </div>
              <button
                onClick={onClose}
                className="mt-4 px-6 py-2 bg-white border border-gray-300 rounded-lg text-gray-600 hover:bg-gray-50 font-medium shadow-sm"
              >
                Đóng
              </button>
            </div>
          )}

          {items.map((item) => {
            const isCombo = !!item.combo;
            const name =
              item.itemNameSnapshot ||
              (isCombo ? `Combo ${item.combo?.name || ''}` : item.menuItem?.name);
            const isPrepared = item.prepared;
            const isCancelled = item.status === 'CANCELLED';
            const isPreparing = preparingItemIds.has(item.id);

            return (
              <div
                key={item.id}
                className={`p-4 rounded-xl border flex gap-4 transition-all ${isCancelled ? 'bg-rose-50 border-rose-200' : isPrepared ? 'bg-green-50 border-green-200' : 'bg-white border-gray-200 shadow-sm'}`}
              >
                <div className="flex-1">
                  <div className="font-bold text-gray-800 text-lg flex items-center gap-2">
                    {name}
                    <span className="text-orange-600">
                      x{editingId === item.id ? editVal.quantity : item.quantity}
                    </span>
                    {isCancelled && (
                      <span className="text-xs bg-rose-200 text-rose-800 px-2 py-0.5 rounded-full font-bold">
                        Đã hủy
                      </span>
                    )}
                    {isPrepared && !isCancelled && (
                      <span className="text-xs bg-green-200 text-green-800 px-2 py-0.5 rounded-full font-bold">
                        Đã phục vụ
                      </span>
                    )}
                  </div>

                  {/* Hiển thị Options (Size, Toppings...) */}
                  {item.options && item.options.length > 0 && (
                    <div className="flex flex-wrap gap-1 mt-1">
                      {item.options.map((opt, idx) => (
                        <span
                          key={idx}
                          className="text-[10px] bg-blue-50 text-blue-600 px-1.5 py-0.5 rounded border border-blue-100 font-medium"
                        >
                          {opt.optionName}: {opt.optionValueName}
                        </span>
                      ))}
                    </div>
                  )}

                  {editingId === item.id ? (
                    <div className="mt-3 bg-blue-50 rounded-xl border border-blue-100 p-3 space-y-3">
                      <div className="flex items-center gap-3">
                        <label className="text-xs font-semibold text-blue-700 w-16 shrink-0">Số lượng</label>
                        <div className="flex items-center gap-1">
                          <button
                            onClick={() => setEditVal((v) => ({ ...v, quantity: Math.max(1, v.quantity - 1) }))}
                            className="w-8 h-8 flex items-center justify-center rounded-lg bg-white border border-gray-200 text-gray-600 hover:bg-gray-100 transition-colors font-bold"
                          >
                            −
                          </button>
                          <span className="w-10 text-center font-bold text-blue-700 text-lg">
                            {editVal.quantity}
                          </span>
                          <button
                            onClick={() => setEditVal((v) => ({ ...v, quantity: Math.min(99, v.quantity + 1) }))}
                            className="w-8 h-8 flex items-center justify-center rounded-lg bg-white border border-gray-200 text-gray-600 hover:bg-gray-100 transition-colors font-bold"
                          >
                            +
                          </button>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <label className="text-xs font-semibold text-blue-700 w-16 shrink-0">Ghi chú</label>
                        <input
                          type="text"
                          className="flex-1 px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm outline-none focus:ring-2 focus:ring-blue-400 focus:border-blue-400 transition-all"
                          placeholder="Ghi chú cho món này..."
                          value={editVal.notes}
                          onChange={(e) => setEditVal((v) => ({ ...v, notes: e.target.value }))}
                        />
                      </div>
                    </div>
                  ) : (
                    item.notes && (
                      <p className="text-sm text-gray-500 italic mt-1 bg-gray-50 p-1.5 rounded-lg inline-block">
                        Note: {item.notes}
                      </p>
                    )
                  )}
                </div>

                <div className="flex flex-col gap-2 justify-center">
                  {isCancelled ? (
                    <X className="text-rose-500" size={28} />
                  ) : isPrepared ? (
                    <CheckCircle className="text-green-500" size={28} />
                  ) : (
                    <>
                      {editingId === item.id ? (
                        <div className="flex gap-2">
                          <button
                            onClick={() => saveEdit(item.id)}
                            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-semibold shadow-sm"
                          >
                            <Save size={16} className="inline mr-1" /> Lưu
                          </button>
                          <button
                            onClick={() => setEditingId(null)}
                            className="px-4 py-2 bg-white border border-gray-300 text-gray-600 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium"
                          >
                            Hủy
                          </button>
                        </div>
                      ) : confirmDeleteId === item.id ? (
                        <div className="flex flex-col gap-1.5">
                          <button
                            onClick={() => handleDelete(item.id)}
                            className="rounded-lg bg-red-500 px-3 py-2 text-xs font-bold text-white hover:bg-red-600 transition-colors shadow-sm"
                          >
                            <Trash2 size={14} className="inline mr-1" /> Xác nhận hủy
                          </button>
                          <button
                            onClick={cancelDelete}
                            className="rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-500 hover:bg-gray-200 transition-colors"
                          >
                            Quay lại
                          </button>
                        </div>
                      ) : (
                        <div className="flex gap-1.5">
                          {/* Nút Báo Xong */}
                          <button
                            onClick={() => handlePrepared(item.id)}
                            disabled={isPreparing}
                            className="rounded-lg bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-600 transition-colors hover:bg-emerald-100 disabled:cursor-wait disabled:opacity-60 border border-emerald-200"
                          >
                            {isPreparing ? (
                              <span className="flex items-center gap-1"><Loader2 size={14} className="animate-spin" /> Đợi...</span>
                            ) : (
                              <span className="flex items-center gap-1"><CheckCircle size={14} /> Xong</span>
                            )}
                          </button>

                          {/* Nút Sửa / Xóa: Cả Manager và Staff đều dùng được */}
                          <button
                            onClick={() => startEdit(item)}
                            className="p-2 bg-gray-100 text-gray-500 rounded-lg hover:bg-gray-200 hover:text-blue-600 transition-colors"
                            title="Sửa số lượng/ghi chú"
                          >
                            <Edit3 size={16} />
                          </button>
                          <button
                            onClick={() => setConfirmDeleteId(item.id)}
                            className="p-2 bg-red-50 text-red-400 rounded-lg hover:bg-red-100 hover:text-red-600 transition-colors"
                            title="Hủy món"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default OrderDetailModal;
