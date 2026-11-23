import assert from 'node:assert/strict';
import test from 'node:test';

import {
  analyzeCustomerNote,
  foldVietnameseAccents,
  getSafetyFlagLabel,
  maxRisk,
  RISK_LEVELS,
  riskRank,
} from '../../src/shared/lib/noteSafety.js';

test('foldVietnameseAccents normalizes Vietnamese characters and đ/Đ', () => {
  assert.equal(foldVietnameseAccents('Đẩy GẤP giúp đơn Bò Né'), 'day gap giup don bo ne');
});

test('analyzeCustomerNote flags accented and unaccented allergens as CRITICAL', () => {
  const accented = analyzeCustomerNote('Khách bị dị ứng đậu phộng và tôm');
  assert.equal(accented.riskLevel, RISK_LEVELS.CRITICAL);
  assert.ok(accented.allergens.includes('ALLERGY'));
  assert.ok(accented.allergens.includes('PEANUT_NUTS'));
  assert.ok(accented.allergens.includes('SEAFOOD'));

  const unaccented = analyzeCustomerNote('khong bo bot ngot va sua bo');
  assert.equal(unaccented.riskLevel, RISK_LEVELS.CRITICAL);
  assert.ok(unaccented.allergens.includes('MSG'));
  assert.ok(unaccented.allergens.includes('DAIRY'));
});

test('analyzeCustomerNote avoids false-positive allergens on accented Vietnamese homographs', () => {
  const everyday = analyzeCustomerNote('Bàn số 5 cần ghế trẻ em cho con của khách, sửa giúp đá');
  assert.equal(everyday.riskLevel, RISK_LEVELS.HIGH);
  assert.deepEqual(everyday.allergens, []);
  assert.deepEqual(everyday.urgencyFlags, ['KIDS_ELDERLY_PREGNANT']);
});

test('analyzeCustomerNote classifies dietary notes as NORMAL and resolves flags', () => {
  const dietary = analyzeCustomerNote('Không cay, ăn chay, ít đường');
  assert.equal(dietary.riskLevel, RISK_LEVELS.NORMAL);
  assert.ok(dietary.dietaryFlags.includes('NO_SPICY'));
  assert.ok(dietary.dietaryFlags.includes('VEGETARIAN'));
  assert.ok(dietary.dietaryFlags.includes('LESS_SEASONING'));
  assert.equal(getSafetyFlagLabel('PEANUT_NUTS', 'vi'), 'Đậu phộng / Hạt');
  assert.equal(getSafetyFlagLabel('PEANUT_NUTS', 'en'), 'Peanuts / Nuts');
});

test('maxRisk and riskRank preserve CRITICAL ordering', () => {
  assert.equal(maxRisk(RISK_LEVELS.CRITICAL, RISK_LEVELS.NORMAL), RISK_LEVELS.CRITICAL);
  assert.equal(maxRisk(RISK_LEVELS.NORMAL, RISK_LEVELS.HIGH), RISK_LEVELS.HIGH);
  assert.ok(riskRank(RISK_LEVELS.CRITICAL) > riskRank(RISK_LEVELS.HIGH));
  assert.ok(riskRank(RISK_LEVELS.HIGH) > riskRank(RISK_LEVELS.NORMAL));
});
