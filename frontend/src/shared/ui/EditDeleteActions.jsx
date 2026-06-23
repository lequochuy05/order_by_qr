import { Loader2, Pencil, Trash2 } from 'lucide-react';

const EditDeleteActions = ({
  onEdit,
  onDelete,
  editing = false,
  deleting = false,
  editLabel = 'Sửa',
  className = '',
}) => (
  <div className={`mt-auto flex gap-2 border-t border-gray-100 pt-4 ${className}`}>
    <button
      type="button"
      onClick={onEdit}
      disabled={editing || deleting}
      className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-blue-50 py-2.5 text-sm font-medium text-blue-600 transition-all hover:bg-blue-600 hover:text-white disabled:cursor-wait disabled:opacity-60"
    >
      {editing ? <Loader2 size={16} className="animate-spin" /> : <Pencil size={16} />}
      {editLabel}
    </button>
    <button
      type="button"
      onClick={onDelete}
      disabled={editing || deleting}
      aria-label="Xóa"
      className="flex w-12 items-center justify-center rounded-xl bg-red-50 py-2.5 text-red-600 transition-all hover:bg-red-600 hover:text-white disabled:cursor-wait disabled:opacity-60"
    >
      {deleting ? <Loader2 size={18} className="animate-spin" /> : <Trash2 size={18} />}
    </button>
  </div>
);

export default EditDeleteActions;
