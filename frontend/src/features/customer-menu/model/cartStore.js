import { create } from 'zustand';

const useCartStore = create((set) => ({
  items: {},
  combos: {},
  clearCart: () => set({ items: {}, combos: {} }),
  setCart: (cart) =>
    set({
      items: cart?.items || {},
      combos: cart?.combos || {},
    }),
}));

export default useCartStore;
