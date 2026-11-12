import { ShoppingBasket } from 'lucide-react';

const AddToCartButton = ({ onClick, disabled = false, label = 'Thêm vào giỏ' }) => (
  <button
    type="button"
    onClick={onClick}
    disabled={disabled}
    className="inline-flex items-center gap-2 rounded-xl bg-[#56372B] hover:bg-[#432A21] dark:bg-[#C17C4A] dark:hover:bg-[#A86537] px-4 py-2 text-sm font-bold text-white shadow-sm transition-colors disabled:cursor-not-allowed disabled:bg-slate-300 dark:disabled:bg-slate-800"
  >
    <ShoppingBasket size={18} />
    <span>{label}</span>
  </button>
);

export default AddToCartButton;
