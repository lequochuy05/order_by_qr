import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  Boxes,
  ClipboardList,
  History,
  Loader2,
  PackageCheck,
  PackagePlus,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Scale,
  SlidersHorizontal,
  X
} from 'lucide-react';

import { inventoryService } from '@modules/inventory-management/api/inventoryService.js';
import { menuItemService } from '@modules/menu-management/api/menuService.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { playNotificationSound } from '@modules/notifications/lib/notificationSound.js';

const numberValue = (value) => Number(value || 0);

const fmtQty = (value) => {
  const number = numberValue(value);
  return Number.isInteger(number)
    ? number.toLocaleString('vi-VN')
    : number.toLocaleString('vi-VN', { maximumFractionDigits: 3 });
};

const movementLabels = {
  STOCK_IN: 'Nhập kho',
  ADJUSTMENT: 'Kiểm kê',
  ORDER_RESERVE: 'Trừ theo đơn',
  ORDER_RELEASE: 'Hoàn theo đơn'
};

const InventoryManagement = () => {
  const [items, setItems] = useState([]);
  const [summary, setSummary] = useState(null);
  const [movements, setMovements] = useState([]);
  const [menuItems, setMenuItems] = useState([]);
  const [recipeItems, setRecipeItems] = useState([]);
  const [selectedMenuItemId, setSelectedMenuItemId] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [stockFilter, setStockFilter] = useState('ALL');
  const [activeTab, setActiveTab] = useState('items');
  const [loading, setLoading] = useState(false);
  const [inventoryModalOpen, setInventoryModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [stockAction, setStockAction] = useState(null);
  const [recipeOpen, setRecipeOpen] = useState(false);
  const [inventoryForm, setInventoryForm] = useState(defaultInventoryForm());
  const [stockForm, setStockForm] = useState({ quantity: '', quantityOnHand: '', note: '' });
  const { showSuccess, showError } = useStatusModal();

  const isMountedRef = React.useRef(true);
  const fetchSeqRef = React.useRef(0);

  const fetchData = useCallback(async (showLoading = false, { force = false } = {}) => {
    const fetchSeq = ++fetchSeqRef.current;
    if (showLoading) setLoading(true);
    try {
      const [inventoryData, movementData, menuData] = await Promise.all([
        inventoryService.getItems({ force }),
        inventoryService.getMovements({ force }),
        menuItemService.getAll(null, { force })
      ]);

      if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;

      setItems(inventoryData || []);
      setSummary(buildSummary(inventoryData || []));
      setMovements(movementData || []);
      setMenuItems(menuData || []);
      setSelectedMenuItemId((current) => current || (menuData?.length ? String(menuData[0].id) : ''));
    } catch (err) {
      if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
      showError(err);
    } finally {
      if (isMountedRef.current && fetchSeq === fetchSeqRef.current) {
        setLoading(false);
      }
    }
  }, [showError]);

  useEffect(() => {
    isMountedRef.current = true;
    fetchData(true);
    return () => {
      isMountedRef.current = false;
    };
  }, [fetchData]);

  useWebSocket('/topic/inventory', (message) => {
    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      playNotificationSound();
      fetchData(false, { force: true });
    }
  });

  const filteredItems = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    return items.filter((item) => {
      const matchesSearch = !term || item.name.toLowerCase().includes(term) || item.unit.toLowerCase().includes(term);
      const matchesFilter =
        stockFilter === 'ALL' ||
        (stockFilter === 'LOW' && item.lowStock) ||
        (stockFilter === 'OUT' && item.outOfStock) ||
        (stockFilter === 'INACTIVE' && item.active === false);
      return matchesSearch && matchesFilter;
    });
  }, [items, searchTerm, stockFilter]);

  const selectedMenuItem = menuItems.find((item) => String(item.id) === String(selectedMenuItemId));

  const openCreate = () => {
    setEditingItem(null);
    setInventoryForm(defaultInventoryForm());
    setInventoryModalOpen(true);
  };

  const openEdit = (item) => {
    setEditingItem(item);
    setInventoryForm({
      name: item.name,
      unit: item.unit,
      quantityOnHand: item.quantityOnHand,
      lowStockThreshold: item.lowStockThreshold,
      active: item.active !== false
    });
    setInventoryModalOpen(true);
  };

  const handleSaveInventory = async (e) => {
    e.preventDefault();
    const payload = {
      name: inventoryForm.name.trim(),
      unit: inventoryForm.unit.trim(),
      quantityOnHand: Number(inventoryForm.quantityOnHand),
      lowStockThreshold: Number(inventoryForm.lowStockThreshold),
      active: inventoryForm.active
    };

    try {
      if (editingItem?.id) {
        await inventoryService.updateItem(editingItem.id, payload);
        const quantityDelta = Number(inventoryForm.quantityOnHand) - Number(editingItem.quantityOnHand || 0);
        if (quantityDelta !== 0) {
          await inventoryService.adjust(editingItem.id, {
            quantityDelta,
            note: 'Cập nhật tồn kho từ form nguyên liệu'
          });
        }
        showSuccess('Đã cập nhật nguyên liệu');
      } else {
        await inventoryService.createItem(payload);
        showSuccess('Đã thêm nguyên liệu');
      }
      setInventoryModalOpen(false);
      setEditingItem(null);
      setInventoryForm(defaultInventoryForm());
      fetchData();
    } catch (err) {
      showError(err);
    }
  };

  const openStockAction = (item, type) => {
    setStockAction({ item, type });
    setStockForm({
      quantity: '',
      quantityOnHand: item.quantityOnHand,
      note: ''
    });
  };

  const handleStockSubmit = async (e) => {
    e.preventDefault();
    if (!stockAction?.item) return;
    try {
      if (stockAction.type === 'stock-in') {
        await inventoryService.stockIn(stockAction.item.id, {
          quantity: Number(stockForm.quantity),
          note: stockForm.note
        });
        showSuccess('Đã nhập kho');
      } else {
        const quantityDelta = Number(stockForm.quantityOnHand) - Number(stockAction.item.quantityOnHand || 0);
        if (quantityDelta === 0) {
          setStockAction(null);
          showSuccess('Tồn kho không thay đổi');
          return;
        }
        await inventoryService.adjust(stockAction.item.id, {
          quantityDelta,
          note: stockForm.note
        });
        showSuccess('Đã cập nhật kiểm kê');
      }
      setStockAction(null);
      fetchData();
    } catch (err) {
      showError(err);
    }
  };

  const openRecipe = async () => {
    setRecipeOpen(true);
    await loadRecipe(selectedMenuItemId);
  };

  const loadRecipe = async (menuItemId) => {
    if (!menuItemId) return;
    setSelectedMenuItemId(String(menuItemId));
    try {
      const data = await inventoryService.getRecipe(menuItemId);
      setRecipeItems((data || []).map((item) => ({
        inventoryItemId: String(item.inventoryItemId),
        quantityRequired: item.quantityRequired
      })));
    } catch (err) {
      showError(err);
    }
  };

  const addRecipeRow = () => {
    const firstAvailable = items.find((item) => !recipeItems.some((row) => Number(row.inventoryItemId) === item.id));
    if (!firstAvailable) return;
    setRecipeItems([...recipeItems, { inventoryItemId: String(firstAvailable.id), quantityRequired: 1 }]);
  };

  const saveRecipe = async (e) => {
    e.preventDefault();
    try {
      await inventoryService.updateRecipe(selectedMenuItemId, {
        items: recipeItems.map((item) => ({
          inventoryItemId: Number(item.inventoryItemId),
          quantityRequired: Number(item.quantityRequired)
        }))
      });
      showSuccess('Đã cập nhật định mức món');
      setRecipeOpen(false);
    } catch (err) {
      showError(err);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 p-6 space-y-6">
      <ManagementHeader
        searchPlaceholder="Tìm nguyên liệu..."
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        onAddClick={openCreate}
        addButtonText="Thêm nguyên liệu"
        addButtonIcon={PackagePlus}
        showFilter
        filterAllLabel="Tất cả tồn kho"
        filterValue={stockFilter}
        setFilterValue={setStockFilter}
        filterOptions={[
          { id: 'LOW', name: 'Sắp hết' },
          { id: 'OUT', name: 'Hết hàng' },
          { id: 'INACTIVE', name: 'Tạm ngưng' }
        ]}
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
        <SummaryCard icon={Boxes} label="Nguyên liệu" value={summary?.totalItems || 0} />
        <SummaryCard icon={PackageCheck} label="Đang dùng" value={summary?.activeItems || 0} tone="green" />
        <SummaryCard icon={AlertTriangle} label="Sắp hết" value={summary?.lowStockItems || 0} tone="amber" />
        <SummaryCard icon={Scale} label="Hết hàng" value={summary?.outOfStockItems || 0} tone="red" />
      </div>

      <div className="rounded-3xl border border-gray-100 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex items-center gap-2 rounded-2xl bg-gray-50 p-1">
            <TabButton active={activeTab === 'items'} icon={Boxes} label="Nguyên liệu" onClick={() => setActiveTab('items')} />
            <TabButton active={activeTab === 'history'} icon={History} label="Lịch sử kho" onClick={() => setActiveTab('history')} />
          </div>

          <div className="flex flex-col gap-3 md:flex-row md:items-center">
            <select
              value={selectedMenuItemId}
              onChange={(e) => {
                setSelectedMenuItemId(e.target.value);
                if (recipeOpen) loadRecipe(e.target.value);
              }}
              className="min-h-10 min-w-64 rounded-xl border border-gray-200 bg-gray-50 px-3 text-sm font-bold text-gray-700 outline-none focus:ring-2 focus:ring-orange-500"
            >
              {menuItems.map((item) => (
                <option key={item.id} value={item.id}>{item.name}</option>
              ))}
            </select>
            <button
              type="button"
              onClick={openRecipe}
              disabled={!selectedMenuItemId}
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-slate-800 px-4 text-sm font-black text-white transition-colors hover:bg-slate-700 disabled:opacity-50"
            >
              <ClipboardList size={16} /> Định mức
            </button>
            <button
              type="button"
              onClick={() => fetchData(true)}
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-gray-100 px-4 text-sm font-black text-gray-700 transition-colors hover:bg-gray-200"
            >
              <RefreshCw size={16} /> Đồng bộ
            </button>
          </div>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center p-20">
          <Loader2 className="animate-spin text-orange-500" size={40} />
        </div>
      ) : activeTab === 'items' ? (
        <InventoryGrid
          items={filteredItems}
          onEdit={openEdit}
          onStockIn={(item) => openStockAction(item, 'stock-in')}
          onAdjust={(item) => openStockAction(item, 'adjust')}
        />
      ) : (
        <MovementList movements={movements} />
      )}

      {inventoryModalOpen && (
        <InventoryModal
          item={editingItem}
          form={inventoryForm}
          setForm={setInventoryForm}
          onClose={() => { setInventoryModalOpen(false); setEditingItem(null); setInventoryForm(defaultInventoryForm()); }}
          onSubmit={handleSaveInventory}
        />
      )}

      {stockAction && (
        <StockModal
          action={stockAction}
          form={stockForm}
          setForm={setStockForm}
          onClose={() => setStockAction(null)}
          onSubmit={handleStockSubmit}
        />
      )}

      {recipeOpen && (
        <RecipeModal
          menuItems={menuItems}
          inventoryItems={items.filter((item) => item.active !== false)}
          selectedMenuItem={selectedMenuItem}
          selectedMenuItemId={selectedMenuItemId}
          onSelectMenuItem={loadRecipe}
          recipeItems={recipeItems}
          setRecipeItems={setRecipeItems}
          onAddRow={addRecipeRow}
          onClose={() => setRecipeOpen(false)}
          onSubmit={saveRecipe}
        />
      )}

      
    </div>
  );
};

