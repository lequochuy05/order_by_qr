import CustomerMenu from '@features/customer-menu/ui/CustomerMenu.jsx';
import { AiChatAssistant } from '@features/ai-assistant';

export default function OrderingPage() {
  return (
    <CustomerMenu 
      renderAiAssistant={(hidden) => <AiChatAssistant hidden={hidden} />}
    />
  );
}
