import { Bot, User } from 'lucide-react';

const ChatMessages = ({ messages, isLoading, suggestions, onSuggestion, messagesEndRef }) => (
  <div className="flex-1 space-y-4 overflow-y-auto scroll-smooth px-4 py-4" id="ai-chat-messages">
    {messages.map((message, index) => (
      <div
        key={`${message.role}-${index}`}
        className={`flex gap-2 ${
          message.role === 'user' ? 'justify-end' : 'justify-start'
        } ai-chat-fade-in`}
      >
        {message.role === 'assistant' && (
          <div className="mt-0.5 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-blue-400 to-blue-600 shadow-md">
            <Bot size={14} className="text-white" />
          </div>
        )}
        <div
          className={`max-w-[80%] whitespace-pre-wrap px-4 py-2.5 text-[13px] leading-relaxed ${
            message.role === 'user'
              ? 'rounded-2xl rounded-br-md bg-gradient-to-br from-orange-500 to-orange-600 text-white shadow-md'
              : 'rounded-2xl rounded-bl-md border border-gray-100/80 bg-white text-gray-700 shadow-sm'
          }`}
        >
          {message.content}
        </div>
        {message.role === 'user' && (
          <div className="mt-0.5 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-gray-200 shadow-sm">
            <User size={14} className="text-gray-500" />
          </div>
        )}
      </div>
    ))}

    {isLoading && (
      <div className="flex items-start gap-2 ai-chat-fade-in">
        <div className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-orange-400 to-orange-600 shadow-md">
          <Bot size={14} className="text-white" />
        </div>
        <div className="rounded-2xl rounded-bl-md border border-gray-100/80 bg-white px-4 py-3 shadow-sm">
          <div className="flex gap-1.5">
            {[0, 150, 300].map((delay) => (
              <span
                key={delay}
                className="h-2 w-2 animate-bounce rounded-full bg-orange-400"
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
            className="rounded-full border border-orange-200/60 bg-orange-50 px-3 py-1.5 text-[11px] font-medium text-orange-600 shadow-sm transition-all duration-200 hover:bg-orange-100 active:scale-95"
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
