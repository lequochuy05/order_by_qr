import { getTableStatusMeta } from '@entities/order/lib/orderStatus.js'

const TableStatusBadge = ({ status }) => {
  const meta = getTableStatusMeta(status)

  return (
    <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-bold ${meta.classes}`}>
      {meta.label}
    </span>
  )
}

export default TableStatusBadge
