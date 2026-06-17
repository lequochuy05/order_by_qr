import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
    AlertTriangle,
    CheckCircle2,
    Clock3,
    Flame,
    Loader2,
    RefreshCw,
    Search,
    SlidersHorizontal,
    StickyNote,
    Volume2,
    VolumeX,
} from 'lucide-react';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { orderService } from '@features/order-management';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useAdminPreferences } from '@shared/hooks/useAdminPreferences.js';
import KitchenColumn from './KitchenColumn';
import { playNotificationSound, playLoudSound } from '@shared/lib/notificationSound.js';
import { showBrowserNotification } from '@shared/lib/browserNotification.js';
import { useAuth } from '@features/auth';

const OVERDUE_MINUTES = 20;

const KitchenManager = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [processingItems, setProcessingItems] = useState(new Set());
    const [searchTerm, setSearchTerm] = useState('');
    const [attentionFilter, setAttentionFilter] = useState('ALL');
    const [categoryFilter, setCategoryFilter] = useState('ALL');
    const [now, setNow] = useState(() => Date.now());
    const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
    const { user } = useAuth();
    const isMountedRef = useRef(true);
    const fetchSeqRef = useRef(0);

    const [preferences, setPreferences] = useAdminPreferences();
    const { showError } = useStatusModal();

    const fetchKitchenOrders = useCallback(async ({ silent = false } = {}) => {
        const fetchSeq = ++fetchSeqRef.current;
        if (!silent) setRefreshing(true);

        try {
            const data = await orderService.getKitchenOrders();
            if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
            setOrders(Array.isArray(data) ? data : []);
            setLastUpdatedAt(new Date());
        } catch (error) {
            if (!isMountedRef.current || fetchSeq !== fetchSeqRef.current) return;
            console.error('Failed to fetch kitchen orders:', error);
            showError('Không thể tải danh sách đơn hàng nhà bếp');
        } finally {
            if (isMountedRef.current && fetchSeq === fetchSeqRef.current) {
                setLoading(false);
                setRefreshing(false);
            }
        }
    }, [showError]);

    useEffect(() => {
        isMountedRef.current = true;
        return () => {
            isMountedRef.current = false;
        };
    }, []);

    useEffect(() => {
        fetchKitchenOrders({ silent: true });
    }, [fetchKitchenOrders]);

    useEffect(() => {
        const intervalId = window.setInterval(() => setNow(Date.now()), 30_000);
        return () => window.clearInterval(intervalId);
    }, []);



    useWebSocket('/topic/kitchen', (message) => {
        if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
            playNotificationSound();
            playLoudSound();
            showBrowserNotification('Đơn hàng nhà bếp', { body: 'Có cập nhật mới cho nhà bếp!' });
            fetchKitchenOrders({ silent: true });
        }
    });



    const handleUpdateStatus = async (itemId, newStatus) => {
        if (processingItems.has(itemId)) return;
        setProcessingItems(prev => new Set(prev).add(itemId));

        setOrders(prevOrders =>
            prevOrders.map(order => ({
                ...order,
                orderItems: order.orderItems.map(item =>
                    item.id === itemId
                        ? { ...item, status: newStatus, updatedAt: new Date().toISOString() }
                        : item
                )
            }))
        );

        try {
            await orderService.updateItemStatus(itemId, newStatus, user?.userId);
        } catch {
            showError('Không thể cập nhật trạng thái món');
            fetchKitchenOrders({ silent: true });
        } finally {
            setProcessingItems(prev => {
                const next = new Set(prev);
                next.delete(itemId);
                return next;
            });
        }
    };

    const kitchenItems = useMemo(() => {
        return orders.flatMap(order =>
            (order.orderItems || [])
                .filter(item => ['PENDING', 'COOKING', 'FINISHED'].includes(item.status))
                .map(item => {
                    const createdAt = item.createdAt || order.createdAt;
                    const statusUpdatedAt = item.updatedAt || createdAt;
                    const waitMinutes = minutesSince(createdAt, now);
                    const stageMinutes = minutesSince(statusUpdatedAt, now);
                    const itemName = item.itemNameSnapshot || item.menuItem?.name || item.combo?.name || 'Món chưa xác định';
                    const category = item.menuItem?.category?.name || (item.combo ? 'Combo' : 'Khác');

                    return {
                        ...item,
                        itemName,
                        category,
                        tableName: order.table?.tableNumber || 'Chưa gán',
                        orderId: order.id,
                        orderCreatedAt: order.createdAt,
                        createdAt,
                        statusUpdatedAt,
                        waitMinutes,
                        stageMinutes,
                        isOverdue: item.status !== 'FINISHED' && waitMinutes >= OVERDUE_MINUTES,
                        hasNotes: Boolean(item.notes?.trim())
                    };
                })
        );
    }, [orders, now]);

    const categories = useMemo(() => {
        return [...new Set(kitchenItems.map(item => item.category))].sort((a, b) => a.localeCompare(b, 'vi'));
    }, [kitchenItems]);

    const filteredItems = useMemo(() => {
        const normalizedSearch = searchTerm.trim().toLocaleLowerCase('vi');

        return kitchenItems.filter(item => {
            const searchableText = [
                item.itemName,
                item.tableName,
                item.orderId,
                item.notes,
                ...(item.options || []).flatMap(option => [option.optionName, option.optionValueName])
            ].join(' ').toLocaleLowerCase('vi');

            const matchesSearch = !normalizedSearch || searchableText.includes(normalizedSearch);
            const matchesCategory = categoryFilter === 'ALL' || item.category === categoryFilter;
            const matchesAttention = attentionFilter === 'ALL'
                || (attentionFilter === 'OVERDUE' && item.isOverdue)
                || (attentionFilter === 'NOTES' && item.hasNotes);

            return matchesSearch && matchesCategory && matchesAttention;
        });
    }, [attentionFilter, categoryFilter, kitchenItems, searchTerm]);

    const itemsByStatus = useMemo(() => {
        const grouped = { PENDING: [], COOKING: [], FINISHED: [] };

        filteredItems.forEach(item => grouped[item.status].push(item));
        grouped.PENDING.sort((a, b) => b.waitMinutes - a.waitMinutes);
        grouped.COOKING.sort((a, b) => b.waitMinutes - a.waitMinutes);
        grouped.FINISHED.sort((a, b) => new Date(b.statusUpdatedAt) - new Date(a.statusUpdatedAt));

        return grouped;
    }, [filteredItems]);

    const summary = useMemo(() => {
        const activeItems = kitchenItems.filter(item => item.status !== 'FINISHED');
        const totalWait = activeItems.reduce((sum, item) => sum + item.waitMinutes, 0);

        return {
            pending: kitchenItems.filter(item => item.status === 'PENDING').length,
            cooking: kitchenItems.filter(item => item.status === 'COOKING').length,
            finished: kitchenItems.filter(item => item.status === 'FINISHED').length,
            overdue: activeItems.filter(item => item.isOverdue).length,
            averageWait: activeItems.length ? Math.round(totalWait / activeItems.length) : 0
        };
    }, [kitchenItems]);

    const toggleSound = () => {
        setPreferences(current => ({ ...current, notificationSound: !current.notificationSound }));
    };

    const columns = [
        {
            title: 'Chờ nấu',
            subtitle: 'Ưu tiên món chờ lâu nhất',
            status: 'PENDING',
            icon: Clock3,
            tone: 'orange'
        },
        {
            title: 'Đang nấu',
            subtitle: 'Các món đang được chế biến',
            status: 'COOKING',
            icon: Flame,
            tone: 'blue'
        },
        {
            title: 'Hoàn thành',
            subtitle: 'Chờ phục vụ hoặc thanh toán',
            status: 'FINISHED',
            icon: CheckCircle2,
            tone: 'green'
        }
    ];

    return (
        <div className="min-h-screen bg-slate-50 text-slate-900 dark:bg-slate-950 dark:text-slate-100">
            <section className="overflow-hidden rounded-3xl bg-white px-5 py-6 shadow-xl shadow-slate-200/70 dark:bg-slate-900 dark:text-slate-100 dark:shadow-none sm:px-7">
                <div className="flex flex-col gap-5 xl:flex-row xl:items-center xl:justify-end">
                    <div className="flex flex-wrap items-center gap-2">
                        <button
                            type="button"
                            onClick={toggleSound}
                            className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-100 px-3 py-2 text-sm font-semibold text-slate-700 transition hover:bg-slate-200 dark:border-white/10 dark:bg-white/5 dark:text-slate-200 dark:hover:bg-white/10"
                            title="Bật hoặc tắt âm thanh thông báo"
                        >
                            {preferences.notificationSound ? <Volume2 size={17} /> : <VolumeX size={17} />}
                            {preferences.notificationSound ? 'Có âm thanh' : 'Tắt âm thanh'}
                        </button>
                        <button
                            type="button"
                            onClick={() => fetchKitchenOrders()}
                            disabled={refreshing}
                            className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2 text-sm font-bold text-white transition hover:bg-orange-400 disabled:cursor-not-allowed disabled:opacity-70"
                        >
                            <RefreshCw size={17} className={refreshing ? 'animate-spin' : ''} />
                            Làm mới
                        </button>
                    </div>
                </div>

                <div className="mt-6 grid grid-cols-2 gap-3 lg:grid-cols-5">
                    <SummaryCard label="Chờ nấu" value={summary.pending} icon={Clock3} tone="orange" />
                    <SummaryCard label="Đang nấu" value={summary.cooking} icon={Flame} tone="blue" />
                    <SummaryCard label="Vừa hoàn thành" value={summary.finished} icon={CheckCircle2} tone="green" />
                    <SummaryCard label="Món quá 20 phút" value={summary.overdue} icon={AlertTriangle} tone="red" />
                    <SummaryCard label="Chờ trung bình" value={`${summary.averageWait} phút`} icon={Clock3} tone="slate" />
                </div>
            </section>

            <section className="mt-5 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900">
                <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
                    <div className="flex flex-1 flex-col gap-3 sm:flex-row">
                        <label className="relative block flex-1">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                            <input
                                type="search"
                                value={searchTerm}
                                onChange={event => setSearchTerm(event.target.value)}
                                placeholder="Tìm theo bàn, món, mã đơn hoặc ghi chú..."
                                className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-4 text-sm outline-none transition focus:border-orange-400 focus:ring-2 focus:ring-orange-100 dark:border-slate-700 dark:bg-slate-950 dark:focus:ring-orange-500/10"
                            />
                        </label>

                        <label className="relative block sm:w-56">
                            <SlidersHorizontal className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={17} />
                            <select
                                value={categoryFilter}
                                onChange={event => setCategoryFilter(event.target.value)}
                                className="w-full appearance-none rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-4 text-sm font-medium outline-none transition focus:border-orange-400 dark:border-slate-700 dark:bg-slate-950"
                            >
                                <option value="ALL">Tất cả danh mục</option>
                                {categories.map(category => (
                                    <option key={category} value={category}>{category}</option>
                                ))}
                            </select>
                        </label>
                    </div>

                    <div className="flex flex-wrap items-center gap-2">
                        <FilterButton
                            active={attentionFilter === 'ALL'}
                            onClick={() => setAttentionFilter('ALL')}
                            label="Tất cả"
                        />
                        <FilterButton
                            active={attentionFilter === 'OVERDUE'}
                            onClick={() => setAttentionFilter('OVERDUE')}
                            label="Quá hạn"
                            icon={AlertTriangle}
                        />
                        <FilterButton
                            active={attentionFilter === 'NOTES'}
                            onClick={() => setAttentionFilter('NOTES')}
                            label="Có ghi chú"
                            icon={StickyNote}
                        />
                        <span className="ml-1 text-xs text-slate-400">
                            {lastUpdatedAt ? `Cập nhật lúc ${lastUpdatedAt.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}` : 'Đang đồng bộ...'}
                        </span>
                    </div>
                </div>
            </section>

            {loading ? (
                <div className="flex h-64 items-center justify-center">
                    <Loader2 className="animate-spin text-orange-500" size={42} />
                </div>
            ) : (
                <div className="mt-5 grid gap-5 xl:grid-cols-3">
                    {columns.map(column => (
                        <KitchenColumn
                            key={column.status}
                            {...column}
                            items={itemsByStatus[column.status]}
                            onUpdateStatus={handleUpdateStatus}
                            processingItems={processingItems}
                        />
                    ))}
                </div>
            )}
        </div>
    );
};

