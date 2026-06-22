import { useCallback } from 'react';

import useInventoryData from './useInventoryData.js';
import useInventoryItemForm from './useInventoryItemForm.js';
import useRecipeEditor from './useRecipeEditor.js';
import useStockAction from './useStockAction.js';

const useInventoryManagement = () => {
  const recipe = useRecipeEditor();
  const data = useInventoryData({
    onInventoryUpdated: recipe.refreshInventoryOptions,
  });
  const { refreshAfterMutation, refreshInventoryView: refreshDataView } = data;
  const { refreshInventoryOptions, refreshRecipeSources } = recipe;

  const handleInventorySaved = useCallback(() => {
    refreshAfterMutation();
    refreshInventoryOptions();
  }, [refreshAfterMutation, refreshInventoryOptions]);

  const itemForm = useInventoryItemForm({ onSaved: handleInventorySaved });
  const stockAction = useStockAction({ onSaved: handleInventorySaved });

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
    refreshInventoryView,
  };
};

export default useInventoryManagement;