const defaultInventoryForm = () => ({
  name: '',
  unit: '',
  quantityOnHand: 0,
  lowStockThreshold: 0,
  active: true
});

const buildSummary = (items) => ({
  totalItems: items.length,
  activeItems: items.filter((item) => item.active !== false).length,
  lowStockItems: items.filter((item) => item.lowStock).length,
  outOfStockItems: items.filter((item) => item.outOfStock && item.active !== false).length
});

const SummaryCard = ({ icon, label, value, tone = 'orange' }) => {
  const colors = {
    orange: 'bg-orange-50 text-orange-600',
    green: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
    red: 'bg-red-50 text-red-600'
  };
  return (
    <div className="rounded-3xl border border-gray-100 bg-white p-5 shadow-sm">
      <div className="flex items-center gap-4">
        <div className={`rounded-2xl p-3 ${colors[tone]}`}>
          {React.createElement(icon, { size: 22 })}
        </div>
        <div>
          <p className="text-[10px] font-black uppercase tracking-[0.16em] text-gray-400">{label}</p>
          <p className="text-3xl font-black text-gray-800">{value}</p>
        </div>
      </div>
    </div>
  );
};

const TabButton = ({ active, icon, label, onClick }) => (
  <button
    type="button"
    onClick={onClick}
    className={`inline-flex min-h-10 items-center gap-2 rounded-xl px-4 text-sm font-black transition-colors ${active ? 'bg-white text-orange-600 shadow-sm' : 'text-gray-500 hover:text-gray-800'}`}
  >
    {React.createElement(icon, { size: 16 })} {label}
  </button>
);

