import { Loader2, Plus } from 'lucide-react';
import { useAuth } from '@features/auth';

import { STOCK_FILTER_OPTIONS } from '../lib/inventoryConstants.js';
import useInventoryManagement from '../model/useInventoryManagement.js';
import ManagementHeader from '@shared/ui/ManagementHeader.jsx';
import PaginationControls from '@shared/ui/PaginationControls.jsx';
import InventoryGrid from './components/InventoryGrid.jsx';
import InventorySummaryCards from './components/InventorySummaryCards.jsx';
import InventoryToolbar from './components/InventoryToolbar.jsx';
import MovementList from './components/MovementList.jsx';
import InventoryModal from './modals/InventoryModal.jsx';
import RecipeModal from './modals/RecipeModal.jsx';
import StockModal from './modals/StockModal.jsx';

const LoadingState = () => (
  <div className="flex justify-center p-20">
    <Loader2 className="animate-spin text-indigo-600 dark:text-indigo-400" size={40} />
  </div>
);

const InventoryManagement = () => {
  const { user } = useAuth();
  const isManager = user?.role === 'ADMIN' || user?.role === 'MANAGER';
  const inventory = useInventoryManagement();

  return (
    <div className="w-full min-w-0 space-y-4 p-0 sm:space-y-6 sm:p-3 lg:p-6">
      <ManagementHeader
        searchPlaceholder="Tìm nguyên liệu..."
        searchTerm={inventory.searchTerm}
        setSearchTerm={inventory.setSearchTerm}
        onAddClick={isManager ? inventory.openCreate : undefined}
        addButtonText={isManager ? "Thêm nguyên liệu" : undefined}
        addButtonIcon={isManager ? Plus : undefined}
        showFilter
        filterAllLabel="Tất cả tồn kho"
        filterValue={inventory.stockFilter}
        setFilterValue={inventory.setStockFilter}
        filterOptions={STOCK_FILTER_OPTIONS}
      />

      <InventorySummaryCards summary={inventory.summary} />

      <InventoryToolbar
        activeTab={inventory.activeTab}
        onTabChange={inventory.setActiveTab}
        selectedMenuItemId={inventory.selectedMenuItemId}
        onMenuItemChange={inventory.handleMenuItemChange}
        onMenuItemFocus={() => inventory.ensureMenuItems()}
        menuItems={inventory.menuItems}
        menuItemsLoading={inventory.menuItemsLoading}
        onOpenRecipe={inventory.openRecipe}
        onRefresh={() => inventory.refreshInventoryView(true)}
      />

      {inventory.loading ? (
        <LoadingState />
      ) : inventory.activeTab === 'items' ? (
        <InventoryGrid
          items={inventory.items}
          onEdit={inventory.openEdit}
          onStockIn={inventory.openStockIn}
          onAdjust={inventory.openStockAdjust}
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
        />
      )}
    </div>
  );
};

export default InventoryManagement;
