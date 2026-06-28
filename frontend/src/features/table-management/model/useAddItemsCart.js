import { useCallback, useEffect, useMemo, useState } from 'react';

import { tableOrderCatalogService } from '../api/tableOrderCatalogService.js';
import { showErrorToast } from '@shared/lib/toast.js';

const useAddItemsCart = ({ isOpen, table, onSubmit }) => {
  const [activeTab, setActiveTab] = useState('ITEMS');
  const [menuItems, setMenuItems] = useState([]);
  const [combos, setCombos] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [cart, setCart] = useState([]);
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [selectedItemForOptions, setSelectedItemForOptions] = useState(null);

  useEffect(() => {
    if (!isOpen) return undefined;
    let isCancelled = false;

    const loadCatalog = async () => {
      setCatalogLoading(true);
      try {
        const catalog = await tableOrderCatalogService.getCatalog();
        if (isCancelled) return;
        setMenuItems(Array.isArray(catalog?.menuItems) ? catalog.menuItems : []);
        setCombos(Array.isArray(catalog?.combos) ? catalog.combos : []);
        setCategories(Array.isArray(catalog?.categories) ? catalog.categories : []);
      } catch (error) {
        showErrorToast(error);
      } finally {
        if (!isCancelled) setCatalogLoading(false);
      }
    };

    loadCatalog();
    return () => {
      isCancelled = true;
    };
  }, [isOpen]);

  const addToCart = useCallback(
    (item, type, options = [], optionObjects = [], totalPrice = null) => {
      const normalizedOptions = [...options].sort();
      const price = totalPrice ?? item.price;
      const cartId = `${item.id}-${type}-${normalizedOptions.join(',')}`;

      setCart((current) => {
        const existing = current.find((cartItem) => cartItem.cartId === cartId);
        if (existing) {
          return current.map((cartItem) =>
            cartItem.cartId === cartId ? { ...cartItem, qty: cartItem.qty + 1 } : cartItem,
          );
        }

        const optionLabel =
          optionObjects.length > 0
            ? ` (${optionObjects.map((option) => option.valueName).join(', ')})`
            : '';
        return [
          ...current,
          {
            cartId,
            id: item.id,
            type,
            name: `${item.name}${optionLabel}`,
            price,
            qty: 1,
            notes: '',
            options: normalizedOptions.map((optionValueId) => ({ optionValueId })),
          },
        ];
      });
    },
    [],
  );

  const handleItemClick = useCallback(
    (item, type) => {
      if (type === 'ITEM' && item.itemOptions?.length > 0) {
        setSelectedItemForOptions(item);
        return;
      }
      addToCart(item, type);
    },
    [addToCart],
  );

  const handleConfirmOptions = useCallback(
    (item, selectedValueIds, selectedOptionObjects, totalPrice) => {
      addToCart(item, 'ITEM', selectedValueIds, selectedOptionObjects, totalPrice);
      setSelectedItemForOptions(null);
    },
    [addToCart],
  );

  const updateCartItem = useCallback((index, patch) => {
    setCart((current) =>
      current.map((item, itemIndex) => (itemIndex === index ? { ...item, ...patch } : item)),
    );
  }, []);

  const removeFromCart = useCallback((index) => {
    setCart((current) => current.filter((_, itemIndex) => itemIndex !== index));
  }, []);

  const displayList = useMemo(
    () =>
      activeTab === 'ITEMS'
        ? menuItems.filter(
            (item) => selectedCategory === 'ALL' || item.category?.id === selectedCategory,
          )
        : combos,
    [activeTab, combos, menuItems, selectedCategory],
  );

  const total = useMemo(() => cart.reduce((sum, item) => sum + item.price * item.qty, 0), [cart]);

  const handleConfirm = useCallback(() => {
    if (cart.length === 0) {
      showErrorToast('Chưa chọn món nào');
      return;
    }

    onSubmit({
      tableId: table.id,
      items: cart
        .filter((item) => item.type === 'ITEM')
        .map((item) => ({
          menuItemId: item.id,
          quantity: item.qty,
          notes: item.notes,
          selectedOptionValueIds: item.options.map((option) => option.optionValueId),
        })),
      combos: cart
        .filter((item) => item.type === 'COMBO')
        .map((item) => ({
          comboId: item.id,
          quantity: item.qty,
          notes: item.notes,
        })),
    });
  }, [cart, onSubmit, table?.id]);

  return {
    activeTab,
    categories,
    selectedCategory,
    cart,
    catalogLoading,
    selectedItemForOptions,
    displayList,
    total,
    setActiveTab,
    setSelectedCategory,
    setSelectedItemForOptions,
    handleItemClick,
    handleConfirmOptions,
    updateCartItem,
    removeFromCart,
    handleConfirm,
  };
};

export default useAddItemsCart;
