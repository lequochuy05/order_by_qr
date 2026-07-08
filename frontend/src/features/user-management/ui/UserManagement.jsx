import React, { useState, useEffect, useRef } from 'react';
import { Loader2, Users } from 'lucide-react';

import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useAuth } from '@features/auth';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';

import { useUserPageQuery } from '../api/userQueries.js';
import { useCreateUserMutation, useUpdateUserMutation, useDeleteUserMutation, useUploadUserAvatarMutation } from '../api/userMutations.js';

import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import UserCard from './UserCard';
import UserModal from './UserModal';
import EmptyState from '@shared/ui/EmptyState.jsx';
import { playNotificationSound } from '@shared/lib/notificationSound.js';
import { USER_STATUS } from '@shared/lib/formatters.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

const staffStatusFilterOptions = Object.entries(USER_STATUS).map(([id, meta]) => ({
  id,
  name: meta.label,
}));

const StaffManager = () => {
      const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState(null);
  const [errors, setErrors] = useState({});
  const [currentPage, setCurrentPage] = useState(0);
      const isMountedRef = useRef(true);
    const pageSize = 24;

  const { confirm } = useConfirmModal();
  const { user, updateUser } = useAuth();
  const debouncedSearchTerm = useDebouncedValue(searchTerm);
  const queryParams = {
    page: currentPage,
    size: pageSize,
    ...(debouncedSearchTerm.trim() ? { q: debouncedSearchTerm.trim() } : {}),
    ...(statusFilter !== 'ALL' ? { status: statusFilter } : {})
  };
  const { data: staffData, isFetching: loading } = useUserPageQuery(queryParams);
  const staffs = staffData?.content || [];
  const totalPages = staffData?.totalPages || 0;
  const totalElements = staffData?.totalElements || 0;
  const { mutateAsync: createUser } = useCreateUserMutation();
  const { mutateAsync: updateUserMut } = useUpdateUserMutation();
  const { mutateAsync: uploadAvatar } = useUploadUserAvatarMutation();
  const { mutateAsync: deleteUser } = useDeleteUserMutation();
  

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  

  // WebSocket Realtime
  useWebSocket(
    '/topic/users',
    (message) => {
      if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
        playNotificationSound();
        
      }
    },
    { scope: 'admin' },
  );

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setCurrentPage(0);
  }, [debouncedSearchTerm, statusFilter]);



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
        result = await updateUserMut({ id: formData.id, data: payload });
        actionType = 'Cập nhật';
      } else {
        result = await createUser(payload);
        actionType = 'Thêm';
      }

      let avatarResult = null;
      if (selectedFile) {
        avatarResult = await uploadAvatar({ id: result.id, file: selectedFile });
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
      showSuccessToast(`${actionType} nhân viên "${savedStaff.fullName}" thành công!`);
      
    } catch (err) {
      console.error('Error saving staff:', err);
      const errorMsg = err.message || '';

      const newErrors = {};
      if (err?.code === 'EMAIL_EXISTS' || errorMsg.includes('Email already exists')) {
        newErrors.email = 'Email này đã tồn tại.';
      } else if (err?.code === 'PHONE_EXISTS' || errorMsg.includes('Phone number already exists')) {
        newErrors.phone = 'Số điện thoại này đã tồn tại.';
      }

      if (Object.keys(newErrors).length > 0) {
        setErrors(newErrors);
      } else {
        showErrorToast(err);
      }
    }
  };
  //  Xóa
  const handleDelete = async (id) => {
    const confirmed = await confirm('Xóa nhân viên', 'Bạn có chắc chắn muốn xóa nhân viên này?');
    if (!confirmed) return;
    try {
      await deleteUser(id);
      showSuccessToast('Đã xóa nhân viên thành công!');
      
    } catch (err) {
      showErrorToast(err);
    }
  };

  return (
    <div className="min-h-screen w-full min-w-0 space-y-4 bg-slate-50 p-0 sm:space-y-6 sm:p-3 lg:p-6 dark:bg-slate-950">
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
        <div className="grid min-w-0 grid-cols-1 gap-4 sm:gap-6 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {staffs.map((staff) => (
            <UserCard
              key={staff.id}
              staff={staff}
              onEdit={() => {
                setEditingStaff(staff);
                setIsModalOpen(true);
              }}
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
      <UserModal
        key={isModalOpen ? editingStaff?.id || 'new' : 'closed'}
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
