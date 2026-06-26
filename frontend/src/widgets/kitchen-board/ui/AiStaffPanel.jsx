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

import { aiStaffService } from '@features/ai-assistant';

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
    <section className="mt-5 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900">
      <div className="flex flex-col gap-3 border-b border-slate-100 px-4 py-4 dark:border-slate-800 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-orange-500 text-white shadow-sm">
            <Sparkles size={20} />
          </span>
          <div className="min-w-0">
            <h2 className="text-base font-black text-slate-900 dark:text-white">Trợ lý bếp</h2>
            <p className="truncate text-xs font-semibold text-slate-500 dark:text-slate-400">
              Dữ liệu bếp, bàn và order gần đây
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={resetChat}
          className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-slate-200 px-3 text-sm font-bold text-slate-600 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"
          title="Xoá phiên chat"
        >
          <RotateCcw size={16} />
          Làm mới chat
        </button>
      </div>

      <div className="grid gap-0 lg:grid-cols-[minmax(0,1fr)_minmax(280px,360px)]">
        <div className="flex h-[360px] min-h-0 flex-col">
          <div className="flex-1 space-y-3 overflow-y-auto px-4 py-4">
            {messages.map((message, index) => (
              <div
                key={`${message.role}-${index}`}
                className={`flex gap-2 ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                {message.role === 'assistant' && (
                  <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-900 text-white dark:bg-slate-700">
                    <Bot size={14} />
                  </span>
                )}
                <p
                  className={`max-w-[82%] whitespace-pre-wrap break-words rounded-2xl px-3 py-2 text-sm leading-relaxed ${
                    message.role === 'user'
                      ? 'rounded-br-md bg-orange-500 text-white'
                      : 'rounded-bl-md border border-slate-200 bg-slate-50 text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100'
                  }`}
                >
                  {message.content}
                </p>
                {message.role === 'user' && (
                  <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-200 text-slate-600 dark:bg-slate-700 dark:text-slate-200">
                    <UserRound size={14} />
                  </span>
                )}
              </div>
            ))}

            {isLoading && (
              <div className="flex items-start gap-2">
                <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-900 text-white dark:bg-slate-700">
                  <Bot size={14} />
                </span>
                <div className="inline-flex items-center gap-2 rounded-2xl rounded-bl-md border border-slate-200 bg-slate-50 px-3 py-2 text-sm font-semibold text-slate-500 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300">
                  <Loader2 size={15} className="animate-spin" />
                  Đang xem dữ liệu bếp...
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          <div className="border-t border-slate-100 p-3 dark:border-slate-800">
            <div className="flex items-end gap-2 rounded-2xl border border-slate-200 bg-slate-50 p-2 focus-within:border-orange-300 focus-within:ring-2 focus-within:ring-orange-100 dark:border-slate-700 dark:bg-slate-950 dark:focus-within:ring-orange-500/20">
              <textarea
                ref={inputRef}
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={handleKeyDown}
                disabled={isLoading}
                maxLength={500}
                rows={2}
                placeholder="Hỏi tình trạng bếp..."
                className="min-h-10 flex-1 resize-none bg-transparent px-2 py-2 text-sm font-semibold text-slate-700 outline-none placeholder:text-slate-400 disabled:cursor-wait dark:text-slate-100"
              />
              <button
                type="button"
                onClick={handleSend}
                disabled={!input.trim() || isLoading}
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-orange-500 text-white transition hover:bg-orange-400 disabled:cursor-not-allowed disabled:bg-slate-300 dark:disabled:bg-slate-700"
                aria-label="Gửi câu hỏi cho trợ lý bếp"
              >
                {isLoading ? <Loader2 size={17} className="animate-spin" /> : <Send size={17} />}
              </button>
            </div>
          </div>
        </div>

        <aside className="border-t border-slate-100 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-950 lg:border-l lg:border-t-0">
          <div className="mb-3 flex items-center gap-2 text-sm font-black text-slate-700 dark:text-slate-200">
            <MessageSquareText size={17} />
            Câu hỏi nhanh
          </div>
          <div className="flex flex-col gap-2">
            {SUGGESTIONS.map((suggestion) => (
              <button
                key={suggestion}
                type="button"
                onClick={() => sendMessage(suggestion)}
                disabled={isLoading}
                className="min-h-11 rounded-xl border border-slate-200 bg-white px-3 py-2 text-left text-sm font-bold text-slate-600 transition hover:border-orange-200 hover:bg-orange-50 hover:text-orange-700 disabled:cursor-wait disabled:opacity-60 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-orange-500/40 dark:hover:bg-orange-500/10 dark:hover:text-orange-200"
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
