import React, { useCallback, useMemo, useState } from 'react';
import {
  AlertTriangle,
  Bell,
  Building2,
  Clock,
  Loader2,
  Moon,
  QrCode,
  Save,
  Settings,
  Sun,
  Volume2,
  Wifi
} from 'lucide-react';
import { QRCodeCanvas } from 'qrcode.react';

import { useAuth } from '@modules/auth/model/AuthContext.jsx';
import { useAdminPreferences } from '@shared/hooks/useAdminPreferences.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useSettingsQuery } from '../api/settingsQueries.js';
import { useUpdateSettingsMutation } from '../api/settingsMutations.js';

const defaultSettings = {
  restaurantName: 'Sắc Màu Quán',
  restaurantAddress: '',
  restaurantPhone: '',
  restaurantEmail: '',
  logoUrl: '',
  taxPercent: 0,
  serviceChargePercent: 0,
  currency: 'VND',
  wifiName: '',
  wifiPassword: '',
  openingTime: '',
  closingTime: '',
  orderingEnabled: true,
  maintenanceMode: false
};

const SettingsPage = () => {
  const { user } = useAuth();
  const isManager = user?.role === 'MANAGER';
  const { showSuccess, showError } = useStatusModal();

  const [preferences, setPreferences] = useAdminPreferences();
  const [activeTab, setActiveTab] = useState(isManager ? 'restaurant' : 'preferences');
  const copy = settingsCopy[preferences.language] || settingsCopy.vi;

  const { data: serverSettings, isLoading: loading } = useSettingsQuery({
    enabled: isManager,
  });

  const normalizedServerSettings = useMemo(
    () => normalizeSettings(serverSettings || defaultSettings),
    [serverSettings]
  );
  const [settingsDraft, setSettingsDraft] = useState(null);
  const settings = settingsDraft ?? normalizedServerSettings;
  const setSettings = useCallback((updater) => {
    setSettingsDraft(prev => {
      const current = prev ?? normalizedServerSettings;
      return typeof updater === 'function' ? updater(current) : updater;
    });
  }, [normalizedServerSettings]);

  const tabs = useMemo(() => {
    const personalTab = { id: 'preferences', label: copy.tabs.preferences, icon: Bell };
    if (!isManager) return [personalTab];

    return [
      { id: 'restaurant', label: copy.tabs.restaurant, icon: Building2 },
      { id: 'wifi', label: copy.tabs.wifi, icon: Wifi },
      { id: 'operation', label: copy.tabs.operation, icon: Settings },
      personalTab
    ];
  }, [copy, isManager]);
  const activeTabId = tabs.some(tab => tab.id === activeTab) ? activeTab : tabs[0].id;

  const updateMutation = useUpdateSettingsMutation({
    onSuccess: (updated) => {
      setSettingsDraft(normalizeSettings(updated));
      showSuccess(copy.success.saved);
    },
    onError: (err) => {
      showError(err);
    },
  });

  const wifiQrValue = useMemo(() => {
    if (!settings.wifiName) return '';
    return `WIFI:T:WPA;S:${escapeWifi(settings.wifiName)};P:${escapeWifi(settings.wifiPassword || '')};;`;
  }, [settings.wifiPassword, settings.wifiName]);

  const handleSaveSettings = () => {
    if (!settings.restaurantName.trim()) {
      showError(copy.errors.restaurantNameRequired, copy.errors.invalidData);
      return;
    }

    updateMutation.mutate({
      restaurantName: settings.restaurantName.trim(),
      restaurantAddress: settings.restaurantAddress?.trim() || null,
      restaurantPhone: settings.restaurantPhone?.trim() || null,
      restaurantEmail: settings.restaurantEmail?.trim() || null,
      logoUrl: settings.logoUrl?.trim() || null,
      wifiName: settings.wifiName?.trim() || null,
      wifiPassword: settings.wifiPassword?.trim() || null,
      openingTime: settings.openingTime || null,
      closingTime: settings.closingTime || null,
      currency: settings.currency?.trim().toUpperCase() || 'VND',
      taxPercent: Number(settings.taxPercent || 0),
      serviceChargePercent: Number(settings.serviceChargePercent || 0),
      orderingEnabled: Boolean(settings.orderingEnabled),
      maintenanceMode: Boolean(settings.maintenanceMode)
    });
  };

  const saving = updateMutation.isPending;

  if (loading) {
    return (
      <div className="flex min-h-[420px] items-center justify-center text-orange-500 dark:text-orange-400">
        <Loader2 className="animate-spin" size={36} />
      </div>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[260px_1fr]">
      <aside className="rounded-lg border border-slate-200 bg-white p-3 shadow-sm transition-colors dark:border-slate-800 dark:bg-slate-900">
        <nav className="space-y-1">
          {tabs.map(tab => {
            const Icon = tab.icon;
            const selected = activeTabId === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                className={`flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm font-bold transition ${selected
                    ? 'bg-orange-50 text-orange-600 dark:bg-orange-500/10 dark:text-orange-300'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white'
                  }`}
              >
                <Icon size={18} />
                {tab.label}
              </button>
            );
          })}
        </nav>
      </aside>

      <main className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm transition-colors dark:border-slate-800 dark:bg-slate-900">
        {activeTabId === 'restaurant' && (
          <RestaurantTab settings={settings} setSettings={setSettings} saving={saving} onSave={handleSaveSettings} copy={copy} />
        )}

        {activeTabId === 'wifi' && (
          <WifiTab
            settings={settings}
            setSettings={setSettings}
            saving={saving}
            onSave={handleSaveSettings}
            wifiQrValue={wifiQrValue}
            copy={copy}
          />
        )}

        {activeTabId === 'operation' && (
          <OperationTab settings={settings} setSettings={setSettings} saving={saving} onSave={handleSaveSettings} copy={copy} />
        )}

        {activeTabId === 'preferences' && (
          <PreferencesTab preferences={preferences} setPreferences={setPreferences} copy={copy} />
        )}
      </main>
    </div>
  );
};

