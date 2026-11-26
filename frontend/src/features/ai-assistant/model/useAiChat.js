import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { aiCustomerService } from '@entities/ai';
import { getAiCopy, isWelcomeMessage } from '../lib/aiCopy.js';
import { normalizeChatSuggestions } from '../lib/chatSuggestions.js';

const useAiChat = (language) => {
  const copy = useMemo(() => getAiCopy(language), [language]);
  const welcomeMessage = useMemo(
    () => ({ role: 'assistant', content: copy.welcome }),
    [copy.welcome],
  );
  const [messages, setMessages] = useState([welcomeMessage]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);
  const isMountedRef = useRef(true);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setMessages((current) => {
        if (
          current.length === 1 &&
          current[0]?.role === 'assistant' &&
          isWelcomeMessage(current[0].content)
        ) {
          return [welcomeMessage];
        }
        return current;
      });
    }, 0);
    return () => window.clearTimeout(timeout);
  }, [welcomeMessage]);

  const focusInput = useCallback(() => {
    window.setTimeout(() => inputRef.current?.focus(), 300);
  }, []);

  const buildHistory = useCallback(
    () =>
      messages
        .filter((message) => !isWelcomeMessage(message.content))
        .slice(-10)
        .map((message) => ({
          role: message.role === 'assistant' ? 'model' : 'user',
          content: (message.content || '').slice(0, 2000),
        })),
    [messages],
  );

  const sendMessage = useCallback(
    async (messageText) => {
      const trimmed = messageText.trim();
      if (!trimmed || isLoading) return;

      setMessages((current) => [...current, { role: 'user', content: trimmed }]);
      setInput('');
      setIsLoading(true);
      try {
        const response = await aiCustomerService.chat(trimmed, buildHistory());
        if (isMountedRef.current) {
          setMessages((current) => [
            ...current,
            {
              role: 'assistant',
              content: response?.reply || copy.fallbackReply,
              suggestions: normalizeChatSuggestions(response?.suggestions),
            },
          ]);
        }
      } catch {
        if (isMountedRef.current) {
          setMessages((current) => [...current, { role: 'assistant', content: copy.errorReply }]);
        }
      } finally {
        if (isMountedRef.current) {
          setIsLoading(false);
        }
      }
    },
    [buildHistory, copy.errorReply, copy.fallbackReply, isLoading],
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

  return {
    copy,
    messages,
    input,
    isLoading,
    messagesEndRef,
    inputRef,
    setInput,
    focusInput,
    sendMessage,
    handleSend,
    handleKeyDown,
  };
};

export default useAiChat;
