import React from 'react';
import { Power, X } from 'lucide-react';

const ConfirmToggleModal = ({ isOpen, onConfirm, onCancel, currentActive }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/60 z-[100] flex items-center justify-center p-4 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl p-8 max-w-sm w-full shadow-2xl animate-in zoom-in duration-300 relative">
        {/* Nút đóng nhanh */}
        <button 
          onClick={onCancel}
          className="absolute top-4 right-4 p-2 text-gray-400 hover:bg-gray-100 rounded-full transition-colors"
        >
          <X size={20} />
        </button>

        {/* Icon cảnh báo */}
        <div className={`mx-auto w-20 h-20 rounded-3xl flex items-center justify-center mb-6 rotate-12 group-hover:rotate-0 transition-transform ${
          currentActive ? 'bg-red-50 text-red-500' : 'bg-green-50 text-green-500'
        }`}>
          <Power size={40} strokeWidth={2.5} />
        </div>
        
        <h3 className="text-xl font-bold text-center text-gray-900 mb-3">
          {currentActive ? 'Xác nhận ngưng bán?' : 'Xác nhận mở bán?'}
        </h3>
        
        <p className="text-sm text-gray-500 text-center mb-8 leading-relaxed">
          {currentActive 
            ? 'Combo này sẽ ẩn khỏi thực đơn khách hàng ngay lập tức. Bạn có chắc chắn muốn thực hiện?' 
            : 'Combo này sẽ xuất hiện lại trên thực đơn để khách hàng có thể đặt món.'}
        </p>

        <div className="flex flex-col gap-3">
          <button 
            onClick={onConfirm}
            className={`w-full py-3.5 text-white rounded-2xl font-bold shadow-lg shadow-orange-100 transition-all active:scale-95 ${
              currentActive ? 'bg-red-500 hover:bg-red-600' : 'bg-green-600 hover:bg-green-700'
            }`}
          >
            Thay đổi
          </button>
          <button 
            onClick={onCancel}
            className="w-full py-3.5 bg-gray-50 text-gray-500 rounded-2xl font-bold hover:bg-gray-100 transition-all"
          >
            Để sau
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmToggleModal;