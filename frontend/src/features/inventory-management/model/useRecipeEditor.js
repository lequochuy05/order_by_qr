import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { inventoryService } from '@features/inventory-management/api/inventoryService.js';
import { menuItemService } from '@features/menu-management';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';

const useRecipeEditor = () => {
  const [menuItems, setMenuItems] = useState([]);
  const [inventoryItems, setInventoryItems] = useState([]);
  const [recipeItems, setRecipeItems] = useState([]);
  const [selectedMenuItemId, setSelectedMenuItemId] = useState('');
  const [menuItemsLoading, setMenuItemsLoading] = useState(false);
  const [inventoryItemsLoading, setInventoryItemsLoading] = useState(false);
  const [recipeLoading, setRecipeLoading] = useState(false);
  const [recipeOpen, setRecipeOpen] = useState(false);
  const [isRecipeSubmitting, setIsRecipeSubmitting] = useState(false);
  const { showError } = useStatusModal();
  const isMountedRef = useRef(true);

  const selectedMenuItem = useMemo(
    () => menuItems.find((item) => String(item.id) === String(selectedMenuItemId)),
    [menuItems, selectedMenuItemId],
  );

  const activeInventoryItems = useMemo(
    () => inventoryItems.filter((item) => item.active !== false),
    [inventoryItems],
  );

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

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
      } catch (error) {
        showErrorToast(error);
        return [];
      } finally {
        if (isMountedRef.current) setMenuItemsLoading(false);
      }
    },
    [menuItems],
  );

  const ensureInventoryItems = useCallback(
    async ({ force = false } = {}) => {
      if (!force && inventoryItems.length > 0) return inventoryItems;

      setInventoryItemsLoading(true);
      try {
        const inventoryData = await inventoryService.getItems({ force });
        if (!isMountedRef.current) return inventoryData || [];

        setInventoryItems(inventoryData || []);
        return inventoryData || [];
      } catch (error) {
        showErrorToast(error);
        return [];
      } finally {
        if (isMountedRef.current) setInventoryItemsLoading(false);
      }
    },
    [inventoryItems],
  );

  const loadRecipe = useCallback(async (menuItemId) => {
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
    } catch (error) {
      showErrorToast(error);
    } finally {
      setRecipeLoading(false);
    }
  }, []);

  const openRecipe = useCallback(async () => {
    const [menuData] = await Promise.all([
      ensureMenuItems(),
      ensureInventoryItems({ force: true }),
    ]);
    const targetMenuItemId = selectedMenuItemId || (menuData.length ? String(menuData[0].id) : '');
    if (!targetMenuItemId) {
      showError('Chưa có món ăn để thiết lập định mức.');
      return;
    }

    setRecipeOpen(true);
    await loadRecipe(targetMenuItemId);
  }, [ensureInventoryItems, ensureMenuItems, loadRecipe, selectedMenuItemId, showError]);

  const handleMenuItemChange = useCallback(
    (menuItemId) => {
      setSelectedMenuItemId(menuItemId);
      if (recipeOpen) loadRecipe(menuItemId);
    },
    [loadRecipe, recipeOpen],
  );

  const addRecipeRow = useCallback(() => {
    const firstAvailable = activeInventoryItems.find(
      (item) => !recipeItems.some((row) => Number(row.inventoryItemId) === item.id),
    );
    if (!firstAvailable) return;

    setRecipeItems((current) => [
      ...current,
      { inventoryItemId: String(firstAvailable.id), quantityRequired: 1 },
    ]);
  }, [activeInventoryItems, recipeItems]);

  const closeRecipeModal = useCallback(() => {
    if (!isRecipeSubmitting) setRecipeOpen(false);
  }, [isRecipeSubmitting]);

  const refreshInventoryOptions = useCallback(() => {
    if (inventoryItems.length > 0 || recipeOpen) ensureInventoryItems({ force: true });
  }, [ensureInventoryItems, inventoryItems.length, recipeOpen]);

  const refreshRecipeSources = useCallback(() => {
    if (menuItems.length > 0) ensureMenuItems({ force: true });
    refreshInventoryOptions();
  }, [ensureMenuItems, menuItems.length, refreshInventoryOptions]);

  const saveRecipe = async (event) => {
    event.preventDefault();
    if (isRecipeSubmitting) return;

    setIsRecipeSubmitting(true);
    try {
      await inventoryService.updateRecipe(selectedMenuItemId, {
        items: recipeItems.map((item) => ({
          inventoryItemId: Number(item.inventoryItemId),
          quantityRequired: Number(item.quantityRequired),
        })),
      });
      showSuccessToast('Đã cập nhật định mức món');
      setRecipeOpen(false);
    } catch (error) {
      showErrorToast(error);
    } finally {
      setIsRecipeSubmitting(false);
    }
  };

  return {
    menuItems,
    activeInventoryItems,
    recipeItems,
    selectedMenuItem,
    selectedMenuItemId,
    menuItemsLoading,
    recipeLoading: recipeLoading || inventoryItemsLoading,
    recipeOpen,
    isRecipeSubmitting,
    setRecipeItems,
    ensureMenuItems,
    refreshInventoryOptions,
    refreshRecipeSources,
    openRecipe,
    closeRecipeModal,
    loadRecipe,
    handleMenuItemChange,
    addRecipeRow,
    saveRecipe,
  };
};

export default useRecipeEditor;
