import { Banknote, CreditCard, QrCode } from 'lucide-react';
import { SaveButton } from '@shared/ui';

import {
  SectionHeader,
  SettingsGroup,
  TextField,
  ToggleRow,
} from '../components/FormControls.jsx';
import { setSettingsValue } from '../settingsUtils.js';

const PaymentSettingsTab = ({ settings, setSettings, saving, saveDisabled, onSave, copy }) => (
  <section className="min-w-0">
    <SectionHeader icon={CreditCard} title={copy.payment.title} subtitle={copy.payment.subtitle} />

    <div className="grid gap-5">
      <SettingsGroup title={copy.payment.methodsTitle}>
        <div className="grid gap-4 lg:grid-cols-2">
          <ToggleRow
            icon={Banknote}
            title={copy.payment.cash}
            description={copy.payment.cashDesc}
            checked={settings.cashPaymentEnabled}
            onChange={(value) => setSettingsValue(setSettings, 'cashPaymentEnabled', value)}
          />
          <ToggleRow
            icon={QrCode}
            title={copy.payment.online}
            description={copy.payment.onlineDesc}
            checked={settings.onlinePaymentEnabled}
            onChange={(value) => setSettingsValue(setSettings, 'onlinePaymentEnabled', value)}
          />
        </div>
      </SettingsGroup>

      <SettingsGroup title={copy.payment.expiryTitle}>
        <TextField
          label={copy.payment.expiry}
          description={copy.payment.expiryHelp}
          type="number"
          min="1"
          max="120"
          value={settings.paymentQrExpiresInMinutes}
          onChange={(value) => setSettingsValue(setSettings, 'paymentQrExpiresInMinutes', value)}
          className="max-w-md"
        />
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

export default PaymentSettingsTab;
