import { useCallback, useMemo, useState } from 'react';
import {
  Bell,
  BellRing,
  Building2,
  CreditCard,
  Loader2,
  ReceiptText,
  Settings,
  UtensilsCrossed,
  Wifi,
} from 'lucide-react';

import { useAuth } from '@features/auth/model/AuthContext.jsx';
import { useAdminPreferences } from '@shared/hooks/useAdminPreferences.js';
import { getErrorDetails } from '@shared/lib/errorMessages.js';
import { showErrorToast, showSuccessToast } from '@shared/lib/toast.js';
import { useSettingsQuery } from '../api/settingsQueries.js';
import { useUpdateSettingsMutation, useUploadLogoMutation } from '../api/settingsMutations.js';
import SettingsLayout from './components/SettingsLayout.jsx';
import BillSettingsTab from './tabs/BillSettingsTab.jsx';
import CustomerMenuSettingsTab from './tabs/CustomerMenuSettingsTab.jsx';
import NotificationSettingsTab from './tabs/NotificationSettingsTab.jsx';
import PaymentSettingsTab from './tabs/PaymentSettingsTab.jsx';
import RestaurantSettingsTab from './tabs/RestaurantSettingsTab.jsx';
import WifiSettingsTab from './tabs/WifiSettingsTab.jsx';
import OperationSettingsTab from './tabs/OperationSettingsTab.jsx';
import PreferencesTab from './tabs/PreferencesTab.jsx';
import { defaultSettings, settingsCopy } from './settingsConstants.js';
import {
  escapeWifi,
  hasSettingsChanges,
  normalizeSettings,
  toSettingsPayload,
} from './settingsUtils.js';

