import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Loader2, Users } from 'lucide-react';

import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useAuth } from '@modules/auth/model/AuthContext.jsx';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';

import { staffService } from '@modules/staff-management/api/staffService.js';

import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import StaffCard from './StaffCard';
import StaffModal from './StaffModal';
import EmptyState from '@shared/ui/EmptyState.jsx';
import { playNotificationSound } from '@modules/notifications/lib/notificationSound.js';
import { USER_STATUS } from '@shared/lib/formatters.js';

const staffStatusFilterOptions = Object.entries(USER_STATUS).map(([id, meta]) => ({
  id,
  name: meta.label
}));

const StaffManager = () => {
  const [staffs, setStaffs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState(null);
  const [errors, setErrors] = useState({});
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const isMountedRef = useRef(true);
  const fetchSeqRef = useRef(0);
  const pageSize = 24;

  // Hook Status Modal
  const { showSuccess, showError } = useStatusModal();
  const { confirm } = useConfirmModal();
  const { user, updateUser } = useAuth();
  const debouncedSearchTerm = useDebouncedValue(searchTerm);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  // Tải dữ liệu 
  const fetchStaffs = useCallback(async (showLoading = false, { force = false } = {}) => {
    const fetchSeq = ++fetchSeqRef.current;
    if (showLoading) setLoading(true);
    try {
      const params = {
        page: currentPage,
        size: pageSize
      };
      if (debouncedSearchTerm.trim()) params.q = debouncedSearchTerm.trim();
      if (statusFilter !== 'ALL') params.status = statusFilter;

      const data = await staffService.getPage(params, { force });
      if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
      setStaffs(data.content || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
      console.error("Lỗi tải nhân viên:", err);
    } finally {
      if (isMountedRef.current && fetchSeq === fetchSeqRef.current) {
        setLoading(false);
      }
    }
  }, [currentPage, debouncedSearchTerm, statusFilter]);

  // WebSocket Realtime 
  useWebSocket('/topic/users', (message) => {
    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      playNotificationSound();
      fetchStaffs(false, { force: true });
    }
  });

  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearchTerm, statusFilter]);

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

      const savedStaff = avatarResult || result;
      if (formData.id && user?.userId && formData.id === user.userId) {
        updateUser({
          fullName: savedStaff.fullName,
          role: savedStaff.role,
          avatarUrl: savedStaff.avatarUrl,
        });
      }

      setIsModalOpen(false);
      showSuccess(`${actionType} nhân viên "${savedStaff.fullName}" thành công!`);
      fetchStaffs(false, { force: true });

    } catch (err) {
      console.error('Error saving staff:', err);
      const errorMsg = err.message || '';

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
      fetchStaffs(false, { force: true });
    } catch (err) {
      showError(err);
    }
  };

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
        showFilter
        filterAllLabel="Tất cả trạng thái"
        filterValue={statusFilter}
        setFilterValue={setStatusFilter}
        filterOptions={staffStatusFilterOptions}
      />

      {loading ? (
        <div className="flex justify-center p-20">
          <Loader2 className="animate-spin text-orange-500" size={40} />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {staffs.map(staff => (
            <StaffCard
              key={staff.id}
              staff={staff}
              onEdit={() => { setEditingStaff(staff); setIsModalOpen(true); }}
              onDelete={() => handleDelete(staff.id)}
            />
          ))}
        </div>
      )}

      {!loading && staffs.length === 0 && (
        <EmptyState icon={Users} message="Không tìm thấy nhân viên phù hợp hoặc chưa có dữ liệu." />
      )}

      <PaginationControls
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        itemLabel="nhân viên"
        loading={loading}
        onPageChange={setCurrentPage}
      />

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
    </div>
  );
};

export default StaffManager;
