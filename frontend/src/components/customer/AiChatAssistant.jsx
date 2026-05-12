import React, { useState, useRef, useEffect, useCallback } from 'react';
import { MessageCircle, X, Send, Loader2, Bot, User, Sparkles } from 'lucide-react';
import { menuService } from '../../services/customer/menuService';

const WELCOME_MESSAGE = {
  role: 'assistant',
  content: '👋 Tôi là Trợ lý Sắc Màu, sẵn sàng giúp bạn chọn món ngon. Bạn muốn ăn gì hôm nay?'
};

const AiChatAssistant = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([WELCOME_MESSAGE]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isAnimating, setIsAnimating] = useState(false);
  const [typedText, setTypedText] = useState('');
  const [bubbleVisible, setBubbleVisible] = useState(true);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  useEffect(() => {
    if (isOpen && inputRef.current) {
      setTimeout(() => inputRef.current?.focus(), 300);
    }
  }, [isOpen]);

  // Typewriter effect for the thought bubble
  useEffect(() => {
    if (isOpen) return;
    const fullText = 'Tôi có thể hỗ trợ gì cho bạn?';
    const words = fullText.split(' ');
    let wordIndex = 0;
    let phase = 'typing'; // typing | pause | hiding | waiting
    let timer;

    const tick = () => {
      if (phase === 'typing') {
        wordIndex++;
        setTypedText(words.slice(0, wordIndex).join(' '));
        setBubbleVisible(true);
        if (wordIndex >= words.length) {
          phase = 'pause';
          timer = setTimeout(tick, 5000);
        } else {
          timer = setTimeout(tick, 200);
        }
      } else if (phase === 'pause') {
        phase = 'hiding';
        setBubbleVisible(false);
        timer = setTimeout(tick, 800);
      } else if (phase === 'hiding') {
        phase = 'waiting';
        setTypedText('');
        timer = setTimeout(tick, 2500);
      } else if (phase === 'waiting') {
        wordIndex = 0;
        phase = 'typing';
        tick();
      }
    };

    timer = setTimeout(tick, 5000);
    return () => clearTimeout(timer);
  }, [isOpen]);

  const handleToggle = () => {
    if (!isOpen) {
      setIsAnimating(true);
      setIsOpen(true);
    } else {
      setIsAnimating(false);
      setTimeout(() => setIsOpen(false), 200);
    }
  };

  const buildHistory = () => {
    return messages
      .filter(m => m !== WELCOME_MESSAGE)
      .map(m => ({
        role: m.role === 'assistant' ? 'model' : 'user',
        content: m.content
      }));
  };

  const handleSend = async () => {
    const trimmed = input.trim();
    if (!trimmed || isLoading) return;

    const userMessage = { role: 'user', content: trimmed };
    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    try {
      const history = buildHistory();
      const res = await menuService.sendAiChat(trimmed, history);
      const reply = res?.reply || 'Xin lỗi, tôi không thể trả lời lúc này.';
      setMessages(prev => [...prev, { role: 'assistant', content: reply }]);
    } catch {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Xin lỗi, đã có lỗi xảy ra. Bạn thử hỏi lại nhé! 😊'
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const quickSuggestions = [
    'Gợi ý món ngon đi',
    'Món nào dưới 50k?',
    'Có đồ uống gì?',
  ];

  const handleQuickSuggestion = (text) => {
    setInput(text);
    setTimeout(() => {
      const fakeEvent = { key: 'Enter', shiftKey: false, preventDefault: () => { } };
      handleKeyDown(fakeEvent);
    }, 50);
  };

  return (
    <>
      {/* Floating Chat Button + Thought Bubble - Only show when closed */}
      {!isOpen && (
        <div className="fixed z-[60] bottom-24 right-5 flex flex-col items-end gap-2">
          {/* Thought Bubble */}
          {typedText && (
            <div
              className={`relative max-w-[200px] px-3 py-2 rounded-2xl rounded-br-md text-[11px] font-semibold text-gray-700 bg-white shadow-lg border border-gray-100/80 transition-all duration-300 ${bubbleVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2'
                }`}
            >
              {typedText}
              <span className="animate-blink text-blue-500">|</span>
              {/* Tail */}
              <div className="absolute -bottom-1.5 right-4 w-3 h-3 bg-white border-r border-b border-gray-100/80 rotate-45"></div>
            </div>
          )}

          {/* Button */}
          <button
            id="ai-chat-toggle"
            onClick={handleToggle}
            className="w-14 h-14 rounded-full shadow-2xl flex items-center justify-center transition-all duration-500 active:scale-90 bg-gradient-to-br from-blue-400 to-blue-600 hover:from-blue-500 hover:to-blue-700 animate-bounce-gentle"
            aria-label="AI Chat Assistant"
            style={{
              boxShadow: '0 4px 25px rgba(59, 38, 218, 0.5), 0 0 40px rgba(249, 115, 22, 0.2)'
            }}
          >
            <div className="relative">
              <MessageCircle size={24} className="text-white" />
              <Sparkles size={10} className="absolute -top-1 -right-1 text-yellow-300 animate-pulse" />
            </div>
          </button>
        </div>
      )}

      {/* Chat Window */}
      {isOpen && (
        <div
          className={`fixed z-[55] bottom-0 right-0 w-full max-w-md h-[85vh] max-h-[700px] flex flex-col transition-all duration-300 ${isAnimating ? 'animate-slide-up' : 'animate-slide-down'
            }`}
          style={{
            background: 'linear-gradient(145deg, rgba(255,255,255,0.95) 0%, rgba(255,248,240,0.98) 100%)',
            backdropFilter: 'blur(20px)',
            borderRadius: '1.5rem 1.5rem 0 0',
            boxShadow: '0 -8px 60px rgba(0,0,0,0.15), 0 -2px 20px rgba(249,115,22,0.1)',
            border: '1px solid rgba(255,255,255,0.6)',
            borderBottom: 'none'
          }}
        >
          {/* Header */}
          <div
            className="flex items-center gap-3 px-5 py-4 flex-shrink-0 relative"
            style={{
              background: 'linear-gradient(135deg, #0f126cff 0%, #7d80deff 100%)',
              borderRadius: '1.5rem 1.5rem 0 0'
            }}
          >
            <div className="w-10 h-10 rounded-full bg-white/20 backdrop-blur-sm flex items-center justify-center border border-white/30 shadow-inner">
              <Bot size={20} className="text-white" />
            </div>
            <div className="flex-1">
              <h3 className="text-white font-bold text-sm tracking-tight">Trợ lý Sắc Màu</h3>
              <div className="flex items-center gap-1 mt-0.5">
                <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse"></span>
                <span className="text-yellow-300 text-[10px]  font-bold uppercase tracking-wider">Online</span>
              </div>
            </div>

            {/* Close Button in Header */}
            <button
              onClick={handleToggle}
              className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white transition-colors border border-white/20 shadow-sm"
              aria-label="Close chat"
            >
              <X size={18} />
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4 scroll-smooth" id="ai-chat-messages">
            {messages.map((msg, idx) => (
              <div
                key={idx}
                className={`flex gap-2 ${msg.role === 'user' ? 'justify-end' : 'justify-start'} animate-fade-in`}
              >
                {msg.role === 'assistant' && (
                  <div className="w-7 h-7 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center flex-shrink-0 shadow-md mt-0.5">
                    <Bot size={14} className="text-white" />
                  </div>
                )}
                <div
                  className={`max-w-[80%] px-4 py-2.5 text-[13px] leading-relaxed whitespace-pre-wrap ${msg.role === 'user'
                    ? 'bg-gradient-to-br from-orange-500 to-orange-600 text-white rounded-2xl rounded-br-md shadow-md'
                    : 'bg-white text-gray-700 rounded-2xl rounded-bl-md shadow-sm border border-gray-100/80'
                    }`}
                >
                  {msg.content}
                </div>
                {msg.role === 'user' && (
                  <div className="w-7 h-7 rounded-full bg-gray-200 flex items-center justify-center flex-shrink-0 shadow-sm mt-0.5">
                    <User size={14} className="text-gray-500" />
                  </div>
                )}
              </div>
            ))}

            {/* Typing indicator */}
            {isLoading && (
              <div className="flex gap-2 items-start animate-fade-in">
                <div className="w-7 h-7 rounded-full bg-gradient-to-br from-orange-400 to-orange-600 flex items-center justify-center flex-shrink-0 shadow-md">
                  <Bot size={14} className="text-white" />
                </div>
                <div className="bg-white px-4 py-3 rounded-2xl rounded-bl-md shadow-sm border border-gray-100/80">
                  <div className="flex gap-1.5">
                    <span className="w-2 h-2 bg-orange-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></span>
                    <span className="w-2 h-2 bg-orange-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></span>
                    <span className="w-2 h-2 bg-orange-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></span>
                  </div>
                </div>
              </div>
            )}

            {/* Quick suggestions - only show when few messages */}
            {messages.length <= 1 && !isLoading && (
              <div className="flex flex-wrap gap-2 pt-2">
                {quickSuggestions.map((text, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleQuickSuggestion(text)}
                    className="px-3 py-1.5 text-[11px] font-medium text-orange-600 bg-orange-50 hover:bg-orange-100 rounded-full border border-orange-200/60 transition-all duration-200 active:scale-95 shadow-sm"
                  >
                    {text}
                  </button>
                ))}
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Input Area */}
          <div className="flex-shrink-0 px-4 py-3 border-t border-gray-100/80 bg-white/80 backdrop-blur-sm" style={{ borderRadius: '0' }}>
            <div className="flex items-center gap-2 bg-gray-50 rounded-2xl px-4 py-1 border border-gray-200/60 focus-within:border-orange-400 focus-within:ring-2 focus-within:ring-orange-100 transition-all duration-200">
              <input
                ref={inputRef}
                id="ai-chat-input"
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Hỏi về món ăn..."
                disabled={isLoading}
                className="flex-1 bg-transparent text-sm text-gray-700 placeholder-gray-400 outline-none py-2.5"
                maxLength={300}
              />
              <button
                id="ai-chat-send"
                onClick={handleSend}
                disabled={!input.trim() || isLoading}
                className={`w-9 h-9 rounded-xl flex items-center justify-center transition-all duration-200 flex-shrink-0 ${input.trim() && !isLoading
                  ? 'bg-orange-500 hover:bg-orange-600 text-white shadow-md active:scale-90'
                  : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                  }`}
                aria-label="Send message"
              >
                {isLoading ? (
                  <Loader2 size={16} className="animate-spin" />
                ) : (
                  <Send size={16} />
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Custom Animations */}
      <style>{`
        @keyframes slide-up {
          from { transform: translateY(100%); opacity: 0; }
          to { transform: translateY(0); opacity: 1; }
        }
        @keyframes slide-down {
          from { transform: translateY(0); opacity: 1; }
          to { transform: translateY(100%); opacity: 0; }
        }
        @keyframes fade-in {
          from { opacity: 0; transform: translateY(8px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes bounce-gentle {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-6px); }
        }
        .animate-slide-up {
          animation: slide-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }
        .animate-slide-down {
          animation: slide-down 0.2s ease-in forwards;
        }
        .animate-fade-in {
          animation: fade-in 0.3s ease-out forwards;
        }
        .animate-bounce-gentle {
          animation: bounce-gentle 2s ease-in-out infinite;
        }
        @keyframes blink {
          0%, 100% { opacity: 1; }
          50% { opacity: 0; }
        }
        .animate-blink {
          animation: blink 0.8s ease-in-out infinite;
        }
      `}</style>
    </>
  );
};

export default AiChatAssistant;