const minutesSince = (dateValue, now) => {
    const timestamp = new Date(dateValue).getTime();
    if (Number.isNaN(timestamp)) return 0;
    return Math.max(0, Math.floor((now - timestamp) / 60_000));
};


const SummaryCard = ({ label, value, icon, tone }) => {
    const tones = {
        orange: 'bg-orange-50 text-orange-600 ring-orange-200 dark:bg-orange-500/10 dark:text-orange-300 dark:ring-orange-400/20',
        blue: 'bg-blue-50 text-blue-600 ring-blue-200 dark:bg-blue-500/10 dark:text-blue-300 dark:ring-blue-400/20',
        green: 'bg-emerald-50 text-emerald-600 ring-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-300 dark:ring-emerald-400/20',
        red: 'bg-red-50 text-red-600 ring-red-200 dark:bg-red-500/10 dark:text-red-300 dark:ring-red-400/20',
        slate: 'bg-slate-50 text-slate-600 ring-slate-200 dark:bg-white/5 dark:text-slate-300 dark:ring-white/10'
    };

    return (
        <div className={`rounded-2xl p-4 ring-1 ${tones[tone]}`}>
            <div className="flex items-center justify-between gap-3">
                <div>
                    <p className="text-xs font-bold uppercase tracking-wider opacity-80">{label}</p>
                    <p className="mt-1 text-2xl font-black text-slate-900 dark:text-white">{value}</p>
                </div>
                {React.createElement(icon, { size: 22, className: 'opacity-80' })}
            </div>
        </div>
    );
};

const FilterButton = ({ active, onClick, label, icon }) => (
    <button
        type="button"
        onClick={onClick}
        className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-bold transition ${active
            ? 'bg-slate-900 text-white shadow-sm dark:bg-orange-500'
            : 'bg-slate-100 text-slate-600 hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'
            }`}
    >
        {icon && React.createElement(icon, { size: 14 })}
        {label}
    </button>
);

export default KitchenManager;