const RestaurantTab = ({ settings, setSettings, saving, onSave, copy }) => (
  <section>
    <SectionHeader icon={Building2} title={copy.restaurant.title} subtitle={copy.restaurant.subtitle} />
    <div className="grid gap-5 md:grid-cols-2">
      <TextField label={copy.restaurant.name} value={settings.restaurantName} onChange={(value) => setSettingsValue(setSettings, 'restaurantName', value)} />
      <TextField label={copy.restaurant.hotline} value={settings.restaurantPhone} onChange={(value) => setSettingsValue(setSettings, 'restaurantPhone', value)} />
      <TextField label={copy.restaurant.email} type="email" value={settings.restaurantEmail} onChange={(value) => setSettingsValue(setSettings, 'restaurantEmail', value)} />
      <TextField label={copy.restaurant.currency} maxLength="10" value={settings.currency} onChange={(value) => setSettingsValue(setSettings, 'currency', value)} />
      <TextField label={copy.restaurant.address} value={settings.restaurantAddress} onChange={(value) => setSettingsValue(setSettings, 'restaurantAddress', value)} className="md:col-span-2" />
      <TextField label={copy.restaurant.logoUrl} value={settings.logoUrl} onChange={(value) => setSettingsValue(setSettings, 'logoUrl', value)} className="md:col-span-2" />
      <TextField label={copy.restaurant.taxPercent} type="number" min="0" max="100" step="0.01" value={settings.taxPercent} onChange={(value) => setSettingsValue(setSettings, 'taxPercent', value)} />
      <TextField label={copy.restaurant.serviceChargePercent} type="number" min="0" max="100" step="0.01" value={settings.serviceChargePercent} onChange={(value) => setSettingsValue(setSettings, 'serviceChargePercent', value)} />
    </div>
    <SaveButton saving={saving} onSave={onSave} label={copy.actions.save} />
  </section>
);

