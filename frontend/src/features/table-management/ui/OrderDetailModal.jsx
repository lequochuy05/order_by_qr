import React, { useEffect, useRef, useState, useCallback } from 'react';
import { X, CheckCircle, Trash2, Edit3, Save, UtensilsCrossed, Loader2 } from 'lucide-react';
import { orderService } from '@entities/order/api/orderService.js';
import { ORDER_ITEM_STATUSES } from '@entities/order/lib/orderStatus.js';
import { useAuth } from '@features/auth';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import SharedModal from '@shared/ui/SharedModal.jsx';

const nextWsSuppressUntil = () => Date.now() + 3_000;

const OrderDetailModal = ({ isOpen, onClose, table, order, onOrderUpdate }) => {
  const [items, setItems] = useState(order?.orderItems || []);
  const [prevOrder, setPrevOrder] = useState(order);
  const [editingId, setEditingId] = useState(null);
  const [editVal, setEditVal] = useState({ quantity: 1, notes: '' });
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);
  const [preparingItemIds, setPreparingItemIds] = useState(() => new Set());
  const preparingItemIdsRef = useRef(new Set());
  const preparedRequestQueueRef = useRef(Promise.resolve());
  const orderIdRef = useRef(order?.id);
  const suppressWsReloadUntilRef = useRef(0);

  const { user } = useAuth();

  useEffect(() => {
    orderIdRef.current = order?.id;
  }, [order?.id]);

  // Realtime: tự động reload order khi có thay đổi từ WebSocket
  const handleWsMessage = useCallback(() => {
    if (!isOpen || !orderIdRef.current || !onOrderUpdate) return;
    if (Date.now() < suppressWsReloadUntilRef.current) return;
    // Delay nhẹ để backend kịp commit, rồi reload qua callback của parent
    setTimeout(onOrderUpdate, 300);
  }, [isOpen, onOrderUpdate]);
  useWebSocket('/topic/orders', handleWsMessage, { scope: 'admin' });

  // Adjust state when order prop changes (React recommended pattern)
  if (order !== prevOrder) {
    setPrevOrder(order);
    setItems(order?.orderItems || []);
  }

  if (!isOpen || !table) return null;

  const handlePrepared = (itemId) => {
    if (preparingItemIdsRef.current.has(itemId)) return;

    preparingItemIdsRef.current.add(itemId);
    suppressWsReloadUntilRef.current = nextWsSuppressUntil();
    setPreparingItemIds(new Set(preparingItemIdsRef.current));

    preparedRequestQueueRef.current = preparedRequestQueueRef.current
      .then(async () => {
        try {
          await orderService.markItemPrepared(itemId, user?.userId);
          setItems((prev) =>
            prev.map((i) =>
              i.id === itemId ? { ...i, prepared: true, status: ORDER_ITEM_STATUSES.FINISHED } : i,
            ),
          );
          showSuccessToast('Đã hoàn tất chuẩn bị món');
          await onOrderUpdate?.({ reloadOrder: false });
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
        prev.map((i) =>
          i.id === itemId ? { ...i, prepared: true, status: ORDER_ITEM_STATUSES.CANCELLED } : i,
        ),
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
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      ariaLabel={table?.tableNumber ? `Chi tiết đơn bàn ${table.tableNumber}` : 'Chi tiết đơn'}
      className="max-w-2xl !h-[calc(100dvh-1rem)] !w-[calc(100vw-1rem)] !overflow-hidden !rounded-2xl !p-0 sm:!h-[80vh] sm:!w-full"
    >
      <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4 transition-colors dark:border-slate-800 dark:bg-slate-950/60">
        <div>
          <h3 className="text-lg font-bold text-slate-900 dark:text-slate-100">
            {table?.tableNumber ? `Bàn ${table.tableNumber} - ` : ''}
            {items.length > 0 ? 'Chi tiết đơn' : 'Trạng thái'}
          </h3>
          <span
            className={`rounded border px-2 py-0.5 text-xs ${items.length > 0 ? 'border-green-200 bg-green-100 text-green-700 dark:border-green-500/30 dark:bg-green-500/10 dark:text-green-300' : 'border-slate-200 bg-slate-100 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300'}`}
          >
            {items.length > 0 ? `Mã đơn: #${order?.id}` : 'Bàn trống'}
          </span>
        </div>
        <button
          onClick={onClose}
          className="rounded-full p-2 text-slate-500 transition-colors hover:bg-slate-200 hover:text-slate-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100"
        >
          <X size={24} />
        </button>
      </div>

      <div className="custom-scrollbar flex-1 space-y-4 overflow-y-auto bg-slate-50/50 p-6 transition-colors dark:bg-slate-950/40">
        {/* Empty State */}
        {items.length === 0 && (
          <div className="flex h-full flex-col items-center justify-center space-y-4 text-slate-400 dark:text-slate-500">
            <div className="flex h-24 w-24 items-center justify-center rounded-full bg-slate-100 dark:bg-slate-800">
              <UtensilsCrossed size={40} className="opacity-50" />
            </div>
            <div className="text-center">
              <p className="text-lg font-medium text-slate-600 dark:text-slate-300">
                Bàn này đang trống
              </p>
              <p className="text-sm">Chưa có món ăn nào được gọi.</p>
            </div>
            <button
              onClick={onClose}
              className="mt-4 rounded-lg border border-slate-300 bg-white px-6 py-2 font-medium text-slate-600 shadow-sm transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
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
          const isCancelled = item.status === ORDER_ITEM_STATUSES.CANCELLED;
          const isPreparing = preparingItemIds.has(item.id);

          return (
            <div
              key={item.id}
              className={`flex gap-4 rounded-xl border p-4 transition-all ${isCancelled ? 'border-rose-200 bg-rose-50 dark:border-rose-500/30 dark:bg-rose-500/10' : isPrepared ? 'border-green-200 bg-green-50 dark:border-green-500/30 dark:bg-green-500/10' : 'border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900'}`}
            >
              <div className="flex-1">
                <div className="flex items-center gap-2 text-lg font-bold text-slate-900 dark:text-slate-100">
                  {name}
                  <span className="text-orange-600">
                    x{editingId === item.id ? editVal.quantity : item.quantity}
                  </span>
                  {isCancelled && (
                    <span className="text-xs bg-rose-200 text-rose-800 px-2 py-0.5 rounded-full font-bold dark:bg-rose-500/20 dark:text-rose-300">
                      Đã hủy
                    </span>
                  )}
                  {isPrepared && !isCancelled && (
                    <span className="text-xs bg-green-200 text-green-800 px-2 py-0.5 rounded-full font-bold dark:bg-green-500/20 dark:text-green-300">
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
                        className="rounded border border-blue-100 bg-blue-50 px-1.5 py-0.5 text-[10px] font-medium text-blue-600 dark:border-blue-500/30 dark:bg-blue-500/10 dark:text-blue-300"
                      >
                        {opt.optionName}: {opt.optionValueName}
                      </span>
                    ))}
                  </div>
                )}

                {editingId === item.id ? (
                  <div className="mt-3 space-y-3 rounded-xl border border-blue-100 bg-blue-50 p-3 dark:border-blue-500/30 dark:bg-blue-500/10">
                    <div className="flex items-center gap-3">
                      <label className="w-16 shrink-0 text-xs font-semibold text-blue-700 dark:text-blue-300">
                        Số lượng
                      </label>
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() =>
                            setEditVal((v) => ({ ...v, quantity: Math.max(1, v.quantity - 1) }))
                          }
                          className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-white font-bold text-slate-600 transition-colors hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
                        >
                          −
                        </button>
                        <span className="w-10 text-center text-lg font-bold text-blue-700 dark:text-blue-300">
                          {editVal.quantity}
                        </span>
                        <button
                          onClick={() =>
                            setEditVal((v) => ({ ...v, quantity: Math.min(99, v.quantity + 1) }))
                          }
                          className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-white font-bold text-slate-600 transition-colors hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
                        >
                          +
                        </button>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <label className="w-16 shrink-0 text-xs font-semibold text-blue-700 dark:text-blue-300">
                        Ghi chú
                      </label>
                      <input
                        type="text"
                        className="flex-1 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition-all focus:border-blue-400 focus:ring-2 focus:ring-blue-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
                        placeholder="Ghi chú cho món này..."
                        value={editVal.notes}
                        onChange={(e) => setEditVal((v) => ({ ...v, notes: e.target.value }))}
                      />
                    </div>
                  </div>
                ) : (
                  item.notes && (
                    <p className="mt-1 inline-block rounded-lg bg-slate-50 p-1.5 text-sm italic text-slate-500 dark:bg-slate-800 dark:text-slate-400">
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
                          className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
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
                          className="rounded-lg bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-500 transition-colors hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
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
                          className="rounded-lg bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-600 transition-colors hover:bg-emerald-100 disabled:cursor-wait disabled:opacity-60 border border-emerald-200 dark:bg-emerald-950/30 dark:text-emerald-400 dark:border-emerald-500/30 dark:hover:bg-emerald-500/20"
                        >
                          {isPreparing ? (
                            <span className="flex items-center gap-1">
                              <Loader2 size={14} className="animate-spin" /> Đợi...
                            </span>
                          ) : (
                            <span className="flex items-center gap-1">
                              <CheckCircle size={14} /> Xong
                            </span>
                          )}
                        </button>

                        {/* Nút Sửa / Xóa: Cả Manager và Staff đều dùng được */}
                        <button
                          onClick={() => startEdit(item)}
                          className="rounded-lg bg-slate-100 p-2 text-slate-500 transition-colors hover:bg-slate-200 hover:text-blue-600 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700 dark:hover:text-blue-300"
                          title="Sửa số lượng/ghi chú"
                        >
                          <Edit3 size={16} />
                        </button>
                        <button
                          onClick={() => setConfirmDeleteId(item.id)}
                          className="p-2 bg-red-50 text-red-400 rounded-lg hover:bg-red-100 hover:text-red-600 transition-colors dark:bg-red-950/30 dark:text-red-400 dark:hover:bg-red-500/20 dark:hover:text-red-300"
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
    </SharedModal>
  );
};

export default OrderDetailModal;
