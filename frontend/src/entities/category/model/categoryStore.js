import { create } from 'zustand';
import { categoryService } from '@entities/category/api/categoryService.js';

const useCategoryStore = create((set, get) => ({
  categories: [],
  loaded: false,

  fetchCategories: async (force = false) => {
    if (get().loaded && !force) return;
    try {
      const data = await categoryService.getAll();
      set({ categories: data.content || data, loaded: true });
    } catch (err) {
      console.error('Error fetching categories:', err);
    }
  },

  invalidate: () => set({ loaded: false }),

  invalidateAndRefetch: async () => {
    set({ loaded: false });
    await get().fetchCategories(true);
  },
}));

export default useCategoryStore;
