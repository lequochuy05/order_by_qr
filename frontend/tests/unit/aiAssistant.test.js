import assert from 'node:assert/strict';
import { afterEach, beforeEach, test } from 'node:test';

import {
  AI_COPY,
  getAiCopy,
  isWelcomeMessage,
} from '../../src/features/ai-assistant/lib/aiCopy.js';
import {
  clampLauncherPosition,
  getInitialLauncherPosition,
  LAUNCHER_STORAGE_KEY,
} from '../../src/features/ai-assistant/lib/launcherPosition.js';

const originalDescriptors = {};

beforeEach(() => {
  for (const key of ['window', 'document', 'localStorage', 'getComputedStyle']) {
    originalDescriptors[key] = Object.getOwnPropertyDescriptor(globalThis, key);
  }

  const storage = new Map();
  Object.defineProperties(globalThis, {
    localStorage: {
      configurable: true,
      value: {
        getItem: (key) => storage.get(key) ?? null,
        setItem: (key, value) => storage.set(key, value),
      },
    },
    window: {
      configurable: true,
      value: {
        innerWidth: 400,
        innerHeight: 800,
        visualViewport: null,
      },
    },
    document: {
      configurable: true,
      value: { documentElement: {} },
    },
    getComputedStyle: {
      configurable: true,
      value: () => ({
        getPropertyValue: () => '0',
      }),
    },
  });
});

afterEach(() => {
  for (const [key, descriptor] of Object.entries(originalDescriptors)) {
    if (descriptor) Object.defineProperty(globalThis, key, descriptor);
    else delete globalThis[key];
  }
});

test('AI copy returns Vietnamese by default', () => {
  assert.equal(getAiCopy('unknown'), AI_COPY.vi);
});

test('AI copy returns English when requested', () => {
  assert.equal(getAiCopy('en').title, 'Assistant');
});

test('welcome message detector recognizes supported languages', () => {
  assert.equal(isWelcomeMessage(AI_COPY.vi.welcome), true);
  assert.equal(isWelcomeMessage(AI_COPY.en.welcome), true);
});

test('welcome message detector rejects normal messages', () => {
  assert.equal(isWelcomeMessage('Cho tôi xem thực đơn'), false);
});

test('launcher position clamps negative coordinates', () => {
  assert.deepEqual(clampLauncherPosition({ x: -100, y: -50 }), { x: 12, y: 12 });
});

test('launcher position clamps coordinates past viewport boundaries', () => {
  assert.deepEqual(clampLauncherPosition({ x: 1000, y: 1000 }), {
    x: 324,
    y: 724,
  });
});

test('initial launcher position uses its default bottom-right placement', () => {
  assert.deepEqual(getInitialLauncherPosition(), { x: 316, y: 632 });
});

test('initial launcher position restores persisted coordinates', () => {
  localStorage.setItem(LAUNCHER_STORAGE_KEY, JSON.stringify({ x: 80, y: 120 }));
  assert.deepEqual(getInitialLauncherPosition(), { x: 80, y: 120 });
});

test('initial launcher position ignores invalid persisted JSON', () => {
  localStorage.setItem(LAUNCHER_STORAGE_KEY, '{invalid');
  assert.deepEqual(getInitialLauncherPosition(), { x: 316, y: 632 });
});
