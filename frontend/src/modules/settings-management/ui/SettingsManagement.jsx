import React, { useEffect, useMemo, useState } from 'react';
import {
  Bell,
  Bot,
  Building2,
  CreditCard,
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

import StatusModal from '@shared/ui/StatusModal.jsx';
import { useAuth } from '@modules/auth/model/AuthContext.jsx';
import { useAdminPreferences } from '@shared/hooks/useAdminPreferences.js';
import { useStatusModal } from '@shared/hooks/useStatusModal.js';
import { useWebSocket } from '@shared/hooks/useWebSocket.js';
import { settingsService } from '@shared/api/settingsService.js';

const defaultSettings = {
  restaurantName: 'Sắc Màu Quán',
  restaurantAddress: '',
  restaurantPhone: '',
  restaurantLogoUrl: '',
  vatRate: 0,
  wifiSsid: '',
  wifiPassword: '',
  autoApproveOrders: true,
  enableAiAssistant: true,
  enablePayos: true,
  enableCash: true
};

const SettingsPage = () => {
  const { user } = useAuth();
  const isManager = user?.role === 'MANAGER';
  const { statusModal, showSuccess, showError, closeStatusModal } = useStatusModal();

  const [settings, setSettings] = useState(defaultSettings);
  const [preferences, setPreferences] = useAdminPreferences();
  const [activeTab, setActiveTab] = useState(isManager ? 'restaurant' : 'preferences');
  const [loading, setLoading] = useState(isManager);
  const [saving, setSaving] = useState(false);
  const copy = settingsCopy[preferences.language] || settingsCopy.vi;

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

  useEffect(() => {
    if (!tabs.some(tab => tab.id === activeTab)) {
      setActiveTab(tabs[0].id);
    }
  }, [activeTab, tabs]);

  const loadSettings = async () => {
    if (!isManager) return;
    setLoading(true);
    try {
      const data = await settingsService.get();
      setSettings(normalizeSettings(data));
    } catch (err) {
      showError(err, copy.errors.load);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSettings();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isManager]);

  useWebSocket('/topic/settings', (message) => {
    if (message?.event === 'SETTINGS_UPDATED' && message.settings) {
      loadSettings();
      return;
    }

    if (message === 'UPDATED' || (typeof message === 'object' && message !== null)) {
      loadSettings();
    }
  });

  const wifiQrValue = useMemo(() => {
    if (!settings.wifiSsid) return '';
    return `WIFI:T:WPA;S:${escapeWifi(settings.wifiSsid)};P:${escapeWifi(settings.wifiPassword || '')};;`;
  }, [settings.wifiPassword, settings.wifiSsid]);

  const handleSaveSettings = async () => {
    if (!settings.restaurantName.trim()) {
      showError(copy.errors.restaurantNameRequired, copy.errors.invalidData);
      return;
    }

    setSaving(true);
    try {
      const updated = await settingsService.update({
        ...settings,
        restaurantName: settings.restaurantName.trim(),
        restaurantAddress: settings.restaurantAddress?.trim() || null,
        restaurantPhone: settings.restaurantPhone?.trim() || null,
        restaurantLogoUrl: settings.restaurantLogoUrl?.trim() || null,
        wifiSsid: settings.wifiSsid?.trim() || null,
        wifiPassword: settings.wifiPassword?.trim() || null,
        vatRate: Number(settings.vatRate || 0)
      });
      setSettings(normalizeSettings(updated));
      showSuccess(copy.success.saved);
    } catch (err) {
      showError(err);
    } finally {
      setSaving(false);
    }
  };

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
            const selected = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                className={`flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left text-sm font-bold transition ${
                  selected
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
        {activeTab === 'restaurant' && (
          <RestaurantTab settings={settings} setSettings={setSettings} saving={saving} onSave={handleSaveSettings} copy={copy} />
        )}

        {activeTab === 'wifi' && (
          <WifiTab
            settings={settings}
            setSettings={setSettings}
            saving={saving}
            onSave={handleSaveSettings}
            wifiQrValue={wifiQrValue}
            copy={copy}
          />
        )}

        {activeTab === 'operation' && (
          <OperationTab settings={settings} setSettings={setSettings} saving={saving} onSave={handleSaveSettings} copy={copy} />
        )}

        {activeTab === 'preferences' && (
          <PreferencesTab preferences={preferences} setPreferences={setPreferences} copy={copy} />
        )}
      </main>

      <StatusModal
        isOpen={statusModal.isOpen}
        onClose={closeStatusModal}
        type={statusModal.type}
        title={statusModal.title}
        message={statusModal.message}
      />
    </div>
  );
};

const RestaurantTab = ({ settings, setSettings, saving, onSave, copy }) => (
  <section>
    <SectionHeader icon={Building2} title={copy.restaurant.title} subtitle={copy.restaurant.subtitle} />
    <div className="grid gap-5 md:grid-cols-2">
      <TextField label={copy.restaurant.name} value={settings.restaurantName} onChange={(value) => setSettingsValue(setSettings, 'restaurantName', value)} />
      <TextField label={copy.restaurant.hotline} value={settings.restaurantPhone} onChange={(value) => setSettingsValue(setSettings, 'restaurantPhone', value)} />
      <TextField label={copy.restaurant.address} value={settings.restaurantAddress} onChange={(value) => setSettingsValue(setSettings, 'restaurantAddress', value)} className="md:col-span-2" />
      <TextField label={copy.restaurant.logoUrl} value={settings.restaurantLogoUrl} onChange={(value) => setSettingsValue(setSettings, 'restaurantLogoUrl', value)} className="md:col-span-2" />
      <TextField label={copy.restaurant.vatRate} type="number" min="0" step="0.01" value={settings.vatRate} onChange={(value) => setSettingsValue(setSettings, 'vatRate', value)} />
    </div>
    <SaveButton saving={saving} onSave={onSave} label={copy.actions.save} />
  </section>
);

const WifiTab = ({ settings, setSettings, saving, onSave, wifiQrValue, copy }) => (
  <section>
    <SectionHeader icon={Wifi} title={copy.wifi.title} subtitle={copy.wifi.subtitle} />
    <div className="grid gap-6 xl:grid-cols-[1fr_280px]">
      <div className="grid gap-5">
        <TextField label={copy.wifi.ssid} value={settings.wifiSsid} onChange={(value) => setSettingsValue(setSettings, 'wifiSsid', value)} />
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
    <div className="grid gap-4">
      <ToggleRow icon={Settings} title={copy.operation.autoApprove} description={copy.operation.autoApproveDesc} checked={settings.autoApproveOrders} onChange={(value) => setSettingsValue(setSettings, 'autoApproveOrders', value)} />
      <ToggleRow icon={Bot} title={copy.operation.ai} description={copy.operation.aiDesc} checked={settings.enableAiAssistant} onChange={(value) => setSettingsValue(setSettings, 'enableAiAssistant', value)} />
      <ToggleRow icon={CreditCard} title={copy.operation.payos} description={copy.operation.payosDesc} checked={settings.enablePayos} onChange={(value) => setSettingsValue(setSettings, 'enablePayos', value)} />
      <ToggleRow icon={CreditCard} title={copy.operation.cash} description={copy.operation.cashDesc} checked={settings.enableCash} onChange={(value) => setSettingsValue(setSettings, 'enableCash', value)} />
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
  vatRate: data.vatRate ?? 0,
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
      logoUrl: 'Logo URL',
      vatRate: 'VAT (%)'
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
      subtitle: 'Các công tắc này được lưu vào cấu hình hệ thống để nối nghiệp vụ theo từng giai đoạn.',
      autoApprove: 'Tự động duyệt đơn hàng',
      autoApproveDesc: 'Khi bật, hệ thống có thể chuyển đơn xuống bếp ngay sau khi khách đặt.',
      ai: 'Kích hoạt trợ lý AI',
      aiDesc: 'Ẩn/hiện chatbot AI ở trang gọi món của khách.',
      payos: 'Thanh toán PayOS',
      payosDesc: 'Cấu hình khả dụng cho cổng thanh toán PayOS/VietQR.',
      cash: 'Thanh toán tiền mặt',
      cashDesc: 'Cho phép ghi nhận thanh toán tiền mặt tại quầy.'
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
      logoUrl: 'Logo URL',
      vatRate: 'VAT (%)'
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
      subtitle: 'These switches are saved as system settings for business-flow integration.',
      autoApprove: 'Auto-approve orders',
      autoApproveDesc: 'When enabled, orders can be sent to the kitchen right after customers place them.',
      ai: 'Enable AI assistant',
      aiDesc: 'Show or hide the AI chatbot on the customer ordering page.',
      payos: 'PayOS payments',
      payosDesc: 'Availability setting for PayOS/VietQR payments.',
      cash: 'Cash payments',
      cashDesc: 'Allow cash payment recording at the counter.'
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
