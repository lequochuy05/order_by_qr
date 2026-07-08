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
          selectClassName="font-black"
        />

        {loading ? (
	          <div className="flex justify-center rounded-3xl border border-dashed border-slate-200 py-12 text-orange-500 dark:border-slate-800 dark:text-orange-300">
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
	                  className="grid grid-cols-1 gap-3 rounded-2xl border border-slate-100 bg-slate-50/50 p-3 transition-colors dark:border-slate-800 dark:bg-slate-950/50 md:grid-cols-[1fr_180px_44px] md:items-end"
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
	                      className="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-bold text-slate-900 outline-none transition focus:ring-2 focus:ring-orange-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
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
	                    className="flex h-[46px] items-center justify-center rounded-xl bg-red-50 text-red-600 transition-colors hover:bg-red-600 hover:text-white dark:bg-red-500/10 dark:text-red-300"
                  >
                    <X size={18} />
                  </button>
                </div>
              );
            })}
          </div>
        )}

        {!loading && recipeItems.length === 0 && (
	          <div className="rounded-3xl border border-dashed border-slate-200 py-12 text-center italic text-slate-400 dark:border-slate-800 dark:text-slate-500">
            Chưa có nguyên liệu trong định mức món này.
          </div>
        )}

        <button
          type="button"
          onClick={onAddRow}
          disabled={loading || isSubmitting}
	          className="inline-flex items-center gap-2 rounded-xl bg-orange-50 px-4 py-2.5 text-sm font-black text-orange-600 transition-colors hover:bg-orange-100 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-orange-500/10 dark:text-orange-300 dark:hover:bg-orange-500/20"
        >
          <Plus size={16} /> Thêm nguyên liệu
        </button>
      </div>
      <ModalActions onClose={onClose} isSubmitting={isSubmitting} />
    </form>
  </SharedModal>
);

export default RecipeModal;
