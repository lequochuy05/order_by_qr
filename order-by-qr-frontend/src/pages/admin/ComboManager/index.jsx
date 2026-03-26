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

    // Modal & Toggle State
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingCombo, setEditingCombo] = useState(null);
    const [toggleConfirm, setToggleConfirm] = useState({ isOpen: false, id: null, active: false });

    // === Hook Status Modal ===
    const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();

    // === Tải dữ liệu ===
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

    // === WebSocket ===
    // Backend bắn "UPDATED" -> Frontend tải lại
    useWebSocket('/topic/combos', (message) => {
        const signal = typeof message === 'string' ? message : message.body;
        if (signal === 'UPDATED') fetchData();
    });

    useEffect(() => { fetchData(true); }, [fetchData]);

    // === XỬ LÝ SUBMIT (THÊM / SỬA) ===
    const handleSubmit = async (data) => {
        try {
            if (data.id) {
                await comboService.update(data.id, data);
                showSuccess("Cập nhật Combo thành công!");
            } else {
                await comboService.create(data);
                showSuccess("Thêm mới Combo thành công!");
            }

            setIsModalOpen(false);

        } catch (err) {
            showError(err);
        }
    };

    // === XÓA ===
    const handleDelete = async (id) => {
        if (!window.confirm("Xác nhận xóa combo này?")) return;
        try {
            await comboService.delete(id);
            showSuccess("Đã xóa Combo thành công!");

            // === CẬP NHẬT NGAY LẬP TỨC ===
            fetchData();
        } catch (err) {
            showError(err);
        }
    };

    // === BẬT/TẮT TRẠNG THÁI ===
    const handleRequestToggle = (id, currentActive) => {
        setToggleConfirm({ isOpen: true, id, active: currentActive });
    };

    const handleConfirmToggle = async () => {
        const { id } = toggleConfirm;
        try {
            await comboService.toggleActive(id);

            // Đóng modal confirm
            setToggleConfirm({ isOpen: false, id: null, active: false });
        } catch (err) {
            // Đóng modal trước khi hiện lỗi
            setToggleConfirm({ isOpen: false, id: null, active: false });
            showError(err);
        }
    };

    // Chuẩn bị dữ liệu để sửa
    const handleEdit = async (id) => {
        try {
            const detail = await comboService.getById(id);
            const formatted = {
                ...detail,
                items: detail.items.map(i => ({ menuItemId: i.menuItem.id, quantity: i.quantity }))
            };
            setEditingCombo(formatted);
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
                onAddClick={() => { setEditingCombo(null); setIsModalOpen(true); }}
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
                isOpen={isModalOpen} onClose={() => setIsModalOpen(false)}
                menuItems={menuItems} initialData={editingCombo} onSubmit={handleSubmit}
            />

            <StatusModal
                isOpen={statusModal.isOpen} onClose={closeStatusModal}
                type={statusModal.type} title={statusModal.title} message={statusModal.message}
            />
        </div>
    );
};

export default ComboManager;