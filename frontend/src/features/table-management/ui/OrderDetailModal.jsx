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
      <div className="flex items-center justify-between border-b border-slate-200/80 bg-slate-50/80 px-6 py-4 transition-colors dark:border-slate-800/80 dark:bg-slate-950/80">
        <div>
          <h3 className="text-lg font-black text-slate-900 dark:text-slate-100">
            {table?.tableNumber ? (
              <>Bàn <span className="font-mono text-indigo-600 dark:text-indigo-400">{table.tableNumber}</span> - </>
            ) : order?.id ? (
              <>Đơn mang về <span className="font-mono text-indigo-600 dark:text-indigo-400">#{order.id}</span> - </>
            ) : ''}
            {items.length > 0 ? 'Chi tiết đơn' : 'Trạng thái'}
          </h3>
          <span
            className={`mt-1 inline-flex items-center rounded-xl border px-2.5 py-0.5 text-xs font-bold ${
              items.length > 0
                ? 'border-emerald-200/80 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-950/40 dark:text-emerald-300'
                : 'border-slate-200/80 bg-slate-100 text-slate-600 dark:border-slate-700/80 dark:bg-slate-800 dark:text-slate-300'
            }`}
          >
            {items.length > 0 ? (
              <>Mã đơn: <span className="ml-1 font-mono font-black">#{order?.id}</span></>
            ) : table?.tableNumber ? (
              'Bàn trống'
            ) : (
              'Đơn chưa có món'
            )}
          </span>
        </div>
        <button
          onClick={onClose}
          className="rounded-xl p-2 text-slate-400 transition-colors hover:bg-slate-200/60 hover:text-slate-700 dark:hover:bg-slate-800 dark:hover:text-slate-200"
          aria-label="Đóng"
        >
          <X size={20} />
        </button>
      </div>

      <div className="custom-scrollbar flex-1 space-y-3.5 overflow-y-auto bg-slate-50/40 p-6 transition-colors dark:bg-slate-950/40">
        {/* Empty State */}
        {items.length === 0 && (
          <div className="flex h-full flex-col items-center justify-center space-y-4 text-slate-400 dark:text-slate-500">
            <div className="flex h-20 w-20 items-center justify-center rounded-2xl bg-slate-100 dark:bg-slate-800">
              <UtensilsCrossed size={36} className="opacity-50" />
            </div>
            <div className="text-center">
              <p className="text-base font-bold text-slate-700 dark:text-slate-300">
                Bàn này đang trống
              </p>
              <p className="text-xs text-slate-400">Chưa có món ăn nào được gọi.</p>
            </div>
            <button
              onClick={onClose}
              className="mt-3 rounded-xl border border-slate-200/80 bg-white px-5 py-2 text-xs font-bold text-slate-700 shadow-sm transition hover:bg-slate-50 active:scale-95 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
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
              className={`flex gap-4 rounded-2xl border p-4 transition-all ${
                isCancelled
                  ? 'border-rose-200/80 bg-rose-50/50 dark:border-rose-500/30 dark:bg-rose-500/10'
                  : isPrepared
                    ? 'border-emerald-200/80 bg-emerald-50/50 dark:border-emerald-500/30 dark:bg-emerald-500/10'
                    : 'border-slate-200/80 bg-white shadow-sm dark:border-slate-800/80 dark:bg-slate-900'
              }`}
            >
              <div className="flex-1">
                <div className="flex items-center gap-2 text-base font-bold text-slate-900 dark:text-slate-100">
                  {name}
                  <span className="font-mono font-black text-indigo-600 dark:text-indigo-400">
                    x{editingId === item.id ? editVal.quantity : item.quantity}
                  </span>
                  {isCancelled && (
                    <span className="rounded-full bg-rose-100 px-2 py-0.5 text-xs font-bold text-rose-800 dark:bg-rose-500/20 dark:text-rose-300">
                      Đã hủy
                    </span>
                  )}
                  {isPrepared && !isCancelled && (
                    <span className="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-bold text-emerald-800 dark:bg-emerald-500/20 dark:text-emerald-300">
                      Đã phục vụ
                    </span>
                  )}
                </div>

                {/* Hiển thị Options (Size, Toppings...) */}
                {item.options && item.options.length > 0 && (
                  <div className="mt-1 flex flex-wrap gap-1">
                    {item.options.map((opt, idx) => (
                      <span
                        key={idx}
                        className="rounded-lg border border-indigo-100 bg-indigo-50/80 px-2 py-0.5 text-[10px] font-semibold text-indigo-700 dark:border-indigo-500/30 dark:bg-indigo-950/40 dark:text-indigo-300"
                      >
                        {opt.optionName}: {opt.optionValueName}
                      </span>
                    ))}
                  </div>
                )}

                {editingId === item.id ? (
                  <div className="mt-3 space-y-3 rounded-2xl border border-indigo-100 bg-indigo-50/50 p-3.5 dark:border-indigo-500/30 dark:bg-indigo-950/30">
                    <div className="flex items-center gap-3">
                      <label className="w-16 shrink-0 text-xs font-bold text-indigo-700 dark:text-indigo-300">
                        Số lượng
                      </label>
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() =>
                            setEditVal((v) => ({ ...v, quantity: Math.max(1, v.quantity - 1) }))
                          }
                          className="flex h-8 w-8 items-center justify-center rounded-xl border border-slate-200/80 bg-white font-bold text-slate-700 transition hover:bg-slate-100 active:scale-95 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
                        >
                          −
                        </button>
                        <span className="w-10 text-center font-mono text-lg font-black text-indigo-600 dark:text-indigo-400">
                          {editVal.quantity}
                        </span>
                        <button
                          onClick={() =>
                            setEditVal((v) => ({ ...v, quantity: Math.min(99, v.quantity + 1) }))
                          }
                          className="flex h-8 w-8 items-center justify-center rounded-xl border border-slate-200/80 bg-white font-bold text-slate-700 transition hover:bg-slate-100 active:scale-95 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
                        >
                          +
                        </button>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <label className="w-16 shrink-0 text-xs font-bold text-indigo-700 dark:text-indigo-300">
                        Ghi chú
                      </label>
                      <input
                        type="text"
                        className="flex-1 rounded-xl border border-slate-200/80 bg-white px-3 py-1.5 text-xs text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
                        placeholder="Ghi chú cho món này..."
                        value={editVal.notes}
                        onChange={(e) => setEditVal((v) => ({ ...v, notes: e.target.value }))}
                      />
                    </div>
                  </div>
                ) : (
                  item.notes && (
                    <p className="mt-1.5 inline-block rounded-xl bg-slate-100/80 px-2.5 py-1 text-xs italic text-slate-600 dark:bg-slate-800/80 dark:text-slate-300">
                      Ghi chú: {item.notes}
                    </p>
                  )
                )}
              </div>

              <div className="flex flex-col justify-center gap-2">
                {isCancelled ? (
                  <X className="text-rose-500" size={26} />
                ) : isPrepared ? (
                  <CheckCircle className="text-emerald-500" size={26} />
                ) : (
                  <>
                    {editingId === item.id ? (
                      <div className="flex gap-2">
                        <button
                          onClick={() => saveEdit(item.id)}
                          className="inline-flex items-center gap-1 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 px-3.5 py-2 text-xs font-bold text-white shadow-md shadow-indigo-600/25 transition hover:from-indigo-500 hover:to-violet-500 active:scale-95"
                        >
                          <Save size={15} /> Lưu
                        </button>
                        <button
                          onClick={() => setEditingId(null)}
                          className="rounded-xl border border-slate-200/80 bg-white px-3 py-2 text-xs font-bold text-slate-600 transition hover:bg-slate-50 active:scale-95 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 dark:hover:bg-slate-800"
                        >
                          Hủy
                        </button>
                      </div>
                    ) : confirmDeleteId === item.id ? (
                      <div className="flex flex-col gap-1.5">
                        <button
                          onClick={() => handleDelete(item.id)}
                          className="inline-flex items-center gap-1 rounded-xl bg-gradient-to-r from-rose-600 to-red-600 px-3 py-2 text-xs font-bold text-white shadow-md shadow-rose-600/25 transition hover:from-rose-500 hover:to-red-500 active:scale-95"
                        >
                          <Trash2 size={13} /> Xác nhận hủy
                        </button>
                        <button
                          onClick={cancelDelete}
                          className="rounded-xl bg-slate-100 px-3 py-1.5 text-xs font-bold text-slate-600 transition hover:bg-slate-200 active:scale-95 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
                        >
                          Quay lại
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center gap-1.5">
                        {/* Nút Báo Xong */}
                        <button
                          onClick={() => handlePrepared(item.id)}
                          disabled={isPreparing}
                          className="inline-flex items-center gap-1 rounded-xl border border-emerald-200/80 bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-700 transition hover:bg-emerald-100 active:scale-95 disabled:cursor-wait disabled:opacity-60 dark:border-emerald-500/30 dark:bg-emerald-950/30 dark:text-emerald-300 dark:hover:bg-emerald-500/20"
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

                        {/* Nút Sửa / Xóa */}
                        <button
                          onClick={() => startEdit(item)}
                          className="rounded-xl border border-slate-200/80 bg-white p-2 text-slate-500 transition hover:bg-indigo-50 hover:text-indigo-600 active:scale-95 dark:border-slate-700/80 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-indigo-950/30 dark:hover:text-indigo-400"
                          title="Sửa số lượng/ghi chú"
                        >
                          <Edit3 size={15} />
                        </button>
                        <button
                          onClick={() => setConfirmDeleteId(item.id)}
                          className="rounded-xl border border-rose-200/60 bg-rose-50/60 p-2 text-rose-500 transition hover:bg-rose-100 hover:text-rose-700 active:scale-95 dark:border-rose-900/40 dark:bg-rose-950/30 dark:text-rose-400 dark:hover:bg-rose-900/40"
                          title="Hủy món"
                        >
                          <Trash2 size={15} />
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
