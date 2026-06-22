import './AiChatAssistant.css';

import useAiChat from '../model/useAiChat.js';
import useAiLauncher from '../model/useAiLauncher.js';
import AiChatLauncher from './AiChatLauncher.jsx';
import AiChatWindow from './AiChatWindow.jsx';

const AiChatAssistant = ({ hidden = false, language = 'vi' }) => {
  const chat = useAiChat(language);
  const launcher = useAiLauncher({ hidden, onOpen: chat.focusInput });

  if (hidden) return null;

  return (
    <>
      {!launcher.isOpen && <AiChatLauncher copy={chat.copy} launcher={launcher} />}
      {launcher.isOpen && <AiChatWindow copy={chat.copy} chat={chat} launcher={launcher} />}
    </>
  );
};

export default AiChatAssistant;
