import { Loader2, Pencil, Trash2 } from 'lucide-react';

const EditDeleteActions = ({
  onEdit,
  onDelete,
  editing = false,
  deleting = false,
  deleteDisabled = false,
  deleteTitle = '',
  editLabel = 'Sửa',
  className = '',
}) => (
  <div className={`mt-auto flex gap-2 border-t border-slate-100/80 pt-3.5 dark:border-slate-800/80 ${className}`}>
    <button
      type="button"
      onClick={onEdit}
      disabled={editing || deleting}
      className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-indigo-50 py-2.5 text-xs font-bold text-indigo-600 transition-all hover:bg-indigo-600 hover:text-white active:scale-95 disabled:cursor-wait disabled:opacity-60 dark:bg-indigo-950/40 dark:text-indigo-400 dark:hover:bg-indigo-600 dark:hover:text-white"
    >
      {editing ? <Loader2 size={15} className="animate-spin" /> : <Pencil size={15} />}
      {editLabel}
    </button>
    <button
      type="button"
      onClick={onDelete}
      disabled={editing || deleting || deleteDisabled}
      title={deleteTitle || undefined}
      aria-label={deleteTitle || 'Xóa'}
      className={`flex w-11 items-center justify-center rounded-xl py-2.5 transition-all ${
        deleteDisabled
          ? 'cursor-not-allowed bg-slate-100 text-slate-400 opacity-70 dark:bg-slate-800/70 dark:text-slate-500'
          : 'bg-rose-50 text-rose-600 hover:bg-rose-600 hover:text-white active:scale-95 disabled:cursor-wait disabled:opacity-60 dark:bg-rose-950/40 dark:text-rose-400 dark:hover:bg-rose-600 dark:hover:text-white'
      }`}
    >
      {deleting ? <Loader2 size={16} className="animate-spin" /> : <Trash2 size={16} />}
    </button>
  </div>
);

export default EditDeleteActions;
