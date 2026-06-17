import { analyticsService } from '@features/analytics/api/analyticsService.js';
import { addDaysToBusinessDate } from '@shared/lib/businessTime.js';

export const dashboardOverviewService = {
  getSummary: (businessDate) => {
    const past7Start = addDaysToBusinessDate(businessDate, -6);
    return analyticsService.getDashboardSummary(past7Start, businessDate);
  },
};
