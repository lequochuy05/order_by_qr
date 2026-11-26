import { Bot, User } from 'lucide-react';

import AiSuggestionList from './AiSuggestionList.jsx';

const ChatMessages = ({
  messages,
  isLoading,
  copy,
  suggestions,
  onSuggestion,
  orderBridge,
  onCloseChat,
  messagesEndRef,
}) => (
  <div
    className="flex-1 space-y-4 overflow-y-auto scroll-smooth px-4 py-4 custom-scrollbar"
    id="ai-chat-messages"
  >
    {messages.map((message, index) => (
      <div
        key={`${message.role}-${index}`}
        className={`flex gap-2 ${
          message.role === 'user' ? 'justify-end' : 'justify-start'
        } ai-chat-fade-in`}
      >
        {message.role === 'assistant' && (
          <div className="mt-0.5 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-tr from-indigo-600 to-violet-500 shadow-md shadow-indigo-500/25">
            <Bot size={14} className="text-white" />
          </div>
        )}
        <div className={message.role === 'user' ? 'max-w-[80%]' : 'min-w-0 flex-1 max-w-[88%]'}>
          <div
            className={`whitespace-pre-wrap px-4 py-2.5 text-xs leading-relaxed ${
              message.role === 'user'
                ? 'rounded-2xl rounded-br-md bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/20'
                : 'rounded-2xl rounded-bl-md border border-slate-200/80 bg-white text-slate-800 shadow-sm dark:border-slate-800 dark:bg-slate-900 dark:text-slate-100'
            }`}
          >
            {message.content}
          </div>
          {message.role === 'assistant' && message.suggestions?.length > 0 && (
            <AiSuggestionList
              suggestions={message.suggestions}
              copy={copy}
              orderBridge={orderBridge}
              onCloseChat={onCloseChat}
            />
          )}
        </div>
        {message.role === 'user' && (
          <div className="mt-0.5 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-slate-200 shadow-sm dark:bg-slate-800">
            <User size={14} className="text-slate-600 dark:text-slate-300" />
          </div>
        )}
      </div>
    ))}

    {isLoading && (
      <div className="flex items-start gap-2 ai-chat-fade-in">
        <div className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-tr from-indigo-600 to-violet-500 shadow-md shadow-indigo-500/25">
          <Bot size={14} className="text-white" />
        </div>
        <div className="rounded-2xl rounded-bl-md border border-slate-200/80 bg-white px-4 py-3 shadow-sm dark:border-slate-800 dark:bg-slate-900">
          <div className="flex gap-1.5">
            {[0, 150, 300].map((delay) => (
              <span
                key={delay}
                className="h-2 w-2 animate-bounce rounded-full bg-indigo-500"
                style={{ animationDelay: `${delay}ms` }}
              />
            ))}
          </div>
        </div>
      </div>
    )}

    {messages.length <= 1 && !isLoading && (
      <div className="flex flex-wrap gap-2 pt-2">
        {suggestions.map((text) => (
          <button
            key={text}
            type="button"
            onClick={() => onSuggestion(text)}
            className="rounded-full border border-indigo-200/60 bg-indigo-50 px-3 py-1.5 text-[11px] font-bold text-indigo-700 shadow-sm transition-all duration-200 hover:bg-indigo-600 hover:text-white active:scale-95 dark:border-indigo-500/40 dark:bg-indigo-950/60 dark:text-indigo-300 dark:hover:bg-indigo-600 dark:hover:text-white"
          >
            {text}
          </button>
        ))}
      </div>
    )}
    <div ref={messagesEndRef} />
  </div>
);

export default ChatMessages;