const InventoryGrid = ({ items, onEdit, onStockIn, onAdjust }) => {
  if (items.length === 0) {
    return (
      <div className="rounded-3xl border border-dashed bg-white py-20 text-center italic text-gray-400">
        <Boxes size={40} className="mx-auto mb-4 opacity-30" />
        Không tìm thấy nguyên liệu phù hợp hoặc chưa có dữ liệu.
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
      {items.map((item) => (
        <InventoryCard key={item.id} item={item} onEdit={onEdit} onStockIn={onStockIn} onAdjust={onAdjust} />
      ))}
    </div>
  );
};

const InventoryCard = ({ item, onEdit, onStockIn, onAdjust }) => {
  const ratio = item.lowStockThreshold > 0
    ? Math.min(100, Math.round((numberValue(item.quantityOnHand) / numberValue(item.lowStockThreshold)) * 100))
    : 100;

  return (
    <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 group hover:border-orange-500 hover:shadow-xl transition-all flex flex-col h-full relative overflow-hidden">
      {/* Trang trí góc thẻ */}
      <div className="absolute -top-6 -right-6 w-20 h-20 bg-orange-50 rounded-full group-hover:bg-orange-100 transition-colors" />

      <div className="mb-5 flex items-start justify-between gap-3 relative z-10">
        <div className="flex min-w-0 items-center gap-3">
          <div className="shrink-0 rounded-2xl bg-orange-50 p-3 text-orange-500">
            <Boxes size={22} />
          </div>
          <div className="min-w-0">
            <h3 className="truncate text-lg font-bold text-gray-800 group-hover:text-orange-600 transition-colors">{item.name}</h3>
            <p className="text-xs font-bold text-gray-400">{item.unit}</p>
          </div>
        </div>
        <StockBadge item={item} />
      </div>

      <div className="mb-4 relative z-10">
        <p className="text-[10px] font-black uppercase tracking-[0.18em] text-gray-400">Tồn hiện tại</p>
        <div className="mt-1 flex items-end gap-2">
          <span className="text-3xl font-black tracking-tight text-gray-900">{fmtQty(item.quantityOnHand)}</span>
          <span className="pb-1 text-sm font-bold text-gray-400">{item.unit}</span>
        </div>
      </div>

      <div className="mb-6 flex-grow relative z-10">
        <div className="mb-2 flex items-center justify-between text-xs font-bold">
          <span className="text-gray-400">Ngưỡng cảnh báo</span>
          <span className="text-gray-700">{fmtQty(item.lowStockThreshold)} {item.unit}</span>
        </div>
        <div className="h-2 rounded-full bg-gray-100">
          <div
            className={`h-2 rounded-full ${item.outOfStock ? 'bg-red-500' : item.lowStock ? 'bg-amber-400' : 'bg-emerald-500'}`}
            style={{ width: `${item.outOfStock ? 4 : Math.max(8, ratio)}%` }}
          />
        </div>
      </div>

      <div className="flex gap-2 mt-auto relative z-10">
        <ActionButton icon={PackagePlus} label="Nhập" onClick={() => onStockIn(item)} tone="orange" />
        <ActionButton icon={SlidersHorizontal} label="Kiểm kê" onClick={() => onAdjust(item)} tone="slate" />
        <button
          type="button"
          title="Sửa"
          onClick={() => onEdit(item)}
          className="flex flex-[0.3] h-11 items-center justify-center rounded-xl bg-blue-50 text-blue-600 transition-colors hover:bg-blue-600 hover:text-white border border-blue-100"
        >
          <Pencil size={18} />
        </button>
      </div>
    </div>
  );
};

const ActionButton = ({ icon, label, onClick, tone }) => {
  const classes = tone === 'orange'
    ? 'bg-orange-50 text-orange-600 hover:bg-orange-600 hover:text-white border border-orange-100'
    : 'bg-slate-50 text-slate-600 hover:bg-slate-800 hover:text-white border border-slate-200';
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex-1 flex h-11 items-center justify-center gap-2 rounded-xl text-sm font-bold transition-all ${classes}`}
    >
      {React.createElement(icon, { size: 16 })} {label}
    </button>
  );
};

const StockBadge = ({ item }) => {
  if (item.active === false) return <span className="shrink-0 rounded-full bg-gray-100 px-3 py-1 text-[10px] font-black uppercase text-gray-500">Tạm ngưng</span>;
  if (item.outOfStock) return <span className="shrink-0 rounded-full bg-red-100 px-3 py-1 text-[10px] font-black uppercase text-red-600">Hết hàng</span>;
  if (item.lowStock) return <span className="shrink-0 rounded-full bg-amber-100 px-3 py-1 text-[10px] font-black uppercase text-amber-700">Sắp hết</span>;
  return <span className="shrink-0 rounded-full bg-emerald-100 px-3 py-1 text-[10px] font-black uppercase text-emerald-700">Ổn định</span>;
};

const MovementList = ({ movements }) => (
  <div className="rounded-3xl border border-gray-100 bg-white shadow-sm">
    <div className="flex items-center gap-2 border-b border-gray-100 px-5 py-4 font-black text-gray-800">
      <History size={18} /> Lịch sử kho gần đây
    </div>
    <div className="divide-y divide-gray-100">
      {movements.map((movement) => (
        <div key={movement.id} className="flex flex-wrap items-center justify-between gap-4 px-5 py-4 text-sm">
          <div className="flex min-w-0 items-center gap-3">
            <div className="rounded-2xl bg-gray-50 p-3 text-gray-500">
              <ClipboardList size={18} />
            </div>
            <div className="min-w-0">
              <p className="truncate font-black text-gray-800">{movement.inventoryItemName}</p>
              <p className="text-xs font-bold text-gray-400">{movementLabels[movement.movementType] || movement.movementType}</p>
            </div>
          </div>
          <div className="text-right">
            <p className="font-black text-gray-800">{fmtQty(movement.quantity)} {movement.unit}</p>
            <p className="text-xs font-bold text-gray-400">{fmtQty(movement.quantityBefore)} → {fmtQty(movement.quantityAfter)}</p>
          </div>
        </div>
      ))}
      {movements.length === 0 && (
        <div className="py-20 text-center italic text-gray-400">
          <History size={40} className="mx-auto mb-4 opacity-30" />
          Chưa có lịch sử kho.
        </div>
      )}
    </div>
  </div>
);

const InventoryModal = ({ item, form, setForm, onClose, onSubmit }) => (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
    <form onSubmit={onSubmit} className="flex max-h-[90vh] w-full max-w-xl flex-col rounded-[2rem] bg-white shadow-2xl">
      <ModalHeader title={item?.id ? 'Sửa nguyên liệu' : 'Thêm nguyên liệu'} onClose={onClose} />
      <div className="space-y-5 overflow-y-auto p-8">
        <TextInput label="Tên nguyên liệu" value={form.name} onChange={(value) => setForm({ ...form, name: value })} required />
        <TextInput label="Đơn vị tính" value={form.unit} onChange={(value) => setForm({ ...form, unit: value })} required />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <TextInput type="number" min="0" step="0.001" label="Tồn hiện tại" value={form.quantityOnHand} onChange={(value) => setForm({ ...form, quantityOnHand: value })} required />
          <TextInput type="number" min="0" step="0.001" label="Ngưỡng cảnh báo" value={form.lowStockThreshold} onChange={(value) => setForm({ ...form, lowStockThreshold: value })} required />
        </div>
        <label className="flex items-center gap-3 rounded-2xl bg-gray-50 px-4 py-3 text-sm font-black text-gray-700">
          <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
          Đang sử dụng
        </label>
      </div>
      <ModalActions onClose={onClose} />
    </form>
  </div>
);

const StockModal = ({ action, form, setForm, onClose, onSubmit }) => (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
    <form onSubmit={onSubmit} className="w-full max-w-md rounded-[2rem] bg-white shadow-2xl">
      <ModalHeader title={action.type === 'stock-in' ? 'Nhập kho' : 'Kiểm kê'} onClose={onClose} />
      <div className="space-y-5 p-8">
        <div className="rounded-2xl bg-orange-50 px-4 py-3 text-sm font-bold text-orange-700">
          {action.item.name}: {fmtQty(action.item.quantityOnHand)} {action.item.unit}
        </div>
        {action.type === 'stock-in' ? (
          <TextInput type="number" min="0.001" step="0.001" label="Số lượng nhập thêm" value={form.quantity} onChange={(value) => setForm({ ...form, quantity: value })} required />
        ) : (
          <TextInput type="number" min="0" step="0.001" label="Tồn thực tế sau kiểm kê" value={form.quantityOnHand} onChange={(value) => setForm({ ...form, quantityOnHand: value })} required />
        )}
        <TextInput label="Ghi chú" value={form.note} onChange={(value) => setForm({ ...form, note: value })} />
      </div>
      <ModalActions onClose={onClose} />
    </form>
  </div>
);

const RecipeModal = ({ menuItems, inventoryItems, selectedMenuItem, selectedMenuItemId, onSelectMenuItem, recipeItems, setRecipeItems, onAddRow, onClose, onSubmit }) => (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
    <form onSubmit={onSubmit} className="flex max-h-[90vh] w-full max-w-3xl flex-col rounded-[2rem] bg-white shadow-2xl">
      <ModalHeader title="Định mức nguyên liệu" subtitle={selectedMenuItem?.name} onClose={onClose} />
      <div className="space-y-5 overflow-y-auto p-8">
        <label className="block">
          <span className="mb-2 ml-1 block text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">Món ăn</span>
          <select
            value={selectedMenuItemId}
            onChange={(e) => onSelectMenuItem(e.target.value)}
            className="w-full rounded-2xl border-2 border-transparent bg-gray-50 px-5 py-3.5 text-sm font-black text-gray-800 outline-none transition-all focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10"
          >
            {menuItems.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
        </label>

        <div className="space-y-3">
          {recipeItems.map((row, index) => {
            const selectedInventory = inventoryItems.find((item) => String(item.id) === String(row.inventoryItemId));
            return (
              <div key={`${row.inventoryItemId}-${index}`} className="grid grid-cols-1 gap-3 rounded-2xl border border-gray-100 bg-gray-50/50 p-3 md:grid-cols-[1fr_180px_44px] md:items-end">
                <label className="block">
                  <span className="mb-1 ml-1 block text-[10px] font-black uppercase tracking-[0.16em] text-gray-400">Nguyên liệu</span>
                  <select
                    value={row.inventoryItemId}
                    onChange={(e) => {
                      const next = [...recipeItems];
                      next[index] = { ...row, inventoryItemId: e.target.value };
                      setRecipeItems(next);
                    }}
                    className="w-full rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm font-bold outline-none focus:ring-2 focus:ring-orange-500"
                  >
                    {inventoryItems.map((item) => <option key={item.id} value={item.id}>{item.name} ({item.unit})</option>)}
                  </select>
                </label>
                <TextInput
                  type="number"
                  min="0.001"
                  step="0.001"
                  label={`Lượng / món${selectedInventory ? ` (${selectedInventory.unit})` : ''}`}
                  value={row.quantityRequired}
                  onChange={(value) => {
                    const next = [...recipeItems];
                    next[index] = { ...row, quantityRequired: value };
                    setRecipeItems(next);
                  }}
                  required
                />
                <button
                  type="button"
                  title="Xóa dòng"
                  onClick={() => setRecipeItems(recipeItems.filter((_, rowIndex) => rowIndex !== index))}
                  className="flex h-[46px] items-center justify-center rounded-xl bg-red-50 text-red-600 transition-colors hover:bg-red-600 hover:text-white"
                >
                  <X size={18} />
                </button>
              </div>
            );
          })}
        </div>

        {recipeItems.length === 0 && (
          <div className="rounded-3xl border border-dashed py-12 text-center italic text-gray-400">
            Chưa có nguyên liệu trong định mức món này.
          </div>
        )}

        <button type="button" onClick={onAddRow} className="inline-flex items-center gap-2 rounded-xl bg-orange-50 px-4 py-2.5 text-sm font-black text-orange-600 transition-colors hover:bg-orange-100">
          <Plus size={16} /> Thêm nguyên liệu
        </button>
      </div>
      <ModalActions onClose={onClose} />
    </form>
  </div>
);

const ModalHeader = ({ title, subtitle, onClose }) => (
  <div className="flex shrink-0 items-center justify-between border-b px-8 py-6">
    <div className="min-w-0">
      <h2 className="truncate text-xl font-black tracking-tight text-gray-800">{title}</h2>
      {subtitle && <p className="mt-1 truncate text-xs font-bold text-gray-400">{subtitle}</p>}
    </div>
    <button type="button" onClick={onClose} className="rounded-full p-2.5 text-gray-400 transition-all hover:bg-gray-100">
      <X size={20} />
    </button>
  </div>
);

const ModalActions = ({ onClose }) => (
  <div className="flex shrink-0 justify-end gap-3 border-t px-8 py-5">
    <button type="button" onClick={onClose} className="rounded-xl bg-gray-100 px-5 py-3 text-sm font-black text-gray-600 transition-colors hover:bg-gray-200">
      Hủy
    </button>
    <button type="submit" className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-5 py-3 text-sm font-black text-white shadow-lg shadow-orange-500/20 transition-colors hover:bg-orange-600">
      <Save size={16} /> Lưu
    </button>
  </div>
);

const TextInput = ({ label, value, onChange, type = 'text', ...props }) => (
  <label className="block">
    <span className="mb-2 ml-1 block text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">{label}</span>
    <input
      type={type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="w-full rounded-2xl border-2 border-transparent bg-gray-50 px-5 py-3.5 text-sm font-bold outline-none transition-all focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10"
      {...props}
    />
  </label>
);

export default InventoryManagement;