const SettingsPage = () => {
  const { user } = useAuth();
  const isManager = user?.role === 'MANAGER';

  const [preferences, setPreferences] = useAdminPreferences();
  const [activeTab, setActiveTab] = useState(isManager ? 'restaurant' : 'preferences');
  const [settingsErrors, setSettingsErrors] = useState({});
  const copy = settingsCopy[preferences.language] || settingsCopy.vi;

  const { data: serverSettings, isLoading: loading } = useSettingsQuery({
    enabled: isManager,
  });

  const normalizedServerSettings = useMemo(
    () => normalizeSettings(serverSettings || defaultSettings),
    [serverSettings],
  );
  const [settingsDraft, setSettingsDraft] = useState(null);
  const settings = settingsDraft ?? normalizedServerSettings;
  const hasChanges = useMemo(
    () => hasSettingsChanges(settings, normalizedServerSettings),
    [settings, normalizedServerSettings],
  );
  const setSettings = useCallback(
    (updater) => {
      setSettingsDraft((prev) => {
        const current = prev ?? normalizedServerSettings;
        return typeof updater === 'function' ? updater(current) : updater;
      });
    },
    [normalizedServerSettings],
  );

  const tabs = useMemo(() => {
    const personalTab = { id: 'preferences', label: copy.tabs.preferences, icon: Bell };
    if (!isManager) return [personalTab];

    return [
      { id: 'restaurant', label: copy.tabs.restaurant, icon: Building2 },
      { id: 'payment', label: copy.tabs.payment, icon: CreditCard },
      { id: 'customerMenu', label: copy.tabs.customerMenu, icon: UtensilsCrossed },
      { id: 'operation', label: copy.tabs.operation, icon: Settings },
      { id: 'bill', label: copy.tabs.bill, icon: ReceiptText },
      { id: 'notifications', label: copy.tabs.notifications, icon: BellRing },
      { id: 'wifi', label: copy.tabs.wifi, icon: Wifi },
      personalTab,
    ];
  }, [copy, isManager]);
  const activeTabId = tabs.some((tab) => tab.id === activeTab) ? activeTab : tabs[0].id;

  const updateMutation = useUpdateSettingsMutation({
    onSuccess: (updated) => {
      setSettingsDraft(normalizeSettings(updated));
      setSettingsErrors({});
      showSuccessToast(copy.success.saved);
    },
    onError: (err) => {
      const details = getErrorDetails(err);
      if (details.restaurantName) {
        setSettingsErrors({ restaurantName: details.restaurantName });
        setActiveTab('restaurant');
      } else {
        showErrorToast(err);
      }
    },
  });

  const uploadLogoMutation = useUploadLogoMutation({
    onSuccess: (updated) => {
      setSettingsDraft((current) => ({
        ...(current ?? normalizedServerSettings),
        logoUrl: updated.logoUrl || '',
        version: updated.version,
      }));
      showSuccessToast(copy.success.logoUploaded);
    },
    onError: showErrorToast,
  });

  const wifiQrValue = useMemo(() => {
    if (!settings.wifiName) return '';
    return `WIFI:T:WPA;S:${escapeWifi(settings.wifiName)};P:${escapeWifi(settings.wifiPassword || '')};;`;
  }, [settings.wifiPassword, settings.wifiName]);

  const handleSaveSettings = () => {
    if (!settings.restaurantName.trim()) {
      setSettingsErrors({ restaurantName: copy.errors.restaurantNameRequired });
      setActiveTab('restaurant');
      return;
    }
    if (!settings.cashPaymentEnabled && !settings.onlinePaymentEnabled) {
      showErrorToast(copy.errors.paymentMethodRequired);
      setActiveTab('payment');
      return;
    }

    updateMutation.mutate(toSettingsPayload(settings));
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
    <SettingsLayout
      title={copy.page.title}
      subtitle={copy.page.subtitle}
      tabs={tabs}
      activeTabId={activeTabId}
      onTabChange={setActiveTab}
    >
      {activeTabId === 'restaurant' && (
        <RestaurantSettingsTab
          settings={settings}
          setSettings={setSettings}
          saving={saving}
          saveDisabled={!hasChanges}
          onSave={handleSaveSettings}
          copy={copy}
          errors={settingsErrors}
          onClearError={(field) => setSettingsErrors((current) => ({ ...current, [field]: null }))}
          onUploadLogo={(file) => uploadLogoMutation.mutate(file)}
          uploadingLogo={uploadLogoMutation.isPending}
        />
      )}

      {activeTabId === 'payment' && (
        <PaymentSettingsTab
          settings={settings}
          setSettings={setSettings}
          saving={saving}
          saveDisabled={!hasChanges}
          onSave={handleSaveSettings}
          copy={copy}
        />
      )}

      {activeTabId === 'customerMenu' && (
        <CustomerMenuSettingsTab
          settings={settings}
          setSettings={setSettings}
          saving={saving}
          saveDisabled={!hasChanges}
          onSave={handleSaveSettings}
          copy={copy}
        />
      )}

      {activeTabId === 'wifi' && (
        <WifiSettingsTab
          settings={settings}
          setSettings={setSettings}
          saving={saving}
          saveDisabled={!hasChanges}
          onSave={handleSaveSettings}
          wifiQrValue={wifiQrValue}
          copy={copy}
        />
      )}

      {activeTabId === 'operation' && (
        <OperationSettingsTab
          settings={settings}
          setSettings={setSettings}
          saving={saving}
          saveDisabled={!hasChanges}
          onSave={handleSaveSettings}
          copy={copy}
        />
      )}

      {activeTabId === 'bill' && (
        <BillSettingsTab
          settings={settings}
          setSettings={setSettings}
          saving={saving}
          saveDisabled={!hasChanges}
          onSave={handleSaveSettings}
          copy={copy}
        />
      )}

      {activeTabId === 'notifications' && (
        <NotificationSettingsTab
          settings={settings}
          setSettings={setSettings}
          saving={saving}
          saveDisabled={!hasChanges}
          onSave={handleSaveSettings}
          copy={copy}
        />
      )}

      {activeTabId === 'preferences' && (
        <PreferencesTab preferences={preferences} setPreferences={setPreferences} copy={copy} />
      )}
    </SettingsLayout>
  );
};

export default SettingsPage;
