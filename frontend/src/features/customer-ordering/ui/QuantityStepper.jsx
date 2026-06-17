import { Minus, Plus } from 'lucide-react'

const QuantityStepper = ({ value = 0, onChange, min = 0, max = 99 }) => {
  const update = (nextValue) => {
    const safeValue = Math.min(max, Math.max(min, nextValue))
    onChange?.(safeValue)
  }

  return (
    <div className="inline-flex items-center rounded-xl border border-slate-200 bg-white text-slate-900 shadow-sm dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100">
      <button type="button" className="p-2" onClick={() => update(value - 1)} disabled={value <= min}>
        <Minus size={16} />
      </button>
      <span className="min-w-8 text-center text-sm font-bold">{value}</span>
      <button type="button" className="p-2" onClick={() => update(value + 1)} disabled={value >= max}>
        <Plus size={16} />
      </button>
    </div>
  )
}

export default QuantityStepper
