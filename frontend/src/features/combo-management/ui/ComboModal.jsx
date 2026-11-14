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
    if (Number(formData.price) !== Number(initialData.price)) return true;
    if (formData.active !== initialData.active) return true;
    if ((formData.available ?? true) !== (initialData.available ?? true)) return true;
    if (Number(formData.displayOrder || 0) !== Number(initialData.displayOrder || 0)) return true;

    if (formData.items.length !== initialData.items.length) return true;

    const sortedFormItems = [...formData.items].sort(
      (a, b) => Number(a.menuItemId) - Number(b.menuItemId),
    );
    const sortedInitialItems = [...initialData.items].sort(
      (a, b) => Number(a.menuItemId) - Number(b.menuItemId),
    );

    for (let i = 0; i < sortedFormItems.length; i++) {
      if (Number(sortedFormItems[i].menuItemId) !== Number(sortedInitialItems[i].menuItemId))
        return true;
      if (Number(sortedFormItems[i].quantity) !== Number(sortedInitialItems[i].quantity))
        return true;
    }

    return false;
  }, [formData, initialData]);

  const validateForm = () => {
    const newErrors = {};
    const name = formData.name?.trim() || '';
    const price = Number(formData.price);

    if (!name) {
      newErrors.name = 'Tên combo không được để trống';
    } else if (name.length < 2) {
      newErrors.name = 'Tên combo phải có ít nhất 2 ký tự';
    } else if (name.length > 100) {
      newErrors.name = 'Tên combo không được vượt quá 100 ký tự';
    }

    if (!Number.isFinite(price) || price < 0) newErrors.price = 'Giá không hợp lệ';
    if (Number(formData.displayOrder) < 0) {
      newErrors.displayOrder = 'Thứ tự hiển thị không được âm';
    }
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
      const menu = menuItems.find((m) => Number(m.id) === Number(item.menuItemId));
      return sum + (menu ? Number(menu.price) * Number(item.quantity || 1) : 0);
    }, 0);
    return Math.round(total * 0.9);
  };

  const handleToggleItem = (menuId) => {
    let newItems = [...formData.items];
    const index = newItems.findIndex((i) => Number(i.menuItemId) === Number(menuId));

    if (index > -1) newItems.splice(index, 1);
    else newItems.push({ menuItemId: menuId, quantity: 1 });

    setFormData({ ...formData, items: newItems, price: calculateAutoPrice(newItems) });
    if (errors.items) setErrors({ ...errors, items: null });
  };

  const handleUpdateQty = (menuId, qty) => {
    const quantity = Math.max(1, Number.parseInt(qty, 10) || 1);
    const newItems = formData.items.map((i) =>
      Number(i.menuItemId) === Number(menuId) ? { ...i, quantity } : i,
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
          maxLength={100}
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
            <span className="rounded-full bg-indigo-50 px-2.5 py-0.5 text-[10px] font-bold text-indigo-700 dark:bg-indigo-950/40 dark:text-indigo-300">
              Chọn các món lẻ
            </span>
          </div>

          <div
            className={`overflow-hidden rounded-[1.5rem] border-2 divide-y divide-slate-100 transition-all dark:divide-slate-800 ${
              errors.items
                ? 'border-rose-500 bg-rose-50/10 dark:bg-rose-500/10'
                : 'border-slate-100 bg-slate-50/30 dark:border-slate-800 dark:bg-slate-950/40'
            }`}
          >
            <div className="max-h-60 overflow-y-auto custom-scrollbar">
              {menuItems.map((m) => {
                const selected = formData.items.find((i) => Number(i.menuItemId) === Number(m.id));
                return (
                  <div
                    key={m.id}
                    className={`flex items-center justify-between p-3.5 transition-colors ${selected ? 'bg-indigo-50/60 dark:bg-indigo-950/30' : 'hover:bg-white dark:hover:bg-slate-900'}`}
                  >
                    <label className="flex items-center gap-3 cursor-pointer flex-1 group">
                      <div
                        className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-all ${
                          selected
                            ? 'bg-gradient-to-r from-indigo-600 to-violet-600 border-indigo-600 shadow-sm'
                            : 'border-slate-200 bg-white group-hover:border-indigo-400 dark:border-slate-700 dark:bg-slate-900'
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
                        <p className="text-[10px] font-medium font-mono text-slate-400 dark:text-slate-500">
                          {fmtVND(m.price)}
                        </p>
                      </div>
                    </label>
                    {selected && (
                      <div className="flex items-center gap-2 animate-in slide-in-from-right-2 duration-300">
                        <input
                          type="number"
                          min="1"
                          className="w-14 rounded-xl border border-indigo-200 bg-white px-2 py-2 text-center font-mono text-xs font-black text-indigo-600 shadow-sm outline-none dark:border-indigo-500/30 dark:bg-slate-900 dark:text-indigo-400"
                          value={selected.quantity}
                          onChange={(e) => handleUpdateQty(m.id, e.target.value)}
                        />
                        <span className="text-[10px] font-bold text-slate-400 dark:text-slate-500">
                          món
                        </span>
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
            inputClassName="font-mono font-black text-indigo-600 dark:text-indigo-400"
            value={formData.price}
            onChange={(value) => {
              setFormData({ ...formData, price: parseInt(value, 10) || 0 });
              if (errors.price) setErrors({ ...errors, price: null });
            }}
            error={errors.price}
          />
          <TextField
            label="Thứ tự hiển thị"
            type="number"
            min="0"
            value={formData.displayOrder ?? 0}
            onChange={(value) => {
              setFormData({ ...formData, displayOrder: value });
              if (errors.displayOrder) setErrors({ ...errors, displayOrder: null });
            }}
            error={errors.displayOrder}
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
