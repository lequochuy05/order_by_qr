import { AlertTriangle, BellRing, CircleDollarSign, ShoppingBag } from 'lucide-react';
import { SaveButton } from '@shared/ui';

import {
  SectionHeader,
  SettingsGroup,
  ToggleRow,
} from '../components/FormControls.jsx';
import { setSettingsValue } from '../settingsUtils.js';

const NotificationSettingsTab = ({ settings, setSettings, saving, saveDisabled, onSave, copy }) => (
  <section className="min-w-0">
    <SectionHeader
      icon={BellRing}
      title={copy.notifications.title}
      subtitle={copy.notifications.subtitle}
    />

    <SettingsGroup title={copy.notifications.eventsTitle}>
      <div className="grid gap-4">
        <ToggleRow
          icon={ShoppingBag}
          title={copy.notifications.newOrder}
          description={copy.notifications.newOrderDesc}
          checked={settings.newOrderNotificationEnabled}
          onChange={(value) => setSettingsValue(setSettings, 'newOrderNotificationEnabled', value)}
        />
        <ToggleRow
          icon={CircleDollarSign}
          title={copy.notifications.payment}
          description={copy.notifications.paymentDesc}
          checked={settings.paymentNotificationEnabled}
          onChange={(value) => setSettingsValue(setSettings, 'paymentNotificationEnabled', value)}
        />
        <ToggleRow
          icon={AlertTriangle}
          title={copy.notifications.overdue}
          description={copy.notifications.overdueDesc}
          checked={settings.kitchenOverdueNotificationEnabled}
          onChange={(value) =>
            setSettingsValue(setSettings, 'kitchenOverdueNotificationEnabled', value)
          }
        />
      </div>
    </SettingsGroup>

    <SaveButton
      saving={saving}
      disabled={saveDisabled}
      onClick={onSave}
      label={copy.actions.save}
      className="mt-6"
    />
  </section>
);

export default NotificationSettingsTab;
