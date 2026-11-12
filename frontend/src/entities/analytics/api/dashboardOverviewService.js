import { addDaysToBusinessDate } from '@shared/lib/businessTime.js';
import { analyticsService } from './analyticsService.js';

export const dashboardOverviewService = {
  getSummary: (businessDate, options = {}) => {
    const past7Start = addDaysToBusinessDate(businessDate, -6);
    return analyticsService.getDashboardSummary(past7Start, businessDate, options);
  },
};
