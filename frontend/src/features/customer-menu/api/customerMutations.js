// Re-export hooks that were moved to entities to preserve backward compatibility
export { useSubmitOrderMutation, createClientRequestId } from '@entities/order/api/orderMutations.js';
export { useStartTableSessionMutation } from '@entities/table';
