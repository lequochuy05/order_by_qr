import { CheckCircle2, Loader2, Lock, Plus, Trash2 } from 'lucide-react';
import { fmtVND } from '@shared/lib/formatters.js';

const MATCH_BADGE_MAP = {
  MATCHED: {
    vi: 'Đã khớp',
    en: 'Matched',
    cls: 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/30 dark:bg-emerald-500/15 dark:text-emerald-300',
  },
  UNIT_MISMATCH: {
    vi: 'Khác đơn vị',
    en: 'Unit differs',
    cls: 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-500/30 dark:bg-amber-500/15 dark:text-amber-300',
  },
  UNMATCHED: {
    vi: 'Chưa khớp',
    en: 'Unmatched',
    cls: 'border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-500/30 dark:bg-rose-500/15 dark:text-rose-300',
  },
};

const InvoiceOcrReviewList = ({
  isEn,
  isManager,
  busy,
  rows,
  inventoryItems,
  inventoryItemsLoading,
  selectedCount,
  estimatedTotal,
  onAddRow,
  onRemoveRow,
  onUpdateRow,
  onSelectCatalogItem,
  onToggleAllUnitCosts,
}) => (
  <>
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h3 className="text-sm font-black text-slate-900 dark:text-white">
            {isEn
              ? '2. Review Matched Inventory Lines'
              : '2. Đối chiếu dòng nguyên liệu trước khi nhập'}
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {isEn
              ? 'Nothing is added to stock until you click Confirm Stock-In.'
              : 'Hệ thống chỉ cộng tồn kho sau khi bạn kiểm tra và bấm Xác nhận nhập kho.'}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {isManager && rows.some((row) => row.unitCost !== '') && (
            <label className="inline-flex cursor-pointer items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200">
              <input
                type="checkbox"
                checked={
                  rows.filter((row) => row.unitCost !== '').length > 0 &&
                  rows.filter((row) => row.unitCost !== '').every((row) => row.updateUnitCost)
                }
                onChange={(e) => onToggleAllUnitCosts(e.target.checked)}
                className="h-3.5 w-3.5 rounded accent-indigo-600"
              />
              <span>{isEn ? 'Update all unit costs' : 'Cập nhật tất cả giá vốn'}</span>
            </label>
          )}

          <button
            type="button"
            onClick={onAddRow}
            disabled={busy}
            className="inline-flex h-9 items-center gap-1.5 rounded-xl border border-indigo-200 bg-indigo-50 px-3 text-xs font-bold text-indigo-700 transition hover:bg-indigo-600 hover:text-white disabled:cursor-not-allowed disabled:opacity-50 dark:border-indigo-700/60 dark:bg-indigo-950/40 dark:text-indigo-300"
          >
            <Plus size={14} />
            {isEn ? 'Add Line' : 'Thêm dòng'}
          </button>
        </div>
      </div>

      {!isManager && (
        <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-medium text-slate-600 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">
          <Lock size={14} className="shrink-0 text-slate-400" />
          <span>
            {isEn
              ? 'Chef role can batch stock-in quantities. Only Managers can update ingredient unit costs.'
              : 'Vai trò Bếp được xác nhận nhập số lượng vào kho. Chỉ Quản lý mới được phép cập nhật giá vốn nguyên liệu.'}
          </span>
        </div>
      )}

      {inventoryItemsLoading && (
        <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
          <Loader2 size={14} className="animate-spin" />
          <span>{isEn ? 'Loading inventory catalog...' : 'Đang tải danh mục nguyên liệu...'}</span>
        </div>
      )}

      {rows.length === 0 ? (
        <div className="rounded-2xl border-2 border-dashed border-slate-200 py-8 text-center text-xs font-medium text-slate-500 dark:border-slate-800 dark:text-slate-400">
          {isEn
            ? 'Scan an invoice image above or click "Add Line" to enter stock-in items manually.'
            : 'Chụp/tải ảnh hóa đơn ở trên để AI trích xuất, hoặc bấm "Thêm dòng" để nhập thủ công.'}
        </div>
      ) : (
        <div className="space-y-3">
          {rows.map((row) => {
            const badge = MATCH_BADGE_MAP[row.matchStatus] || MATCH_BADGE_MAP.UNMATCHED;
            return (
              <div
                key={row._key}
                className={`rounded-2xl border p-3.5 transition ${
                  row.selected
                    ? 'border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900'
                    : 'border-slate-200/60 bg-slate-50/70 opacity-70 dark:border-slate-800 dark:bg-slate-950/60'
                }`}
              >
                <div className="mb-2.5 flex flex-wrap items-center justify-between gap-2">
                  <div className="flex min-w-0 items-center gap-2.5">
                    <input
                      type="checkbox"
                      aria-label={isEn ? 'Select line' : 'Chọn dòng nhập'}
                      checked={Boolean(row.selected)}
                      onChange={(e) => onUpdateRow(row._key, { selected: e.target.checked })}
                      className="h-4 w-4 rounded accent-indigo-600"
                    />
                    <div className="min-w-0">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="truncate text-sm font-bold text-slate-900 dark:text-white">
                          {row.rawName || (isEn ? 'Manual item' : 'Dòng nhập thủ công')}
                        </span>
                        <span
                          className={`rounded-full border px-2 py-0.5 text-[10px] font-bold ${badge.cls}`}
                        >
                          {isEn ? badge.en : badge.vi}
                        </span>
                      </div>
                      {(row.invoiceQuantity || row.invoiceUnit || row.invoiceUnitPrice) && (
                        <p className="text-[11px] text-slate-500 dark:text-slate-400">
                          {isEn ? 'Receipt:' : 'Hóa đơn:'} {row.invoiceQuantity || '?'}{' '}
                          {row.invoiceUnit}
                          {row.invoiceUnitPrice !== '' && Number(row.invoiceUnitPrice) > 0
                            ? ` · ${fmtVND(row.invoiceUnitPrice)}`
                            : ''}
                        </p>
                      )}
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => onRemoveRow(row._key)}
                    title={isEn ? 'Remove line' : 'Xóa dòng'}
                    aria-label={isEn ? 'Remove line' : 'Xóa dòng'}
                    className="inline-flex h-8 w-8 items-center justify-center rounded-xl border border-rose-200/80 bg-rose-50 text-rose-600 transition hover:bg-rose-600 hover:text-white dark:border-rose-800/40 dark:bg-rose-950/40 dark:text-rose-400"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>

                {row.conversionNote && (
                  <p className="mb-2.5 rounded-xl bg-amber-50 px-3 py-1.5 text-[11px] font-medium text-amber-800 dark:bg-amber-950/35 dark:text-amber-300">
                    {row.conversionNote}
                  </p>
                )}

                <div
                  className={`grid grid-cols-1 gap-3 ${
                    isManager ? 'md:grid-cols-12' : 'md:grid-cols-8'
                  }`}
                >
                  <div className="md:col-span-4">
                    <label className="block">
                      <span className="mb-1 block text-[11px] font-bold text-slate-600 dark:text-slate-300">
                        {isEn ? 'Inventory Item' : 'Nguyên liệu kho'}
                      </span>
                      <select
                        value={row.inventoryItemId}
                        onChange={(e) => onSelectCatalogItem(row._key, e.target.value)}
                        className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-800 outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                      >
                        <option value="">
                          {isEn ? '-- Select ingredient --' : '-- Chọn nguyên liệu --'}
                        </option>
                        {inventoryItems.map((item) => (
                          <option key={item.id} value={String(item.id)}>
                            {item.name} ({item.unit})
                          </option>
                        ))}
                      </select>
                    </label>
                  </div>

                  <div className="md:col-span-2">
                    <label className="block">
                      <span className="mb-1 block text-[11px] font-bold text-slate-600 dark:text-slate-300">
                        {isEn ? 'Stock Qty' : 'SL nhập'} {row.unit ? `(${row.unit})` : ''}
                      </span>
                      <input
                        type="number"
                        min="0.001"
                        step="0.001"
                        value={row.quantity}
                        onChange={(e) => onUpdateRow(row._key, { quantity: e.target.value })}
                        className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-900 outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white"
                      />
                    </label>
                  </div>

                  {isManager && (
                    <div className="md:col-span-4">
                      <label className="block">
                        <span className="mb-1 flex items-center justify-between text-[11px] font-bold text-slate-600 dark:text-slate-300">
                          <span>
                            {isEn ? 'Unit Cost' : 'Đơn giá'} ({row.unit || 'đv'})
                          </span>
                          <span className="font-normal text-slate-400">
                            {isEn ? 'Cur:' : 'Hiện tại:'} {fmtVND(row.currentUnitCost || 0)}
                          </span>
                        </span>
                        <input
                          type="number"
                          min="0"
                          step="100"
                          value={row.unitCost}
                          onChange={(e) => onUpdateRow(row._key, { unitCost: e.target.value })}
                          placeholder="0"
                          className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-xs font-semibold text-slate-900 outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white"
                        />
                      </label>
                      <label className="mt-1.5 inline-flex cursor-pointer items-center gap-1.5 text-[11px] font-semibold text-slate-600 dark:text-slate-300">
                        <input
                          type="checkbox"
                          checked={Boolean(row.updateUnitCost)}
                          disabled={row.unitCost === ''}
                          onChange={(e) =>
                            onUpdateRow(row._key, { updateUnitCost: e.target.checked })
                          }
                          className="h-3.5 w-3.5 rounded accent-indigo-600"
                        />
                        <span>{isEn ? 'Update item unit cost' : 'Cập nhật giá vốn'}</span>
                      </label>
                    </div>
                  )}

                  <div className="md:col-span-2">
                    <label className="block">
                      <span className="mb-1 block text-[11px] font-bold text-slate-600 dark:text-slate-300">
                        {isEn ? 'Line Note' : 'Ghi chú dòng'}
                      </span>
                      <input
                        type="text"
                        maxLength={300}
                        value={row.note}
                        onChange={(e) => onUpdateRow(row._key, { note: e.target.value })}
                        placeholder="..."
                        className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-xs text-slate-800 outline-none focus:border-indigo-500 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100"
                      />
                    </label>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>

    {rows.length > 0 && (
      <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-emerald-200/80 bg-emerald-50/70 px-4 py-3 text-xs font-semibold text-emerald-900 dark:border-emerald-500/25 dark:bg-emerald-950/30 dark:text-emerald-200">
        <span className="inline-flex items-center gap-1.5">
          <CheckCircle2 size={15} className="text-emerald-600 dark:text-emerald-400" />
          {isEn
            ? `${selectedCount} item(s) selected for batch stock-in`
            : `Đã chọn ${selectedCount} dòng nguyên liệu để nhập kho`}
        </span>
        {estimatedTotal > 0 && (
          <span>
            {isEn ? 'Estimated value:' : 'Giá trị ước tính:'} {fmtVND(estimatedTotal)}
          </span>
        )}
      </div>
    )}
  </>
);

export default InvoiceOcrReviewList;
