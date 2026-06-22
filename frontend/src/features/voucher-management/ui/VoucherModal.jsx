import React, { useState } from 'react';
import { Wand2 } from 'lucide-react';
import {
  CheckboxCard,
  FormError,
  ModalActions,
  ModalHeader,
  SharedModal,
  TextField,
} from '@shared/ui';

const VoucherModal = ({
  isOpen,
  onClose,
  onSubmit,
  initialData,
  isSubmitting,
  serverErrors = {},
  onClearServerError,
}) => {
  // State dữ liệu form
  const [formData, setFormData] = useState(() => {
    if (initialData) {
      return {
        ...initialData,
        validFrom: initialData.validFrom ? initialData.validFrom.split('T')[0] : '',
        validTo: initialData.validTo ? initialData.validTo.split('T')[0] : '',
        discountPercent: initialData.discountPercent || '',
        discountAmount: initialData.discountAmount || '',
        usageLimit: initialData.usageLimit || '',
      };
    }
    return {
      code: '',
      discountPercent: '',
      discountAmount: '',
      usageLimit: '',
      validFrom: '',
      validTo: '',
      active: true,
    };
  });

  // State lưu lỗi validation
  const [errors, setErrors] = useState({});
  const codeError = errors.code || serverErrors.code;

  const originalData = React.useMemo(() => {
    if (!initialData) return null;
    return {
      code: initialData.code || '',
      discountPercent: initialData.discountPercent || '',
      discountAmount: initialData.discountAmount || '',
      usageLimit: initialData.usageLimit || '',
      validFrom: initialData.validFrom ? initialData.validFrom.split('T')[0] : '',
      validTo: initialData.validTo ? initialData.validTo.split('T')[0] : '',
      active: initialData.active ?? true,
      type: initialData.discountPercent ? 'PERCENTAGE' : 'FIXED_AMOUNT',
    };
  }, [initialData]);

  const isChanged = React.useMemo(() => {
    if (!initialData) return true;
    const currentType = formData.discountPercent ? 'PERCENTAGE' : 'FIXED_AMOUNT';
    return (
      JSON.stringify(originalData) !==
      JSON.stringify({
        code: formData.code,
        discountPercent: formData.discountPercent,
        discountAmount: formData.discountAmount,
        usageLimit: formData.usageLimit,
        validFrom: formData.validFrom,
        validTo: formData.validTo,
        active: formData.active,
        type: currentType,
      })
    );
  }, [formData, originalData, initialData]);

  // Hàm tạo mã ngẫu nhiên
  const generateCode = () => {
    const randomStr = Math.random().toString(36).substring(2, 8).toUpperCase();
    setFormData((prev) => ({ ...prev, code: `SALE_${randomStr}`, codeError: null }));
    setErrors((prev) => ({ ...prev, code: null }));
    onClearServerError?.('code');
  };

  // Hàm Validate chi tiết
  const validateForm = () => {
    const newErrors = {};
    let isValid = true;

    // 1. Validate Mã
    if (!formData.code || !formData.code.trim()) {
      newErrors.code = 'Mã voucher không được để trống';
      isValid = false;
    } else if (/\s/.test(formData.code)) {
      newErrors.code = 'Mã voucher không được chứa khoảng trắng';
      isValid = false;
    }

    // 2. Validate Giảm giá
    const hasPercent = formData.discountPercent && parseFloat(formData.discountPercent) > 0;
    const hasAmount = formData.discountAmount && parseFloat(formData.discountAmount) > 0;

    if (!hasPercent && !hasAmount) {
      newErrors.discount = 'Phải nhập ít nhất một loại giảm giá';
      isValid = false;
    } else if (hasPercent && hasAmount) {
      newErrors.discount = 'Chỉ được chọn 1 loại giảm giá';
      isValid = false;
    }

    if (hasPercent) {
      const p = parseFloat(formData.discountPercent);
      if (p <= 0 || p > 100) {
        newErrors.discountPercent = '% giảm phải từ 1 đến 100';
        isValid = false;
      }
    }

    if (hasAmount) {
      const a = parseFloat(formData.discountAmount);
      if (a < 1000) {
        newErrors.discountAmount = 'Số tiền giảm tối thiểu là 1.000đ';
        isValid = false;
      }
    }

    // 3. Validate Ngày tháng
    if (!formData.validFrom) {
      newErrors.validFrom = 'Ngày bắt đầu không được để trống';
      isValid = false;
    }
    if (!formData.validTo) {
      newErrors.validTo = 'Ngày kết thúc không được để trống';
      isValid = false;
    }

    if (formData.validFrom && formData.validTo) {
      const start = new Date(formData.validFrom);
      const end = new Date(formData.validTo);
      if (start > end) {
        newErrors.validTo = 'Ngày kết thúc phải sau ngày bắt đầu';
        isValid = false;
      }
    }

    // 4. Validate Giới hạn (0 hoặc rỗng = không giới hạn → gửi null)
    const parsedLimit =
      formData.usageLimit === '' || formData.usageLimit === null
        ? null
        : parseInt(formData.usageLimit);
    if (parsedLimit !== null && parsedLimit < 0) {
      newErrors.usageLimit = 'Giới hạn lượt dùng không được âm';
      isValid = false;
    }

    setErrors(newErrors);
    return isValid;
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    // Chạy validate trước khi submit
    if (!validateForm()) return;

    // Chuẩn bị payload sạch
    const payload = {
      ...formData,
      code: formData.code.trim().toUpperCase(),
      validFrom: formData.validFrom ? `${formData.validFrom}T00:00:00` : null,
      validTo: formData.validTo ? `${formData.validTo}T23:59:59` : null,
      usageLimit:
        formData.usageLimit === '' ||
        formData.usageLimit === '0' ||
        parseInt(formData.usageLimit) === 0
          ? null
          : parseInt(formData.usageLimit),
      // Đảm bảo gửi null nếu field rỗng
      discountPercent: formData.discountPercent ? parseFloat(formData.discountPercent) : null,
      discountAmount: formData.discountAmount ? parseFloat(formData.discountAmount) : null,
      type: formData.discountPercent ? 'PERCENTAGE' : 'FIXED_AMOUNT',
    };

    onSubmit(payload);
  };

  if (!isOpen) return null;

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      className="max-w-lg !p-0"
      ariaLabel={initialData ? 'Sửa voucher' : 'Thêm voucher'}
    >
      <ModalHeader
        title={initialData ? 'Sửa Voucher' : 'Thêm Voucher Mới'}
        subtitle="Thiết lập ưu đãi"
        onClose={onClose}
        disabled={isSubmitting}
      />

      <form
        id="voucherForm"
        onSubmit={handleSubmit}
        className="custom-scrollbar space-y-6 overflow-y-auto p-6 sm:p-8"
      >
        <div>
          <div className="flex items-start gap-2">
            <TextField
              label="Mã Voucher"
              required
              placeholder="HELLO2026"
              className="min-w-0 flex-1"
              inputClassName="font-mono font-black uppercase"
              value={formData.code}
              onChange={(value) => {
                setFormData({ ...formData, code: value.toUpperCase().replace(/\s/g, '') });
                if (errors.code) setErrors({ ...errors, code: null });
                onClearServerError?.('code');
              }}
              error={codeError}
            />
            <button
              type="button"
              onClick={generateCode}
              className="mt-[26px] rounded-2xl bg-gray-100 p-4 text-gray-600 transition-colors hover:bg-gray-200"
              title="Tạo mã ngẫu nhiên"
              aria-label="Tạo mã voucher ngẫu nhiên"
            >
              <Wand2 size={20} />
            </button>
          </div>
        </div>

        <div className="rounded-3xl border border-orange-100 bg-orange-50/60 p-5">
          <p className="mb-4 text-[10px] font-black uppercase tracking-[0.2em] text-orange-600">
            Hình thức giảm giá
          </p>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <TextField
              label="Giảm theo %"
              type="number"
              placeholder="0-100"
              suffix="%"
              value={formData.discountPercent || ''}
              disabled={Boolean(formData.discountAmount)}
              onChange={(value) =>
                setFormData({ ...formData, discountPercent: value, discountAmount: '' })
              }
              error={errors.discountPercent}
            />
            <TextField
              label="Giảm số tiền"
              type="number"
              placeholder="0"
              suffix="VNĐ"
              value={formData.discountAmount || ''}
              disabled={Boolean(formData.discountPercent)}
              onChange={(value) =>
                setFormData({ ...formData, discountAmount: value, discountPercent: '' })
              }
              error={errors.discountAmount}
            />
          </div>
          <FormError message={errors.discount} />
        </div>

        <TextField
          label="Giới hạn lượt dùng"
          type="number"
          min="0"
          placeholder="Nhập 0 = Không giới hạn"
          value={formData.usageLimit}
          onChange={(value) => {
            setFormData({ ...formData, usageLimit: value });
            if (errors.usageLimit) setErrors({ ...errors, usageLimit: null });
          }}
          error={errors.usageLimit}
        />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField
            label="Từ ngày"
            required
            type="date"
            value={formData.validFrom}
            onChange={(value) => {
              setFormData({ ...formData, validFrom: value });
              if (errors.validFrom) setErrors({ ...errors, validFrom: null });
            }}
            error={errors.validFrom}
          />
          <TextField
            label="Đến ngày"
            required
            type="date"
            value={formData.validTo}
            onChange={(value) => {
              setFormData({ ...formData, validTo: value });
              if (errors.validTo) setErrors({ ...errors, validTo: null });
            }}
            error={errors.validTo}
          />
        </div>

        <CheckboxCard
          checked={formData.active}
          onChange={(checked) => setFormData({ ...formData, active: checked })}
          label="Kích hoạt voucher ngay"
          description="Voucher có thể được áp dụng ngay sau khi lưu"
        />
      </form>

      <ModalActions
        onClose={onClose}
        formId="voucherForm"
        submitLabel={initialData ? 'Cập nhật' : 'Tạo Voucher'}
        isSubmitting={isSubmitting}
        disabled={!isChanged}
      />
    </SharedModal>
  );
};

export default VoucherModal;
