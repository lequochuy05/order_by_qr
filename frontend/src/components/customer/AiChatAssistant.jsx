import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import './AiChatAssistant.css';
import { MessageCircle, X, Send, Loader2, Bot, User } from 'lucide-react';
import { menuService } from '../../services/customer/menuService';

const AI_COPY = {
  vi: {
    welcome: 'Xin chào! Tôi có thể giúp gì cho bạn?',
    title: 'Trợ lý',
    online: 'Online',
    openLabel: 'Mở trợ lý AI',
    openTitle: 'Kéo để đổi vị trí, chạm để mở trợ lý',
    closeLabel: 'Đóng trò chuyện',
    sendLabel: 'Gửi tin nhắn',
    placeholder: 'Hỏi món, combo, danh mục...',
    fallbackReply: 'Xin lỗi, tôi không thể trả lời lúc này.',
    errorReply: 'Xin lỗi, đã có lỗi xảy ra. Bạn thử hỏi lại nhé!',
    suggestions: ['Gợi ý món ngon đi', 'Có combo nào?', 'Có danh mục gì?']
  },
  en: {
    welcome: 'Hi! How can I help you today?',
    title: 'Assistant',
    online: 'Online',
    openLabel: 'Open AI assistant',
    openTitle: 'Drag to move, tap to open assistant',
    closeLabel: 'Close chat',
    sendLabel: 'Send message',
    placeholder: 'Ask about dishes, combos, categories...',
    fallbackReply: 'Sorry, I cannot answer right now.',
    errorReply: 'Sorry, something went wrong. Please try again.',
    suggestions: ['Recommend something tasty', 'Any combos?', 'What categories are available?']
  }
};

const LAUNCHER_SIZE = 64;
const LAUNCHER_MARGIN = 12;
const LAUNCHER_STORAGE_KEY = 'customer_ai_chat_launcher_position';

const clampLauncherPosition = (position) => {
  if (typeof window === 'undefined') return position;

  return {
    x: Math.min(
      Math.max(LAUNCHER_MARGIN, position.x),
      window.innerWidth - LAUNCHER_SIZE - LAUNCHER_MARGIN
    ),
    y: Math.min(
      Math.max(LAUNCHER_MARGIN, position.y),
      window.innerHeight - LAUNCHER_SIZE - LAUNCHER_MARGIN
    )
  };
};

const getInitialLauncherPosition = () => {
  if (typeof window === 'undefined') {
    return { x: 0, y: 0 };
  }

  try {
    const saved = JSON.parse(localStorage.getItem(LAUNCHER_STORAGE_KEY));
    if (Number.isFinite(saved?.x) && Number.isFinite(saved?.y)) {
      return clampLauncherPosition(saved);
    }
  } catch {
    // Ignore invalid persisted UI state.
  }

  return clampLauncherPosition({
    x: window.innerWidth - LAUNCHER_SIZE - 20,
    y: window.innerHeight - LAUNCHER_SIZE - 104
  });
};

