import { useEffect, useRef, useState } from 'react';
import { Check, Plus, ShoppingBag, SlidersHorizontal, Utensils } from 'lucide-react';

import { fmtVND } from '@shared/lib/formatters.js';
import { getQuickAddableSuggestions } from '../lib/chatSuggestions.js';

const AiSuggestionList = ({ suggestions, copy, orderBridge, onCloseChat }) => {
  const [recentAddedKey, setRecentAddedKey] = useState(null);
  const timerRef = useRef(null);

  useEffect(
    () => () => {
      window.clearTimeout(timerRef.current);
    },
    [],
  );

  if (!Array.isArray(suggestions) || suggestions.length === 0) {
    return null;
  }

  const canOrder = Boolean(orderBridge?.onAddSuggestion);
  const orderingBlocked = Boolean(orderBridge?.orderingBlocked);
  const quickAddable = getQuickAddableSuggestions(suggestions);
  const cartItemCount = orderBridge?.cartItemCount || 0;

  const markRecentlyAdded = (key) => {
    setRecentAddedKey(key);
    window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => setRecentAddedKey(null), 1400);
  };

  const handleAddSingle = (item) => {
    if (!canOrder || orderingBlocked || !item.available) return;
    const added = orderBridge.onAddSuggestion(item);
    if (added !== false && !item.hasOptions) {
      markRecentlyAdded(`${item.type}:${item.id}`);
    }
  };

  const handleQuickAddAll = () => {
    if (!canOrder || orderingBlocked || quickAddable.length === 0) return;
    let addedAny = false;
    for (const item of quickAddable) {
      const ok = orderBridge.onAddSuggestion(item);
      if (ok !== false) addedAny = true;
    }
    if (addedAny) {
      markRecentlyAdded('ALL');
    }
  };

  const handleOpenCart = () => {
    onCloseChat?.();
    orderBridge?.onOpenCart?.();
  };

  return (
    <div className="mt-2 space-y-2">
      {orderingBlocked && orderBridge?.orderingBlockReason && (
        <div className="rounded-xl border border-amber-300/70 bg-amber-50 px-3 py-1.5 text-[11px] font-semibold text-amber-800 dark:border-amber-500/30 dark:bg-amber-950/40 dark:text-amber-300">
          {orderBridge.orderingBlockReason}
        </div>
      )}

      <div className="space-y-1.5">
        {suggestions.map((item) => {
          const itemKey = `${item.type}:${item.id}`;
          const inCartQty = orderBridge?.getSuggestionCartQty?.(item) || 0;
          const isJustAdded = recentAddedKey === itemKey || (recentAddedKey === 'ALL' && !item.hasOptions);
          const isDisabled = !canOrder || orderingBlocked || !item.available;

          return (
            <div
              key={itemKey}
              className="flex items-center gap-2.5 rounded-2xl border border-slate-200/90 bg-white/95 p-2 shadow-xs transition-colors dark:border-slate-800 dark:bg-slate-900/95"
            >
              <div className="h-12 w-12 shrink-0 overflow-hidden rounded-xl bg-slate-100 dark:bg-slate-800">
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.name} className="h-full w-full object-cover" />
                ) : (
                  <div className="flex h-full w-full items-center justify-center text-indigo-500 dark:text-indigo-400">
                    <Utensils size={16} />
                  </div>
                )}
              </div>

              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-1.5">
                  <span className="truncate text-xs font-bold text-slate-900 dark:text-white">
                    {item.name}
                  </span>
                  {item.type === 'COMBO' && (
                    <span className="shrink-0 rounded-full bg-indigo-100 px-1.5 py-0.5 text-[9px] font-extrabold uppercase tracking-wide text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300">
                      {copy.comboLabel}
                    </span>
                  )}
                </div>

                {item.reason && (
                  <p className="mt-0.5 line-clamp-1 text-[10px] font-medium text-slate-500 dark:text-slate-400">
                    {item.reason}
                  </p>
                )}

                <div className="mt-1 flex items-center gap-2">
                  <span className="font-mono text-xs font-extrabold tabular-nums text-indigo-600 dark:text-indigo-400">
                    {fmtVND(item.price)}
                  </span>
                  {item.quantity > 1 && (
                    <span className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-[10px] font-bold text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                      x{item.quantity}
                    </span>
                  )}
                  {inCartQty > 0 && (
                    <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400">
                      • {copy.inCart}: {inCartQty}
                    </span>
                  )}
                </div>
              </div>

              {canOrder && (
                <button
                  type="button"
                  disabled={isDisabled}
                  onClick={() => handleAddSingle(item)}
                  className={`inline-flex shrink-0 items-center gap-1 rounded-xl px-2.5 py-1.5 text-[11px] font-bold transition-all active:scale-95 ${
                    isDisabled
                      ? 'cursor-not-allowed bg-slate-200 text-slate-400 dark:bg-slate-800 dark:text-slate-500'
                      : isJustAdded
                        ? 'bg-emerald-600 text-white shadow-sm'
                        : 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-sm shadow-indigo-600/20 hover:from-indigo-500 hover:to-violet-500'
                  }`}
                >
                  {!item.available ? (
                    copy.soldOut
                  ) : isJustAdded ? (
                    <>
                      <Check size={12} />
                      <span>{copy.added}</span>
                    </>
                  ) : item.hasOptions ? (
                    <>
                      <SlidersHorizontal size={12} />
                      <span>{copy.chooseOptions}</span>
                    </>
                  ) : (
                    <>
                      <Plus size={12} />
                      <span>{copy.addToCart}</span>
                    </>
                  )}
                </button>
              )}
            </div>
          );
        })}
      </div>

      {canOrder && (quickAddable.length >= 2 || cartItemCount > 0) && (
        <div className="flex flex-wrap items-center gap-2 pt-0.5">
          {quickAddable.length >= 2 && (
            <button
              type="button"
              disabled={orderingBlocked}
              onClick={handleQuickAddAll}
              className="inline-flex items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 px-2.5 py-1.5 text-[11px] font-bold text-indigo-700 transition-colors hover:bg-indigo-600 hover:text-white disabled:cursor-not-allowed disabled:opacity-50 dark:border-indigo-500/40 dark:bg-indigo-950/60 dark:text-indigo-300 dark:hover:bg-indigo-600 dark:hover:text-white"
            >
              {recentAddedKey === 'ALL' ? <Check size={12} /> : <Plus size={12} />}
              <span>
                {copy.addAllPrefix} {quickAddable.length} {copy.addAllSuffix}
              </span>
            </button>
          )}

          {cartItemCount > 0 && orderBridge?.onOpenCart && (
            <button
              type="button"
              onClick={handleOpenCart}
              className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50 px-2.5 py-1.5 text-[11px] font-bold text-emerald-700 transition-colors hover:bg-emerald-600 hover:text-white dark:border-emerald-500/40 dark:bg-emerald-950/50 dark:text-emerald-300 dark:hover:bg-emerald-600 dark:hover:text-white"
            >
              <ShoppingBag size={12} />
              <span>
                {copy.viewCart} ({cartItemCount})
              </span>
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default AiSuggestionList;
