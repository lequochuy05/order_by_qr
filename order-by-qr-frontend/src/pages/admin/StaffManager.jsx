import React, { useState, useEffect, useCallback } from 'react';
import { Loader2 } from 'lucide-react';

import { useWebSocket } from '../../hooks/useWebSocket';
import { useStatusModal } from '../../hooks/useStatusModal';

import { staffService } from '../../services/admin/staffService';

import ManagementHeader from '../../components/admin/common/ManagementHeader';
import StaffCard from '../../components/admin/staff/StaffCard';
import StaffModal from '../../components/admin/staff/StaffModal';
import StatusModal from '../../components/admin/common/StatusModal';

const StaffManager = () => {
  const [staffs, setStaffs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingStaff, setEditingStaff] = useState(null);
  
  // State riêng cho file ảnh trong Modal (để truyền vào handleSubmit)
  // Lưu ý: State này thường được quản lý bên trong StaffModal và truyền ra qua callback, 
  // nhưng ở đây mình giả định handleSubmit nhận cả data và file.

  // Hook Status Modal
  const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();

  // === 1. Tải dữ liệu ===
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

  // === 2. WebSocket Realtime ===
  useWebSocket('/topic/users', (message) => {
    const signal = typeof message === 'string' ? message : message.body;
    if (signal === 'UPDATED') fetchStaffs();
  });

  useEffect(() => { fetchStaffs(true); }, [fetchStaffs]);

  // === 3. Xử lý Submit (QUAN TRỌNG: 2 bước) ===
  // formData chứa thông tin text, selectedFile là file ảnh (nếu có)
  const handleSubmit = async (formData, selectedFile) => {
    try {
      let result;
      if (formData.id) {
        // --- UPDATE ---
        // Bỏ password nếu rỗng để không bị lỗi hoặc reset nhầm
        const { password, ...updateData } = formData; 
        result = await staffService.update(formData.id, updateData);
        showSuccess(`Cập nhật nhân viên "${result.fullName}" thành công!`);
      } else {
        // --- CREATE ---
        result = await staffService.create(formData);
        showSuccess(`Thêm nhân viên "${result.fullName}" thành công!`);
      }

      // --- UPLOAD AVATAR (Nếu có chọn ảnh) ---
      // Dùng ID vừa tạo hoặc ID đang sửa
      if (selectedFile) {
        await staffService.uploadAvatar(result.id, selectedFile);
      }

      setIsModalOpen(false);
      
      // Cập nhật ngay lập tức
      fetchStaffs();

    } catch (err) {
      showError(err);
    }
  };

  // === 4. Xử lý Xóa ===
  const handleDelete = async (id) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa nhân viên này?")) return;
    try {
      await staffService.delete(id);
      showSuccess("Đã xóa nhân viên thành công!");
      fetchStaffs();
    } catch (err) {
      showError(err);
    }
  };

  const filteredStaffs = staffs.filter(s => 
    s.fullName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    s.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    s.phone?.includes(searchTerm)
  );

  return (
    <div className="p-6 space-y-6 bg-slate-50 min-h-screen">
      <ManagementHeader 
        searchPlaceholder="Tìm tên, email, sđt..."
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        onAddClick={() => { setEditingStaff(null); setIsModalOpen(true); }}
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

      {/* Modal Thêm/Sửa */}
      <StaffModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        initialData={editingStaff}
        onSubmit={handleSubmit} // Truyền hàm handle đã sửa
      />

      <StatusModal 
        isOpen={statusModal.isOpen}
        onClose={closeStatusModal}
        type={statusModal.type}
        title={statusModal.title}
        message={statusModal.message}
      />
    </div>
  );
};

export default StaffManager;