const AiChatAssistant = ({ hidden = false, language = 'vi' }) => {
  const copy = useMemo(() => AI_COPY[language] || AI_COPY.vi, [language]);
  const welcomeMessage = useMemo(() => ({ role: 'assistant', content: copy.welcome }), [copy.welcome]);
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([welcomeMessage]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isAnimating, setIsAnimating] = useState(false);
  const [launcherPosition, setLauncherPosition] = useState(getInitialLauncherPosition);
  const [isDraggingLauncher, setIsDraggingLauncher] = useState(false);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);
  const ignoreBackdropClickUntilRef = useRef(0);
  const dragStateRef = useRef({
    pointerId: null,
    startX: 0,
    startY: 0,
    originX: 0,
    originY: 0,
    moved: false
  });

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

  useEffect(() => {
    const knownWelcomeMessages = Object.values(AI_COPY).map(value => value.welcome);
    setMessages(prev => {
      if (prev.length === 1 && prev[0]?.role === 'assistant' && knownWelcomeMessages.includes(prev[0].content)) {
        return [welcomeMessage];
      }
      return prev;
    });
  }, [welcomeMessage]);

  useEffect(() => {
    const handleResize = () => {
      setLauncherPosition(prev => clampLauncherPosition(prev));
    };

    window.addEventListener('resize', handleResize);
    window.addEventListener('orientationchange', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('orientationchange', handleResize);
    };
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    localStorage.setItem(LAUNCHER_STORAGE_KEY, JSON.stringify(launcherPosition));
  }, [launcherPosition]);

  useEffect(() => {
    if (hidden && isOpen) {
      setIsAnimating(false);
      setIsOpen(false);
    }
  }, [hidden, isOpen]);

  const openChat = () => {
    ignoreBackdropClickUntilRef.current = Date.now() + 250;
    setIsAnimating(true);
    setIsOpen(true);
  };

  const closeChat = () => {
    setIsAnimating(false);
    setTimeout(() => setIsOpen(false), 200);
  };

  const handleToggle = () => {
    if (!isOpen) {
      openChat();
    } else {
      closeChat();
    }
  };

  const handleBackdropClick = () => {
    if (Date.now() < ignoreBackdropClickUntilRef.current) return;
    closeChat();
  };

  const handleLauncherPointerDown = (e) => {
    if (e.button !== undefined && e.button !== 0) return;

    dragStateRef.current = {
      pointerId: e.pointerId,
      startX: e.clientX,
      startY: e.clientY,
      originX: launcherPosition.x,
      originY: launcherPosition.y,
      moved: false
    };
    e.currentTarget.setPointerCapture?.(e.pointerId);
    setIsDraggingLauncher(true);
  };

  const handleLauncherPointerMove = (e) => {
    if (dragStateRef.current.pointerId !== e.pointerId) return;

    const deltaX = e.clientX - dragStateRef.current.startX;
    const deltaY = e.clientY - dragStateRef.current.startY;

    if (Math.abs(deltaX) + Math.abs(deltaY) > 6) {
      dragStateRef.current.moved = true;
    }

    setLauncherPosition(clampLauncherPosition({
      x: dragStateRef.current.originX + deltaX,
      y: dragStateRef.current.originY + deltaY
    }));
  };

  const finishLauncherPointer = (e) => {
    if (dragStateRef.current.pointerId !== e.pointerId) return;

    e.currentTarget.releasePointerCapture?.(e.pointerId);
    setIsDraggingLauncher(false);

    if (!dragStateRef.current.moved) {
      openChat();
    }

    dragStateRef.current.pointerId = null;
  };

  const handleLauncherKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      openChat();
    }
  };

  const buildHistory = () => {
    return messages
      .filter(m => !Object.values(AI_COPY).some(value => value.welcome === m.content))
      .map(m => ({
        role: m.role === 'assistant' ? 'model' : 'user',
        content: m.content
      }));
  };

  const sendMessage = async (messageText) => {
    const trimmed = messageText.trim();
    if (!trimmed || isLoading) return;

    const userMessage = { role: 'user', content: trimmed };
    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    try {
      const history = buildHistory();
      const res = await menuService.sendAiChat(trimmed, history);
      const reply = res?.reply || copy.fallbackReply;
      setMessages(prev => [...prev, { role: 'assistant', content: reply }]);
    } catch {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: copy.errorReply
      }]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSend = async () => {
    await sendMessage(input);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const quickSuggestions = copy.suggestions;

  const handleQuickSuggestion = (text) => {
    sendMessage(text);
  };

  if (hidden) return null;

  return (
    <>
      {/* Draggable launcher - Only show when closed */}
      {!isOpen && (
        <div
          className="fixed z-[60]"
          style={{
            left: launcherPosition.x,
            top: launcherPosition.y
          }}
        >
          <button
            id="ai-chat-toggle"
            type="button"
            onPointerDown={handleLauncherPointerDown}
            onPointerMove={handleLauncherPointerMove}
            onPointerUp={finishLauncherPointer}
            onPointerCancel={finishLauncherPointer}
            onKeyDown={handleLauncherKeyDown}
            className={`ai-chat-launcher ${isDraggingLauncher ? 'ai-chat-launcher-dragging' : ''}`}
            aria-label={copy.openLabel}
            title={copy.openTitle}
          >
            <span className="ai-chat-launcher-halo" />
            <span className="ai-chat-launcher-face">
              <MessageCircle size={26} />
            </span>
            <div className="ai-chat-launcher-status" aria-hidden="true">
              <span />
            </div>
          </button>
        </div>
      )}

      {/* Chat Window */}
      {isOpen && (
        <div
          className="fixed inset-0 z-[55] bg-black/20 backdrop-blur-[1px] ai-chat-fade-in"
          onClick={handleBackdropClick}
        >
          <div
            className={`absolute bottom-0 right-0 w-full max-w-md h-[85vh] max-h-[700px] flex flex-col transition-all duration-300 ${isAnimating ? 'ai-chat-slide-up' : 'ai-chat-slide-down'
              }`}
            onClick={(e) => e.stopPropagation()}
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
                <h3 className="text-white font-bold text-sm tracking-tight">{copy.title}</h3>
                <div className="flex items-center gap-1 mt-0.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse"></span>
                  <span className="text-yellow-300 text-[10px]  font-bold uppercase tracking-wider">{copy.online}</span>
                </div>
              </div>

              {/* Close Button in Header */}
              <button
                onClick={handleToggle}
                className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white transition-colors border border-white/20 shadow-sm"
                aria-label={copy.closeLabel}
              >
                <X size={18} />
              </button>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4 scroll-smooth" id="ai-chat-messages">
              {messages.map((msg, idx) => (
                <div
                  key={idx}
                  className={`flex gap-2 ${msg.role === 'user' ? 'justify-end' : 'justify-start'} ai-chat-fade-in`}
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
                <div className="flex gap-2 items-start ai-chat-fade-in">
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
                  placeholder={copy.placeholder}
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
                  aria-label={copy.sendLabel}
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
        </div>
      )}

    </>
  );
};

export default AiChatAssistant;
