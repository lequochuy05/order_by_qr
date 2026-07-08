import React from 'react';
import { AlertCircle, Loader2, Plus, Sparkles, Trash2 } from 'lucide-react';
import {
  CheckboxCard,
  ImageUploadField,
  ModalActions,
  ModalHeader,
  SelectField,
  SharedModal,
  TextareaField,
  TextField,
} from '@shared/ui';

const MenuItemModal = ({
  isOpen,
  onClose,
  onSubmit,
  categories,
  formData,
  setFormData,
  preview,
  handleFileChange,
  isSubmitting,
  initialFormData,
  selectedFile,
  errors = {},
  setErrors,
  isAiGenerating = false,
  onAiGenerate,
}) => {
  const isChanged = React.useMemo(() => {
    if (!initialFormData) return true;
    if (selectedFile) return true;

    if (formData.name !== initialFormData.name) return true;
    if ((formData.description || '') !== (initialFormData.description || '')) return true;
    if (Number(formData.price) !== Number(initialFormData.price)) return true;
    if (Number(formData.categoryId) !== Number(initialFormData.categoryId)) return true;
    if ((formData.active ?? true) !== (initialFormData.active ?? true)) return true;
    if ((formData.available ?? true) !== (initialFormData.available ?? true)) return true;
    if (Number(formData.displayOrder || 0) !== Number(initialFormData.displayOrder || 0))
      return true;

    // Deep compare itemOptions
    return JSON.stringify(formData.itemOptions) !== JSON.stringify(initialFormData.itemOptions);
  }, [formData, initialFormData, selectedFile]);

  if (!isOpen) return null;

  const addOption = () => {
    const newOption = { name: '', required: false, maxSelection: 1, optionValues: [] };
    setFormData({ ...formData, itemOptions: [...formData.itemOptions, newOption] });
  };

  const removeOption = (index) => {
    const newOptions = formData.itemOptions.filter((_, i) => i !== index);
    setFormData({ ...formData, itemOptions: newOptions });
  };

  const updateOption = (index, field, value) => {
    const newOptions = [...formData.itemOptions];
    newOptions[index] = { ...newOptions[index], [field]: value };
    setFormData({ ...formData, itemOptions: newOptions });

    const errorKey = `option_${index}_${field}`;
    if (errors[errorKey]) {
      const newErrors = { ...errors };
      delete newErrors[errorKey];
      setErrors(newErrors);
    }
  };

  // Validate Form
  const validateForm = () => {
    const newErrors = {};
    if (!formData.name?.trim()) {
      newErrors.name = 'Tên món ăn không được để trống';
    } else if (formData.name.length < 2) {
      newErrors.name = 'Tên món ăn phải có ít nhất 2 ký tự';
    }

    if (!String(formData.price || '').trim()) {
      newErrors.price = 'Giá món không được để trống';
    } else if (Number(formData.price) < 1000) {
      newErrors.price = 'Giá món tối thiểu là 1,000đ';
    }

    if (!formData.categoryId) {
      newErrors.categoryId = 'Vui lòng chọn danh mục';
    }

    // Validate Item Options
    if (formData.itemOptions && formData.itemOptions.length > 0) {
      formData.itemOptions.forEach((opt, optIdx) => {
        if (!opt.name?.trim()) {
          newErrors[`option_${optIdx}_name`] = 'Tên nhóm không được để trống';
        }
        if (opt.maxSelection < 1) {
          newErrors[`option_${optIdx}_max`] = 'Chọn tối đa phải ít nhất là 1';
        }

        if (opt.optionValues && opt.optionValues.length > 0) {
          opt.optionValues.forEach((val, valIdx) => {
            if (!val.name?.trim()) {
              newErrors[`option_${optIdx}_val_${valIdx}_name`] = 'Tên giá trị không được để trống';
            }
          });
        } else {
          newErrors[`option_${optIdx}_values`] = 'Nhóm này cần ít nhất 1 lựa chọn';
        }
      });
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleLocalSubmit = (e) => {
    e.preventDefault();
    if (validateForm()) {
      onSubmit(e);
    }
  };

  const addValue = (optIndex) => {
    const newValue = { name: '', extraPrice: 0 };
    const newOptions = [...formData.itemOptions];
    newOptions[optIndex].optionValues = [...newOptions[optIndex].optionValues, newValue];
    setFormData({ ...formData, itemOptions: newOptions });

    if (errors[`option_${optIndex}_values`]) {
      const newErrors = { ...errors };
      delete newErrors[`option_${optIndex}_values`];
      setErrors(newErrors);
    }
  };

  const removeValue = (optIndex, valIndex) => {
    const newOptions = [...formData.itemOptions];
    newOptions[optIndex].optionValues = newOptions[optIndex].optionValues.filter(
      (_, i) => i !== valIndex,
    );
    setFormData({ ...formData, itemOptions: newOptions });
  };

  const updateValue = (optIndex, valIndex, field, value) => {
    const newOptions = [...formData.itemOptions];
    newOptions[optIndex].optionValues[valIndex] = {
      ...newOptions[optIndex].optionValues[valIndex],
      [field]: value,
    };
    setFormData({ ...formData, itemOptions: newOptions });

    // Xóa lỗi tương ứng khi người dùng nhập liệu
    const errorKey = `option_${optIndex}_val_${valIndex}_${field}`;
    if (errors[errorKey]) {
      const newErrors = { ...errors };
      delete newErrors[errorKey];
      setErrors(newErrors);
    }
  };

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      className="max-w-2xl !p-0"
      ariaLabel={formData.id ? 'Sửa món ăn' : 'Thêm món ăn'}
    >
      <ModalHeader
        title={formData.id ? 'Sửa Món Ăn' : 'Thêm Món Mới'}
        subtitle="Thông tin thực đơn"
        onClose={onClose}
        disabled={isSubmitting}
      />

      <form
        id="menuForm"
        onSubmit={handleLocalSubmit}
        className="custom-scrollbar space-y-8 overflow-y-auto p-6 sm:p-8"
      >
        <div className="grid grid-cols-1 gap-8 md:grid-cols-2">
          <div className="space-y-5">
            <TextField
              label="Tên món ăn"
              required
              placeholder="Ví dụ: Trà sữa thái xanh"
              value={formData.name}
              onChange={(value) => {
                setFormData({ ...formData, name: value });
                if (errors.name) setErrors({ ...errors, name: null });
              }}
              error={errors.name}
            />

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <TextField
                label="Giá bán"
                required
                type="number"
                min="0"
                placeholder="0"
                suffix="đ"
                inputClassName="text-orange-600"
                value={formData.price}
                onChange={(value) => {
                  setFormData({ ...formData, price: value });
                  if (errors.price) setErrors({ ...errors, price: null });
                }}
                error={errors.price}
              />
              <SelectField
                label="Danh mục"
                required
                value={formData.categoryId}
                onChange={(value) => {
                  setFormData({ ...formData, categoryId: value });
                  if (errors.categoryId) setErrors({ ...errors, categoryId: null });
                }}
                error={errors.categoryId}
              >
                <option value="" disabled>
                  Chọn...
                </option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </SelectField>
            </div>
          </div>

          <ImageUploadField
            label="Hình ảnh đại diện"
            preview={preview}
            onChange={handleFileChange}
            inputId="menu-item-image"
            helperText="Click để tải ảnh"
            changeLabel="Đổi ảnh khác"
          />
        </div>

        <div className="space-y-5">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-slate-700 dark:text-slate-200">Mô tả món ăn</label>
            {onAiGenerate && formData.name && (
              <button
                type="button"
                onClick={onAiGenerate}
                disabled={isAiGenerating || !formData.name}
                className="flex items-center gap-1.5 rounded-lg border border-orange-200 bg-orange-50 px-3 py-1.5 text-[10px] font-bold text-orange-600 transition-all hover:bg-orange-100 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50 dark:border-orange-500/30 dark:bg-orange-500/10 dark:text-orange-300 dark:hover:bg-orange-500/20"
              >
                {isAiGenerating ? (
                  <Loader2 size={12} className="animate-spin" />
                ) : (
                  <Sparkles size={12} />
                )}
                {isAiGenerating ? 'Đang tạo...' : 'AI tạo mô tả'}
              </button>
            )}
          </div>
          <TextareaField
            rows={3}
            maxLength={500}
            placeholder="Mô tả ngắn gọn về món ăn..."
            value={formData.description || ''}
            onChange={(value) => setFormData({ ...formData, description: value })}
          />

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <TextField
              label="Thứ tự"
              type="number"
              min="0"
              value={formData.displayOrder ?? 0}
              onChange={(value) => setFormData({ ...formData, displayOrder: value })}
            />
            <div className="flex items-end">
              <CheckboxCard
                checked={formData.active ?? true}
                onChange={(checked) => setFormData({ ...formData, active: checked })}
                label="Kinh doanh"
              />
            </div>
            <div className="flex items-end">
              <CheckboxCard
                checked={formData.available ?? true}
                onChange={(checked) => setFormData({ ...formData, available: checked })}
                label="Còn hàng"
                tone="green"
              />
            </div>
          </div>
        </div>

        {/* Tùy chọn (Options) */}
        <div className="space-y-5">
          <div className="flex justify-between items-center px-1">
            <h3 className="flex items-center gap-2.5 text-xs font-black uppercase tracking-[0.2em] text-slate-800 dark:text-slate-100">
              <span className="w-1 h-4 bg-orange-500 rounded-full" />
              Tùy Chọn Mở Rộng
              <span className="rounded-lg bg-slate-100 px-2 py-0.5 text-[9px] font-black text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                {formData.itemOptions.length}
              </span>
            </h3>
            <button
              type="button"
              onClick={addOption}
              className="flex items-center gap-1.5 rounded-xl bg-slate-900 px-5 py-2.5 text-[10px] font-black uppercase tracking-tight text-white shadow-lg transition-all hover:bg-black active:scale-95 dark:bg-slate-100 dark:text-slate-950 dark:hover:bg-white"
            >
              <Plus size={14} /> Thêm Nhóm
            </button>
          </div>

          <div className="space-y-6">
            {formData.itemOptions.map((opt, optIdx) => (
              <div
                key={optIdx}
                className="group/option relative space-y-6 rounded-3xl border border-slate-100 bg-slate-50/50 p-6 transition-colors animate-in slide-in-from-bottom-2 duration-500 dark:border-slate-800 dark:bg-slate-950/50"
              >
                <button
                  type="button"
                  onClick={() => removeOption(optIdx)}
                  className="absolute -right-3 -top-3 z-10 rounded-full border border-red-50 bg-white p-2 text-red-500 opacity-0 shadow-lg transition-all hover:bg-red-500 hover:text-white group-hover/option:opacity-100 dark:border-red-500/20 dark:bg-slate-900 dark:text-red-300"
                >
                  <Trash2 size={16} />
                </button>

                <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
                  <div className="md:col-span-6">
                    <label className="mb-1.5 ml-1 block text-[9px] font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500">
                      Tên nhóm tùy chọn
                    </label>
                    <input
                      type="text"
                      placeholder="Ví dụ: Kích cỡ, Topping..."
                      className={`w-full rounded-2xl border-none bg-white px-4 py-3 text-sm font-bold text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:ring-2 focus:ring-orange-500/10 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-600 ${errors[`option_${optIdx}_name`] ? 'ring-2 ring-red-500' : ''}`}
                      value={opt.name}
                      onChange={(e) => updateOption(optIdx, 'name', e.target.value)}
                    />
                    {errors[`option_${optIdx}_name`] && (
                      <p className="text-red-500 text-[10px] mt-1.5 flex items-center gap-1 font-bold">
                        <AlertCircle size={10} /> {errors[`option_${optIdx}_name`]}
                      </p>
                    )}
                  </div>
                  <div className="md:col-span-3">
                    <label className="mb-1.5 ml-1 block text-[9px] font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500">
                      Bắt buộc?
                    </label>
                    <label className="flex h-[44px] cursor-pointer items-center justify-center rounded-2xl bg-white shadow-sm transition-colors hover:bg-slate-50 dark:bg-slate-900 dark:hover:bg-slate-800">
                      <input
                        type="checkbox"
                        className="h-4 w-4 cursor-pointer rounded border-slate-100 text-orange-500 focus:ring-orange-200 dark:border-slate-700"
                        checked={opt.required ?? false}
                        onChange={(e) => updateOption(optIdx, 'required', e.target.checked)}
                      />
                      <span className="ml-2 text-[11px] font-bold text-slate-600 dark:text-slate-300">
                        Yêu cầu chọn
                      </span>
                    </label>
                  </div>
                  <div className="md:col-span-3">
                    <label className="mb-1.5 ml-1 block text-[9px] font-bold uppercase tracking-widest text-slate-400 dark:text-slate-500">
                      Chọn tối đa
                    </label>
                    <input
                      type="number"
                      placeholder="Max"
                      className={`w-full rounded-2xl border-none bg-white px-4 py-3 text-center text-sm font-bold text-slate-900 shadow-sm outline-none transition focus:ring-2 focus:ring-orange-500/10 dark:bg-slate-900 dark:text-slate-100 ${errors[`option_${optIdx}_max`] ? 'ring-2 ring-red-500' : ''}`}
                      value={opt.maxSelection}
                      onChange={(e) =>
                        updateOption(optIdx, 'maxSelection', parseInt(e.target.value) || 1)
                      }
                    />
                    {errors[`option_${optIdx}_max`] && (
                      <p className="text-red-500 text-[9px] mt-1 text-center font-bold">
                        {errors[`option_${optIdx}_max`]}
                      </p>
                    )}
                  </div>
                </div>

                {/* Giá trị của tùy chọn */}
                <div className="space-y-4">
                  <div className="flex justify-between items-center px-1">
                    <p className="flex items-center gap-2 text-[9px] font-black uppercase tracking-widest text-slate-400 dark:text-slate-500">
                      <span className="h-0.5 w-3 bg-slate-200 dark:bg-slate-700" /> Các giá trị lựa chọn
                    </p>
                    {errors[`option_${optIdx}_values`] && (
                      <p className="text-red-500 text-[10px] font-bold flex items-center gap-1">
                        <AlertCircle size={10} /> {errors[`option_${optIdx}_values`]}
                      </p>
                    )}
                    <button
                      type="button"
                      onClick={() => addValue(optIdx)}
                      className="flex items-center gap-1 text-[10px] font-black uppercase text-orange-500 transition-colors hover:text-orange-600 dark:text-orange-300"
                    >
                      <Plus size={14} /> Thêm giá trị
                    </button>
                  </div>

                  <div className="grid grid-cols-1 gap-2.5">
                    {opt.optionValues.map((val, valIdx) => (
                      <div
                        key={valIdx}
                        className="grid grid-cols-12 gap-3 items-center animate-in fade-in slide-in-from-right-2 duration-300"
                      >
                        <div className="col-span-7">
                          <input
                            type="text"
                            placeholder="Tên (Size L, Thêm đường...)"
                            className={`w-full rounded-xl border bg-white px-4 py-3 text-xs font-medium text-slate-900 shadow-sm outline-none transition placeholder:text-slate-400 focus:border-orange-200 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-600 ${errors[`option_${optIdx}_val_${valIdx}_name`] ? 'border-red-500' : 'border-slate-100 dark:border-slate-700'}`}
                            value={val.name}
                            onChange={(e) => updateValue(optIdx, valIdx, 'name', e.target.value)}
                          />
                          {errors[`option_${optIdx}_val_${valIdx}_name`] && (
                            <p className="text-red-500 text-[9px] mt-1 flex items-center gap-1 font-bold">
                              <AlertCircle size={10} />{' '}
                              {errors[`option_${optIdx}_val_${valIdx}_name`]}
                            </p>
                          )}
                        </div>
                        <div className="col-span-4">
                          <div className="relative">
                            <input
                              type="number"
                              placeholder="+0"
                              className="w-full rounded-xl border border-slate-100 bg-white py-3 pl-3 pr-8 text-xs font-bold text-orange-600 shadow-sm outline-none transition focus:border-orange-200 dark:border-slate-700 dark:bg-slate-900 dark:text-orange-300"
                              value={val.extraPrice}
                              onChange={(e) =>
                                updateValue(
                                  optIdx,
                                  valIdx,
                                  'extraPrice',
                                  parseFloat(e.target.value) || 0,
                                )
                              }
                            />
                            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-[9px] font-bold text-slate-400 dark:text-slate-500">
                              đ
                            </span>
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => removeValue(optIdx, valIdx)}
                          className="col-span-1 flex items-center justify-center text-slate-300 transition-all hover:scale-110 hover:text-red-500 dark:text-slate-600 dark:hover:text-red-300"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    ))}
                    {opt.optionValues.length === 0 && (
                      <p className="rounded-2xl border border-dashed border-slate-200 bg-white/40 py-4 text-center text-[10px] italic text-slate-300 dark:border-slate-700 dark:bg-slate-900/40 dark:text-slate-500">
                        Chưa có giá trị nào. Nhấn "Thêm giá trị" để bắt đầu.
                      </p>
                    )}
                  </div>
                </div>
              </div>
            ))}
            {formData.itemOptions.length === 0 && (
              <div className="rounded-[2rem] border-2 border-dashed border-slate-100 bg-slate-50/50 py-12 text-center dark:border-slate-800 dark:bg-slate-950/50">
                <p className="text-xs font-bold uppercase tracking-tighter text-slate-400 opacity-60 dark:text-slate-500">
                  Món này chưa có tùy chọn đi kèm
                </p>
              </div>
            )}
          </div>
        </div>
      </form>

      <ModalActions
        onClose={onClose}
        formId="menuForm"
        submitLabel={formData.id ? 'Lưu thay đổi' : 'Tạo món ăn'}
        submittingLabel="Đang đồng bộ..."
        isSubmitting={isSubmitting}
        disabled={!isChanged}
      />
    </SharedModal>
  );
};

export default MenuItemModal;
