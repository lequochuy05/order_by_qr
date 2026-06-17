import { Send } from 'lucide-react'

const SubmitOrderButton = ({ onClick, disabled = false, loading = false, totalLabel = '' }) => (
  <button
    type="button"
    onClick={onClick}
    disabled={disabled || loading}
    className="flex w-full items-center justify-center gap-2 rounded-2xl bg-orange-500 px-5 py-4 text-sm font-black uppercase tracking-wider text-white shadow-lg shadow-orange-200 transition-all hover:bg-orange-600 disabled:cursor-not-allowed disabled:bg-slate-300 disabled:shadow-none"
  >
    <Send size={18} />
    <span>{loading ? 'Đang gửi...' : `Đặt món${totalLabel ? ` - ${totalLabel}` : ''}`}</span>
  </button>
)

export default SubmitOrderButton
