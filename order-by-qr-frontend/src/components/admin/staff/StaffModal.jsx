import React, { useState, useEffect } from 'react';
import { X, Upload, KeyRound, User, Mail, Phone, Shield, Power } from 'lucide-react';

const StaffModal = ({ isOpen, onClose, data, onSubmit }) => {
  const [formData, setFormData] = useState({
    id: null,
    fullName: '',
    email: '',
    phone: '',
    role: 'STAFF',
    status: 'ACTIVE',
    password: ''
  });

  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState('');

  // 1. Thêm State để lưu lỗi
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (isOpen) {
      // Reset lỗi mỗi khi mở modal
      setErrors({});

      if (data) {
        setFormData({
          id: data.id,
          fullName: data.fullName || '',
          email: data.email || '',
          phone: data.phone || '',
          role: data.role || 'STAFF',
          status: data.status || 'ACTIVE',
          password: ''
        });
        setPreview(data.avatarUrl || '');
      } else {
        setFormData({
          id: null,
          fullName: '',
          email: '',
          phone: '',
          role: 'STAFF',
          status: 'ACTIVE',
          password: ''
        });
        setPreview('');
      }
      setSelectedFile(null);
    }
  }, [isOpen, data]);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        alert("File ảnh quá lớn (Max 5MB)");
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
      newErrors.fullName = 'Họ tên không được để trống';
    } else if (formData.fullName.length < 2) {
      newErrors.fullName = 'Họ tên phải từ 2 ký tự trở lên';
    }

    // Validate Email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email.trim()) {
      newErrors.email = 'Email không được để trống';
    } else if (!emailRegex.test(formData.email)) {
      newErrors.email = 'Email không đúng định dạng';
    }

    // Validate Password (chỉ khi tạo mới)
    if (!formData.id) {
      if (!formData.password) {
        newErrors.password = 'Mật khẩu không được để trống';
      } else if (formData.password.length < 6) {
        newErrors.password = 'Mật khẩu phải có ít nhất 6 ký tự';
      }
    }

    // Validate Phone (nếu có nhập)
    if (formData.phone && !/^(84|0)[0-9]{9}$\b/.test(formData.phone)) {
      newErrors.phone = 'Số điện thoại không hợp lệ (VN)';
    }

    setErrors(newErrors);

    // Trả về true nếu không có lỗi nào
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
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl w-full max-w-2xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">

        <div className="px-8 py-5 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
          <h2 className="text-xl font-bold text-gray-800">
            {formData.id ? 'Cập nhật hồ sơ' : 'Thêm nhân viên mới'}
          </h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-200 rounded-full transition-colors text-gray-500">
            <X size={24} />
          </button>
        </div>

        <div className="p-8 overflow-y-auto custom-scrollbar">
          {/* 4. Thêm noValidate để tắt bong bóng lỗi mặc định của trình duyệt */}
          <form id="staffForm" onSubmit={handleSubmit} className="space-y-6" noValidate>

            <div className="flex justify-center mb-6">
              <div className="relative group">
                <div className="w-28 h-28 rounded-full border-4 border-white shadow-lg overflow-hidden bg-gray-100">
                  {preview ? (
                    <img src={preview} alt="Preview" className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-400">
                      <User size={48} />
                    </div>
                  )}
                </div>
                <label className="absolute bottom-0 right-0 bg-orange-500 text-white p-2 rounded-full cursor-pointer hover:bg-orange-600 shadow-md transition-transform hover:scale-110">
                  <Upload size={18} />
                  <input type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
                </label>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="col-span-2">
                <label className="block text-sm font-bold text-gray-700 mb-2">Họ và tên <span className="text-red-500">*</span></label>
                <div className="relative">
                  <User size={18} className="absolute left-3 top-3.5 text-gray-400" />
                  <input
                    type="text"
                    className={`w-full pl-10 pr-4 py-3 bg-gray-50 rounded-xl border outline-none transition-all
                      ${errors.fullName ? 'border-red-500 focus:ring-red-200' : 'border-gray-100 focus:ring-orange-500 focus:ring-2'}`}
                    placeholder="Nguyễn Văn A"
                    value={formData.fullName}
                    onChange={e => {
                      setFormData({ ...formData, fullName: e.target.value });
                      // Xóa lỗi khi người dùng bắt đầu nhập lại
                      if (errors.fullName) setErrors({ ...errors, fullName: '' });
                    }}
                  />
                </div>
                {/* 5. Hiển thị lỗi ngay bên dưới */}
                {errors.fullName && <p className="text-red-500 text-xs mt-1 ml-1">{errors.fullName}</p>}
              </div>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-2">Email <span className="text-red-500">*</span></label>
                <div className="relative">
                  <Mail size={18} className="absolute left-3 top-3.5 text-gray-400" />
                  <input
                    type="email"
                    className={`w-full pl-10 pr-4 py-3 bg-gray-50 rounded-xl border outline-none transition-all
                      ${errors.email ? 'border-red-500 focus:ring-red-200' : 'border-gray-100 focus:ring-orange-500 focus:ring-2'}`}
                    value={formData.email}
                    onChange={e => {
                      setFormData({ ...formData, email: e.target.value });
                      if (errors.email) setErrors({ ...errors, email: '' });
                    }}
                    disabled={!!formData.id}
                  />
                </div>
                {errors.email && <p className="text-red-500 text-xs mt-1 ml-1">{errors.email}</p>}
              </div>

              <div>
                <label className="block text-sm font-bold text-gray-700 mb-2">Số điện thoại</label>
                <div className="relative">
                  <Phone size={18} className="absolute left-3 top-3.5 text-gray-400" />
                  <input
                    type="tel"
                    className={`w-full pl-10 pr-4 py-3 bg-gray-50 rounded-xl border outline-none transition-all
                      ${errors.phone ? 'border-red-500 focus:ring-red-200' : 'border-gray-100 focus:ring-orange-500 focus:ring-2'}`}
                    value={formData.phone}
                    onChange={e => {
                      setFormData({ ...formData, phone: e.target.value });
                      if (errors.phone) setErrors({ ...errors, phone: '' });
                    }}
                  />
                </div>
                {errors.phone && <p className="text-red-500 text-xs mt-1 ml-1">{errors.phone}</p>}
              </div>

              {!formData.id && (
                <div className="col-span-2">
                  <label className="block text-sm font-bold text-gray-700 mb-2">Mật khẩu khởi tạo <span className="text-red-500">*</span></label>
                  <div className="relative">
                    <KeyRound size={18} className="absolute left-3 top-3.5 text-gray-400" />
                    <input
                      type="password"
                      className={`w-full pl-10 pr-4 py-3 bg-gray-50 rounded-xl border outline-none transition-all
                        ${errors.password ? 'border-red-500 focus:ring-red-200' : 'border-gray-100 focus:ring-orange-500 focus:ring-2'}`}
                      value={formData.password}
                      onChange={e => {
                        setFormData({ ...formData, password: e.target.value });
                        if (errors.password) setErrors({ ...errors, password: '' });
                      }}
                      placeholder="Tối thiểu 6 ký tự"
                    />
                  </div>
                  {errors.password && <p className="text-red-500 text-xs mt-1 ml-1">{errors.password}</p>}
                </div>
              )}

              {formData.id && (
                <>
                  <div>
                    <label className="block text-sm font-bold text-gray-700 mb-2">Vai trò</label>
                    <div className="relative">
                      <Shield size={18} className="absolute left-3 top-3.5 text-gray-400" />
                      <select
                        className="w-full pl-10 pr-4 py-3 bg-gray-50 rounded-xl border-gray-100 focus:ring-2 focus:ring-orange-500 outline-none cursor-pointer appearance-none"
                        value={formData.role}
                        onChange={e => setFormData({ ...formData, role: e.target.value })}
                      >
                        <option value="STAFF">Nhân viên</option>
                        <option value="MANAGER">Quản lý</option>
                        <option value="CHEF">Bếp</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-bold text-gray-700 mb-2">Trạng thái</label>
                    <div className="relative">
                      <Power size={18} className="absolute left-3 top-3.5 text-gray-400" />
                      <select
                        className="w-full pl-10 pr-4 py-3 bg-gray-50 rounded-xl border-gray-100 focus:ring-2 focus:ring-orange-500 outline-none cursor-pointer appearance-none"
                        value={formData.status}
                        onChange={e => setFormData({ ...formData, status: e.target.value })}
                      >
                        <option value="ACTIVE">Hoạt động</option>
                        <option value="INACTIVE">Ngừng hoạt động</option>
                        <option value="BANNED">Khóa</option>
                      </select>
                    </div>
                  </div>
                </>
              )}
            </div>
          </form>
        </div>

        <div className="px-8 py-5 border-t border-gray-100 bg-gray-50/50 flex justify-end gap-3">
          <button type="button" onClick={onClose} className="px-6 py-2.5 rounded-xl text-gray-600 font-medium hover:bg-gray-200 transition-colors">Hủy bỏ</button>
          <button form="staffForm" type="submit" className="px-6 py-2.5 rounded-xl bg-orange-500 text-white font-bold hover:bg-orange-600 shadow-lg shadow-orange-200 transition-transform active:scale-95">
            {formData.id ? 'Lưu thay đổi' : 'Tạo nhân viên'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default StaffModal;