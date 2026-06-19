import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
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
  X,
} from 'lucide-react';

import { inventoryService } from '@features/inventory-management/api/inventoryService.js';
import { menuItemService } from '@features/menu-management/api/menuService.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import SharedModal from '@shared/ui/SharedModal.jsx';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useDebouncedValue } from '@shared/hooks/useDebouncedValue.js';
import { playNotificationSound } from '@shared/lib/notificationSound.js';

const INVENTORY_UNITS = ['g', 'kg', 'ml', 'l'];

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
  ORDER_RELEASE: 'Hoàn theo đơn',
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
  const [movementsLoading, setMovementsLoading] = useState(false);
  const [menuItemsLoading, setMenuItemsLoading] = useState(false);
  const [recipeLoading, setRecipeLoading] = useState(false);
  const [inventoryModalOpen, setInventoryModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [stockAction, setStockAction] = useState(null);
  const [recipeOpen, setRecipeOpen] = useState(false);
  const [inventoryForm, setInventoryForm] = useState(defaultInventoryForm());
  const [inventoryErrors, setInventoryErrors] = useState({});
  const [stockForm, setStockForm] = useState({ quantity: '', quantityOnHand: '', note: '' });
  const [isInventorySubmitting, setIsInventorySubmitting] = useState(false);
  const [isStockSubmitting, setIsStockSubmitting] = useState(false);
  const [isRecipeSubmitting, setIsRecipeSubmitting] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const { showSuccess, showError } = useStatusModal();
  const debouncedSearchTerm = useDebouncedValue(searchTerm);

  const isMountedRef = React.useRef(true);
  const inventoryFetchSeqRef = React.useRef(0);
  const movementFetchSeqRef = React.useRef(0);
  const pageSize = 24;

  const fetchInventoryItems = useCallback(
    async (showLoading = false, { force = false } = {}) => {
      const fetchSeq = ++inventoryFetchSeqRef.current;
      if (showLoading) setLoading(true);
      try {
        const inventoryData = await inventoryService.getItemPage({
          page: currentPage,
          size: pageSize,
          keyword: debouncedSearchTerm.trim() || undefined,
          stockFilter,
          force,
        });
        if (!isMountedRef.current || fetchSeq !== inventoryFetchSeqRef.current) return;

        setItems(inventoryData.content || []);
        setTotalPages(inventoryData.totalPages || 0);
        setTotalElements(inventoryData.totalElements || 0);
      } catch (err) {
        if (!isMountedRef.current || fetchSeq !== inventoryFetchSeqRef.current) return;
        showError(err);
      } finally {
        if (isMountedRef.current && fetchSeq === inventoryFetchSeqRef.current) {
          setLoading(false);
        }
      }
    },
    [currentPage, debouncedSearchTerm, showError, stockFilter],
  );

  const fetchInventorySummary = useCallback(
    async ({ force = false } = {}) => {
      try {
        const data = await inventoryService.getSummary({ force });
        if (!isMountedRef.current) return;
        setSummary(data);
      } catch (err) {
        if (!isMountedRef.current) return;
        showError(err);
      }
    },
    [showError],
  );

  const fetchMovements = useCallback(
    async (showLoading = false, { force = false } = {}) => {
      const fetchSeq = ++movementFetchSeqRef.current;
      if (showLoading) setMovementsLoading(true);
      try {
        const movementData = await inventoryService.getMovements({ force });
        if (!isMountedRef.current || fetchSeq !== movementFetchSeqRef.current) return;

        setMovements(movementData || []);
      } catch (err) {
        if (!isMountedRef.current || fetchSeq !== movementFetchSeqRef.current) return;
        showError(err);
      } finally {
        if (isMountedRef.current && fetchSeq === movementFetchSeqRef.current) {
          setMovementsLoading(false);
        }
      }
    },
    [showError],
  );

  const ensureMenuItems = useCallback(
    async ({ force = false } = {}) => {
      if (!force && menuItems.length > 0) return menuItems;

      setMenuItemsLoading(true);
      try {
        const menuData = await menuItemService.getAll(undefined, { force });
        if (!isMountedRef.current) return menuData || [];

        setMenuItems(menuData || []);
        setSelectedMenuItemId(
          (current) => current || (menuData?.length ? String(menuData[0].id) : ''),
        );
        return menuData || [];
      } catch (err) {
        showError(err);
        return [];
      } finally {
        if (isMountedRef.current) {
          setMenuItemsLoading(false);
        }
      }
    },
    [menuItems, showError],
  );

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    fetchInventoryItems(true);
  }, [fetchInventoryItems]);

  useEffect(() => {
    fetchInventorySummary({ force: true });
  }, [fetchInventorySummary]);

  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearchTerm, stockFilter]);

  useEffect(() => {
    if (activeTab === 'history' && movements.length === 0) {
      fetchMovements(true);
    }
  }, [activeTab, fetchMovements, movements.length]);

  useWebSocket('/topic/inventory', (message) => {
    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      playNotificationSound();
      fetchInventoryItems(false, { force: true });
      fetchInventorySummary({ force: true });
      if (activeTab === 'history' || movements.length > 0) {
        fetchMovements(false, { force: true });
      }
    }
  });

  const selectedMenuItem = menuItems.find((item) => String(item.id) === String(selectedMenuItemId));

  const validateInventoryForm = useCallback(() => {
    const nextErrors = {};
    const name = inventoryForm.name?.trim() || '';
    const unit = inventoryForm.unit?.trim() || '';
    const quantityOnHand = Number(inventoryForm.quantityOnHand);
    const lowStockThreshold = Number(inventoryForm.lowStockThreshold);

    if (!name) {
      nextErrors.name = 'Tên nguyên liệu không được để trống';
    } else if (name.length < 2) {
      nextErrors.name = 'Tên nguyên liệu phải có ít nhất 2 ký tự';
    }

    if (!unit) {
      nextErrors.unit = 'Vui lòng chọn đơn vị tính';
    }

    if (String(inventoryForm.quantityOnHand ?? '').trim() === '') {
      nextErrors.quantityOnHand = 'Tồn hiện tại không được để trống';
    } else if (!Number.isFinite(quantityOnHand) || quantityOnHand < 0) {
      nextErrors.quantityOnHand = 'Tồn hiện tại không hợp lệ';
    }

    if (String(inventoryForm.lowStockThreshold ?? '').trim() === '') {
      nextErrors.lowStockThreshold = 'Ngưỡng cảnh báo không được để trống';
    } else if (!Number.isFinite(lowStockThreshold) || lowStockThreshold < 0) {
      nextErrors.lowStockThreshold = 'Ngưỡng cảnh báo không hợp lệ';
    }

    setInventoryErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }, [inventoryForm]);

  const closeInventoryModal = useCallback(() => {
    if (isInventorySubmitting) return;
    setInventoryModalOpen(false);
    setEditingItem(null);
    setInventoryForm(defaultInventoryForm());
    setInventoryErrors({});
  }, [isInventorySubmitting]);

  const refreshInventoryView = useCallback(
    (showLoading = true) => {
      fetchInventoryItems(showLoading, { force: true });
      fetchInventorySummary({ force: true });
      if (activeTab === 'history') {
        fetchMovements(showLoading, { force: true });
      }
      if (menuItems.length > 0) {
        ensureMenuItems({ force: true });
      }
    },
    [
      activeTab,
      ensureMenuItems,
      fetchInventoryItems,
      fetchInventorySummary,
      fetchMovements,
      menuItems.length,
    ],
  );

  const openCreate = () => {
    setEditingItem(null);
    setInventoryForm(defaultInventoryForm());
    setInventoryErrors({});
    setInventoryModalOpen(true);
  };

  const openEdit = (item) => {
    setEditingItem(item);
    setInventoryForm({
      name: item.name || '',
      unit: item.unit || INVENTORY_UNITS[0],
      quantityOnHand: item.quantityOnHand,
      lowStockThreshold: item.lowStockThreshold,
      active: item.active !== false,
    });
    setInventoryErrors({});
    setInventoryModalOpen(true);
  };

  const handleSaveInventory = async (e) => {
    e.preventDefault();
    if (isInventorySubmitting) return;
    if (!validateInventoryForm()) return;

    const payload = {
      name: inventoryForm.name.trim(),
      unit: inventoryForm.unit.trim(),
      quantityOnHand: Number(inventoryForm.quantityOnHand),
      lowStockThreshold: Number(inventoryForm.lowStockThreshold),
      active: inventoryForm.active,
    };

    setIsInventorySubmitting(true);
    try {
      if (editingItem?.id) {
        await inventoryService.updateItem(editingItem.id, payload);
        const quantityDelta =
          Number(inventoryForm.quantityOnHand) - Number(editingItem.quantityOnHand || 0);
        if (quantityDelta !== 0) {
          await inventoryService.adjust(editingItem.id, {
            quantityDelta,
            note: 'Cập nhật tồn kho từ form nguyên liệu',
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
      setInventoryErrors({});
      fetchInventoryItems(false, { force: true });
      fetchInventorySummary({ force: true });
      if (movements.length > 0) {
        fetchMovements(false, { force: true });
      }
    } catch (err) {
      showError(err);
    } finally {
      setIsInventorySubmitting(false);
    }
  };

  const openStockAction = (item, type) => {
    setStockAction({ item, type });
    setStockForm({
      quantity: '',
      quantityOnHand: item.quantityOnHand,
      note: '',
    });
  };

  const handleStockSubmit = async (e) => {
    e.preventDefault();
    if (!stockAction?.item || isStockSubmitting) return;
    setIsStockSubmitting(true);
    try {
      if (stockAction.type === 'stock-in') {
        await inventoryService.stockIn(stockAction.item.id, {
          quantity: Number(stockForm.quantity),
          note: stockForm.note,
        });
        showSuccess('Đã nhập kho');
      } else {
        const quantityDelta =
          Number(stockForm.quantityOnHand) - Number(stockAction.item.quantityOnHand || 0);
        if (quantityDelta === 0) {
          setStockAction(null);
          showSuccess('Tồn kho không thay đổi');
          return;
        }
        await inventoryService.adjust(stockAction.item.id, {
          quantityDelta,
          note: stockForm.note,
        });
        showSuccess('Đã cập nhật kiểm kê');
      }
      setStockAction(null);
      fetchInventoryItems(false, { force: true });
      fetchInventorySummary({ force: true });
      if (activeTab === 'history' || movements.length > 0) {
        fetchMovements(false, { force: true });
      }
    } catch (err) {
      showError(err);
    } finally {
      setIsStockSubmitting(false);
    }
  };

  const openRecipe = async () => {
    const menuData = await ensureMenuItems();
    const targetMenuItemId = selectedMenuItemId || (menuData.length ? String(menuData[0].id) : '');
    if (!targetMenuItemId) {
      showError('Chưa có món ăn để thiết lập định mức.');
      return;
    }
    setRecipeOpen(true);
    await loadRecipe(targetMenuItemId);
  };

  const loadRecipe = async (menuItemId) => {
    if (!menuItemId) return;
    setSelectedMenuItemId(String(menuItemId));
    setRecipeLoading(true);
    try {
      const data = await inventoryService.getRecipe(menuItemId);
      setRecipeItems(
        (data || []).map((item) => ({
          inventoryItemId: String(item.inventoryItemId),
          quantityRequired: item.quantityRequired,
        })),
      );
    } catch (err) {
      showError(err);
    } finally {
      setRecipeLoading(false);
    }
  };

  const addRecipeRow = () => {
    const firstAvailable = items.find(
      (item) => !recipeItems.some((row) => Number(row.inventoryItemId) === item.id),
    );
    if (!firstAvailable) return;
    setRecipeItems([
      ...recipeItems,
      { inventoryItemId: String(firstAvailable.id), quantityRequired: 1 },
    ]);
  };

  const saveRecipe = async (e) => {
    e.preventDefault();
    if (isRecipeSubmitting) return;
    setIsRecipeSubmitting(true);
    try {
      await inventoryService.updateRecipe(selectedMenuItemId, {
        items: recipeItems.map((item) => ({
          inventoryItemId: Number(item.inventoryItemId),
          quantityRequired: Number(item.quantityRequired),
        })),
      });
      showSuccess('Đã cập nhật định mức món');
      setRecipeOpen(false);
    } catch (err) {
      showError(err);
    } finally {
      setIsRecipeSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen w-full min-w-0 space-y-4 bg-slate-50 p-0 sm:space-y-6 sm:p-3 lg:p-6">
      <ManagementHeader
        searchPlaceholder="Tìm nguyên liệu..."
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        onAddClick={openCreate}
        addButtonText="Thêm nguyên liệu"
        addButtonIcon={Plus}
        showFilter
        filterAllLabel="Tất cả tồn kho"
        filterValue={stockFilter}
        setFilterValue={setStockFilter}
        filterOptions={[
          { id: 'LOW', name: 'Sắp hết' },
          { id: 'OUT', name: 'Hết hàng' },
          { id: 'INACTIVE', name: 'Tạm ngưng' },
        ]}
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
        <SummaryCard icon={Boxes} label="Nguyên liệu" value={summary?.totalItems || 0} />
        <SummaryCard
          icon={PackageCheck}
          label="Đang dùng"
          value={summary?.activeItems || 0}
          tone="green"
        />
        <SummaryCard
          icon={AlertTriangle}
          label="Sắp hết"
          value={summary?.lowStockItems || 0}
          tone="amber"
        />
        <SummaryCard
          icon={Scale}
          label="Hết hàng"
          value={summary?.outOfStockItems || 0}
          tone="red"
        />
      </div>

      <div className="rounded-3xl border border-gray-100 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="grid min-w-0 grid-cols-2 gap-1 rounded-2xl bg-gray-50 p-1 sm:flex sm:items-center sm:gap-2">
            <TabButton
              active={activeTab === 'items'}
              icon={Boxes}
              label="Nguyên liệu"
              onClick={() => setActiveTab('items')}
            />
            <TabButton
              active={activeTab === 'history'}
              icon={History}
              label="Lịch sử kho"
              onClick={() => setActiveTab('history')}
            />
          </div>

          <div className="flex min-w-0 flex-col gap-3 md:flex-row md:items-center">
            <select
              value={selectedMenuItemId}
              onFocus={() => ensureMenuItems()}
              onChange={(e) => {
                setSelectedMenuItemId(e.target.value);
                if (recipeOpen) loadRecipe(e.target.value);
              }}
              disabled={menuItemsLoading}
              className="min-h-10 w-full min-w-0 rounded-xl border border-gray-200 bg-gray-50 px-3 text-sm font-bold text-gray-700 outline-none focus:ring-2 focus:ring-orange-500 disabled:cursor-wait disabled:opacity-60 md:w-64"
            >
              {menuItemsLoading && <option value="">Đang tải món...</option>}
              {!menuItemsLoading && menuItems.length === 0 && (
                <option value="">Chọn món để định mức</option>
              )}
              {menuItems.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </select>
            <button
              type="button"
              onClick={openRecipe}
              disabled={menuItemsLoading}
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-slate-800 px-4 text-sm font-black text-white transition-colors hover:bg-slate-700 disabled:opacity-50"
            >
              {menuItemsLoading ? (
                <Loader2 size={16} className="animate-spin" />
              ) : (
                <ClipboardList size={16} />
              )}{' '}
              Định mức
            </button>
            <button
              type="button"
              onClick={() => refreshInventoryView(true)}
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
          items={items}
          onEdit={openEdit}
          onStockIn={(item) => openStockAction(item, 'stock-in')}
          onAdjust={(item) => openStockAction(item, 'adjust')}
        />
      ) : movementsLoading ? (
        <div className="flex justify-center p-20">
          <Loader2 className="animate-spin text-orange-500" size={40} />
        </div>
      ) : (
        <MovementList movements={movements} />
      )}

      {activeTab === 'items' && !loading && (
        <PaginationControls
          currentPage={currentPage}
          totalPages={totalPages}
          totalElements={totalElements}
          itemLabel="nguyên liệu"
          loading={loading}
          onPageChange={setCurrentPage}
        />
      )}

      {inventoryModalOpen && (
        <InventoryModal
          item={editingItem}
          form={inventoryForm}
          setForm={setInventoryForm}
          errors={inventoryErrors}
          setErrors={setInventoryErrors}
          isSubmitting={isInventorySubmitting}
          onClose={closeInventoryModal}
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
          isSubmitting={isStockSubmitting}
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
          isSubmitting={isRecipeSubmitting}
          loading={recipeLoading}
        />
      )}
    </div>
  );
};

const defaultInventoryForm = () => ({
  name: '',
  unit: INVENTORY_UNITS[0],
  quantityOnHand: 0,
  lowStockThreshold: 0,
  active: true,
});

const SummaryCard = ({ icon, label, value, tone = 'orange' }) => {
  const colors = {
    orange: 'bg-orange-50 text-orange-600',
    green: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
    red: 'bg-red-50 text-red-600',
  };
  return (
    <div className="min-w-0 rounded-3xl border border-gray-100 bg-white p-4 shadow-sm sm:p-5">
      <div className="flex items-center gap-4">
        <div className={`rounded-2xl p-3 ${colors[tone]}`}>
          {React.createElement(icon, { size: 22 })}
        </div>
        <div>
          <p className="text-[10px] font-black uppercase tracking-[0.16em] text-gray-400">
            {label}
          </p>
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
    className={`inline-flex min-h-10 min-w-0 items-center justify-center gap-2 rounded-xl px-3 text-sm font-black transition-colors sm:px-4 ${active ? 'bg-white text-orange-600 shadow-sm' : 'text-gray-500 hover:text-gray-800'}`}
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
    <div className="grid min-w-0 grid-cols-1 gap-4 sm:gap-5 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
      {items.map((item) => (
        <InventoryCard
          key={item.id}
          item={item}
          onEdit={onEdit}
          onStockIn={onStockIn}
          onAdjust={onAdjust}
        />
      ))}
    </div>
  );
};

const InventoryCard = ({ item, onEdit, onStockIn, onAdjust }) => {
  const ratio =
    item.lowStockThreshold > 0
      ? Math.min(
          100,
          Math.round(
            (numberValue(item.quantityOnHand) / numberValue(item.lowStockThreshold)) * 100,
          ),
        )
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
            <h3 className="truncate text-lg font-bold text-gray-800 group-hover:text-orange-600 transition-colors">
              {item.name}
            </h3>
            <p className="text-xs font-bold text-gray-400">{item.unit}</p>
          </div>
        </div>
        <StockBadge item={item} />
      </div>

      <div className="mb-4 relative z-10">
        <p className="text-[10px] font-black uppercase tracking-[0.18em] text-gray-400">
          Tồn hiện tại
        </p>
        <div className="mt-1 flex items-end gap-2">
          <span className="text-3xl font-black tracking-tight text-gray-900">
            {fmtQty(item.quantityOnHand)}
          </span>
          <span className="pb-1 text-sm font-bold text-gray-400">{item.unit}</span>
        </div>
      </div>

      <div className="mb-6 flex-grow relative z-10">
        <div className="mb-2 flex items-center justify-between text-xs font-bold">
          <span className="text-gray-400">Ngưỡng cảnh báo</span>
          <span className="text-gray-700">
            {fmtQty(item.lowStockThreshold)} {item.unit}
          </span>
        </div>
        <div className="h-2 rounded-full bg-gray-100">
          <div
            className={`h-2 rounded-full ${item.outOfStock ? 'bg-red-500' : item.lowStock ? 'bg-amber-400' : 'bg-emerald-500'}`}
            style={{ width: `${item.outOfStock ? 4 : Math.max(8, ratio)}%` }}
          />
        </div>
      </div>

      <div className="flex gap-2 mt-auto relative z-10">
        <ActionButton
          icon={PackagePlus}
          label="Nhập"
          onClick={() => onStockIn(item)}
          tone="orange"
        />
        <ActionButton
          icon={SlidersHorizontal}
          label="Kiểm kê"
          onClick={() => onAdjust(item)}
          tone="slate"
        />
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
  const classes =
    tone === 'orange'
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
  if (item.active === false)
    return (
      <span className="shrink-0 rounded-full bg-gray-100 px-3 py-1 text-[10px] font-black uppercase text-gray-500">
        Tạm ngưng
      </span>
    );
  if (item.outOfStock)
    return (
      <span className="shrink-0 rounded-full bg-red-100 px-3 py-1 text-[10px] font-black uppercase text-red-600">
        Hết hàng
      </span>
    );
  if (item.lowStock)
    return (
      <span className="shrink-0 rounded-full bg-amber-100 px-3 py-1 text-[10px] font-black uppercase text-amber-700">
        Sắp hết
      </span>
    );
  return (
    <span className="shrink-0 rounded-full bg-emerald-100 px-3 py-1 text-[10px] font-black uppercase text-emerald-700">
      Ổn định
    </span>
  );
};

const MovementList = ({ movements }) => (
  <div className="rounded-3xl border border-gray-100 bg-white shadow-sm">
    <div className="flex items-center gap-2 border-b border-gray-100 px-5 py-4 font-black text-gray-800">
      <History size={18} /> Lịch sử kho gần đây
    </div>
    <div className="divide-y divide-gray-100">
      {movements.map((movement) => (
        <div
          key={movement.id}
          className="flex flex-wrap items-center justify-between gap-4 px-5 py-4 text-sm"
        >
          <div className="flex min-w-0 items-center gap-3">
            <div className="rounded-2xl bg-gray-50 p-3 text-gray-500">
              <ClipboardList size={18} />
            </div>
            <div className="min-w-0">
              <p className="truncate font-black text-gray-800">{movement.inventoryItemName}</p>
              <p className="text-xs font-bold text-gray-400">
                {movementLabels[movement.movementType] || movement.movementType}
              </p>
            </div>
          </div>
          <div className="text-right">
            <p className="font-black text-gray-800">
              {fmtQty(movement.quantity)} {movement.unit}
            </p>
            <p className="text-xs font-bold text-gray-400">
              {fmtQty(movement.quantityBefore)} → {fmtQty(movement.quantityAfter)}
            </p>
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

const InventoryModal = ({
  item,
  form,
  setForm,
  errors = {},
  setErrors,
  onClose,
  onSubmit,
  isSubmitting = false,
}) => {
  const unitOptions = getInventoryUnitOptions(form.unit);
  const isChanged = useMemo(() => {
    if (!item?.id) return true;
    if ((form.name || '') !== (item.name || '')) return true;
    if ((form.unit || '') !== (item.unit || '')) return true;
    if (Number(form.quantityOnHand || 0) !== Number(item.quantityOnHand || 0)) return true;
    if (Number(form.lowStockThreshold || 0) !== Number(item.lowStockThreshold || 0)) return true;
    return (form.active !== false) !== (item.active !== false);
  }, [form, item]);

  const updateField = (field, value) => {
    setForm({ ...form, [field]: value });
    if (errors[field]) {
      setErrors({ ...errors, [field]: null });
    }
  };

  return (
    <SharedModal isOpen onClose={onClose} className="max-w-xl !p-0">
      <ModalHeader
        title={item?.id ? 'Sửa nguyên liệu' : 'Thêm nguyên liệu'}
        onClose={onClose}
        disabled={isSubmitting}
      />

      <form
        id="inventoryForm"
        onSubmit={onSubmit}
        className="space-y-6 overflow-y-auto p-8 custom-scrollbar"
      >
        <TextInput
          label="Tên nguyên liệu"
          value={form.name}
          onChange={(value) => updateField('name', value)}
          error={errors.name}
          placeholder="Ví dụ: Bột mì, Đường, Sữa tươi..."
          required
        />

        <div>
          <label className="mb-2 ml-1 block text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">
            Đơn vị tính <span className="text-red-500">*</span>
          </label>
          <select
            value={form.unit}
            onChange={(e) => updateField('unit', e.target.value)}
            className={`w-full rounded-2xl border-2 bg-gray-50 px-5 py-3.5 text-sm font-bold outline-none transition-all ${
              errors.unit
                ? 'border-red-500 ring-red-50'
                : 'border-transparent focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10'
            }`}
          >
            {unitOptions.map((unit) => (
              <option key={unit} value={unit}>
                {unit}
              </option>
            ))}
          </select>
          <FieldError message={errors.unit} />
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <TextInput
            type="number"
            min="0"
            step="0.001"
            label="Tồn hiện tại"
            value={form.quantityOnHand}
            onChange={(value) => updateField('quantityOnHand', value)}
            error={errors.quantityOnHand}
            required
          />
          <TextInput
            type="number"
            min="0"
            step="0.001"
            label="Ngưỡng cảnh báo"
            value={form.lowStockThreshold}
            onChange={(value) => updateField('lowStockThreshold', value)}
            error={errors.lowStockThreshold}
            required
          />
        </div>

        <label className="flex h-[52px] w-full cursor-pointer items-center gap-3 rounded-2xl border-2 border-transparent bg-gray-50 px-5 transition-all hover:bg-gray-100">
          <input
            type="checkbox"
            className="h-5 w-5 cursor-pointer rounded-md border-gray-200 text-orange-500 focus:ring-orange-200"
            checked={form.active}
            onChange={(e) => updateField('active', e.target.checked)}
          />
          <span className="text-xs font-black uppercase tracking-tight text-gray-600">
            Đang sử dụng
          </span>
        </label>
      </form>

      <ModalActions
        formId="inventoryForm"
        onClose={onClose}
        submitLabel={item?.id ? 'Lưu thay đổi' : 'Thêm nguyên liệu'}
        isSubmitting={isSubmitting}
        disabled={!isChanged}
      />
    </SharedModal>
  );
};

const StockModal = ({ action, form, setForm, onClose, onSubmit, isSubmitting = false }) => (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
    <form onSubmit={onSubmit} className="w-full max-w-md rounded-[2rem] bg-white shadow-2xl">
      <ModalHeader
        title={action.type === 'stock-in' ? 'Nhập kho' : 'Kiểm kê'}
        onClose={onClose}
        disabled={isSubmitting}
      />
      <div className="space-y-5 p-8">
        <div className="rounded-2xl bg-orange-50 px-4 py-3 text-sm font-bold text-orange-700">
          {action.item.name}: {fmtQty(action.item.quantityOnHand)} {action.item.unit}
        </div>
        {action.type === 'stock-in' ? (
          <TextInput
            type="number"
            min="0.001"
            step="0.001"
            label="Số lượng nhập thêm"
            value={form.quantity}
            onChange={(value) => setForm({ ...form, quantity: value })}
            required
          />
        ) : (
          <TextInput
            type="number"
            min="0"
            step="0.001"
            label="Tồn thực tế sau kiểm kê"
            value={form.quantityOnHand}
            onChange={(value) => setForm({ ...form, quantityOnHand: value })}
            required
          />
        )}
        <TextInput
          label="Ghi chú"
          value={form.note}
          onChange={(value) => setForm({ ...form, note: value })}
        />
      </div>
      <ModalActions onClose={onClose} isSubmitting={isSubmitting} />
    </form>
  </div>
);

const RecipeModal = ({
  menuItems,
  inventoryItems,
  selectedMenuItem,
  selectedMenuItemId,
  onSelectMenuItem,
  recipeItems,
  setRecipeItems,
  onAddRow,
  onClose,
  onSubmit,
  isSubmitting = false,
  loading = false,
}) => (
  <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
    <form
      onSubmit={onSubmit}
      className="flex max-h-[90vh] w-full max-w-3xl flex-col rounded-[2rem] bg-white shadow-2xl"
    >
      <ModalHeader
        title="Định mức nguyên liệu"
        subtitle={selectedMenuItem?.name}
        onClose={onClose}
        disabled={isSubmitting}
      />
      <div className="space-y-5 overflow-y-auto p-8">
        <label className="block">
          <span className="mb-2 ml-1 block text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">
            Món ăn
          </span>
          <select
            value={selectedMenuItemId}
            onChange={(e) => onSelectMenuItem(e.target.value)}
            className="w-full rounded-2xl border-2 border-transparent bg-gray-50 px-5 py-3.5 text-sm font-black text-gray-800 outline-none transition-all focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10"
          >
            {menuItems.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
        </label>

        {loading ? (
          <div className="flex justify-center rounded-3xl border border-dashed py-12 text-orange-500">
            <Loader2 size={28} className="animate-spin" />
          </div>
        ) : (
          <div className="space-y-3">
            {recipeItems.map((row, index) => {
              const selectedInventory = inventoryItems.find(
                (item) => String(item.id) === String(row.inventoryItemId),
              );
              return (
                <div
                  key={`${row.inventoryItemId}-${index}`}
                  className="grid grid-cols-1 gap-3 rounded-2xl border border-gray-100 bg-gray-50/50 p-3 md:grid-cols-[1fr_180px_44px] md:items-end"
                >
                  <label className="block">
                    <span className="mb-1 ml-1 block text-[10px] font-black uppercase tracking-[0.16em] text-gray-400">
                      Nguyên liệu
                    </span>
                    <select
                      value={row.inventoryItemId}
                      onChange={(e) => {
                        const next = [...recipeItems];
                        next[index] = { ...row, inventoryItemId: e.target.value };
                        setRecipeItems(next);
                      }}
                      className="w-full rounded-xl border border-gray-200 bg-white px-4 py-3 text-sm font-bold outline-none focus:ring-2 focus:ring-orange-500"
                    >
                      {inventoryItems.map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.name} ({item.unit})
                        </option>
                      ))}
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
                    onClick={() =>
                      setRecipeItems(recipeItems.filter((_, rowIndex) => rowIndex !== index))
                    }
                    className="flex h-[46px] items-center justify-center rounded-xl bg-red-50 text-red-600 transition-colors hover:bg-red-600 hover:text-white"
                  >
                    <X size={18} />
                  </button>
                </div>
              );
            })}
          </div>
        )}

        {!loading && recipeItems.length === 0 && (
          <div className="rounded-3xl border border-dashed py-12 text-center italic text-gray-400">
            Chưa có nguyên liệu trong định mức món này.
          </div>
        )}

        <button
          type="button"
          onClick={onAddRow}
          disabled={loading || isSubmitting}
          className="inline-flex items-center gap-2 rounded-xl bg-orange-50 px-4 py-2.5 text-sm font-black text-orange-600 transition-colors hover:bg-orange-100 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Plus size={16} /> Thêm nguyên liệu
        </button>
      </div>
      <ModalActions onClose={onClose} isSubmitting={isSubmitting} />
    </form>
  </div>
);

const getInventoryUnitOptions = (currentUnit) => {
  const normalizedUnit = currentUnit?.trim();
  if (normalizedUnit && !INVENTORY_UNITS.includes(normalizedUnit)) {
    return [...INVENTORY_UNITS, normalizedUnit];
  }
  return INVENTORY_UNITS;
};

const ModalHeader = ({ title, subtitle, onClose, disabled = false }) => (
  <div className="flex shrink-0 items-center justify-between border-b px-8 py-6">
    <div className="min-w-0">
      <h2 className="truncate text-xl font-black tracking-tight text-gray-800">{title}</h2>
      {subtitle && (
        <p className="mt-0.5 truncate text-[10px] font-bold uppercase tracking-widest text-gray-400">
          {subtitle}
        </p>
      )}
    </div>
    <button
      type="button"
      onClick={onClose}
      disabled={disabled}
      className="rounded-full p-2.5 text-gray-400 transition-all hover:bg-gray-100 active:scale-90 disabled:cursor-not-allowed disabled:opacity-50"
    >
      <X size={20} />
    </button>
  </div>
);

const ModalActions = ({
  onClose,
  formId,
  submitLabel = 'Lưu',
  isSubmitting = false,
  disabled = false,
}) => (
  <div className="flex shrink-0 gap-4 rounded-b-[2rem] border-t bg-gray-50/50 px-8 py-6">
    <button
      type="button"
      onClick={onClose}
      disabled={isSubmitting}
      className="flex-1 rounded-2xl border border-gray-200 bg-white py-4 text-[11px] font-black uppercase tracking-[0.1em] text-gray-600 shadow-sm transition-all hover:bg-gray-100 active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
    >
      Hủy bỏ
    </button>
    <button
      form={formId}
      type="submit"
      disabled={isSubmitting || disabled}
      className={`flex-[2] rounded-2xl py-4 text-[11px] font-black uppercase tracking-[0.2em] shadow-xl transition-all active:scale-95 ${
        isSubmitting || disabled
          ? 'cursor-not-allowed bg-gray-300 text-gray-500 shadow-none'
          : 'bg-orange-500 text-white shadow-orange-200 hover:bg-orange-600'
      }`}
    >
      <div className="flex items-center justify-center gap-2">
        {isSubmitting ? <Loader2 size={16} className="animate-spin" /> : <span>{submitLabel}</span>}
      </div>
    </button>
  </div>
);

const FieldError = ({ message }) => {
  if (!message) return null;
  return (
    <p className="mt-1.5 flex items-center gap-1.5 text-[11px] font-bold text-red-500 animate-in slide-in-from-top-1">
      <AlertCircle size={12} /> {message}
    </p>
  );
};

const TextInput = ({
  label,
  value,
  onChange,
  type = 'text',
  error,
  required = false,
  ...props
}) => (
  <label className="block">
    <span className="mb-2 ml-1 block text-[10px] font-black uppercase tracking-[0.2em] text-gray-400">
      {label} {required && <span className="text-red-500">*</span>}
    </span>
    <input
      type={type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      required={required}
      className={`w-full rounded-2xl border-2 bg-gray-50 px-5 py-3.5 text-sm font-bold outline-none transition-all ${
        error
          ? 'border-red-500 ring-red-50'
          : 'border-transparent focus:border-orange-500 focus:bg-white focus:ring-4 focus:ring-orange-500/10'
      }`}
      {...props}
    />
    <FieldError message={error} />
  </label>
);

export default InventoryManagement;
