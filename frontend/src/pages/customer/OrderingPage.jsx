import { CustomerMenu } from '@features/customer-menu';
import { AiChatAssistant } from '@features/ai-assistant';
import { useSettingsStore } from '@entities/settings';
import ErrorBoundary from '@shared/ui/ErrorBoundary.jsx';

export default function OrderingPage() {
  const aiCustomerAssistantEnabled = useSettingsStore(
    (state) => state.settings.aiCustomerAssistantEnabled !== false,
  );

  return (
    <CustomerMenu
      renderAiAssistant={(hidden, orderBridge) =>
        aiCustomerAssistantEnabled ? (
          // ErrorBoundary riêng để crash AI (Web Speech API, streaming, v.v.)
          // không làm sập toàn bộ trang menu đặt món của khách hàng
          <ErrorBoundary fallbackComponent={() => null}>
            <AiChatAssistant hidden={hidden} orderBridge={orderBridge} />
          </ErrorBoundary>
        ) : null
      }
    />
  );
}
