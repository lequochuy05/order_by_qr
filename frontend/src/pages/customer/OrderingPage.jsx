import CustomerMenu from '@features/customer-menu/ui/CustomerMenu.jsx';
import { AiChatAssistant } from '@features/ai-assistant';
import ErrorBoundary from '@shared/ui/ErrorBoundary.jsx';

export default function OrderingPage() {
  return (
    <CustomerMenu
      renderAiAssistant={(hidden) => (
        // ErrorBoundary riêng để crash AI (Web Speech API, streaming, v.v.)
        // không làm sập toàn bộ trang menu đặt món của khách hàng
        <ErrorBoundary fallbackComponent={() => null}>
          <AiChatAssistant hidden={hidden} />
        </ErrorBoundary>
      )}
    />
  );
}
