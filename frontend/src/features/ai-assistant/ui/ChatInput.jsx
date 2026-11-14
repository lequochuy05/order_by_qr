import { Loader2, Send } from 'lucide-react';

const ChatInput = ({ copy, input, isLoading, inputRef, onInputChange, onKeyDown, onSend }) => (
  <div
    className="flex-shrink-0 border-t border-slate-100 bg-white/80 px-4 py-3 backdrop-blur-sm"
    style={{ paddingBottom: 'calc(0.75rem + var(--safe-area-inset-bottom))' }}
  >
    <div className="flex items-center gap-2 rounded-2xl border border-slate-200/80 bg-slate-50 px-4 py-1 transition-all duration-200 focus-within:border-indigo-500 focus-within:ring-2 focus-within:ring-indigo-500/20">
      <input
        ref={inputRef}
        id="ai-chat-input"
        type="text"
        value={input}
        onChange={(event) => onInputChange(event.target.value)}
        onKeyDown={onKeyDown}
        placeholder={copy.placeholder}
        disabled={isLoading}
        className="flex-1 bg-transparent py-2.5 text-xs font-medium text-slate-800 outline-none placeholder-slate-400"
        maxLength={300}
      />
      <button
        id="ai-chat-send"
        type="button"
        onClick={onSend}
        disabled={!input.trim() || isLoading}
        className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl transition-all duration-200 active:scale-95 ${
          input.trim() && !isLoading
            ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/25 hover:from-indigo-500 hover:to-violet-500'
            : 'cursor-not-allowed bg-slate-200 text-slate-400'
        }`}
        aria-label={copy.sendLabel}
      >
        {isLoading ? <Loader2 size={15} className="animate-spin" /> : <Send size={15} />}
      </button>
    </div>
  </div>
);

export default ChatInput;
