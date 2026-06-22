import assert from 'node:assert/strict';
import test from 'node:test';

import {
  FORM_CONTROL_CLASS,
  FORM_CONTROL_ERROR_CLASS,
  FORM_CONTROL_NORMAL_CLASS,
  FORM_LABEL_CLASS,
} from '../../src/shared/ui/form/formStyles.js';

test('form labels share the compact uppercase typography contract', () => {
  assert.match(FORM_LABEL_CLASS, /uppercase/);
  assert.match(FORM_LABEL_CLASS, /tracking-\[0\.2em\]/);
  assert.match(FORM_LABEL_CLASS, /text-gray-400/);
});

test('form controls share shape, spacing, and disabled states', () => {
  assert.match(FORM_CONTROL_CLASS, /rounded-2xl/);
  assert.match(FORM_CONTROL_CLASS, /border-2/);
  assert.match(FORM_CONTROL_CLASS, /disabled:cursor-not-allowed/);
  assert.match(FORM_CONTROL_CLASS, /disabled:bg-gray-100/);
});

test('normal form controls use the orange focus treatment', () => {
  assert.match(FORM_CONTROL_NORMAL_CLASS, /focus:border-orange-500/);
  assert.match(FORM_CONTROL_NORMAL_CLASS, /focus:ring-orange-500\/10/);
});

test('invalid form controls use the shared red error treatment', () => {
  assert.match(FORM_CONTROL_ERROR_CLASS, /border-red-500/);
  assert.match(FORM_CONTROL_ERROR_CLASS, /ring-red-50/);
});
