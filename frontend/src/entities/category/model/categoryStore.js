import { create } from 'zustand';

const useCategoryStore = create((set) => ({
  categories: [],
  loaded: false,

  setCategories: (categories = []) => {
    set({ categories, loaded: true });
  },

  resetCategories: () => {
    set({ categories: [], loaded: false });
  },

  invalidate: () => {
    set({ loaded: false });
  },
}));

export default useCategoryStore;
