import { EyeOff, Sparkles, UtensilsCrossed } from 'lucide-react';
import { SaveButton } from '@shared/ui';

import {
  SectionHeader,
  SettingsGroup,
  ToggleRow,
} from '../components/FormControls.jsx';
import { setSettingsValue } from '../settingsUtils.js';

const CustomerMenuSettingsTab = ({ settings, setSettings, saving, saveDisabled, onSave, copy }) => (
  <section className="min-w-0">
    <SectionHeader
      icon={UtensilsCrossed}
      title={copy.customerMenu.title}
      subtitle={copy.customerMenu.subtitle}
    />

    <SettingsGroup title={copy.customerMenu.visibilityTitle}>
      <div className="grid gap-4">
        <ToggleRow
          icon={EyeOff}
          title={copy.customerMenu.unavailable}
          description={copy.customerMenu.unavailableDesc}
          checked={settings.showUnavailableItems}
          onChange={(value) => setSettingsValue(setSettings, 'showUnavailableItems', value)}
        />
        <ToggleRow
          icon={Sparkles}
          title={copy.customerMenu.recommendations}
          description={copy.customerMenu.recommendationsDesc}
          checked={settings.showRecommendations}
          onChange={(value) => setSettingsValue(setSettings, 'showRecommendations', value)}
        />
        <ToggleRow
          icon={UtensilsCrossed}
          title={copy.customerMenu.combos}
          description={copy.customerMenu.combosDesc}
          checked={settings.showCombos}
          onChange={(value) => setSettingsValue(setSettings, 'showCombos', value)}
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

export default CustomerMenuSettingsTab;
