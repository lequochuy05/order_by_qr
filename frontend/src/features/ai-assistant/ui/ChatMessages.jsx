import { Bot, User } from 'lucide-react';

const ChatMessages = ({ messages, isLoading, suggestions, onSuggestion, messagesEndRef }) => (
  <div className="flex-1 space-y-4 overflow-y-auto scroll-smooth px-4 py-4 custom-scrollbar" id="ai-chat-messages">
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
        <div
          className={`max-w-[80%] whitespace-pre-wrap px-4 py-2.5 text-xs leading-relaxed ${
            message.role === 'user'
              ? 'rounded-2xl rounded-br-md bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/20'
              : 'rounded-2xl rounded-bl-md border border-slate-200/80 bg-white text-slate-800 shadow-sm'
          }`}
        >
          {message.content}
        </div>
        {message.role === 'user' && (
          <div className="mt-0.5 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-slate-200 shadow-sm">
            <User size={14} className="text-slate-600" />
          </div>
        )}
      </div>
    ))}

    {isLoading && (
      <div className="flex items-start gap-2 ai-chat-fade-in">
        <div className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-tr from-indigo-600 to-violet-500 shadow-md shadow-indigo-500/25">
          <Bot size={14} className="text-white" />
        </div>
        <div className="rounded-2xl rounded-bl-md border border-slate-200/80 bg-white px-4 py-3 shadow-sm">
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
            className="rounded-full border border-indigo-200/60 bg-indigo-50 px-3 py-1.5 text-[11px] font-bold text-indigo-700 shadow-sm transition-all duration-200 hover:bg-indigo-600 hover:text-white active:scale-95"
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
