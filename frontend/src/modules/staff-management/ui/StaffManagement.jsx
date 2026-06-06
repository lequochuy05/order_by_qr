import React, { useState, useEffect, useCallback } from 'react';
import { Loader2, Users } from 'lucide-react';

import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useAuth } from '@modules/auth/model/AuthContext.jsx';

import { staffService } from '@modules/staff-management/api/staffService.js';

import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import StaffCard from './StaffCard';
import StaffModal from './StaffModal';
import StatusModal from '@shared/ui/StatusModal.jsx';
import ConfirmModal from '@shared/ui/ConfirmModal.jsx';
import { playNotificationSound } from '@modules/notifications/lib/notificationSound.js';

const StaffManager = () => {
  const [staffs, setStaffs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState(null);
  const [errors, setErrors] = useState({});

  // Hook Status Modal
  const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();
  const { confirmModal, confirm, closeConfirm } = useConfirmModal();
  const { user, updateUser } = useAuth();

  // Tải dữ liệu 
  const fetchStaffs = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true);
    try {
      const data = await staffService.getAll();
      setStaffs(data);
    } catch (err) {
      console.error("Lỗi tải nhân viên:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  // WebSocket Realtime 
  useWebSocket('/topic/users', (message) => {
    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      playNotificationSound();
      fetchStaffs();
    }
  });

  useEffect(() => { fetchStaffs(true); }, [fetchStaffs]);

  // Submit
  const handleSubmit = async (formData, selectedFile) => {
    try {
      // CHUẨN HÓA DỮ LIỆU
      const payload = { ...formData };

      if (!payload.phone || payload.phone.trim() === '') {
        payload.phone = null;
      }

      if (!payload.id) {
        delete payload.id;
      }

      if (!payload.password) {
        payload.password = null;
      }

      payload.email = payload.email?.trim();
      payload.fullName = payload.fullName?.trim();

      let result;
      let actionType = '';

      if (formData.id) {
        result = await staffService.update(formData.id, payload);
        actionType = 'Cập nhật';
      } else {
        result = await staffService.create(payload);
        actionType = 'Thêm';
      }

      let avatarResult = null;
      if (selectedFile) {
        avatarResult = await staffService.uploadAvatar(result.id, selectedFile);
      }

      if (formData.id && user?.userId && formData.id === user.userId) {
        updateUser({
          fullName: result.fullName,
          role: result.role,
          avatarUrl: avatarResult?.avatarUrl || result.avatarUrl,
        });
      }

      setIsModalOpen(false);
      showSuccess(`${actionType} nhân viên "${result.fullName}" thành công!`);
      fetchStaffs();

    } catch (err) {
      console.error('Error saving staff:', err);
      const errorMsg = err.response?.data?.detail || err.message || '';

      const newErrors = {};
      if (errorMsg.includes("Email already exists")) {
        newErrors.email = "Email này đã tồn tại.";
      }
      else if (errorMsg.includes("Phone number already exists")) {
        newErrors.phone = "Số điện thoại này đã tồn tại.";
      }

      if (Object.keys(newErrors).length > 0) {
        setErrors(newErrors);
      } else {
        showError(errorMsg || "Có lỗi xảy ra");
      }
    }
  };
  //  Xóa 
  const handleDelete = async (id) => {
    const confirmed = await confirm('Xóa nhân viên', 'Bạn có chắc chắn muốn xóa nhân viên này?');
    if (!confirmed) return;
    try {
      await staffService.delete(id);
      showSuccess("Đã xóa nhân viên thành công!");
      fetchStaffs();
    } catch (err) {
      showError(err);
    }
  };

  const filteredStaffs = staffs.filter(s =>
    (s.fullName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (s.email || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (s.phone || '').includes(searchTerm)
  );

  return (
    <div className="p-6 space-y-6 bg-slate-50 min-h-screen">
      <ManagementHeader
        searchPlaceholder="Tìm tên, email, sđt..."
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        onAddClick={() => {
          setEditingStaff(null);
          setErrors({});
          setIsModalOpen(true);
        }}
        addButtonText="Thêm Nhân viên"
      />

      {loading ? (
        <div className="flex justify-center p-20">
          <Loader2 className="animate-spin text-orange-500" size={40} />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredStaffs.map(staff => (
            <StaffCard
              key={staff.id}
              staff={staff}
              onEdit={() => { setEditingStaff(staff); setIsModalOpen(true); }}
              onDelete={() => handleDelete(staff.id)}
            />
          ))}
        </div>
      )}

      {!loading && filteredStaffs.length === 0 && (
        <div className="text-center py-20 text-gray-400 italic bg-white rounded-3xl border border-dashed">
          <Users size={40} className="mx-auto mb-4 opacity-30" />
          Không tìm thấy nhân viên phù hợp hoặc chưa có dữ liệu.
        </div>
      )}

      {/* Modal Thêm/Sửa */}
      <StaffModal
        key={isModalOpen ? (editingStaff?.id || 'new') : 'closed'}
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        data={editingStaff}
        onSubmit={handleSubmit}
        errors={errors}
        setErrors={setErrors}
      />

      <StatusModal
        isOpen={statusModal.isOpen}
        onClose={closeStatusModal}
        type={statusModal.type}
        title={statusModal.title}
        message={statusModal.message}
      />
      <ConfirmModal
        isOpen={confirmModal.isOpen}
        onClose={closeConfirm}
        onConfirm={confirmModal.onConfirm}
        title={confirmModal.title}
        message={confirmModal.message}
      />
    </div>
  );
};

export default StaffManager;