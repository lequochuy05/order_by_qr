import { comboService } from '@entities/combo/api/comboService.js';
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Loader2, Package } from 'lucide-react';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import { useComboPageQuery } from '../api/comboQueries.js';
import { useCreateComboMutation, useUpdateComboMutation, useDeleteComboMutation, useToggleComboActiveMutation } from '../api/comboMutations.js';
import { menuItemService } from '@entities/menu-item/api/menuItemService.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import ComboCard from './ComboCard';
import ComboModal from './ComboModal';
import ConfirmToggleModal from './ConfirmToggleModal';
import { playNotificationSound } from '@shared/lib/notificationSound.js';

const ComboManager = () => {
  const [menuItems, setMenuItems] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCombo, setEditingCombo] = useState(null);
  const [toggleConfirm, setToggleConfirm] = useState({ isOpen: false, id: null, active: false });
  const [errors, setErrors] = useState({});
  const [detailLoadingId, setDetailLoadingId] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
      const isMountedRef = useRef(true);
    const pageSize = 24;

  const { confirm } = useConfirmModal();
  const debouncedSearchTerm = useDebouncedValue(searchTerm);
  const queryParams = {
    page: currentPage,
    size: pageSize,
    ...(debouncedSearchTerm.trim() ? { q: debouncedSearchTerm.trim() } : {}),
    ...(statusFilter === 'ACTIVE' ? { active: true } : {}),
    ...(statusFilter === 'INACTIVE' ? { active: false } : {})
  };
  const { data: comboData, isFetching: loading } = useComboPageQuery(queryParams);
  const combos = comboData?.content || [];
  const totalPages = comboData?.totalPages || 0;
  const totalElements = comboData?.totalElements || 0;
  const { mutateAsync: createCombo } = useCreateComboMutation();
  const { mutateAsync: updateCombo } = useUpdateComboMutation();
  const { mutateAsync: deleteCombo } = useDeleteComboMutation();
  const { mutateAsync: toggleActive } = useToggleComboActiveMutation();
  

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  

  const ensureMenuItems = useCallback(async () => {
    if (menuItems.length > 0) return menuItems;
    const data = await menuItemService.getAll();
    if (isMountedRef.current) {
      setMenuItems(data || []);
    }
    return data || [];
  }, [menuItems]);

  useWebSocket(
    '/topic/combos',
    (message) => {
      if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
        playNotificationSound();
        
      }
    },
    { scope: 'admin' },
  );

  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearchTerm, statusFilter]);

  const handleSubmit = async (data) => {
    try {
      if (data.id) {
        await updateCombo({ id: data.id, data });
        showSuccessToast('Cập nhật Combo thành công!');
      } else {
        await createCombo(data);
        showSuccessToast('Thêm mới Combo thành công!');
      }
      setErrors({});
      setIsModalOpen(false);
      
    } catch (err) {
      console.error('Error saving combo:', err);
      const errorMsg = err.message || '';
      if (
        err?.code === 'COMBO_NAME_EXISTS' ||
        errorMsg.toLowerCase().includes('combo name already exists')
      ) {
        setErrors({ ...errors, name: 'Tên Combo này đã tồn tại' });
      } else {
        showErrorToast(err);
      }
    }
  };

  const handleDelete = async (id) => {
    const confirmed = await confirm('Xóa combo', 'Bạn có chắc chắn muốn xóa combo này?');
    if (!confirmed) return;
    try {
      await deleteCombo(id);
      showSuccessToast('Đã xóa Combo thành công!');
      
    } catch (err) {
      showErrorToast(err);
    }
  };

  const handleRequestToggle = (id, currentActive) => {
    setToggleConfirm({ isOpen: true, id, active: currentActive });
  };

  const handleConfirmToggle = async () => {
    const { id } = toggleConfirm;
    try {
      await toggleActive(id);
      setToggleConfirm({ isOpen: false, id: null, active: false });
      
    } catch (err) {
      setToggleConfirm({ isOpen: false, id: null, active: false });
      showErrorToast(err);
    }
  };

  const handleEdit = async (id) => {
    setDetailLoadingId(id);
    try {
      const [detail] = await Promise.all([comboService.getById(id), ensureMenuItems()]);
      if (!isMountedRef.current) return;
      const formatted = {
        ...detail,
        items: (detail.items || [])
          .map((i) => ({
            id: i.id,
            menuItemId: i.menuItemId ?? i.menuItem?.id,
            quantity: i.quantity,
          }))
          .filter((i) => i.menuItemId),
      };
      setEditingCombo(formatted);
      setErrors({});
      setIsModalOpen(true);
    } catch (err) {
      showErrorToast(err);
    } finally {
      if (isMountedRef.current) {
        setDetailLoadingId(null);
      }
    }
  };

  const handleOpenCreate = async () => {
    setEditingCombo(null);
    setErrors({});
    try {
      await ensureMenuItems();
      if (isMountedRef.current) {
        setIsModalOpen(true);
      }
    } catch (err) {
      showErrorToast(err);
    }
  };

  return (
    <div className="min-h-screen w-full min-w-0 space-y-4 bg-slate-50/50 p-0 font-sans dark:bg-slate-950/50 sm:space-y-6 sm:p-3 lg:p-6">
      <ManagementHeader
        searchPlaceholder="Tìm kiếm combo..."
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        onAddClick={handleOpenCreate}
        addButtonText="Thêm mới"
        showFilter
        filterAllLabel="Tất cả trạng thái"
        filterValue={statusFilter}
        setFilterValue={setStatusFilter}
        filterOptions={[
          { id: 'ACTIVE', name: 'Đang kinh doanh' },
          { id: 'INACTIVE', name: 'Tạm ngưng' },
        ]}
      />

      {loading ? (
        <div className="flex flex-col items-center justify-center p-20 gap-4">
          <Loader2 className="animate-spin text-orange-500" size={48} />
        </div>
      ) : (
        <div className="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2 sm:gap-6 lg:grid-cols-3 xl:grid-cols-4">
          {combos.map((c) => (
            <ComboCard
              key={c.id}
              combo={c}
              onEdit={() => handleEdit(c.id)}
              onToggle={() => handleRequestToggle(c.id, c.active)}
              onDelete={() => handleDelete(c.id)}
              isEditing={detailLoadingId === c.id}
            />
          ))}
        </div>
      )}

      {!loading && combos.length === 0 && (
        <div className="rounded-3xl border border-dashed border-slate-200 bg-white py-20 text-center italic text-slate-400 transition-colors dark:border-slate-800 dark:bg-slate-900 dark:text-slate-500">
          <Package size={40} className="mx-auto mb-4 opacity-30" />
          Không tìm thấy combo phù hợp hoặc chưa có dữ liệu.
        </div>
      )}

      <PaginationControls
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        itemLabel="combo"
        loading={loading}
        onPageChange={setCurrentPage}
      />

      <ConfirmToggleModal
        isOpen={toggleConfirm.isOpen}
        currentActive={toggleConfirm.active}
        onConfirm={handleConfirmToggle}
        onCancel={() => setToggleConfirm({ isOpen: false, id: null, active: false })}
      />

      <ComboModal
        key={isModalOpen ? editingCombo?.id || 'new' : 'closed'}
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        menuItems={menuItems}
        initialData={editingCombo}
        onSubmit={handleSubmit}
        errors={errors}
        setErrors={setErrors}
      />
    </div>
  );
};

export default ComboManager;
