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

  // Hook Status Modal
  const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();

  // === Tải dữ liệu ===
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
    if (message === 'UPDATED' || message.body === 'UPDATED') {
      fetchStaffs();
    }
  });

  useEffect(() => { fetchStaffs(true); }, [fetchStaffs]);

  // === Xử lý Submit ===
  const handleSubmit = async (formData, selectedFile) => {
    try {
      // === 1. CHUẨN HÓA DỮ LIỆU (QUAN TRỌNG) ===
      const payload = { ...formData };

      // Nếu phone rỗng -> gửi null (để Backend bỏ qua check Regex)
      if (!payload.phone || payload.phone.trim() === '') {
        payload.phone = null;
      }

      // Xóa trường ID nếu là null (Create mode) để tránh lỗi thừa trường
      if (!payload.id) {
        delete payload.id;
      }

      if (!payload.password) {
        payload.password = null; // Hoặc: delete payload.password;
      }
      
      // Trim email và tên cho sạch
      payload.email = payload.email?.trim();
      payload.fullName = payload.fullName?.trim();

      // === 2. GỬI API ===
      let result;
      let actionType = '';

      if (formData.id) {
        // UPDATE
        result = await staffService.update(formData.id, payload);
        actionType = 'Cập nhật';
      } else {
        // CREATE
        result = await staffService.create(payload);
        actionType = 'Thêm';
      }

      // === 3. UPLOAD ẢNH (Nếu có) ===
      if (selectedFile) {
        await staffService.uploadAvatar(result.id, selectedFile);
      }

      setIsModalOpen(false);
      showSuccess(`${actionType} nhân viên "${result.fullName}" thành công!`);
      fetchStaffs();

    } catch (err) {
      // Log lỗi ra console để debug nếu cần
      console.error("Submit Error:", err);
      
      const errorMsg = err.response?.data?.error || 
                       err.response?.data?.message || 
                       // Nếu lỗi validation trả về Map, lấy lỗi đầu tiên
                       (typeof err.response?.data === 'object' ? Object.values(err.response.data)[0] : "Có lỗi xảy ra");
                       
      showError(errorMsg);
    }
  };

  // === Xử lý Xóa ===
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
        data={editingStaff}
        onSubmit={handleSubmit} 
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