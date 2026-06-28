import { ShoppingBasket } from 'lucide-react';

const AddToCartButton = ({ onClick, disabled = false, label = 'Thêm vào giỏ' }) => (
  <button
    type="button"
    onClick={onClick}
    disabled={disabled}
    className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2 text-sm font-bold text-white shadow-sm transition-colors hover:bg-orange-600 disabled:cursor-not-allowed disabled:bg-slate-300"
  >
    <ShoppingBasket size={18} />
    <span>{label}</span>
  </button>
);

export default AddToCartButton;
