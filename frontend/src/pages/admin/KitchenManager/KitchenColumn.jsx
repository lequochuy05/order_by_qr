import React from 'react';
import KitchenCard from './KitchenCard';

const KitchenColumn = ({ title, status, items, accentColor, onUpdateStatus, processingItems }) => (
    <div className="flex-1 min-w-[300px] bg-slate-100 rounded-lg p-4 flex flex-col h-[calc(100vh-200px)]">
        <div className={`flex items-center gap-2 mb-4 pb-2 border-b-2 ${accentColor}`}>
            <h2 className="text-lg font-bold text-slate-700">{title}</h2>
            <span className="bg-slate-200 text-slate-600 px-2 py-0.5 rounded-full text-sm font-medium">
                {items.length}
            </span>
        </div>

        <div className="flex-1 overflow-y-auto space-y-3 pr-2 scrollbar-thin scrollbar-thumb-slate-300">
            {items.length === 0 ? (
                <div className="text-center py-10 text-slate-400 italic">Trống</div>
            ) : (
                items.map(item => (
                    <KitchenCard
                        key={item.id}
                        item={item}
                        status={status}
                        onUpdateStatus={onUpdateStatus}
                        isProcessing={processingItems.has(item.id)}
                    />
                ))
            )}
        </div>
    </div>
);

export default KitchenColumn;
