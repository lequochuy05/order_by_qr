import React from 'react';
import {
    AlertTriangle,
    Check,
    Clock3,
    Flame,
    Loader2,
    RotateCcw,
    StickyNote,
    Tag
} from 'lucide-react';
import { fmtTime } from '@shared/lib/formatters.js';

const KitchenCard = ({ item, status, onUpdateStatus, isProcessing }) => {
    const priority = getPriority(item);

    return (
        <article className={`overflow-hidden rounded-2xl border bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:bg-slate-900 ${priority.card}`}>
            <div className={`h-1 ${priority.bar}`} />

            <div className="p-4">
                <div className="flex items-start justify-between gap-3">
                    <div className="flex flex-wrap items-center gap-2">
                        <span className="rounded-lg bg-slate-900 px-2.5 py-1 text-xs font-black text-white dark:bg-slate-700">
                            Bàn {item.tableName}
                        </span>
                        <span className="text-xs font-semibold text-slate-400">Đơn #{item.orderId}</span>
                    </div>
                    <WaitBadge item={item} priority={priority} />
                </div>

                <div className="mt-4">
                    <div className="flex items-start justify-between gap-3">
                        <div>
                            <p className="mb-1 flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-slate-400">
                                <Tag size={13} />
                                {item.category}
                            </p>
                            <h3 className="text-lg font-black leading-snug text-slate-800 dark:text-slate-100">
                                {item.itemName}
                            </h3>
                        </div>
                        <span className="min-w-12 rounded-xl bg-orange-50 px-2.5 py-1.5 text-center text-lg font-black text-orange-600 dark:bg-orange-500/10 dark:text-orange-300">
                            x{item.quantity}
                        </span>
                    </div>
                </div>

                {item.options?.length > 0 && (
                    <div className="mt-3 flex flex-wrap gap-1.5">
                        {item.options.map(option => (
                            <span
                                key={`${option.optionName}-${option.optionValueName}-${option.valueId || ''}`}
                                className="rounded-lg border border-slate-200 bg-slate-50 px-2 py-1 text-xs font-semibold text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                            >
                                {option.optionName}: {option.optionValueName}
                            </span>
                        ))}
                    </div>
                )}

                {item.hasNotes && (
                    <div className="mt-3 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-200">
                        <div className="flex items-start gap-2">
                            <StickyNote size={16} className="mt-0.5 shrink-0" />
                            <div>
                                <p className="text-[11px] font-black uppercase tracking-wider opacity-70">Ghi chú bếp</p>
                                <p className="mt-0.5 font-semibold">{item.notes}</p>
                            </div>
                        </div>
                    </div>
                )}

                <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-400 dark:border-slate-800">
                    <span>Nhận lúc {fmtTime(item.createdAt)}</span>
                    {status === 'COOKING' && <span>Đang nấu {item.stageMinutes} phút</span>}
                    {status === 'FINISHED' && <span>Xong lúc {fmtTime(item.statusUpdatedAt)}</span>}
                </div>

                <div className="mt-3">
                    {status === 'PENDING' && (
                        <ActionButton
                            onClick={() => onUpdateStatus(item.id, 'COOKING')}
                            disabled={isProcessing}
                            loading={isProcessing}
                            icon={Flame}
                            label="Bắt đầu nấu"
                            loadingLabel="Đang chuyển món..."
                            className="bg-blue-600 hover:bg-blue-700"
                        />
                    )}
                    {status === 'COOKING' && (
                        <ActionButton
                            onClick={() => onUpdateStatus(item.id, 'FINISHED')}
                            disabled={isProcessing}
                            loading={isProcessing}
                            icon={Check}
                            label="Hoàn thành món"
                            loadingLabel="Đang hoàn thành..."
                            className="bg-emerald-600 hover:bg-emerald-700"
                        />
                    )}
                    {status === 'FINISHED' && (
                        <button
                            type="button"
                            onClick={() => onUpdateStatus(item.id, 'COOKING')}
                            disabled={isProcessing}
                            className="mx-auto flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-bold text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 disabled:cursor-not-allowed disabled:opacity-50 dark:hover:bg-slate-800 dark:hover:text-slate-200"
                        >
                            {isProcessing ? <Loader2 size={14} className="animate-spin" /> : <RotateCcw size={14} />}
                            {isProcessing ? 'Đang hoàn tác...' : 'Hoàn tác về đang nấu'}
                        </button>
                    )}
                </div>
            </div>
        </article>
    );
};

const WaitBadge = ({ item, priority }) => {
    if (item.status === 'FINISHED') {
        return (
            <span className="inline-flex items-center gap-1 rounded-lg bg-emerald-50 px-2 py-1 text-xs font-black text-emerald-600 dark:bg-emerald-500/10 dark:text-emerald-300">
                <Check size={13} />
                Đã xong
            </span>
        );
    }

    return (
        <span className={`inline-flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-black ${priority.badge}`}>
            {item.isOverdue ? <AlertTriangle size={13} /> : <Clock3 size={13} />}
            {item.waitMinutes} phút
        </span>
    );
};

const ActionButton = ({
    onClick,
    disabled,
    loading,
    icon,
    label,
    loadingLabel,
    className
}) => (
    <button
        type="button"
        onClick={onClick}
        disabled={disabled}
        className={`flex w-full items-center justify-center gap-2 rounded-xl py-2.5 text-sm font-black text-white shadow-sm transition active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60 ${className}`}
    >
        {loading ? <Loader2 size={17} className="animate-spin" /> : React.createElement(icon, { size: 17 })}
        {loading ? loadingLabel : label}
    </button>
);

const getPriority = (item) => {
    if (item.status === 'FINISHED') {
        return {
            card: 'border-emerald-100 dark:border-emerald-500/20',
            bar: 'bg-emerald-500',
            badge: ''
        };
    }

    if (item.waitMinutes >= 20) {
        return {
            card: 'border-red-300 ring-1 ring-red-100 dark:border-red-500/40 dark:ring-red-500/10',
            bar: 'bg-red-500',
            badge: 'bg-red-50 text-red-600 dark:bg-red-500/10 dark:text-red-300'
        };
    }

    if (item.waitMinutes >= 12) {
        return {
            card: 'border-amber-200 dark:border-amber-500/30',
            bar: 'bg-amber-400',
            badge: 'bg-amber-50 text-amber-600 dark:bg-amber-500/10 dark:text-amber-300'
        };
    }

    return {
        card: 'border-slate-200 dark:border-slate-800',
        bar: 'bg-slate-200 dark:bg-slate-700',
        badge: 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-300'
    };
};

export default KitchenCard;
