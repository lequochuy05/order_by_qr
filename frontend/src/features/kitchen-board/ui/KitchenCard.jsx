import React from 'react';
import {
  AlertTriangle,
  Ban,
  BookOpen,
  Check,
  CheckCircle2,
  Clock3,
  Flame,
  Loader2,
  StickyNote,
  Tag,
  Trash2,
} from 'lucide-react';
import { ORDER_ITEM_STATUSES } from '@entities/order';
import { useConfirm } from '@shared/hooks/useConfirmModal.js';
import { fmtTime } from '@shared/lib/formatters.js';
import { useTranslation } from '@shared/lib/i18n';
import { NoteSafetyBadges } from '@entities/ai';

const KitchenCard = ({
  item,
  status,
  onUpdateStatus,
  onCancelItem,
  onOpenRecipe,
  onToggleAvailability,
  isProcessing,
  isTogglingAvailability = false,
}) => {
  const { language, isEn } = useTranslation('kitchen');
  const priority = getPriority(item);
  const confirm = useConfirm();
  const isAvailable = item.isAvailable !== false;
  const hasCatalogLink = Boolean(item.menuItemId || item.comboId);
  const isOrderPaid = item.orderPaymentStatus === 'PAID';

  const handleCancel = async () => {
    if (!onCancelItem || isProcessing) return;
    const isTakeaway = item.orderType === 'TAKEAWAY';
    const destinationLabel = isTakeaway
      ? (isEn ? 'Takeaway order' : 'đơn Mang về')
      : (isEn ? `Table ${item.tableName}` : `Bàn ${item.tableName}`);
    const confirmed = await confirm(
      isEn ? 'Cancel item' : 'Hủy món ăn',
      isEn
        ? `Are you sure you want to cancel "${item.itemName}" for ${destinationLabel}?\nInventory will be automatically restored.`
        : `Bạn có chắc chắn muốn hủy món "${item.itemName}" của ${destinationLabel}?\nKho nguyên liệu sẽ được tự động hoàn trả.`,
    );
    if (confirmed) {
      await onCancelItem(item.id, isEn ? 'Kitchen cancelled' : 'Bếp hủy món');
    }
  };

  const isTakeaway = item.orderType === 'TAKEAWAY';

  return (
    <article
      className={`overflow-hidden rounded-2xl border bg-white/95 backdrop-blur-sm shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:bg-[#0F172A]/95 ${priority.card}`}
    >
      <div className={`h-1 ${priority.bar}`} />

      <div className="p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex flex-wrap items-center gap-2">
            {isTakeaway ? (
              <span className="rounded-lg bg-gradient-to-r from-violet-600 to-indigo-600 px-2.5 py-1 text-xs font-black text-white shadow-sm">
                {isEn ? 'Takeaway' : 'Mang về'}
              </span>
            ) : (
              <span className="rounded-lg bg-slate-900 px-2.5 py-1 text-xs font-black text-white dark:bg-slate-700">
                {isEn ? `Table ${item.tableName}` : `Bàn ${item.tableName}`}
              </span>
            )}
            <span className="text-xs font-semibold text-slate-400">
              {isEn ? 'Order' : 'Đơn'} <span className="font-mono font-bold text-slate-600 dark:text-slate-300">#{item.orderId}</span>
            </span>
            {item.batchNumber > 1 && (
              <span className="rounded-lg bg-amber-100 px-2 py-0.5 text-xs font-bold text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
                {isEn ? `Batch ${item.batchNumber} (Add-on)` : `Đợt ${item.batchNumber} (Gọi thêm)`}
              </span>
            )}
          </div>
          <div className="flex items-center gap-1.5">
            <WaitBadge item={item} priority={priority} isEn={isEn} />
            {onCancelItem &&
              !isOrderPaid &&
              (status === ORDER_ITEM_STATUSES.PENDING || status === ORDER_ITEM_STATUSES.COOKING) && (
              <button
                type="button"
                onClick={handleCancel}
                disabled={isProcessing}
                className="rounded-lg p-1 text-slate-400 transition hover:bg-rose-50 hover:text-rose-500 disabled:cursor-not-allowed disabled:opacity-50 dark:hover:bg-rose-950/30"
                title={isEn ? 'Cancel item' : 'Hủy món ăn'}
              >
                <Trash2 size={14} />
              </button>
            )}
          </div>
        </div>

        <div className="mt-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <div className="mb-1 flex flex-wrap items-center gap-1.5">
                <p className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-slate-400">
                  <Tag size={13} />
                  {item.category}
                </p>
                {!isAvailable && (
                  <span className="inline-flex items-center gap-1 rounded-md border border-rose-200 bg-rose-50 px-1.5 py-0.5 text-[10px] font-black uppercase text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/15 dark:text-rose-300">
                    <Ban size={11} />
                    {isEn ? 'Out of stock' : 'Tạm hết món'}
                  </span>
                )}
              </div>
              <h3 className="text-lg font-black leading-snug text-slate-800 dark:text-slate-100">
                {item.itemName}
              </h3>
            </div>
            <span className="min-w-12 rounded-2xl bg-indigo-50 border border-indigo-200/60 px-3 py-1.5 text-center text-lg font-black font-mono text-indigo-700 dark:bg-indigo-950/40 dark:border-indigo-800/40 dark:text-indigo-400 shadow-sm">
              x{item.quantity}
            </span>
          </div>
        </div>

        {item.options?.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1.5">
            {item.options.map((option) => (
              <span
                key={`${option.optionName}-${option.optionValueName}-${option.valueId || ''}`}
                className="rounded-lg border border-slate-200 bg-slate-50 px-2 py-1 text-xs font-semibold text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
              >
                {option.optionName}: {option.optionValueName}
              </span>
            ))}
          </div>
        )}

        {item.hasNotes && (
          <div className="mt-3 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-200">
            <div className="flex items-start gap-2">
              <StickyNote size={16} className="mt-0.5 shrink-0" />
              <div className="min-w-0 flex-1 space-y-1.5">
                <p className="text-[11px] font-black uppercase tracking-wider opacity-70">
                  {isEn ? 'Kitchen notes' : 'Ghi chú bếp'}
                </p>
                <p className="font-semibold">{item.notes}</p>
                <NoteSafetyBadges
                  note={item.notes}
                  safety={item.noteSafety}
                  language={language}
                  compact
                />
              </div>
            </div>
          </div>
        )}

        <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-400 dark:border-slate-800">
          <span>{isEn ? `Received at ${fmtTime(item.createdAt)}` : `Nhận lúc ${fmtTime(item.createdAt)}`}</span>
          {status === ORDER_ITEM_STATUSES.COOKING && <span>{isEn ? `Cooking for ${item.stageMinutes} mins` : `Đang nấu ${item.stageMinutes} phút`}</span>}
          {status === ORDER_ITEM_STATUSES.FINISHED && <span>{isEn ? `Done at ${fmtTime(item.statusUpdatedAt)}` : `Xong lúc ${fmtTime(item.statusUpdatedAt)}`}</span>}
        </div>

        {hasCatalogLink && (onOpenRecipe || onToggleAvailability) && (
          <div className="mt-2.5 flex flex-wrap items-center gap-2">
            {onOpenRecipe && (
              <button
                type="button"
                onClick={() =>
                  onOpenRecipe({
                    menuItemId: item.menuItemId,
                    comboId: item.comboId,
                    isCombo: item.isCombo,
                    itemName: item.itemName,
                    category: item.category,
                    quantity: item.quantity,
                    isAvailable,
                  })
                }
                className="inline-flex flex-1 items-center justify-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-xs font-bold text-slate-700 transition hover:border-indigo-200 hover:bg-indigo-50 hover:text-indigo-700 dark:border-slate-700 dark:bg-slate-800/80 dark:text-slate-200 dark:hover:border-indigo-500/30 dark:hover:bg-indigo-500/15 dark:hover:text-indigo-300"
              >
                <BookOpen size={14} />
                {isEn ? 'Recipe' : 'Công thức'}
              </button>
            )}
            {onToggleAvailability && (
              <button
                type="button"
                onClick={() =>
                  onToggleAvailability({
                    menuItemId: item.menuItemId,
                    comboId: item.comboId,
                    itemName: item.itemName,
                    isAvailable,
                  })
                }
                disabled={isTogglingAvailability}
                className={`inline-flex flex-1 items-center justify-center gap-1.5 rounded-xl border px-2.5 py-1.5 text-xs font-bold transition disabled:cursor-not-allowed disabled:opacity-50 ${
                  isAvailable
                    ? 'border-rose-200 bg-rose-50/70 text-rose-700 hover:bg-rose-100 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300 dark:hover:bg-rose-500/20'
                    : 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 dark:border-emerald-500/30 dark:bg-emerald-500/10 dark:text-emerald-300 dark:hover:bg-emerald-500/20'
                }`}
              >
                {isTogglingAvailability ? (
                  <Loader2 size={14} className="animate-spin" />
                ) : isAvailable ? (
                  <Ban size={14} />
                ) : (
                  <CheckCircle2 size={14} />
                )}
                {isAvailable
                  ? isEn
                    ? 'Mark out of stock'
                    : 'Báo hết món'
                  : isEn
                    ? 'Re-enable'
                    : 'Mở bán lại'}
              </button>
            )}
          </div>
        )}

        <div className="mt-3">
          {status === ORDER_ITEM_STATUSES.PENDING && (
            <ActionButton
              onClick={() => onUpdateStatus(item.id, ORDER_ITEM_STATUSES.COOKING)}
              disabled={isProcessing}
              loading={isProcessing}
              icon={Flame}
              label={isEn ? 'Start cooking' : 'Bắt đầu nấu'}
              loadingLabel={isEn ? 'Moving...' : 'Đang chuyển món...'}
              className="bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 text-white shadow-md shadow-indigo-600/25"
            />
          )}
          {status === ORDER_ITEM_STATUSES.COOKING && (
            <ActionButton
              onClick={() => onUpdateStatus(item.id, ORDER_ITEM_STATUSES.FINISHED)}
              disabled={isProcessing}
              loading={isProcessing}
              icon={Check}
              label={isEn ? 'Complete item' : 'Hoàn thành món'}
              loadingLabel={isEn ? 'Completing...' : 'Đang hoàn thành...'}
              className="bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white shadow-md shadow-emerald-600/25"
            />
          )}
          {status === ORDER_ITEM_STATUSES.FINISHED && (
            <div className="flex items-center justify-center gap-1.5 rounded-xl bg-emerald-50/80 py-2 text-xs font-bold text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300">
              <CheckCircle2 size={14} />
              {isEn ? 'Completed — waiting for service' : 'Đã hoàn thành — chờ phục vụ'}
            </div>
          )}
        </div>
      </div>
    </article>
  );
};

