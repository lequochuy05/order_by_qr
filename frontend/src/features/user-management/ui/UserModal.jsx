import React, { useState } from 'react';
import { KeyRound, Mail, Phone, Power, Shield, User } from 'lucide-react';
import { USER_STATUS } from '@shared/lib/formatters.js';
import { showErrorToast } from '@shared/lib/toast.js';
import {
  ImageUploadField,
  ModalActions,
  ModalHeader,
  SelectField,
  SharedModal,
  TextField,
} from '@shared/ui';

const USER_STATUS_OPTIONS = Object.entries(USER_STATUS).map(([value, meta]) => ({
  value,
  label: meta.label,
}));

const UserModal = ({ isOpen, onClose, data, onSubmit, errors = {}, setErrors }) => {
  const [formData, setFormData] = useState(() => {
    if (data) {
      return {
        id: data.id,
        fullName: data.fullName || '',
        email: data.email || '',
        phone: data.phone || '',
        role: data.role || 'STAFF',
        status: data.status || 'ACTIVE',
        password: '',
      };
    }
    return {
      id: null,
      fullName: '',
      email: '',
      phone: '',
      role: 'STAFF',
      status: 'ACTIVE',
      password: '',
    };
  });

  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState(data?.avatarUrl || '');

  // Sử dụng errors và setErrors từ props truyền xuống

  // === Senior UX: Dirty Checking ===
  const isChanged = React.useMemo(() => {
    if (!formData.id) return true; // Luôn cho phép khi tạo mới

    // Nếu chọn ảnh mới
    if (selectedFile) return true;

    // So sánh dữ liệu gốc
    if (formData.fullName !== (data.fullName || '')) return true;
    if (formData.email !== (data.email || '')) return true;
    if (formData.phone !== (data.phone || '')) return true;
    if (formData.role !== (data.role || 'STAFF')) return true;
    if (formData.status !== (data.status || 'ACTIVE')) return true;
    if (formData.password !== '') return true;

    return false;
  }, [formData, selectedFile, data]);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        showErrorToast('File ảnh quá lớn (Max 5MB)');
        return;
      }
      setSelectedFile(file);
      setPreview(URL.createObjectURL(file));
    }
  };

  // 2. Hàm Validate Form
  const validateForm = () => {
    const newErrors = {};

    // Validate Họ tên
    if (!formData.fullName.trim()) {
      newErrors.fullName = 'Họ và tên không được để trống';
    } else if (formData.fullName.length < 2) {
      newErrors.fullName = 'Họ và tên phải có ít nhất 2 ký tự';
    }

    // Validate Email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email.trim()) {
      newErrors.email = 'Email không được để trống';
    } else if (!emailRegex.test(formData.email)) {
      newErrors.email = 'Email không hợp lệ (vd: example@gmail.com)';
    }

    // Validate Password
    if (!formData.id) {
      if (!formData.password) {
        newErrors.password = 'Mật khẩu không được để trống';
      } else if (formData.password.length < 6) {
        newErrors.password = 'Mật khẩu phải có ít nhất 6 ký tự';
      }
    }

    // Validate Phone
    if (formData.phone && !/^(84|0)[0-9]{9}$\b/.test(formData.phone)) {
      newErrors.phone = 'Số điện thoại không hợp lệ';
    }

    setErrors(newErrors);

    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    // 3. Gọi hàm validate trước khi submit
    if (validateForm()) {
      onSubmit(formData, selectedFile);
    }
  };

  if (!isOpen) return null;

  return (
    <SharedModal
      isOpen={isOpen}
      onClose={onClose}
      className="max-w-2xl !p-0"
      ariaLabel={formData.id ? 'Cập nhật hồ sơ nhân viên' : 'Thêm nhân viên'}
    >
      <ModalHeader
        title={formData.id ? 'Cập nhật hồ sơ' : 'Thêm nhân viên mới'}
        subtitle="Thông tin tài khoản"
        onClose={onClose}
      />

      <form
        id="staffForm"
        onSubmit={handleSubmit}
        className="custom-scrollbar space-y-6 overflow-y-auto p-6 sm:p-8"
        noValidate
      >
        <ImageUploadField
          variant="avatar"
          preview={preview}
          onChange={handleFileChange}
          inputId="staff-avatar"
          changeLabel="Thay ảnh đại diện"
        />

        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <TextField
            label="Họ và tên"
            required
            icon={User}
            className="md:col-span-2"
            placeholder="Nguyễn Văn A"
            value={formData.fullName}
            onChange={(value) => {
              setFormData({ ...formData, fullName: value });
              if (errors.fullName) setErrors({ ...errors, fullName: '' });
            }}
            error={errors.fullName}
          />

          <TextField
            label="Email"
            required
            type="email"
            icon={Mail}
            placeholder="example@gmail.com"
            value={formData.email}
            onChange={(value) => {
              setFormData({ ...formData, email: value });
              if (errors.email) setErrors({ ...errors, email: '' });
            }}
            error={errors.email}
            disabled={Boolean(formData.id)}
          />

          <TextField
            label="Số điện thoại"
            type="tel"
            icon={Phone}
            placeholder="070616xxxx"
            value={formData.phone}
            onChange={(value) => {
              setFormData({ ...formData, phone: value });
              if (errors.phone) setErrors({ ...errors, phone: '' });
            }}
            error={errors.phone}
          />

          {!formData.id && (
            <TextField
              label="Mật khẩu khởi tạo"
              required
              type="password"
              icon={KeyRound}
              className="md:col-span-2"
              value={formData.password}
              onChange={(value) => {
                setFormData({ ...formData, password: value });
                if (errors.password) setErrors({ ...errors, password: '' });
              }}
              placeholder="Tối thiểu 6 ký tự"
              error={errors.password}
            />
          )}

          {formData.id && (
            <>
              <SelectField
                label="Vai trò"
                icon={Shield}
                value={formData.role}
                onChange={(value) => setFormData({ ...formData, role: value })}
                options={[
                  { value: 'STAFF', label: 'Nhân viên' },
                  { value: 'MANAGER', label: 'Quản lý' },
                  { value: 'CHEF', label: 'Bếp' },
                ]}
              />
              <SelectField
                label="Trạng thái"
                icon={Power}
                value={formData.status}
                onChange={(value) => setFormData({ ...formData, status: value })}
                options={USER_STATUS_OPTIONS}
              />
            </>
          )}
        </div>
      </form>

      <ModalActions
        onClose={onClose}
        formId="staffForm"
        submitLabel={formData.id ? 'Lưu thay đổi' : 'Tạo nhân viên'}
        disabled={!isChanged}
      />
    </SharedModal>
  );
};

export default UserModal;
