import React, { useState } from 'react';
import {
  CheckboxCard,
  FormError,
  FormLabel,
  ModalActions,
  ModalHeader,
  SharedModal,
  TextareaField,
  TextField,
} from '@shared/ui';
import { fmtVND } from '@shared/lib/formatters.js';

const ComboModal = ({
  isOpen,
  onClose,
  onSubmit,
  menuItems,
  initialData,
  errors = {},
  setErrors,
}) => {
  const [formData, setFormData] = useState(
    initialData || {
      name: '',
      description: '',
      price: 0,
      active: true,
      available: true,
      displayOrder: 0,
      items: [],
    },
  );

  const isChanged = React.useMemo(() => {
    if (!initialData) return true;

    if (formData.name !== initialData.name) return true;
    if ((formData.description || '') !== (initialData.description || '')) return true;
    if (formData.price !== initialData.price) return true;
    if (formData.active !== initialData.active) return true;
    if ((formData.available ?? true) !== (initialData.available ?? true)) return true;
    if (Number(formData.displayOrder || 0) !== Number(initialData.displayOrder || 0)) return true;

    if (formData.items.length !== initialData.items.length) return true;

    const sortedFormItems = [...formData.items].sort((a, b) => a.menuItemId - b.menuItemId);
    const sortedInitialItems = [...initialData.items].sort((a, b) => a.menuItemId - b.menuItemId);

    for (let i = 0; i < sortedFormItems.length; i++) {
      if (sortedFormItems[i].menuItemId !== sortedInitialItems[i].menuItemId) return true;
      if (sortedFormItems[i].quantity !== sortedInitialItems[i].quantity) return true;
    }

    return false;
  }, [formData, initialData]);

  const validateForm = () => {
    const newErrors = {};
    if (!formData.name?.trim()) newErrors.name = 'Tên combo không được để trống';
    if (formData.price < 0) newErrors.price = 'Giá không hợp lệ';
    if (formData.items.length < 2) newErrors.items = 'Vui lòng chọn ít nhất 2 món';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleFormSubmit = (e) => {
    e.preventDefault();
    if (validateForm()) {
      onSubmit(formData);
    }
  };

  const calculateAutoPrice = (selectedItems) => {
    const total = selectedItems.reduce((sum, item) => {
      const menu = menuItems.find((m) => m.id === item.menuItemId);
      return sum + (menu ? menu.price * item.quantity : 0);
    }, 0);
    return Math.round(total * 0.9);
  };

  const handleToggleItem = (menuId) => {
    let newItems = [...formData.items];
    const index = newItems.findIndex((i) => i.menuItemId === menuId);

    if (index > -1) newItems.splice(index, 1);
    else newItems.push({ menuItemId: menuId, quantity: 1 });

    setFormData({ ...formData, items: newItems, price: calculateAutoPrice(newItems) });
    if (errors.items) setErrors({ ...errors, items: null });
  };

  const handleUpdateQty = (menuId, qty) => {
    const newItems = formData.items.map((i) =>
      i.menuItemId === menuId ? { ...i, quantity: Math.max(1, qty) } : i,
    );
    setFormData({ ...formData, items: newItems, price: calculateAutoPrice(newItems) });
  };

  if (!isOpen) return null;

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      className="max-w-xl !p-0"
      ariaLabel={initialData?.id ? 'Chỉnh sửa combo' : 'Tạo combo'}
    >
      <ModalHeader
        title={initialData?.id ? 'Chỉnh Sửa Combo' : 'Tạo Combo Mới'}
        subtitle="Tiết kiệm hơn cho khách hàng"
        onClose={onClose}
      />

      <form
        id="comboForm"
        onSubmit={handleFormSubmit}
        className="custom-scrollbar space-y-6 overflow-y-auto p-6 sm:p-8"
      >
        <TextField
          label="Tên Combo"
          required
          placeholder="Ví dụ: Combo Gia Đình, Combo Cuối Tuần..."
          value={formData.name}
          onChange={(value) => {
            setFormData({ ...formData, name: value });
            if (errors.name) setErrors({ ...errors, name: null });
          }}
          error={errors.name}
        />

        <TextareaField
          label="Mô tả"
          rows={2}
          maxLength={500}
          placeholder="Mô tả ngắn gọn về combo..."
          value={formData.description || ''}
          onChange={(value) => setFormData({ ...formData, description: value })}
        />

        <div>
          <div className="flex justify-between items-center mb-2 px-1">
            <FormLabel required className="!mb-0">
              Thành phần Combo
            </FormLabel>
            <span className="rounded-lg bg-orange-50 px-2 py-0.5 text-[10px] font-bold text-orange-500 dark:bg-orange-500/10 dark:text-orange-300">
              Chọn các món lẻ
            </span>
          </div>

          <div
            className={`overflow-hidden rounded-[1.5rem] border-2 divide-y divide-slate-100 transition-all dark:divide-slate-800 ${
              errors.items
                ? 'border-red-500 bg-red-50/10 dark:bg-red-500/10'
                : 'border-slate-100 bg-slate-50/30 dark:border-slate-800 dark:bg-slate-950/40'
            }`}
          >
            <div className="max-h-60 overflow-y-auto custom-scrollbar">
              {menuItems.map((m) => {
                const selected = formData.items.find((i) => i.menuItemId === m.id);
                return (
                  <div
                    key={m.id}
                    className={`flex items-center justify-between p-3.5 transition-colors ${selected ? 'bg-orange-50/50 dark:bg-orange-500/10' : 'hover:bg-white dark:hover:bg-slate-900'}`}
                  >
                    <label className="flex items-center gap-3 cursor-pointer flex-1 group">
                      <div
                        className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-all ${
	                          selected
	                            ? 'bg-orange-500 border-orange-500 shadow-sm'
	                            : 'border-slate-200 bg-white group-hover:border-orange-300 dark:border-slate-700 dark:bg-slate-900'
	                        }`}
                      >
                        {selected && (
                          <div className="w-2 h-2 bg-white rounded-full animate-in zoom-in duration-200" />
                        )}
                      </div>
                      <input
                        type="checkbox"
                        checked={!!selected}
                        onChange={() => handleToggleItem(m.id)}
                        className="hidden"
                      />
                      <div>
                        <p
	                          className={`text-sm font-bold transition-colors ${selected ? 'text-slate-800 dark:text-slate-100' : 'text-slate-500 group-hover:text-slate-700 dark:text-slate-400 dark:group-hover:text-slate-200'}`}
                        >
                          {m.name}
                        </p>
	                        <p className="text-[10px] font-medium text-slate-400 dark:text-slate-500">{fmtVND(m.price)}</p>
                      </div>
                    </label>
                    {selected && (
                      <div className="flex items-center gap-2 animate-in slide-in-from-right-2 duration-300">
                        <input
                          type="number"
	                          className="w-14 rounded-xl border border-orange-200 bg-white px-2 py-2 text-center text-xs font-black text-orange-600 shadow-sm outline-none dark:border-orange-500/30 dark:bg-slate-900 dark:text-orange-300"
                          value={selected.quantity}
                          onChange={(e) => handleUpdateQty(m.id, parseInt(e.target.value))}
                        />
	                        <span className="text-[10px] font-bold text-slate-400 dark:text-slate-500">món</span>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
          <FormError message={errors.items} />
        </div>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <TextField
            label="Giá Combo (Khuyến mãi)"
            required
            type="number"
            min="0"
            suffix="VNĐ"
            inputClassName="text-orange-600"
            value={formData.price}
            onChange={(value) => setFormData({ ...formData, price: parseInt(value) || 0 })}
            error={errors.price}
          />
          <TextField
            label="Thứ tự hiển thị"
            type="number"
            min="0"
            value={formData.displayOrder ?? 0}
            onChange={(value) => setFormData({ ...formData, displayOrder: value })}
          />
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <CheckboxCard
            checked={formData.active}
            onChange={(checked) => setFormData({ ...formData, active: checked })}
            label="Đang kinh doanh"
          />
          <CheckboxCard
            checked={formData.available ?? true}
            onChange={(checked) => setFormData({ ...formData, available: checked })}
            label="Còn hàng"
            tone="green"
          />
        </div>
      </form>

      <ModalActions
        onClose={onClose}
        formId="comboForm"
        submitLabel={initialData?.id ? 'Lưu thay đổi' : 'Tạo Combo'}
        disabled={!isChanged}
      />
    </SharedModal>
  );
};

export default ComboModal;
