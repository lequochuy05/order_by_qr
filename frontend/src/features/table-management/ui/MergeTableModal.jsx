import { useMemo, useState } from 'react';
import { UsersRound } from 'lucide-react';

import { TABLE_STATUSES } from '@entities/table/lib/tableStatus.js';
import SharedModal from '@shared/ui/SharedModal.jsx';
import ModalHeader from '@shared/ui/modal/ModalHeader.jsx';
import ModalActions from '@shared/ui/modal/ModalActions.jsx';

const FORM_ID = 'merge-table-form';

const sortTables = (tables) =>
  [...tables].sort((a, b) =>
    String(a.tableNumber).localeCompare(String(b.tableNumber), undefined, { numeric: true }),
  );

const MergeTableModal = ({
  isOpen,
  onClose,
  tables = [],
  initialTable,
  onSubmit,
  isSubmitting,
}) => {
  const [selectedIds, setSelectedIds] = useState(() =>
    initialTable?.memberTableIds?.length
      ? initialTable.memberTableIds
      : initialTable?.id
        ? [initialTable.id]
        : [],
  );
  const [primaryTableId, setPrimaryTableId] = useState(() =>
    String(initialTable?.primaryTableId || initialTable?.id || ''),
  );
  const [guestCount, setGuestCount] = useState(() =>
    initialTable?.guestCount ? String(initialTable.guestCount) : '',
  );
  const [error, setError] = useState('');
  const MAX_MERGE = 6;

  const candidates = useMemo(
    () =>
      sortTables(
        tables.filter(
          (table) =>
            table.status !== TABLE_STATUSES.INACTIVE &&
            (!table.sessionId || table.sessionId === initialTable?.sessionId),
        ),
      ),
    [initialTable?.sessionId, tables],
  );

  const selectedTables = useMemo(
    () => candidates.filter((table) => selectedIds.includes(table.id)),
    [candidates, selectedIds],
  );
  const totalCapacity = selectedTables.reduce(
    (sum, table) => sum + (Number(table.capacity) || 0),
    0,
  );
  const displayName = selectedTables.length
    ? `Bàn ${selectedTables.map((table) => table.tableNumber).join(' + ')}`
    : 'Chưa chọn bàn';

  if (!isOpen) return null;

  const toggleTable = (tableId) => {
    const next = selectedIds.includes(tableId)
      ? selectedIds.filter((id) => id !== tableId)
      : [...selectedIds, tableId];

    setSelectedIds(next);
    if (String(tableId) === String(primaryTableId)) {
      setPrimaryTableId(next[0] ? String(next[0]) : '');
    } else if (!primaryTableId) {
      setPrimaryTableId(String(tableId));
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const guests = Number(guestCount);

    if (selectedIds.length < 2) {
      setError(`Vui lòng chọn tối thiểu 2 bàn để gộp.`);
      return;
    }
    if (selectedIds.length > MAX_MERGE) {
      setError(`Chỉ được chọn tối đa ${MAX_MERGE} bàn trong một lần gộp.`);
      return;
    }
    if (!primaryTableId || !selectedIds.includes(Number(primaryTableId))) {
      setError('Vui lòng chọn bàn chính trong nhóm đã chọn.');
      return;
    }
    if (!Number.isInteger(guests) || guests < 1) {
      setError('Số khách phải lớn hơn 0.');
      return;
    }
    if (guests > totalCapacity) {
      setError(`Số khách vượt quá tổng sức chứa ${totalCapacity} chỗ.`);
      return;
    }

    setError('');
    await onSubmit({
      primaryTableId: Number(primaryTableId),
      tableIds: selectedIds,
      guestCount: guests,
    });
  };

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      className="max-w-3xl !overflow-hidden !p-0"
      ariaLabel="Gộp bàn"
    >
      <ModalHeader title="Gộp bàn" subtitle="Một phiên, một order chung" onClose={onClose} />

      <form id={FORM_ID} onSubmit={handleSubmit} className="flex-1 space-y-5 overflow-y-auto p-6">
        <div className="rounded-2xl border border-indigo-200/60 bg-indigo-50/80 p-4 text-sm font-semibold text-indigo-700 dark:border-indigo-800/40 dark:bg-indigo-950/40 dark:text-indigo-300">
          <div className="flex items-center gap-2 text-base font-black">
            <UsersRound size={18} /> {displayName}
          </div>
          <p className="mt-1 text-xs uppercase tracking-wider">
            {guestCount || 0} khách / {totalCapacity} chỗ
          </p>
        </div>

        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {candidates.map((table) => {
            const checked = selectedIds.includes(table.id);
            return (
              <label
                key={table.id}
                className={`cursor-pointer rounded-xl border p-3 transition ${
                  checked
                    ? 'border-indigo-300 bg-indigo-50/80 text-indigo-700 dark:border-indigo-500/40 dark:bg-indigo-950/40 dark:text-indigo-300 shadow-sm'
                    : 'border-slate-200/80 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800'
                }`}
              >
                <div className="flex items-center gap-2">
                  <input
                    type="checkbox"
                    checked={checked}
                    disabled={!checked && selectedIds.length >= MAX_MERGE}
                    onChange={() => toggleTable(table.id)}
                    className="h-4 w-4 accent-indigo-600"
                  />
                  <span className="font-black">Bàn {table.tableNumber}</span>
                </div>
                <p className="mt-1 text-xs font-semibold text-slate-500 dark:text-slate-400">
                  {table.capacity} chỗ • {table.status}
                </p>
              </label>
            );
          })}
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <label className="space-y-2 text-sm font-bold text-slate-700 dark:text-slate-200">
            <span>Bàn chính</span>
            <select
              value={primaryTableId}
              onChange={(event) => setPrimaryTableId(event.target.value)}
              className="w-full rounded-2xl border border-slate-200/80 bg-white px-3.5 py-2.5 text-sm font-medium text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700/80 dark:bg-slate-950 dark:text-slate-100"
            >
              <option value="">Chọn bàn chính</option>
              {selectedTables.map((table) => (
                <option key={table.id} value={table.id}>
                  Bàn {table.tableNumber}
                </option>
              ))}
            </select>
          </label>

          <label className="space-y-2 text-sm font-bold text-slate-700 dark:text-slate-200">
            <span>Số khách</span>
            <input
              type="number"
              min="1"
              value={guestCount}
              onChange={(event) => setGuestCount(event.target.value)}
              className="w-full rounded-2xl border border-slate-200/80 bg-white px-3.5 py-2.5 font-mono text-sm font-bold text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 dark:border-slate-700/80 dark:bg-slate-950 dark:text-slate-100"
              placeholder="Ví dụ: 6"
            />
          </label>
        </div>

        {error && (
          <p className="rounded-xl border border-rose-100 bg-rose-50 px-3 py-2 text-sm font-semibold text-rose-600 dark:border-rose-500/30 dark:bg-rose-500/10 dark:text-rose-300">
            {error}
          </p>
        )}
      </form>

      <ModalActions
        formId={FORM_ID}
        onClose={onClose}
        submitLabel="Gộp bàn"
        submittingLabel="Đang gộp..."
        isSubmitting={isSubmitting}
      />
    </SharedModal>
  );
};

export default MergeTableModal;
