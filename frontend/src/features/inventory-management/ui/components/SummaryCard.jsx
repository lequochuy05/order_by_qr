const TONE_CLASSES = {
  orange: 'bg-orange-50 text-orange-600',
  green: 'bg-emerald-50 text-emerald-600',
  amber: 'bg-amber-50 text-amber-600',
  red: 'bg-red-50 text-red-600',
};

const SummaryCard = ({ icon, label, value, tone = 'orange' }) => (
  <div className="min-w-0 rounded-3xl border border-gray-100 bg-white p-4 shadow-sm sm:p-5">
    <div className="flex items-center gap-4">
      <div className={`rounded-2xl p-3 ${TONE_CLASSES[tone]}`}>
        {createElement(icon, { size: 22 })}
      </div>
      <div>
        <p className="text-[10px] font-black uppercase tracking-[0.16em] text-gray-400">{label}</p>
        <p className="text-3xl font-black text-gray-800">{value}</p>
      </div>
    </div>
  </div>
);

export default SummaryCard;
import { createElement } from 'react';
