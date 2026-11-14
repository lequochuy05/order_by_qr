import React, { useMemo } from 'react';
import {
  AlertTriangle,
  Check,
  ChefHat,
  Clock3,
  Flame,
  Loader2,
  StickyNote,
  Tag,
  Trash2,
} from 'lucide-react';
import { ORDER_ITEM_STATUSES } from '@entities/order/lib/orderStatus.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { fmtTime } from '@shared/lib/formatters.js';
import { aggregateBatchCookingItems } from '../lib/kitchenItems.js';

const BatchCookingView = ({
  items,
  onBatchUpdateStatus,
  onUpdateStatus,
  onCancelItem,
  processingItems,
}) => {
  const groups = useMemo(() => aggregateBatchCookingItems(items), [items]);
  const { confirm } = useConfirmModal();

  const handleCancelItem = async (item) => {
    if (!onCancelItem || processingItems.has(item.id)) return;
    const isTakeaway = item.orderType === 'TAKEAWAY';
    const destinationLabel = isTakeaway ? 'đơn Mang về' : `Bàn ${item.tableName}`;
    const confirmed = await confirm(
      'Hủy món ăn',
      `Bạn có chắc chắn muốn hủy món "${item.itemName}" của ${destinationLabel}?\nKho nguyên liệu sẽ được tự động hoàn trả.`,
    );
    if (confirmed) {
      await onCancelItem(item.id, 'Bếp hủy món');
    }
  };

  if (groups.length === 0) {
    return (
      <div className="mt-6 flex min-h-[380px] flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white/60 p-8 text-center dark:border-slate-800 dark:bg-slate-900/40">
        <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25">
          <ChefHat size={32} />
        </div>
        <h3 className="text-lg font-black text-slate-800 dark:text-slate-100">
          Không có món cần nấu lúc này
        </h3>
        <p className="mt-1 text-sm text-slate-400">
          Tất cả các món đang chờ hoặc đang nấu sẽ xuất hiện tại đây để gom nấu hàng loạt.
        </p>
      </div>
    );
  }

  return (
    <div className="mt-6 space-y-5">
      <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        {groups.map((group) => {
          const pendingIds = group.pendingItems.map((i) => i.id);
          const cookingIds = group.cookingItems.map((i) => i.id);
          const hasPendingProcessing = pendingIds.some((id) => processingItems.has(id));
          const hasCookingProcessing = cookingIds.some((id) => processingItems.has(id));

          return (
            <article
              key={group.key}
              className={`flex flex-col justify-between overflow-hidden rounded-2xl border bg-white shadow-sm transition hover:shadow-md dark:bg-slate-900 ${
                group.isOverdue
                  ? 'border-rose-300 ring-1 ring-rose-100 dark:border-rose-500/40 dark:ring-rose-500/10'
                  : 'border-slate-200 dark:border-slate-800'
              }`}
            >
              <div
                className={`h-1.5 ${
                  group.isOverdue
                    ? 'bg-rose-500'
                    : group.pendingQuantity > 0
                      ? 'bg-amber-500'
                      : 'bg-indigo-500'
                }`}
              />

              <div className="p-4 sm:p-5">
                {/* Header */}
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <span className="inline-flex items-center gap-1 text-[11px] font-black uppercase tracking-wider text-slate-400">
                      <Tag size={12} />
                      {group.category}
                    </span>
                    <h3 className="text-lg font-black text-slate-900 dark:text-slate-100">
                      {group.itemName}
                    </h3>
                  </div>

                  <span
                    className={`inline-flex items-center gap-1 rounded-lg px-2.5 py-1 text-xs font-black ${
                      group.isOverdue
                        ? 'bg-rose-50 text-rose-600 dark:bg-rose-500/10 dark:text-rose-300'
                        : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'
                    }`}
                  >
                    {group.isOverdue ? <AlertTriangle size={13} /> : <Clock3 size={13} />}
                    {group.maxWaitMinutes} phút
                  </span>
                </div>

                {/* Aggregate count badges */}
                <div className="mt-3 flex flex-wrap items-center gap-2">
                  <span className="rounded-xl bg-slate-900 px-3 py-1 font-mono text-sm font-black text-white dark:bg-slate-700">
                    Tổng: x{group.totalQuantity}
                  </span>
                  {group.pendingQuantity > 0 && (
                    <span className="rounded-xl bg-amber-100 px-2.5 py-1 font-mono text-xs font-black text-amber-800 dark:bg-amber-950/40 dark:text-amber-300">
                      Chờ: x{group.pendingQuantity}
                    </span>
                  )}
                  {group.cookingQuantity > 0 && (
                    <span className="rounded-xl bg-indigo-100 px-2.5 py-1 font-mono text-xs font-black text-indigo-800 dark:bg-indigo-950/40 dark:text-indigo-300">
                      Đang nấu: x{group.cookingQuantity}
                    </span>
                  )}
                </div>

                {/* Table details list */}
                <div className="mt-4 space-y-2 border-t border-slate-100 pt-3 dark:border-slate-800">
                  <p className="text-[11px] font-black uppercase tracking-wider text-slate-400">
                    Danh sách đơn bàn
                  </p>
                  <div className="max-h-56 space-y-2 overflow-y-auto pr-1">
                    {group.items.map((item) => {
                      const isItemProcessing = processingItems.has(item.id);
                      const isTakeaway = item.orderType === 'TAKEAWAY';

                      return (
                        <div
                          key={item.id}
                          className="flex items-center justify-between gap-2 rounded-xl border border-slate-100 bg-slate-50/80 p-2.5 text-xs transition dark:border-slate-800 dark:bg-slate-800/40"
                        >
                          <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-1.5">
                              {isTakeaway ? (
                                <span className="rounded bg-violet-600 px-1.5 py-0.5 font-bold text-white">
                                  Mang về
                                </span>
                              ) : (
                                <span className="rounded bg-slate-800 px-1.5 py-0.5 font-bold text-white dark:bg-slate-700">
                                  Bàn {item.tableName}
                                </span>
                              )}
                              <span className="font-mono font-black text-indigo-600 dark:text-indigo-400">
                                x{item.quantity}
                              </span>
                              {item.batchNumber > 1 && (
                                <span className="rounded bg-amber-100 px-1.5 py-0.5 font-bold text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
                                  Đợt {item.batchNumber}
                                </span>
                              )}
                              <span
                                className={`rounded px-1.5 py-0.5 font-semibold ${
                                  item.status === ORDER_ITEM_STATUSES.PENDING
                                    ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/40 dark:text-amber-300'
                                    : 'bg-indigo-100 text-indigo-800 dark:bg-indigo-950/40 dark:text-indigo-300'
                                }`}
                              >
                                {item.status === ORDER_ITEM_STATUSES.PENDING ? 'Chờ' : 'Đang nấu'}
                              </span>
                            </div>

                            {item.hasNotes && (
                              <div className="mt-1 flex items-center gap-1 text-[11px] font-semibold text-amber-700 dark:text-amber-300">
                                <StickyNote size={12} className="shrink-0" />
                                <span className="truncate">{item.notes}</span>
                              </div>
                            )}

                            {item.options?.length > 0 && (
                              <p className="mt-0.5 truncate text-[11px] text-slate-500 dark:text-slate-400">
                                {item.options
                                  .map((opt) => `${opt.optionName}: ${opt.optionValueName}`)
                                  .join(', ')}
                              </p>
                            )}

                            <span className="mt-0.5 block text-[10px] text-slate-400">
                              Nhận: {fmtTime(item.createdAt)} ({item.waitMinutes}p)
                            </span>
                          </div>

                          <div className="flex shrink-0 items-center gap-1">
                            {item.status === ORDER_ITEM_STATUSES.PENDING && (
                              <button
                                type="button"
                                onClick={() =>
                                  onUpdateStatus(item.id, ORDER_ITEM_STATUSES.COOKING)
                                }
                                disabled={isItemProcessing}
                                className="rounded-lg bg-indigo-50 p-1.5 text-indigo-600 transition hover:bg-indigo-100 disabled:opacity-50 dark:bg-indigo-500/10 dark:text-indigo-300 dark:hover:bg-indigo-500/20"
                                title="Nấu món này"
                              >
                                {isItemProcessing ? (
                                  <Loader2 size={14} className="animate-spin" />
                                ) : (
                                  <Flame size={14} />
                                )}
                              </button>
                            )}
                            {item.status === ORDER_ITEM_STATUSES.COOKING && (
                              <button
                                type="button"
                                onClick={() =>
                                  onUpdateStatus(item.id, ORDER_ITEM_STATUSES.FINISHED)
                                }
                                disabled={isItemProcessing}
                                className="rounded-lg bg-emerald-50 p-1.5 text-emerald-600 transition hover:bg-emerald-100 disabled:opacity-50 dark:bg-emerald-500/10 dark:text-emerald-300 dark:hover:bg-emerald-500/20"
                                title="Hoàn thành món này"
                              >
                                {isItemProcessing ? (
                                  <Loader2 size={14} className="animate-spin" />
                                ) : (
                                  <Check size={14} />
                                )}
                              </button>
                            )}
                            {onCancelItem && (
                              <button
                                type="button"
                                onClick={() => handleCancelItem(item)}
                                disabled={isItemProcessing}
                                className="rounded-lg p-1.5 text-slate-400 transition hover:bg-rose-50 hover:text-rose-500 disabled:opacity-50 dark:hover:bg-rose-950/30"
                                title="Hủy món này"
                              >
                                <Trash2 size={13} />
                              </button>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>

              {/* Batch Action Buttons */}
              <div className="border-t border-slate-100 bg-slate-50/50 p-3 sm:px-5 dark:border-slate-800 dark:bg-slate-900/60">
                <div className="flex flex-col gap-2 sm:flex-row">
                  {group.pendingQuantity > 0 && (
                    <button
                      type="button"
                      onClick={() =>
                        onBatchUpdateStatus(pendingIds, ORDER_ITEM_STATUSES.COOKING)
                      }
                      disabled={hasPendingProcessing}
                      className="inline-flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 py-2.5 text-xs font-black text-white shadow-md shadow-indigo-600/25 transition hover:from-indigo-500 hover:to-violet-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {hasPendingProcessing ? (
                        <Loader2 size={15} className="animate-spin" />
                      ) : (
                        <Flame size={15} />
                      )}
                      Nấu tất cả chờ ({group.pendingQuantity})
                    </button>
                  )}
                  {group.cookingQuantity > 0 && (
                    <button
                      type="button"
                      onClick={() =>
                        onBatchUpdateStatus(cookingIds, ORDER_ITEM_STATUSES.FINISHED)
                      }
                      disabled={hasCookingProcessing}
                      className="inline-flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 py-2.5 text-xs font-black text-white shadow-md shadow-emerald-600/25 transition hover:from-emerald-500 hover:to-teal-500 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {hasCookingProcessing ? (
                        <Loader2 size={15} className="animate-spin" />
                      ) : (
                        <Check size={15} />
                      )}
                      Xong tất cả ({group.cookingQuantity})
                    </button>
                  )}
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </div>
  );
};

export default BatchCookingView;
