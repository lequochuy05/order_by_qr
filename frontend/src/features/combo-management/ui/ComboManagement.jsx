import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Loader2, Package } from 'lucide-react';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import { comboService, useComboPageQuery } from '@entities/combo';
import { menuItemService } from '@entities/menu-item';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import ComboCard from './ComboCard';
import ComboModal from './ComboModal';
import ConfirmToggleModal from './ConfirmToggleModal';
import { playNotificationSound } from '@shared/lib/notificationSound.js';

const normalizeComboPayload = (data) => ({
  name: data.name.trim(),
  description: data.description?.trim() || null,
  price: Number(data.price),
  active: data.active ?? true,
  available: data.available ?? true,
  displayOrder: Number(data.displayOrder) || 0,
  version: data.version ?? null,
  items: (data.items || []).map((item) => ({
    id: item.id,
    menuItemId: Number(item.menuItemId),
    quantity: Number(item.quantity) || 1,
  })),
});

const ComboManager = () => {
  const [menuItems, setMenuItems] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCombo, setEditingCombo] = useState(null);
  const [toggleConfirm, setToggleConfirm] = useState({
    isOpen: false,
    id: null,
    active: false,
    version: null,
  });
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
    ...(statusFilter === 'INACTIVE' ? { active: false } : {}),
  };
  const { data: comboData, isFetching: loading } = useComboPageQuery(queryParams);
  const combos = comboData?.content || [];
  const totalPages = comboData?.totalPages || 0;
  const totalElements = comboData?.totalElements || 0;

  const invalidateCombos = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.combos.all });
    queryClient.invalidateQueries({ queryKey: queryKeys.menu.all });
  };

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
        comboService.clearCache();
        invalidateCombos();
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
      const payload = normalizeComboPayload(data);

      if (data.id) {
        await comboService.update(data.id, payload);
        showSuccessToast('Cập nhật Combo thành công!');
      } else {
        await comboService.create(payload);
        showSuccessToast('Thêm mới Combo thành công!');
      }
      setErrors({});
      setIsModalOpen(false);
      invalidateCombos();
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
      await comboService.delete(id);
      showSuccessToast('Đã xóa Combo thành công!');
      invalidateCombos();
    } catch (err) {
      showErrorToast(err);
    }
  };

  const handleRequestToggle = (id, currentActive, version) => {
    setToggleConfirm({ isOpen: true, id, active: currentActive, version });
  };

  const handleConfirmToggle = async () => {
    const { id, version } = toggleConfirm;
    try {
      await comboService.toggleActive(id, version);
      setToggleConfirm({ isOpen: false, id: null, active: false, version: null });
      invalidateCombos();
    } catch (err) {
      setToggleConfirm({ isOpen: false, id: null, active: false, version: null });
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
              onToggle={() => handleRequestToggle(c.id, c.active, c.version)}
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
        onCancel={() => setToggleConfirm({ isOpen: false, id: null, active: false, version: null })}
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
