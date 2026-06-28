import { ItemOptionsModal } from '@shared/ui';
import useAddItemsCart from '../model/useAddItemsCart.js';
import SharedModal from '@shared/ui/SharedModal.jsx';
import AddItemCartPane from './add-item/AddItemCartPane.jsx';
import AddItemCatalogPane from './add-item/AddItemCatalogPane.jsx';

const AddItemModal = ({ isOpen, onClose, table, onSubmit, isSubmitting }) => {
  const addItems = useAddItemsCart({ isOpen, table, onSubmit });

  if (!isOpen || !table) return null;

  return (
    <>
      <SharedModal
        isOpen={isOpen}
        onClose={onClose}
        className="max-w-5xl !h-[94vh] !flex-col !overflow-hidden !rounded-2xl !p-0 md:!h-[90vh] md:!flex-row"
        ariaLabel={`Thêm món cho bàn ${table.tableNumber}`}
      >
        <AddItemCatalogPane
          tableNumber={table.tableNumber}
          activeTab={addItems.activeTab}
          onTabChange={addItems.setActiveTab}
          categories={addItems.categories}
          selectedCategory={addItems.selectedCategory}
          onCategoryChange={addItems.setSelectedCategory}
          catalogLoading={addItems.catalogLoading}
          displayList={addItems.displayList}
          onItemClick={addItems.handleItemClick}
        />
        <AddItemCartPane
          cart={addItems.cart}
          total={addItems.total}
          onClose={onClose}
          onUpdateItem={addItems.updateCartItem}
          onRemoveItem={addItems.removeFromCart}
          onConfirm={addItems.handleConfirm}
          isSubmitting={isSubmitting}
        />
      </SharedModal>

      <ItemOptionsModal
        key={addItems.selectedItemForOptions?.id || 'closed'}
        item={addItems.selectedItemForOptions}
        isOpen={Boolean(addItems.selectedItemForOptions)}
        onClose={() => addItems.setSelectedItemForOptions(null)}
        onConfirm={addItems.handleConfirmOptions}
      />
    </>
  );
};

export default AddItemModal;
