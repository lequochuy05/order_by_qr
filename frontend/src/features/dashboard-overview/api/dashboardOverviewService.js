import { analyticsService } from '@entities/analytics/api/analyticsService.js';
import { addDaysToBusinessDate } from '@shared/lib/businessTime.js';

export const dashboardOverviewService = {
  getSummary: (businessDate, options = {}) => {
    const past7Start = addDaysToBusinessDate(businessDate, -6);
    return analyticsService.getDashboardSummary(past7Start, businessDate, options);
  },
};
