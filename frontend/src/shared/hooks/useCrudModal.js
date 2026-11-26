import { useCallback, useReducer } from 'react';

export const initialCrudModalState = {
  isOpen: false,
  editingItem: null,
  isSubmitting: false,
  deletingId: null,
  formErrors: {},
};

export const crudModalReducer = (state, action) => {
  switch (action.type) {
    case 'OPEN_CREATE':
      return {
        ...state,
        isOpen: true,
        editingItem: action.payload ?? null,
        formErrors: {},
      };
    case 'OPEN_EDIT':
      return {
        ...state,
        isOpen: true,
        editingItem: action.payload ?? null,
        formErrors: {},
      };
    case 'SET_EDITING_ITEM':
      return { ...state, editingItem: action.payload ?? null };
    case 'CLOSE':
      return {
        ...state,
        isOpen: false,
        editingItem: null,
        formErrors: {},
      };
    case 'SET_SUBMITTING':
      return { ...state, isSubmitting: Boolean(action.payload) };
    case 'SET_DELETING_ID':
      return { ...state, deletingId: action.payload ?? null };
    case 'SET_FORM_ERRORS': {
      const nextErrors =
        typeof action.payload === 'function'
          ? action.payload(state.formErrors)
          : action.payload;
      return { ...state, formErrors: nextErrors || {} };
    }
    case 'CLEAR_FORM_ERROR': {
      if (!action.field || !(action.field in state.formErrors)) return state;
      const next = { ...state.formErrors };
      delete next[action.field];
      return { ...state, formErrors: next };
    }
    default:
      return state;
  }
};

/**
 * Shared reducer-backed state manager for admin CRUD modals.
 * Replaces 5–6 repeated useState calls (isModalOpen, editingItem, isSaving, deletingId, formErrors).
 */
export const useCrudModal = () => {
  const [state, dispatch] = useReducer(crudModalReducer, initialCrudModalState);

  const openCreate = useCallback((draft = null) => {
    dispatch({ type: 'OPEN_CREATE', payload: draft });
  }, []);

  const openEdit = useCallback((item) => {
    dispatch({ type: 'OPEN_EDIT', payload: item });
  }, []);

  const setEditingItem = useCallback((item) => {
    dispatch({ type: 'SET_EDITING_ITEM', payload: item });
  }, []);

  const close = useCallback(() => {
    dispatch({ type: 'CLOSE' });
  }, []);

  const setSubmitting = useCallback((value) => {
    dispatch({ type: 'SET_SUBMITTING', payload: value });
  }, []);

  const setDeletingId = useCallback((id) => {
    dispatch({ type: 'SET_DELETING_ID', payload: id });
  }, []);

  const setFormErrors = useCallback((errors) => {
    dispatch({ type: 'SET_FORM_ERRORS', payload: errors });
  }, []);

  const clearFormError = useCallback((field) => {
    dispatch({ type: 'CLEAR_FORM_ERROR', field });
  }, []);

  return {
    ...state,
    openCreate,
    openEdit,
    setEditingItem,
    close,
    setSubmitting,
    setDeletingId,
    setFormErrors,
    clearFormError,
  };
};
