import { Bot, X } from 'lucide-react';

import ChatInput from './ChatInput.jsx';
import ChatMessages from './ChatMessages.jsx';

const AiChatWindow = ({ copy, chat, launcher }) => (
  <div
    className="fixed inset-0 z-[55] bg-black/20 backdrop-blur-[1px] ai-chat-fade-in"
    onClick={launcher.handleBackdropClick}
  >
    <div
      className={`absolute bottom-0 right-0 flex h-[85vh] max-h-[700px] w-full max-w-md flex-col transition-all duration-300 ${
        launcher.isAnimating ? 'ai-chat-slide-up' : 'ai-chat-slide-down'
      }`}
      onClick={(event) => event.stopPropagation()}
      style={{
        background:
          'linear-gradient(145deg, rgba(255,255,255,0.95) 0%, rgba(255,248,240,0.98) 100%)',
        backdropFilter: 'blur(20px)',
        borderRadius: '1.5rem 1.5rem 0 0',
        boxShadow: '0 -8px 60px rgba(0,0,0,0.15), 0 -2px 20px rgba(249,115,22,0.1)',
        border: '1px solid rgba(255,255,255,0.6)',
        borderBottom: 'none',
      }}
    >
      <div
        className="relative flex flex-shrink-0 items-center gap-3 px-5 py-4"
        style={{
          background: 'linear-gradient(135deg, #0f126cff 0%, #7d80deff 100%)',
          borderRadius: '1.5rem 1.5rem 0 0',
        }}
      >
        <div className="flex h-10 w-10 items-center justify-center rounded-full border border-white/30 bg-white/20 shadow-inner backdrop-blur-sm">
          <Bot size={20} className="text-white" />
        </div>
        <div className="flex-1">
          <h3 className="text-sm font-bold tracking-tight text-white">{copy.title}</h3>
          <div className="mt-0.5 flex items-center gap-1">
            <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-green-400" />
            <span className="text-[10px] font-bold uppercase tracking-wider text-yellow-300">
              {copy.online}
            </span>
          </div>
        </div>
        <button
          type="button"
          onClick={launcher.close}
          className="flex h-8 w-8 items-center justify-center rounded-full border border-white/20 bg-white/10 text-white shadow-sm transition-colors hover:bg-white/20"
          aria-label={copy.closeLabel}
        >
          <X size={18} />
        </button>
      </div>

      <ChatMessages
        messages={chat.messages}
        isLoading={chat.isLoading}
        suggestions={copy.suggestions}
        onSuggestion={chat.sendMessage}
        messagesEndRef={chat.messagesEndRef}
      />
      <ChatInput
        copy={copy}
        input={chat.input}
        isLoading={chat.isLoading}
        inputRef={chat.inputRef}
        onInputChange={chat.setInput}
        onKeyDown={chat.handleKeyDown}
        onSend={chat.handleSend}
      />
    </div>
  </div>
);

export default AiChatWindow;
