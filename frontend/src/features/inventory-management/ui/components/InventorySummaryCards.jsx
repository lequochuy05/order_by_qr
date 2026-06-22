import { AlertTriangle, Boxes, PackageCheck, Scale } from 'lucide-react';

import SummaryCard from './SummaryCard.jsx';

const InventorySummaryCards = ({ summary }) => (
  <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
    <SummaryCard icon={Boxes} label="Nguyên liệu" value={summary?.totalItems || 0} />
    <SummaryCard
      icon={PackageCheck}
      label="Đang dùng"
      value={summary?.activeItems || 0}
      tone="green"
    />
    <SummaryCard
      icon={AlertTriangle}
      label="Sắp hết"
      value={summary?.lowStockItems || 0}
      tone="amber"
    />
    <SummaryCard icon={Scale} label="Hết hàng" value={summary?.outOfStockItems || 0} tone="red" />
  </div>
);

export default InventorySummaryCards;
