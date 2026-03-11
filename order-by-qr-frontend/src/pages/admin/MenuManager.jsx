import React, { useState, useEffect, useCallback } from 'react';
import { Loader2 } from 'lucide-react';

import { useWebSocket } from '../../hooks/useWebSocket';
import { useStatusModal } from '../../hooks/useStatusModal';

import { menuItemService } from '../../services/admin/menuService';
import { categoryService } from '../../services/admin/categoryService';

import ManagementHeader from '../../components/admin/common/ManagementHeader';
import MenuCard from '../../components/admin/menu/MenuCard';
import MenuModal from '../../components/admin/menu/MenuModal';
import StatusModal from '../../components/admin/common/StatusModal';

const MenuManager = () => {
  const [items, setItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [filterCate, setFilterCate] = useState('ALL');
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  
  const [formData, setFormData] = useState({ id: null, name: '', price: '', categoryId: '' });
  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState('');

  // === 1. Hook Status Modal ===
  const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();

  // 2. Load Categories
  useEffect(() => {
    categoryService.search('').then(data => {
      const cats = data.content || [];
      setCategories(cats);
      if(cats.length > 0) setFormData(prev => ({...prev, categoryId: cats[0].id}));
    });
  }, []);

  // 3. Fetch Items (Lấy dữ liệu từ Server)
  const fetchItems = useCallback(async (showLoading = false) => {
    if (showLoading) setLoading(true);
    try {
      const data = await menuItemService.getAll(filterCate);
      setItems(data);
    } catch (err) { 
      console.error("Lỗi đồng bộ thực đơn:", err); 
    } finally { 
      setLoading(false); 
    }
  }, [filterCate]);

  // === 4. WebSocket (Giống hệt Voucher) ===
  // Khi Backend xử lý xong sẽ bắn tín hiệu "UPDATED" về đây -> Frontend tự tải lại
  useWebSocket('/topic/menu', (message) => {
    const signal = typeof message === 'string' ? message : message.body;
    if (signal === 'UPDATED') fetchItems(); 
  });

  useEffect(() => { fetchItems(true); }, [fetchItems]);

  // === XỬ LÝ SUBMIT ===
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = { 
        name: formData.name, 
        price: Number(formData.price), 
        category: { id: Number(formData.categoryId) } 
      };
      
      let res;
      if (formData.id) {
         res = await menuItemService.update(formData.id, payload);
         showSuccess(`Đã cập nhật món ăn`);
      } else {
         res = await menuItemService.create(payload);
         showSuccess(`Đã thêm món ăn`);
      }

      if (selectedFile) {
        await menuItemService.uploadImage(res.id || formData.id, selectedFile);
      }
      
      setIsModalOpen(false);
      // Không cần gọi fetchItems() thủ công ở đây vì WebSocket sẽ lo việc đó
    } catch (err) { 
      showError(err);
    }
  };

  // === XỬ LÝ DELETE ===
  const handleDelete = async (id) => {
    if (!window.confirm("Xóa món ăn này?")) return;
    try {
        await menuItemService.delete(id);
        showSuccess("Đã xóa món ăn thành công");
        // WebSocket sẽ tự lo việc load lại danh sách sau khi xóa xong
    } catch (err) {
        showError(err);
    }
  };

  return (
    <div className="p-4 space-y-4">
      <ManagementHeader 
        showFilter={true}
        filterValue={filterCate}
        setFilterValue={setFilterCate}
        filterOptions={categories}
        onAddClick={() => {
            setFormData({ id: null, name: '', price: '', categoryId: categories[0]?.id || '' });
            setPreview('');
            setSelectedFile(null);
            setIsModalOpen(true);
        }}
        addButtonText="Thêm mới"
      />

      {loading ? (
        <div className="flex justify-center p-20">
          <Loader2 className="animate-spin text-orange-500" size={40} />
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4">
          {items.map(it => (
            <MenuCard 
              key={it.id} 
              item={it} 
              onEdit={(item) => {
                setFormData({ id: item.id, name: item.name, price: item.price, categoryId: item.category?.id });
                setPreview(item.img || ''); 
                setIsModalOpen(true);
              }} 
              onDelete={() => handleDelete(it.id)} 
            />
          ))}
        </div>
      )}

      <MenuModal 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
        onSubmit={handleSubmit} 
        categories={categories}
        formData={formData} 
        setFormData={setFormData}
        preview={preview} 
        handleFileChange={(e) => {
          const f = e.target.files[0];
          if(f) { setSelectedFile(f); setPreview(URL.createObjectURL(f)); }
        }}
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

export default MenuManager;