const WifiTab = ({ settings, setSettings, saving, onSave, wifiQrValue, copy }) => (
  <section>
    <SectionHeader icon={Wifi} title={copy.wifi.title} subtitle={copy.wifi.subtitle} />
    <div className="grid gap-6 xl:grid-cols-[1fr_280px]">
      <div className="grid gap-5">
        <TextField label={copy.wifi.ssid} value={settings.wifiName} onChange={(value) => setSettingsValue(setSettings, 'wifiName', value)} />
        <TextField label={copy.wifi.password} value={settings.wifiPassword} onChange={(value) => setSettingsValue(setSettings, 'wifiPassword', value)} />
      </div>
      <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-200 bg-slate-50 p-5 transition-colors dark:border-slate-700 dark:bg-slate-950">
        {wifiQrValue ? (
          <>
            <QRCodeCanvas value={wifiQrValue} size={190} includeMargin />
            <p className="mt-3 text-center text-xs font-semibold text-slate-500 dark:text-slate-400">{copy.wifi.qrLabel}</p>
          </>
        ) : (
          <div className="flex h-48 flex-col items-center justify-center text-slate-400 dark:text-slate-500">
            <QrCode size={44} />
            <p className="mt-3 text-sm font-semibold">{copy.wifi.empty}</p>
          </div>
        )}
      </div>
    </div>
    <SaveButton saving={saving} onSave={onSave} label={copy.actions.save} />
  </section>
);

const OperationTab = ({ settings, setSettings, saving, onSave, copy }) => (
  <section>
    <SectionHeader icon={Settings} title={copy.operation.title} subtitle={copy.operation.subtitle} />
    <div className="grid gap-5">
      <div className="grid gap-5 md:grid-cols-2">
        <TextField label={copy.operation.openingTime} type="time" value={settings.openingTime} onChange={(value) => setSettingsValue(setSettings, 'openingTime', value)} />
        <TextField label={copy.operation.closingTime} type="time" value={settings.closingTime} onChange={(value) => setSettingsValue(setSettings, 'closingTime', value)} />
      </div>
      <ToggleRow icon={Clock} title={copy.operation.orderingEnabled} description={copy.operation.orderingEnabledDesc} checked={settings.orderingEnabled} onChange={(value) => setSettingsValue(setSettings, 'orderingEnabled', value)} />
      <ToggleRow icon={AlertTriangle} title={copy.operation.maintenanceMode} description={copy.operation.maintenanceModeDesc} checked={settings.maintenanceMode} onChange={(value) => setSettingsValue(setSettings, 'maintenanceMode', value)} />
    </div>
    <SaveButton saving={saving} onSave={onSave} label={copy.actions.save} />
  </section>
);

const PreferencesTab = ({ preferences, setPreferences, copy }) => (
  <section>
    <SectionHeader icon={Bell} title={copy.preferences.title} subtitle={copy.preferences.subtitle} />
    <div className="grid gap-4">
      <ToggleRow icon={Volume2} title={copy.preferences.sound} description={copy.preferences.soundDesc} checked={preferences.notificationSound} onChange={(value) => setPreferenceValue(setPreferences, 'notificationSound', value)} />
      <ToggleRow icon={Bell} title={copy.preferences.loudSound} description={copy.preferences.loudSoundDesc} checked={preferences.loudSound} onChange={(value) => setPreferenceValue(setPreferences, 'loudSound', value)} />
      <ToggleRow icon={preferences.darkMode ? Moon : Sun} title={copy.preferences.darkMode} description={copy.preferences.darkModeDesc} checked={preferences.darkMode} onChange={(value) => setPreferenceValue(setPreferences, 'darkMode', value)} />
      <label className="grid gap-2 rounded-lg border border-slate-200 p-4 transition-colors dark:border-slate-800">
        <span className="text-sm font-bold text-slate-800 dark:text-slate-100">{copy.preferences.language}</span>
        <select
          value={preferences.language}
          onChange={(event) => setPreferenceValue(setPreferences, 'language', event.target.value)}
          className="h-11 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition focus:border-orange-400 focus:ring-2 focus:ring-orange-100 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100 dark:focus:ring-orange-500/20"
        >
          <option value="vi">Tiếng Việt</option>
          <option value="en">English</option>
        </select>
      </label>
    </div>
  </section>
);

