import { useState } from 'react';
import { Crown, Loader2, Lock, Plus } from 'lucide-react';
import { useAuth } from '@entities/session';
import {
  PLAN_FEATURES,
  UpgradeProModal,
  useSubscription,
} from '@entities/subscription';

import { STOCK_FILTER_OPTIONS } from '../lib/inventoryConstants.js';
import useInventoryManagement from '../model/useInventoryManagement.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import InventoryGrid from './components/InventoryGrid.jsx';
import AiRestockPanel from './components/AiRestockPanel.jsx';
import InventorySummaryCards from './components/InventorySummaryCards.jsx';
import InventoryToolbar from './components/InventoryToolbar.jsx';
import MovementList from './components/MovementList.jsx';
import InventoryModal from './modals/InventoryModal.jsx';
import InvoiceOcrModal from './modals/InvoiceOcrModal.jsx';
import RecipeModal from './modals/RecipeModal.jsx';
import StockModal from './modals/StockModal.jsx';

const LoadingState = () => (
  <div className="flex justify-center p-20">
    <Loader2 className="animate-spin text-indigo-600 dark:text-indigo-400" size={40} />
  </div>
);

const InventoryManagement = () => {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const isManager = isAdmin || user?.role === 'MANAGER';
  const inventory = useInventoryManagement();
  const { hasFeature } = useSubscription();
  const [upgradeFeature, setUpgradeFeature] = useState(null);

  const canUseInventoryBom = hasFeature(PLAN_FEATURES.INVENTORY_BOM);
  const canUseAiOps = hasFeature(PLAN_FEATURES.AI_OPERATIONS_SUITE);

  const guardBom = (fn) => (...args) => {
    if (!canUseInventoryBom) {
      setUpgradeFeature(PLAN_FEATURES.INVENTORY_BOM);
      return;
    }
    fn?.(...args);
  };

  const guardAiOps = (fn) => (...args) => {
    if (!canUseAiOps) {
      setUpgradeFeature(PLAN_FEATURES.AI_OPERATIONS_SUITE);
      return;
    }
    fn?.(...args);
  };

  return (
    <div className="w-full min-w-0 space-y-4 p-0 sm:space-y-6 sm:p-3 lg:p-6">
      {!canUseInventoryBom && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-amber-300/80 bg-amber-50/80 px-4 py-3 text-xs dark:border-amber-500/30 dark:bg-amber-950/35">
          <div className="flex items-center gap-2.5 text-amber-900 dark:text-amber-200">
            <Lock size={15} className="shrink-0 text-amber-600 dark:text-amber-400" />
            <span className="font-semibold">
              Gói Miễn phí giữ dữ liệu kho ở chế độ chỉ đọc. Nâng cấp QROS Pro để nhập kho, kiểm kê, dùng AI Vision và chỉnh định mức BOM.
            </span>
          </div>
          <button
            type="button"
            onClick={() => setUpgradeFeature(PLAN_FEATURES.INVENTORY_BOM)}
            className="inline-flex h-8 items-center gap-1.5 rounded-xl bg-amber-500 px-3 text-xs font-black text-slate-950 transition hover:bg-amber-400"
          >
            <Crown size={13} />
            <span>Mở khóa Pro</span>
          </button>
        </div>
      )}

      <ManagementHeader
        searchPlaceholder="Tìm nguyên liệu..."
        searchTerm={inventory.searchTerm}
        setSearchTerm={inventory.setSearchTerm}
        onAddClick={isManager ? guardBom(inventory.openCreate) : undefined}
        addButtonText={isManager ? 'Thêm nguyên liệu' : undefined}
        addButtonIcon={isManager ? Plus : undefined}
        hideAddButton={!isManager}
        showFilter
        filterAllLabel="Tất cả tồn kho"
        filterValue={inventory.stockFilter}
        setFilterValue={inventory.setStockFilter}
        filterOptions={STOCK_FILTER_OPTIONS}
      />

      <InventorySummaryCards summary={inventory.summary} />

      <AiRestockPanel
        onQuickRestock={guardBom(inventory.handleQuickRestock)}
        locked={!canUseAiOps}
        onRequestUpgrade={() => setUpgradeFeature(PLAN_FEATURES.AI_OPERATIONS_SUITE)}
      />

      <InventoryToolbar
        activeTab={inventory.activeTab}
        onTabChange={inventory.setActiveTab}
        selectedMenuItemId={inventory.selectedMenuItemId}
        onMenuItemChange={inventory.handleMenuItemChange}
        onMenuItemFocus={() => inventory.ensureMenuItems()}
        menuItems={inventory.menuItems}
        menuItemsLoading={inventory.menuItemsLoading}
        onOpenInvoiceOcr={guardAiOps(inventory.openInvoiceOcr)}
        invoiceOcrLocked={!canUseAiOps}
        onOpenRecipe={guardBom(inventory.openRecipe)}
        recipeLocked={!canUseInventoryBom}
        onRefresh={() => inventory.refreshInventoryView(true)}
      />

      {inventory.loading ? (
        <LoadingState />
      ) : inventory.activeTab === 'items' ? (
        <InventoryGrid
          items={inventory.items}
          onEdit={guardBom(inventory.openEdit)}
          onStockIn={guardBom(inventory.openStockIn)}
          onAdjust={guardBom(inventory.openStockAdjust)}
          isManager={isManager}
        />
      ) : inventory.movementsLoading ? (
        <LoadingState />
      ) : (
        <MovementList movements={inventory.movements} />
      )}

      {inventory.activeTab === 'items' && !inventory.loading && (
        <PaginationControls
          currentPage={inventory.currentPage}
          totalPages={inventory.totalPages}
          totalElements={inventory.totalElements}
          itemLabel="nguyên liệu"
          loading={inventory.loading}
          onPageChange={inventory.setCurrentPage}
        />
      )}

      {inventory.inventoryModalOpen && (
        <InventoryModal
          item={inventory.editingItem}
          form={inventory.inventoryForm}
          setForm={inventory.setInventoryForm}
          errors={inventory.inventoryErrors}
          setErrors={inventory.setInventoryErrors}
          isSubmitting={inventory.isInventorySubmitting}
          onClose={inventory.closeInventoryModal}
          onSubmit={inventory.handleSaveInventory}
        />
      )}

      {inventory.stockAction && (
        <StockModal
          action={inventory.stockAction}
          form={inventory.stockForm}
          setForm={inventory.setStockForm}
          onClose={inventory.closeStockModal}
          onSubmit={inventory.handleStockSubmit}
          isSubmitting={inventory.isStockSubmitting}
        />
      )}

      {inventory.recipeOpen && (
        <RecipeModal
          menuItems={inventory.menuItems}
          inventoryItems={inventory.activeInventoryItems}
          selectedMenuItem={inventory.selectedMenuItem}
          selectedMenuItemId={inventory.selectedMenuItemId}
          onSelectMenuItem={inventory.loadRecipe}
          recipeItems={inventory.recipeItems}
          setRecipeItems={inventory.setRecipeItems}
          onAddRow={inventory.addRecipeRow}
          onClose={inventory.closeRecipeModal}
          onSubmit={inventory.saveRecipe}
          isSubmitting={inventory.isRecipeSubmitting}
          loading={inventory.recipeLoading}
          isManager={isManager}
        />
      )}

      {inventory.invoiceOcrOpen && (
        <InvoiceOcrModal
          inventoryItems={inventory.activeInventoryItems}
          inventoryItemsLoading={inventory.inventoryItemsLoading}
          isManager={isManager}
          onClose={inventory.closeInvoiceOcr}
          onSuccess={inventory.handleInventorySaved}
        />
      )}

      <UpgradeProModal
        isOpen={Boolean(upgradeFeature)}
        feature={upgradeFeature}
        isAdmin={isAdmin}
        canViewBilling={isManager}
        onClose={() => setUpgradeFeature(null)}
      />
    </div>
  );
};

export default InventoryManagement;
