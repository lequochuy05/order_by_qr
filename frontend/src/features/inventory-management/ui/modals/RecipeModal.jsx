import { Loader2, Plus, X } from 'lucide-react';

import {
  FormLabel,
  ModalActions,
  ModalHeader,
  SelectField,
  SharedModal,
  TextField,
} from '@shared/ui';

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
  <SharedModal isOpen onClose={onClose} className="max-w-3xl !p-0" ariaLabel="Định mức nguyên liệu">
    <form onSubmit={onSubmit} className="flex min-h-0 w-full flex-1 flex-col">
      <ModalHeader
        title="Định mức nguyên liệu"
        subtitle={selectedMenuItem?.name}
        onClose={onClose}
        disabled={isSubmitting}
      />
      <div className="space-y-5 overflow-y-auto p-8">
        <SelectField
          label="Món ăn"
          value={selectedMenuItemId}
          onChange={onSelectMenuItem}
          options={menuItems.map((item) => ({ value: item.id, label: item.name }))}
          selectClassName="font-black text-gray-800"
        />

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
                    <FormLabel className="!mb-1 !tracking-[0.16em]">Nguyên liệu</FormLabel>
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
                  <TextField
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
  </SharedModal>
);

export default RecipeModal;