const SectionHeader = ({ icon, title, subtitle }) => (
  <div className="mb-6 flex items-start gap-3">
    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-orange-50 text-orange-500 dark:bg-orange-500/10 dark:text-orange-300">
      {React.createElement(icon, { size: 20 })}
    </div>
    <div>
      <h2 className="text-lg font-black text-slate-900 dark:text-slate-100">{title}</h2>
      <p className="text-sm text-slate-500 dark:text-slate-400">{subtitle}</p>
    </div>
  </div>
);

const TextField = ({ label, value, onChange, className = '', ...props }) => (
  <label className={`space-y-2 ${className}`}>
    <span className="text-sm font-bold text-slate-700 dark:text-slate-200">{label}</span>
    <input
      value={value ?? ''}
      onChange={(event) => onChange(event.target.value)}
      className="h-11 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-orange-400 focus:ring-2 focus:ring-orange-100 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-100 dark:focus:ring-orange-500/20"
      {...props}
    />
  </label>
);

const ToggleRow = ({ icon, title, description, checked, onChange }) => (
  <label className="flex cursor-pointer items-center justify-between gap-4 rounded-lg border border-slate-200 p-4 transition hover:bg-slate-50 dark:border-slate-800 dark:hover:bg-slate-800/70">
    <span className="flex items-start gap-3">
      <span className="mt-0.5 flex h-9 w-9 items-center justify-center rounded-lg bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300">
        {React.createElement(icon, { size: 17 })}
      </span>
      <span>
        <span className="block text-sm font-black text-slate-900 dark:text-slate-100">{title}</span>
        <span className="text-sm text-slate-500 dark:text-slate-400">{description}</span>
      </span>
    </span>
    <input
      type="checkbox"
      checked={Boolean(checked)}
      onChange={(event) => onChange(event.target.checked)}
      className="h-5 w-5 accent-orange-500"
    />
  </label>
);

const SaveButton = ({ saving, onSave, label }) => (
  <button
    type="button"
    onClick={onSave}
    disabled={saving}
    className="mt-6 inline-flex items-center gap-2 rounded-lg bg-orange-500 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-orange-600 disabled:cursor-not-allowed disabled:opacity-60"
  >
    {saving ? <Loader2 className="animate-spin" size={17} /> : <Save size={17} />}
    {label}
  </button>
);

const normalizeSettings = (data = {}) => ({
  ...defaultSettings,
  ...data,
  logoUrl: data.logoUrl ?? data.restaurantLogoUrl ?? '',
  wifiName: data.wifiName ?? data.wifiSsid ?? '',
  taxPercent: data.taxPercent ?? data.vatRate ?? 0,
  serviceChargePercent: data.serviceChargePercent ?? 0,
  currency: data.currency ?? 'VND',
  openingTime: data.openingTime ?? '',
  closingTime: data.closingTime ?? '',
  orderingEnabled: data.orderingEnabled ?? data.autoApproveOrders ?? true,
  maintenanceMode: data.maintenanceMode ?? false,
  wifiPassword: data.wifiPassword ?? ''
});

const setSettingsValue = (setSettings, key, value) => {
  setSettings(prev => ({ ...prev, [key]: value }));
};

const setPreferenceValue = (setPreferences, key, value) => {
  setPreferences(prev => ({ ...prev, [key]: value }));
};

