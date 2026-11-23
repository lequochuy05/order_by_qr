import assert from 'node:assert/strict';
import { test } from 'node:test';

import {
  computeInsightsDateRange,
  formatDeltaPercent,
  getSeverityMeta,
  normalizeExecutiveSummary,
} from '../../src/features/dashboard-overview/lib/executiveInsights.js';

test('computeInsightsDateRange handles today, 7d, and 30d presets across month boundaries', () => {
  assert.deepEqual(computeInsightsDateRange('2026-11-03', 'today'), {
    from: '2026-11-03',
    to: '2026-11-03',
  });
  assert.deepEqual(computeInsightsDateRange('2026-11-03', '7d'), {
    from: '2026-10-28',
    to: '2026-11-03',
  });
  assert.deepEqual(computeInsightsDateRange('2026-03-05', '30d'), {
    from: '2026-02-04',
    to: '2026-03-05',
  });
});

test('formatDeltaPercent formats positive, negative, and null values cleanly', () => {
  assert.equal(formatDeltaPercent(18.54), '+18.5%');
  assert.equal(formatDeltaPercent(-12.3), '-12.3%');
  assert.equal(formatDeltaPercent(0), '+0%');
  assert.equal(formatDeltaPercent(null), null);
});

test('getSeverityMeta maps HIGH, MEDIUM, and INFO labels bilingually', () => {
  assert.equal(getSeverityMeta('high', 'vi').key, 'HIGH');
  assert.equal(getSeverityMeta('high', 'en').label, 'High risk');
  assert.equal(getSeverityMeta('medium', 'vi').label, 'Cần theo dõi');
  assert.equal(getSeverityMeta('unknown', 'en').key, 'INFO');
});

test('normalizeExecutiveSummary sanitizes metrics and arrays cleanly', () => {
  const normalized = normalizeExecutiveSummary({
    from: '2026-11-16',
    to: '2026-11-22',
    language: 'vi',
    headline: '  Doanh thu tăng trưởng tốt. ',
    highlights: ['Điểm 1', ' ', 42],
    anomalies: [
      { severity: 'high', title: 'Bếp trễ', detail: '2 món > 20p' },
      { severity: 'INFO', title: '', detail: 'invalid' },
    ],
    recommendations: ['Mở rộng ca bếp'],
    aiGenerated: true,
    metrics: {
      revenue: '1250000',
      revenueChangePercent: 12.4,
      completedOrders: 15,
      overdueKitchenItems: 2,
    },
  });

  assert.equal(normalized.headline, 'Doanh thu tăng trưởng tốt.');
  assert.deepEqual(normalized.highlights, ['Điểm 1']);
  assert.equal(normalized.anomalies.length, 1);
  assert.equal(normalized.anomalies[0].severity, 'HIGH');
  assert.equal(normalized.metrics.revenue, 1250000);
  assert.equal(normalized.metrics.overdueKitchenItems, 2);
});
