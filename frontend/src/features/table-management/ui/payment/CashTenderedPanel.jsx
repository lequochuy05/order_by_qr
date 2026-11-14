import React, { useMemo } from 'react';
import { Banknote, AlertCircle, CheckCircle2 } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const QUICK_DENOMINATIONS = [50000, 100000, 200000, 500000];

/**
 * Calculates smart rounding options for cash payments.
 * For example, if amount is 135,000:
 * - Next 10k: 140,000
 * - Next 50k: 150,000
 * - Next 100k: 200,000
 */
const getSmartSuggestions = (amount) => {
  if (!amount || amount <= 0) return [];
  const suggestions = new Set();

  // Round up to next 10,000
  const next10k = Math.ceil(amount / 10000) * 10000;
  if (next10k > amount) suggestions.add(next10k);

  // Round up to next 50,000
  const next50k = Math.ceil(amount / 50000) * 50000;
  if (next50k > amount && next50k !== next10k) suggestions.add(next50k);

  // Round up to next 100,000
  const next100k = Math.ceil(amount / 100000) * 100000;
  if (next100k > amount && !suggestions.has(next100k)) suggestions.add(next100k);

  return Array.from(suggestions).slice(0, 3);
};

const CashTenderedPanel = ({
  finalAmount = 0,
  cashReceived = '',
  onChangeCashReceived,
}) => {
  const numericCash = Number(cashReceived) || 0;
  const isInsufficient = numericCash > 0 && numericCash < finalAmount;
  const changeAmount = Math.max(0, numericCash - finalAmount);
  const smartSuggestions = useMemo(() => getSmartSuggestions(finalAmount), [finalAmount]);

  const handleSelectDenomination = (value) => {
    onChangeCashReceived(value);
  };

  const handleAddDenomination = (denom) => {
    onChangeCashReceived((prev) => (Number(prev) || 0) + denom);
  };

  return (
    <div className="space-y-4 rounded-2xl border border-slate-200/80 bg-slate-50/70 p-4 transition-colors dark:border-slate-800 dark:bg-slate-900/60">
      <div className="flex items-center justify-between">
        <label
          htmlFor="cash-tendered-input"
          className="flex items-center gap-2 text-sm font-bold text-slate-800 dark:text-slate-200"
        >
          <Banknote size={18} className="text-emerald-600 dark:text-emerald-400" />
          Tiền khách đưa
        </label>
        {numericCash > 0 && (
          <button
            type="button"
            onClick={() => onChangeCashReceived('')}
            className="text-xs font-semibold text-slate-500 hover:text-rose-600 dark:text-slate-400 dark:hover:text-rose-400"
          >
            Nhập lại
          </button>
        )}
      </div>

      {/* Main input for cash handed */}
      <div className="relative">
        <input
          id="cash-tendered-input"
          type="number"
          min="0"
          step="1000"
          value={cashReceived}
          onChange={(e) => onChangeCashReceived(e.target.value)}
          placeholder={`Nhập số tiền (Mặc định: ${fmtVND(finalAmount)})`}
          className={`w-full rounded-2xl border bg-white px-4 py-3 font-mono text-lg font-black tracking-wide text-slate-900 shadow-sm transition-all focus:outline-none focus:ring-2 dark:bg-slate-950 dark:text-white ${
            isInsufficient
              ? 'border-rose-400 focus:border-rose-500 focus:ring-rose-200 dark:border-rose-600 dark:focus:ring-rose-950'
              : 'border-slate-200/80 focus:border-emerald-500 focus:ring-emerald-200 dark:border-slate-700 dark:focus:ring-emerald-950'
          }`}
        />
        <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-sm font-black text-slate-400 dark:text-slate-500">
          VND
        </span>
      </div>

      {/* Quick Denomination Buttons */}
      <div className="space-y-2">
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => handleSelectDenomination(finalAmount)}
            className={`rounded-lg px-3 py-1.5 text-xs font-bold transition-all ${
              numericCash === finalAmount
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'bg-white text-slate-700 border border-slate-200 hover:bg-slate-100 dark:bg-slate-800 dark:text-slate-200 dark:border-slate-700 dark:hover:bg-slate-700'
            }`}
          >
            Đủ tiền ({fmtVND(finalAmount)})
          </button>

          {smartSuggestions.map((amount) => (
            <button
              key={amount}
              type="button"
              onClick={() => handleSelectDenomination(amount)}
              className={`rounded-lg px-3 py-1.5 text-xs font-bold transition-all ${
                numericCash === amount
                  ? 'bg-emerald-600 text-white shadow-sm'
                  : 'bg-white text-emerald-700 border border-emerald-200 hover:bg-emerald-50 dark:bg-slate-800 dark:text-emerald-400 dark:border-slate-700 dark:hover:bg-slate-700'
              }`}
            >
              {fmtVND(amount)}
            </button>
          ))}
        </div>

        {/* Standard VN Currency Quick Keys */}
        <div className="flex flex-wrap items-center gap-1.5 pt-1">
          <span className="text-[11px] font-medium text-slate-400 dark:text-slate-500">
            Thêm:
          </span>
          {QUICK_DENOMINATIONS.map((denom) => (
            <button
              key={denom}
              type="button"
              onClick={() => handleAddDenomination(denom)}
              className="rounded-md border border-slate-200 bg-white px-2 py-1 text-[11px] font-bold text-slate-600 transition-colors hover:border-emerald-300 hover:bg-emerald-50/50 hover:text-emerald-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
            >
              +{denom >= 1000 ? `${denom / 1000}k` : denom}
            </button>
          ))}
        </div>
      </div>

      {/* Change / Warning feedback */}
      {numericCash > 0 && (
        <div
          className={`flex items-center justify-between rounded-xl px-4 py-3 transition-colors ${
            isInsufficient
              ? 'border border-rose-200 bg-rose-50 text-rose-800 dark:border-rose-900/50 dark:bg-rose-950/40 dark:text-rose-300'
              : 'border border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900/50 dark:bg-emerald-950/40 dark:text-emerald-300'
          }`}
        >
          <div className="flex items-center gap-2">
            {isInsufficient ? (
              <AlertCircle size={18} className="shrink-0 text-rose-600 dark:text-rose-400" />
            ) : (
              <CheckCircle2 size={18} className="shrink-0 text-emerald-600 dark:text-emerald-400" />
            )}
            <span className="text-xs font-bold uppercase tracking-wider">
              {isInsufficient ? 'Khách đưa thiếu' : 'Tiền thừa trả khách'}
            </span>
          </div>
          <span
            className={`font-mono text-base font-black ${
              isInsufficient ? 'text-rose-600 dark:text-rose-400' : 'text-emerald-600 dark:text-emerald-400'
            }`}
          >
            {isInsufficient ? fmtVND(finalAmount - numericCash) : fmtVND(changeAmount)}
          </span>
        </div>
      )}
    </div>
  );
};

export default CashTenderedPanel;