const escapeWifi = (value) => String(value).replace(/([\\;,":])/g, '\\$1');

const settingsCopy = {
  vi: {
    tabs: {
      restaurant: 'Thông tin quán',
      wifi: 'WiFi & QR',
      operation: 'Vận hành & thanh toán',
      preferences: 'Cá nhân hóa'
    },
    actions: {
      save: 'Lưu cấu hình'
    },
    success: {
      saved: 'Cấu hình hệ thống đã được cập nhật.'
    },
    errors: {
      load: 'Không thể tải cài đặt',
      invalidData: 'Dữ liệu chưa hợp lệ',
      restaurantNameRequired: 'Tên quán không được để trống.'
    },
    restaurant: {
      title: 'Thông tin quán',
      subtitle: 'Các thông tin này có thể hiển thị ở trang gọi món của khách.',
      name: 'Tên quán',
      hotline: 'Hotline',
      address: 'Địa chỉ',
      email: 'Email',
      currency: 'Tiền tệ',
      logoUrl: 'Logo URL',
      taxPercent: 'Thuế (%)',
      serviceChargePercent: 'Phí dịch vụ (%)'
    },
    wifi: {
      title: 'WiFi & mã QR',
      subtitle: 'Sinh QR để khách quét và kết nối WiFi nhanh.',
      ssid: 'Tên WiFi (SSID)',
      password: 'Mật khẩu WiFi',
      qrLabel: 'QR kết nối WiFi',
      empty: 'Nhập SSID để tạo QR'
    },
    operation: {
      title: 'Vận hành & thanh toán',
      subtitle: 'Khung giờ và trạng thái nhận đơn được hiển thị ở trang gọi món của khách.',
      openingTime: 'Giờ mở cửa',
      closingTime: 'Giờ đóng cửa',
      orderingEnabled: 'Nhận đơn từ khách',
      orderingEnabledDesc: 'Khi tắt, khách vẫn xem được menu nhưng không thể gửi đơn mới.',
      maintenanceMode: 'Chế độ bảo trì',
      maintenanceModeDesc: 'Tạm khóa trải nghiệm đặt món khi quán cần bảo trì hoặc đồng bộ dữ liệu.'
    },
    preferences: {
      title: 'Cá nhân hóa',
      subtitle: 'Tùy chọn này lưu trên trình duyệt của từng nhân viên.',
      sound: 'Âm thanh thông báo',
      soundDesc: 'Bật chuông báo khi có cập nhật trong ca làm việc.',
      loudSound: 'Âm báo lớn',
      loudSoundDesc: 'Phù hợp cho khu vực bếp hoặc môi trường ồn.',
      darkMode: 'Giao diện tối',
      darkModeDesc: 'Lưu lựa chọn giao diện cho trình duyệt hiện tại.',
      language: 'Ngôn ngữ'
    }
  },
  en: {
    tabs: {
      restaurant: 'Restaurant Info',
      wifi: 'WiFi & QR',
      operation: 'Operations & Payments',
      preferences: 'Preferences'
    },
    actions: {
      save: 'Save settings'
    },
    success: {
      saved: 'System settings have been updated.'
    },
    errors: {
      load: 'Unable to load settings',
      invalidData: 'Invalid data',
      restaurantNameRequired: 'Restaurant name is required.'
    },
    restaurant: {
      title: 'Restaurant Info',
      subtitle: 'These details can be shown on the customer ordering page.',
      name: 'Restaurant name',
      hotline: 'Hotline',
      address: 'Address',
      email: 'Email',
      currency: 'Currency',
      logoUrl: 'Logo URL',
      taxPercent: 'Tax (%)',
      serviceChargePercent: 'Service charge (%)'
    },
    wifi: {
      title: 'WiFi & QR Code',
      subtitle: 'Generate a QR code so customers can connect to WiFi quickly.',
      ssid: 'WiFi name (SSID)',
      password: 'WiFi password',
      qrLabel: 'WiFi connection QR',
      empty: 'Enter an SSID to generate QR'
    },
    operation: {
      title: 'Operations & Payments',
      subtitle: 'Ordering hours and availability are shown on the customer ordering page.',
      openingTime: 'Opening time',
      closingTime: 'Closing time',
      orderingEnabled: 'Accept customer orders',
      orderingEnabledDesc: 'When disabled, customers can still view the menu but cannot submit new orders.',
      maintenanceMode: 'Maintenance mode',
      maintenanceModeDesc: 'Temporarily lock ordering while the restaurant is under maintenance or syncing data.'
    },
    preferences: {
      title: 'Preferences',
      subtitle: 'These options are saved in each staff member browser.',
      sound: 'Notification sound',
      soundDesc: 'Play a sound when updates arrive during a shift.',
      loudSound: 'Loud alert sound',
      loudSoundDesc: 'Useful for the kitchen or noisy environments.',
      darkMode: 'Dark mode',
      darkModeDesc: 'Save the display preference for this browser.',
      language: 'Language'
    }
  }
};

export default SettingsPage;
