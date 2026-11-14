const OrderingUnavailableBanner = ({ maintenanceMode }) => (
  <div className="mx-4 mt-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm font-bold text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200">
    {maintenanceMode
      ? 'Quán đang bảo trì, hiện chưa nhận đơn mới.'
      : 'Quán đang tạm ngưng nhận đơn mới.'}
  </div>
);

export default OrderingUnavailableBanner;
