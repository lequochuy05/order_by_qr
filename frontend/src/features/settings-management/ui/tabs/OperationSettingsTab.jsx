import { AlertTriangle, CheckCircle2, Clock, Settings } from 'lucide-react';
import { SaveButton } from '@shared/ui';

import {
  SectionHeader,
  SettingsGroup,
  TextField,
  ToggleRow,
} from '../components/FormControls.jsx';
import { setSettingsValue } from '@features/settings-management/lib/settingsUtils.js';

const OperationSettingsTab = ({
  settings,
  setSettings,
  saving,
  saveDisabled,
  onSave,
  copy,
  errors = {},
  onClearError,
}) => (
  <section className="min-w-0">
    <SectionHeader
      icon={Settings}
      title={copy.operation.title}
      subtitle={copy.operation.subtitle}
    />
    <div className="grid gap-5">
      <SettingsGroup title={copy.operation.hoursTitle}>
        <div className="grid gap-5 md:grid-cols-2">
          <TextField
            label={copy.operation.openingTime}
            // description={copy.operation.openingTimeHelp}
            type="time"
            value={settings.openingTime}
            error={errors?.openingTime || errors?.validTimeRange}
            onChange={(value) => {
              setSettingsValue(setSettings, 'openingTime', value);
              onClearError?.('openingTime');
            }}
          />
          <TextField
            label={copy.operation.closingTime}
            // description={copy.operation.closingTimeHelp}
            type="time"
            value={settings.closingTime}
            error={errors?.closingTime || errors?.validTimeRange}
            onChange={(value) => {
              setSettingsValue(setSettings, 'closingTime', value);
              onClearError?.('closingTime');
            }}
          />
        </div>
      </SettingsGroup>

      <SettingsGroup title={copy.operation.availabilityTitle}>
        <div className="grid gap-4">
          <ToggleRow
            icon={Clock}
            title={copy.operation.orderingEnabled}
            description={copy.operation.orderingEnabledDesc}
            checked={settings.orderingEnabled}
            onChange={(value) => setSettingsValue(setSettings, 'orderingEnabled', value)}
          />
          <ToggleRow
            icon={AlertTriangle}
            title={copy.operation.maintenanceMode}
            description={copy.operation.maintenanceModeDesc}
            checked={settings.maintenanceMode}
            onChange={(value) => setSettingsValue(setSettings, 'maintenanceMode', value)}
          />
        </div>
      </SettingsGroup>

      <SettingsGroup title={copy.operation.workflowTitle}>
        <div className="grid gap-4">
          <ToggleRow
            icon={CheckCircle2}
            title={copy.operation.autoConfirm}
            description={copy.operation.autoConfirmDesc}
            checked={settings.autoConfirmOrders}
            onChange={(value) => setSettingsValue(setSettings, 'autoConfirmOrders', value)}
          />
          <TextField
            label={copy.operation.overdueThreshold}
            description={copy.operation.overdueThresholdHelp}
            type="number"
            min="1"
            max="240"
            value={settings.kitchenOverdueThresholdMinutes}
            error={errors?.kitchenOverdueThresholdMinutes}
            onChange={(value) => {
              setSettingsValue(setSettings, 'kitchenOverdueThresholdMinutes', value);
              onClearError?.('kitchenOverdueThresholdMinutes');
            }}
          />
        </div>
      </SettingsGroup>
    </div>
    <SaveButton
      saving={saving}
      disabled={saveDisabled}
      onClick={onSave}
      label={copy.actions.save}
      className="mt-6"
    />
  </section>
);

export default OperationSettingsTab;
