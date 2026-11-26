import React, { useState, useEffect, useRef } from 'react';
import { Loader2, Package } from 'lucide-react';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useCrudModal } from '@shared/hooks/useCrudModal.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import { comboService, useComboPageQuery } from '@entities/combo';
import { useMenuAllQuery } from '@entities/menu-item';
import { queryClient } from '@shared/api/queryClient.js';
import { queryKeys } from '@shared/api/queryKeys.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import { comboProposalToDraft } from '@entities/ai';
import AiComboSuggestionsPanel from './AiComboSuggestionsPanel.jsx';
import ComboCard from './ComboCard';
import ComboModal from './ComboModal';

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
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [detailLoadingId, setDetailLoadingId] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [isToggling, setIsToggling] = useState(false);
  const isMountedRef = useRef(true);
  const pageSize = 24;

  const crud = useCrudModal();
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

  const { data: menuItems = [] } = useMenuAllQuery();

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

  const handleSearchChange = (value) => {
    setSearchTerm(value);
    setCurrentPage(0);
  };

  const handleStatusFilterChange = (value) => {
    setStatusFilter(value);
    setCurrentPage(0);
  };

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
      crud.close();
      invalidateCombos();
    } catch (err) {
      console.error('Error saving combo:', err);
      const errorMsg = err.message || '';
      if (
        err?.code === 'COMBO_NAME_EXISTS' ||
        errorMsg.toLowerCase().includes('combo name already exists')
      ) {
        crud.setFormErrors((current) => ({ ...current, name: 'Tên Combo này đã tồn tại' }));
      } else {
        showErrorToast(err);
      }
    }
  };

  const handleDelete = async (id, version) => {
    if (crud.deletingId) return;
    const confirmed = await confirm('Xóa combo', 'Bạn có chắc chắn muốn xóa combo này?');
    if (!confirmed) return;
    crud.setDeletingId(id);
    try {
      await comboService.delete(id, version);
      showSuccessToast('Đã xóa Combo thành công!');
      invalidateCombos();
    } catch (err) {
      showErrorToast(err);
    } finally {
      crud.setDeletingId(null);
    }
  };

  const handleRequestToggle = async (id, currentActive, version) => {
    if (isToggling) return;
    const confirmed = await confirm(
      currentActive ? 'Xác nhận ngưng bán?' : 'Xác nhận mở bán?',
      currentActive
        ? 'Combo này sẽ ẩn khỏi thực đơn khách hàng ngay lập tức. Bạn có chắc chắn muốn thực hiện?'
        : 'Combo này sẽ xuất hiện lại trên thực đơn để khách hàng có thể đặt món.',
    );
    if (!confirmed) return;

    setIsToggling(true);
    try {
      await comboService.toggleActive(id, version);
      invalidateCombos();
    } catch (err) {
      showErrorToast(err);
    } finally {
      setIsToggling(false);
    }
  };

  const handleEdit = async (id) => {
    setDetailLoadingId(id);
    try {
      const detail = await comboService.getById(id);
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
      crud.openEdit(formatted);
    } catch (err) {
      showErrorToast(err);
    } finally {
      if (isMountedRef.current) {
        setDetailLoadingId(null);
      }
    }
  };

  const handleOpenCreate = () => {
    crud.openCreate();
  };

  const handleUseComboDraft = (proposal, index) => {
    crud.openCreate(comboProposalToDraft(proposal, index));
  };

  const handleToggleAvailable = async (id, version) => {
    try {
      await comboService.toggleAvailable(id, version);
      invalidateCombos();
    } catch (err) {
      showErrorToast(err);
    }
  };

  return (
    <div className="w-full min-w-0 space-y-4 p-0 font-sans sm:space-y-6 sm:p-3 lg:p-6">
      <ManagementHeader
        searchPlaceholder="Tìm kiếm combo..."
        searchTerm={searchTerm}
        setSearchTerm={handleSearchChange}
        onAddClick={handleOpenCreate}
        addButtonText="Thêm mới"
        showFilter
        filterAllLabel="Tất cả trạng thái"
        filterValue={statusFilter}
        setFilterValue={handleStatusFilterChange}
        filterOptions={[
          { id: 'ACTIVE', name: 'Đang kinh doanh' },
          { id: 'INACTIVE', name: 'Tạm ngưng' },
        ]}
      />

      <AiComboSuggestionsPanel onUseDraft={handleUseComboDraft} />

      {loading ? (
        <div className="flex flex-col items-center justify-center p-20 gap-4">
          <Loader2 className="animate-spin text-indigo-600 dark:text-indigo-400" size={48} />
        </div>
      ) : (
        <div className="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2 sm:gap-6 lg:grid-cols-3 xl:grid-cols-4">
          {combos.map((c) => (
            <ComboCard
              key={c.id}
              combo={c}
              onEdit={() => handleEdit(c.id)}
              onToggle={() => handleRequestToggle(c.id, c.active, c.version)}
              onToggleAvailable={handleToggleAvailable}
              onDelete={() => handleDelete(c.id, c.version)}
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

      <ComboModal
        key={
          crud.isOpen ? crud.editingItem?.id || crud.editingItem?._draftKey || 'new' : 'closed'
        }
        isOpen={crud.isOpen}
        onClose={crud.close}
        menuItems={menuItems}
        initialData={crud.editingItem}
        onSubmit={handleSubmit}
        errors={crud.formErrors}
        setErrors={crud.setFormErrors}
      />
    </div>
  );
};

export default ComboManager;
