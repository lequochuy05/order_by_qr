import { fmtVND } from '@shared/lib/formatters.js'

const OrderPrice = ({ value = 0, className = '' }) => (
  <span className={`font-bold tabular-nums ${className}`}>{fmtVND(value)}</span>
)

export default OrderPrice
