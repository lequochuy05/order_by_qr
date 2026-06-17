import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Loader2, Package } from 'lucide-react';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useConfirmModal } from '@shared/hooks/useConfirmModal.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import { comboService } from '@features/combo-management/api/comboService.js';
import { menuItemService } from '@features/menu-management/api/menuService.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import ComboCard from './ComboCard';
import ComboModal from './ComboModal';
import ConfirmToggleModal from './ConfirmToggleModal';
import { playNotificationSound } from '@shared/lib/notificationSound.js';

const ComboManager = () => {
    const [combos, setCombos] = useState([]);
    const [menuItems, setMenuItems] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('ALL');

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingCombo, setEditingCombo] = useState(null);
    const [toggleConfirm, setToggleConfirm] = useState({ isOpen: false, id: null, active: false });
    const [errors, setErrors] = useState({});
    const [detailLoadingId, setDetailLoadingId] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const isMountedRef = useRef(true);
    const fetchSeqRef = useRef(0);
    const pageSize = 24;

    const { showSuccess, showError } = useStatusModal();
    const { confirm } = useConfirmModal();
    const debouncedSearchTerm = useDebouncedValue(searchTerm);

    useEffect(() => {
        isMountedRef.current = true;
        return () => {
            isMountedRef.current = false;
        };
    }, []);

    const fetchCombos = useCallback(async (showLoading = false, { force = false } = {}) => {
        const fetchSeq = ++fetchSeqRef.current;
        if (showLoading) setLoading(true);
        try {
            const params = {
                page: currentPage,
                size: pageSize
            };
            if (debouncedSearchTerm.trim()) params.q = debouncedSearchTerm.trim();
            if (statusFilter === 'ACTIVE') params.active = true;
            if (statusFilter === 'INACTIVE') params.active = false;

            const comboData = await comboService.getPage(params, { force });
            if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
            setCombos(comboData.content || []);
            setTotalPages(comboData.totalPages || 0);
            setTotalElements(comboData.totalElements || 0);
        } catch (err) {
            if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
            console.error("Lỗi đồng bộ dữ liệu:", err);
        } finally {
            if (isMountedRef.current && fetchSeq === fetchSeqRef.current) {
                setLoading(false);
            }
        }
    }, [currentPage, debouncedSearchTerm, statusFilter]);

    const ensureMenuItems = useCallback(async () => {
        if (menuItems.length > 0) return menuItems;
        const data = await menuItemService.getAll();
        if (isMountedRef.current) {
            setMenuItems(data || []);
        }
        return data || [];
    }, [menuItems]);

    useWebSocket('/topic/combos', (message) => {
        if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
            playNotificationSound();
            fetchCombos(false, { force: true });
        }
    });

    useEffect(() => {
        setCurrentPage(0);
    }, [debouncedSearchTerm, statusFilter]);

    useEffect(() => { fetchCombos(true); }, [fetchCombos]);

    const handleSubmit = async (data) => {
        try {
            if (data.id) {
                await comboService.update(data.id, data);
                showSuccess("Cập nhật Combo thành công!");
            } else {
                await comboService.create(data);
                showSuccess("Thêm mới Combo thành công!");
            }
            setErrors({});
            setIsModalOpen(false);
            fetchCombos(false, { force: true });

        } catch (err) {
            console.error('Error saving combo:', err)
            const errorMsg = err.message || '';
            if (errorMsg.toLowerCase().includes('combo name already exists')) {
                setErrors({ ...errors, name: "Tên Combo này đã tồn tại" });
            } else {
                showError(err);
            }
        }
    };

    const handleDelete = async (id) => {
        const confirmed = await confirm('Xóa combo', 'Bạn có chắc chắn muốn xóa combo này?');
        if (!confirmed) return;
        try {
            await comboService.delete(id);
            showSuccess("Đã xóa Combo thành công!");
            fetchCombos(false, { force: true });
        } catch (err) {
            showError(err);
        }
    };

    const handleRequestToggle = (id, currentActive) => {
        setToggleConfirm({ isOpen: true, id, active: currentActive });
    };

    const handleConfirmToggle = async () => {
        const { id } = toggleConfirm;
        try {
            const updated = await comboService.toggleActive(id);
            setCombos(prev => prev.map(combo => combo.id === id ? { ...combo, active: updated.active } : combo));

            setToggleConfirm({ isOpen: false, id: null, active: false });
            fetchCombos(false, { force: true });
        } catch (err) {
            setToggleConfirm({ isOpen: false, id: null, active: false });
            showError(err);
        }
    };

    const handleEdit = async (id) => {
        setDetailLoadingId(id);
        try {
            const [detail] = await Promise.all([
                comboService.getById(id),
                ensureMenuItems()
            ]);
            if (!isMountedRef.current) return;
            const formatted = {
                ...detail,
                items: (detail.items || []).map(i => ({
                    id: i.id,
                    menuItemId: i.menuItemId ?? i.menuItem?.id,
                    quantity: i.quantity
                })).filter(i => i.menuItemId)
            };
            setEditingCombo(formatted);
            setErrors({});
            setIsModalOpen(true);
        } catch (err) {
            showError(err);
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
            showError(err);
        }
    };

    return (
        <div className="p-6 space-y-6 bg-slate-50/50 min-h-screen font-sans">
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
                    { id: 'INACTIVE', name: 'Tạm ngưng' }
                ]}
            />

            {loading ? (
                <div className="flex flex-col items-center justify-center p-20 gap-4">
                    <Loader2 className="animate-spin text-orange-500" size={48} />
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                    {combos.map(c => (
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
                <div className="text-center py-20 text-gray-400 italic bg-white rounded-3xl border border-dashed">
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
                key={isModalOpen ? (editingCombo?.id || 'new') : 'closed'}
                isOpen={isModalOpen} onClose={() => setIsModalOpen(false)}
                menuItems={menuItems} initialData={editingCombo} onSubmit={handleSubmit}
                errors={errors} setErrors={setErrors}
            />
        </div>
    );
};

export default ComboManager;
