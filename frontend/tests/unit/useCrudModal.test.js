import assert from 'node:assert/strict';
import test from 'node:test';

import { crudModalReducer, initialCrudModalState } from '../../src/shared/hooks/useCrudModal.js';
import { translations } from '../../src/shared/lib/i18n/translations.js';

test('crudModalReducer opens create modal with optional draft and resets errors', () => {
  const dirty = { ...initialCrudModalState, formErrors: { name: 'err' } };
  const opened = crudModalReducer(dirty, { type: 'OPEN_CREATE', payload: { _draftKey: 'd1' } });
  assert.equal(opened.isOpen, true);
  assert.deepEqual(opened.editingItem, { _draftKey: 'd1' });
  assert.deepEqual(opened.formErrors, {});
});

test('crudModalReducer opens edit modal, patches editingItem, and closes cleanly', () => {
  const edited = crudModalReducer(initialCrudModalState, {
    type: 'OPEN_EDIT',
    payload: { id: 7, name: 'VIP' },
  });
  assert.equal(edited.isOpen, true);
  assert.deepEqual(edited.editingItem, { id: 7, name: 'VIP' });

  const patched = crudModalReducer(edited, {
    type: 'SET_EDITING_ITEM',
    payload: { id: 7, name: 'VIP-2' },
  });
  assert.deepEqual(patched.editingItem, { id: 7, name: 'VIP-2' });

  const closed = crudModalReducer(patched, { type: 'CLOSE' });
  assert.deepEqual(closed, initialCrudModalState);
});

test('crudModalReducer supports object/function error updates and field clearing', () => {
  const s1 = crudModalReducer(initialCrudModalState, {
    type: 'SET_FORM_ERRORS',
    payload: { code: 'dup', name: 'req' },
  });
  assert.deepEqual(s1.formErrors, { code: 'dup', name: 'req' });

  const s2 = crudModalReducer(s1, {
    type: 'SET_FORM_ERRORS',
    payload: (prev) => ({ ...prev, email: 'invalid' }),
  });
  assert.deepEqual(s2.formErrors, { code: 'dup', name: 'req', email: 'invalid' });

  const s3 = crudModalReducer(s2, { type: 'CLEAR_FORM_ERROR', field: 'code' });
  assert.deepEqual(s3.formErrors, { name: 'req', email: 'invalid' });

  const s4 = crudModalReducer(s3, { type: 'CLEAR_FORM_ERROR', field: 'missing' });
  assert.equal(s4, s3);
});

test('crudModalReducer tracks submitting and deletingId flags', () => {
  const s1 = crudModalReducer(initialCrudModalState, { type: 'SET_SUBMITTING', payload: true });
  assert.equal(s1.isSubmitting, true);

  const s2 = crudModalReducer(s1, { type: 'SET_DELETING_ID', payload: 42 });
  assert.equal(s2.deletingId, 42);
});

test('modular i18n locale dictionaries keep vi and en namespaces in full parity', () => {
  const viSections = Object.keys(translations.vi).sort();
  const enSections = Object.keys(translations.en).sort();
  assert.deepEqual(viSections, enSections);

  for (const section of viSections) {
    const viKeys = Object.keys(translations.vi[section]).sort();
    const enKeys = Object.keys(translations.en[section]).sort();
    assert.deepEqual(viKeys, enKeys, `Locale key mismatch in namespace "${section}"`);
  }
});
