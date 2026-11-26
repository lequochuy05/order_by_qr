import { TableManagement } from '@features/table-management';
import { AddItemModal, OrderDetailModal, PaymentModal } from '@widgets/order-modals';

export default function TableMapPage() {
  return (
    <TableManagement
      AddItemModal={AddItemModal}
      OrderDetailModal={OrderDetailModal}
      PaymentModal={PaymentModal}
    />
  );
}
