import { Bot, X } from 'lucide-react';

import ChatInput from './ChatInput.jsx';
import ChatMessages from './ChatMessages.jsx';

const AiChatWindow = ({ copy, chat, launcher }) => (
  <div
    className="fixed inset-0 z-[55] bg-slate-950/40 backdrop-blur-sm ai-chat-fade-in"
    onClick={launcher.handleBackdropClick}
  >
    <div
      className={`absolute bottom-0 right-0 flex h-[85vh] max-h-[700px] w-full max-w-md flex-col transition-all duration-300 ${
        launcher.isAnimating ? 'ai-chat-slide-up' : 'ai-chat-slide-down'
      }`}
      onClick={(event) => event.stopPropagation()}
      style={{
        background: 'rgba(255, 255, 255, 0.98)',
        backdropFilter: 'blur(20px)',
        borderRadius: '1.5rem 1.5rem 0 0',
        boxShadow: '0 -10px 40px rgba(99, 102, 241, 0.15)',
        border: '1px solid rgba(226, 232, 240, 0.8)',
        borderBottom: 'none',
      }}
    >
      <div
        className="relative flex flex-shrink-0 items-center gap-3 px-5 py-4 bg-gradient-to-r from-indigo-600 via-indigo-700 to-violet-700"
        style={{
          borderRadius: '1.5rem 1.5rem 0 0',
        }}
      >
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl border border-white/30 bg-white/20 shadow-inner backdrop-blur-sm">
          <Bot size={20} className="text-white" />
        </div>
        <div className="flex-1">
          <h3 className="text-sm font-bold tracking-tight text-white">{copy.title}</h3>
          <div className="mt-0.5 flex items-center gap-1.5">
            <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-emerald-400" />
            <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-200">
              {copy.online}
            </span>
          </div>
        </div>
        <button
          type="button"
          onClick={launcher.close}
          className="flex h-8 w-8 items-center justify-center rounded-full border border-white/20 bg-white/10 text-white shadow-sm transition-colors hover:bg-white/20 active:scale-95"
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
