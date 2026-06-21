import { Loader2, Send } from 'lucide-react';

const ChatInput = ({ copy, input, isLoading, inputRef, onInputChange, onKeyDown, onSend }) => (
  <div
    className="flex-shrink-0 border-t border-gray-100/80 bg-white/80 px-4 py-3 backdrop-blur-sm"
    style={{ paddingBottom: 'calc(0.75rem + var(--safe-area-inset-bottom))' }}
  >
    <div className="flex items-center gap-2 rounded-2xl border border-gray-200/60 bg-gray-50 px-4 py-1 transition-all duration-200 focus-within:border-orange-400 focus-within:ring-2 focus-within:ring-orange-100">
      <input
        ref={inputRef}
        id="ai-chat-input"
        type="text"
        value={input}
        onChange={(event) => onInputChange(event.target.value)}
        onKeyDown={onKeyDown}
        placeholder={copy.placeholder}
        disabled={isLoading}
        className="flex-1 bg-transparent py-2.5 text-sm text-gray-700 outline-none placeholder-gray-400"
        maxLength={300}
      />
      <button
        id="ai-chat-send"
        type="button"
        onClick={onSend}
        disabled={!input.trim() || isLoading}
        className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl transition-all duration-200 ${
          input.trim() && !isLoading
            ? 'bg-orange-500 text-white shadow-md hover:bg-orange-600 active:scale-90'
            : 'cursor-not-allowed bg-gray-200 text-gray-400'
        }`}
        aria-label={copy.sendLabel}
      >
        {isLoading ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
      </button>
    </div>
  </div>
);

export default ChatInput;
