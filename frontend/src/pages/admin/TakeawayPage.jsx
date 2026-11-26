import { TakeawayManagement } from '@features/takeaway-management';
import { AddItemModal, OrderDetailModal, PaymentModal } from '@widgets/order-modals';

export default function TakeawayPage() {
  return (
    <TakeawayManagement
      AddItemModal={AddItemModal}
      OrderDetailModal={OrderDetailModal}
      PaymentModal={PaymentModal}
    />
  );
}
