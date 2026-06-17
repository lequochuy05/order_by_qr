import { getOrderStatusMeta } from '../lib/orderStatus.js';

const OrderStatusBadge = ({ status }) => {
  const meta = getOrderStatusMeta(status);

  return (
    <span
      className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-bold ${meta.classes}`}
    >
      {meta.label}
    </span>
  );
};

export default OrderStatusBadge;
