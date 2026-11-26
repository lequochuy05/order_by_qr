import { useCallback, useState } from 'react';

import {
  buildStockInPreset,
  findLiveInventoryTarget,
} from '../lib/aiRestockHelpers.js';
import useInventoryData from './useInventoryData.js';
import useInventoryItemForm from './useInventoryItemForm.js';
import useRecipeEditor from './useRecipeEditor.js';
import useStockAction from './useStockAction.js';

const useInventoryManagement = () => {
  const recipe = useRecipeEditor();
  const data = useInventoryData();
  const [invoiceOcrOpen, setInvoiceOcrOpen] = useState(false);
  const { items, refreshAfterMutation, refreshInventoryView: refreshDataView } = data;
  const { ensureInventoryOptions, refreshInventoryOptions, refreshRecipeSources } = recipe;

  const openInvoiceOcr = useCallback(() => {
    setInvoiceOcrOpen(true);
    ensureInventoryOptions?.();
  }, [ensureInventoryOptions]);

  const closeInvoiceOcr = useCallback(() => {
    setInvoiceOcrOpen(false);
  }, []);

  const handleInventorySaved = useCallback(() => {
    refreshAfterMutation();
    refreshInventoryOptions();
  }, [refreshAfterMutation, refreshInventoryOptions]);

  const itemForm = useInventoryItemForm({ onSaved: handleInventorySaved });
  const stockAction = useStockAction({ onSaved: handleInventorySaved });
  const { openStockIn } = stockAction;

  const handleQuickRestock = useCallback(
    (proposal, language = 'vi') => {
      const target = findLiveInventoryTarget(items, proposal);
      if (!target) return;
      openStockIn(target, buildStockInPreset(proposal, language));
    },
    [items, openStockIn],
  );

  const refreshInventoryView = useCallback(
    (showLoading = true) => {
      refreshDataView(showLoading);
      refreshRecipeSources();
    },
    [refreshDataView, refreshRecipeSources],
  );

  return {
    ...data,
    ...recipe,
    ...itemForm,
    ...stockAction,
    invoiceOcrOpen,
    openInvoiceOcr,
    closeInvoiceOcr,
    handleInventorySaved,
    handleQuickRestock,
    refreshInventoryView,
  };
};

export default useInventoryManagement;
