import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Bot,
  Loader2,
  MessageSquareText,
  RotateCcw,
  Send,
  Sparkles,
  UserRound,
} from 'lucide-react';

import { aiStaffService } from '@shared/api/aiService.js';


const WELCOME_MESSAGE =
  'Mình đang theo dõi dữ liệu bếp hiện tại. Hỏi nhanh về bàn chờ lâu, món cần ưu tiên hoặc tóm tắt một order nhé.';

const SUGGESTIONS = ['Bàn nào đang chờ lâu?', 'Món nào cần làm trước?', 'Tóm tắt order bàn 1'];

const ERROR_REPLY = 'Không thể gọi trợ lý lúc này. Bạn thử lại sau hoặc làm mới trang bếp nhé.';

const AiStaffPanel = () => {
  const [messages, setMessages] = useState([{ role: 'assistant', content: WELCOME_MESSAGE }]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, isLoading]);

  const history = useMemo(
    () =>
      messages
        .filter((message) => message.content !== WELCOME_MESSAGE)
        .slice(-10)
        .map((message) => ({
          role: message.role === 'assistant' ? 'model' : 'user',
          content: message.content,
        })),
    [messages],
  );

  const resetChat = useCallback(() => {
    setMessages([{ role: 'assistant', content: WELCOME_MESSAGE }]);
    setInput('');
    window.setTimeout(() => inputRef.current?.focus(), 0);
  }, []);

  const sendMessage = useCallback(
    async (messageText) => {
      const trimmed = messageText.trim();
      if (!trimmed || isLoading) return;

      setMessages((current) => [...current, { role: 'user', content: trimmed }]);
      setInput('');
      setIsLoading(true);

      try {
        const response = await aiStaffService.query(trimmed, history);
        setMessages((current) => [
          ...current,
          { role: 'assistant', content: response?.reply || ERROR_REPLY },
        ]);
      } catch (error) {
        console.error('Failed to query staff AI:', error);
        setMessages((current) => [...current, { role: 'assistant', content: ERROR_REPLY }]);
      } finally {
        setIsLoading(false);
      }
    },
    [history, isLoading],
  );

  const handleSend = useCallback(() => sendMessage(input), [input, sendMessage]);

  const handleKeyDown = useCallback(
    (event) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        handleSend();
      }
    },
    [handleSend],
  );

  return (
    <section className="mt-5 overflow-hidden rounded-3xl border border-slate-200/80 bg-white shadow-sm dark:border-slate-800/80 dark:bg-slate-900/90">
      <div className="flex flex-col gap-3 border-b border-slate-100 px-5 py-4 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-center gap-3.5">
          <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-lg shadow-indigo-500/25">
            <Sparkles size={20} />
          </span>
          <div className="min-w-0">
            <h2 className="text-base font-black tracking-tight text-slate-900 dark:text-white">Trợ lý bếp</h2>
            <p className="truncate text-xs font-semibold text-slate-500 dark:text-slate-400">
              Dữ liệu bếp, bàn và order gần đây
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={resetChat}
          className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-slate-200/80 px-3.5 text-xs font-bold text-slate-600 transition hover:bg-slate-50 hover:text-slate-900 active:scale-95 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-white"
          title="Xoá phiên chat"
        >
          <RotateCcw size={15} />
          Làm mới chat
        </button>
      </div>

      <div className="grid gap-0 lg:grid-cols-[minmax(0,1fr)_minmax(280px,360px)]">
        <div className="flex h-[360px] min-h-0 flex-col">
          <div className="flex-1 space-y-3 overflow-y-auto px-5 py-4 custom-scrollbar">
            {messages.map((message, index) => (
              <div
                key={`${message.role}-${index}`}
                className={`flex gap-2.5 ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                {message.role === 'assistant' && (
                  <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-sm">
                    <Bot size={14} />
                  </span>
                )}
                <p
                  className={`max-w-[82%] whitespace-pre-wrap break-words rounded-2xl px-4 py-2.5 text-xs leading-relaxed ${
                    message.role === 'user'
                      ? 'rounded-br-md bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/20'
                      : 'rounded-bl-md border border-slate-200/80 bg-slate-50 text-slate-800 shadow-sm dark:border-slate-800 dark:bg-slate-800/80 dark:text-slate-100'
                  }`}
                >
                  {message.content}
                </p>
                {message.role === 'user' && (
                  <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-200 text-slate-600 dark:bg-slate-700 dark:text-slate-200 shadow-sm">
                    <UserRound size={14} />
                  </span>
                )}
              </div>
            ))}

            {isLoading && (
              <div className="flex items-start gap-2.5">
                <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-tr from-indigo-600 to-violet-500 text-white shadow-sm">
                  <Bot size={14} />
                </span>
                <div className="inline-flex items-center gap-2 rounded-2xl rounded-bl-md border border-slate-200/80 bg-slate-50 px-4 py-2.5 text-xs font-semibold text-slate-500 dark:border-slate-800 dark:bg-slate-800/80 dark:text-slate-300">
                  <Loader2 size={15} className="animate-spin text-indigo-600 dark:text-indigo-400" />
                  Đang xem dữ liệu bếp...
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          <div className="border-t border-slate-100 p-3.5 dark:border-slate-800">
            <div className="flex items-end gap-2 rounded-2xl border border-slate-200/80 bg-slate-50/80 p-2 transition-all focus-within:border-indigo-500 focus-within:bg-white focus-within:ring-2 focus-within:ring-indigo-500/20 dark:border-slate-700/80 dark:bg-slate-950/80 dark:focus-within:bg-slate-950 dark:focus-within:ring-indigo-500/20">
              <textarea
                ref={inputRef}
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={handleKeyDown}
                disabled={isLoading}
                maxLength={500}
                rows={2}
                placeholder="Hỏi tình trạng bếp..."
                className="min-h-10 flex-1 resize-none bg-transparent px-2.5 py-2 text-xs font-medium text-slate-800 outline-none placeholder:text-slate-400 disabled:cursor-wait dark:text-slate-100"
              />
              <button
                type="button"
                onClick={handleSend}
                disabled={!input.trim() || isLoading}
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-md shadow-indigo-600/25 transition-all hover:from-indigo-500 hover:to-violet-500 active:scale-95 disabled:cursor-not-allowed disabled:bg-slate-300 dark:disabled:bg-slate-800"
                aria-label="Gửi câu hỏi cho trợ lý bếp"
              >
                {isLoading ? <Loader2 size={17} className="animate-spin" /> : <Send size={17} />}
              </button>
            </div>
          </div>
        </div>

        <aside className="border-t border-slate-100 bg-slate-50/60 p-4 dark:border-slate-800 dark:bg-slate-950/60 lg:border-l lg:border-t-0">
          <div className="mb-3 flex items-center gap-2 text-xs font-black tracking-wider uppercase text-slate-500 dark:text-slate-400">
            <MessageSquareText size={15} />
            Câu hỏi nhanh
          </div>
          <div className="flex flex-col gap-2">
            {SUGGESTIONS.map((suggestion) => (
              <button
                key={suggestion}
                type="button"
                onClick={() => sendMessage(suggestion)}
                disabled={isLoading}
                className="min-h-11 rounded-xl border border-slate-200/80 bg-white px-3.5 py-2.5 text-left text-xs font-bold text-slate-700 shadow-sm transition-all hover:border-indigo-300 hover:bg-indigo-50/60 hover:text-indigo-700 active:scale-[0.98] disabled:cursor-wait disabled:opacity-60 dark:border-slate-700/80 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-indigo-500/40 dark:hover:bg-indigo-950/30 dark:hover:text-indigo-300"
              >
                {suggestion}
              </button>
            ))}
          </div>
        </aside>
      </div>
    </section>
  );
};

export default AiStaffPanel;
