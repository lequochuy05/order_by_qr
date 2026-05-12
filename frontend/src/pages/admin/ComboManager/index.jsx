import React, { useState, useEffect, useCallback } from 'react';
import { Loader2, Package } from 'lucide-react';
import { useWebSocket } from '../../../hooks/useWebSocket';
import { useStatusModal } from '../../../hooks/useStatusModal';
import { comboService } from '../../../services/admin/comboService';
import { menuItemService } from '../../../services/admin/menuService';
import ManagementHeader from '../../../components/admin/common/ManagementHeader';
import ComboCard from './ComboCard';
import ComboModal from './ComboModal';
import ConfirmToggleModal from './ConfirmToggleModal';
import StatusModal from '../../../components/admin/common/StatusModal';

const ComboManager = () => {
    const [combos, setCombos] = useState([]);
    const [menuItems, setMenuItems] = useState([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingCombo, setEditingCombo] = useState(null);
    const [toggleConfirm, setToggleConfirm] = useState({ isOpen: false, id: null, active: false });
    const [errors, setErrors] = useState({});

    const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();

    const fetchData = useCallback(async (showLoading = false) => {
        if (showLoading) setLoading(true);
        try {
            const [comboData, menuData] = await Promise.all([
                comboService.getAll(),
                menuItemService.getAll()
            ]);
            setCombos(comboData);
            setMenuItems(menuData);
        } catch (err) {
            console.error("Lỗi đồng bộ dữ liệu:", err);
        } finally {
            setLoading(false);
        }
    }, []);

    useWebSocket('/topic/combos', (message) => {
        if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
            fetchData();
        }
    });

    useEffect(() => { fetchData(true); }, [fetchData]);

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

        } catch (err) {
            console.error('Error saving combo:', err)
            const errorMsg = err.response?.data?.detail || err.message || '';
            if (errorMsg.toLowerCase().includes('combo name already exists')) {
                setErrors({ ...errors, name: "Tên Combo này đã tồn tại" });
            } else {
                showError(err);
            }
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm("Xác nhận xóa combo này?")) return;
        try {
            await comboService.delete(id);
            showSuccess("Đã xóa Combo thành công!");

            fetchData();
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
            await comboService.toggleActive(id);

            setToggleConfirm({ isOpen: false, id: null, active: false });
        } catch (err) {
            setToggleConfirm({ isOpen: false, id: null, active: false });
            showError(err);
        }
    };

    const handleEdit = async (id) => {
        try {
            const detail = await comboService.getById(id);
            const formatted = {
                ...detail,
                items: detail.items.map(i => ({ id: i.id, menuItemId: i.menuItem.id, quantity: i.quantity }))
            };
            setEditingCombo(formatted);
            setErrors({});
            setIsModalOpen(true);
        } catch (err) {
            showError(err);
        }
    };

    const filteredCombos = React.useMemo(() => {
        return combos.filter(c => c.name.toLowerCase().includes(searchTerm.toLowerCase()));
    }, [combos, searchTerm]);

    return (
        <div className="p-6 space-y-6 bg-slate-50/50 min-h-screen font-sans">
            <ManagementHeader
                searchPlaceholder="Tìm kiếm combo..."
                searchTerm={searchTerm}
                setSearchTerm={setSearchTerm}
                onAddClick={() => {
                    setEditingCombo(null);
                    setErrors({});
                    setIsModalOpen(true);
                }}
                addButtonText="Thêm mới"
            />

            {loading ? (
                <div className="flex flex-col items-center justify-center p-20 gap-4">
                    <Loader2 className="animate-spin text-orange-500" size={48} />
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                    {filteredCombos.map(c => (
                        <ComboCard
                            key={c.id}
                            combo={c}
                            onEdit={() => handleEdit(c.id)}
                            onToggle={() => handleRequestToggle(c.id, c.active)}
                            onDelete={() => handleDelete(c.id)}
                        />
                    ))}
                </div>
            )}

            {!loading && filteredCombos.length === 0 && (
                <div className="text-center py-20 text-gray-400 italic bg-white rounded-3xl border border-dashed">
                    <Package size={40} className="mx-auto mb-4 opacity-30" />
                    Không tìm thấy combo phù hợp hoặc chưa có dữ liệu.
                </div>
            )}

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

            <StatusModal
                isOpen={statusModal.isOpen} onClose={closeStatusModal}
                type={statusModal.type} title={statusModal.title} message={statusModal.message}
            />
        </div>
    );
};

export default ComboManager;