const WaitBadge = ({ item, priority, isEn }) => {
  if (item.status === ORDER_ITEM_STATUSES.FINISHED) {
    return (
      <span className="inline-flex items-center gap-1 rounded-lg bg-emerald-50 px-2 py-1 text-xs font-black text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300">
        <Check size={13} />
        {isEn ? 'Done' : 'Đã xong'}
      </span>
    );
  }

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-black ${priority.badge}`}
    >
      {item.isOverdue ? <AlertTriangle size={13} /> : <Clock3 size={13} />}
      {item.waitMinutes} {isEn ? 'mins' : 'phút'}
    </span>
  );
};

const ActionButton = ({ onClick, disabled, loading, icon, label, loadingLabel, className }) => (
  <button
    type="button"
    onClick={onClick}
    disabled={disabled}
    className={`flex w-full items-center justify-center gap-2 rounded-xl py-2.5 text-sm font-black text-white shadow-sm transition active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60 ${className}`}
  >
    {loading ? (
      <Loader2 size={17} className="animate-spin" />
    ) : (
      React.createElement(icon, { size: 17 })
    )}
    {loading ? loadingLabel : label}
  </button>
);

const getPriority = (item) => {
  if (item.status === ORDER_ITEM_STATUSES.FINISHED) {
    return {
      card: 'border-emerald-100 dark:border-emerald-500/20',
      bar: 'bg-emerald-500',
      badge: '',
    };
  }

  if (item.hasAllergenAlert) {
    return {
      card: 'border-rose-500 ring-2 ring-rose-500/20 dark:border-rose-500/75 dark:ring-rose-500/20',
      bar: 'bg-rose-600',
      badge: 'bg-rose-100 text-rose-700 dark:bg-rose-500/20 dark:text-rose-200',
    };
  }

  if (item.waitMinutes >= 20) {
    return {
      card: 'border-rose-300 ring-1 ring-rose-100 dark:border-rose-500/40 dark:ring-rose-500/10',
      bar: 'bg-rose-500',
      badge: 'bg-rose-50 text-rose-600 dark:bg-rose-500/10 dark:text-rose-300',
    };
  }

  if (item.waitMinutes >= 12) {
    return {
      card: 'border-amber-200 dark:border-amber-500/30',
      bar: 'bg-amber-400',
      badge: 'bg-amber-50 text-amber-600 dark:bg-amber-500/10 dark:text-amber-300',
    };
  }

  return {
    card: 'border-slate-200 dark:border-slate-800',
    bar: 'bg-slate-200 dark:bg-slate-700',
    badge: 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-300',
  };
};

export default KitchenCard;
