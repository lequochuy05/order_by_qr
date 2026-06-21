export const AI_COPY = {
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
    suggestions: ['Gợi ý món ngon đi', 'Có combo nào?', 'Có danh mục gì?'],
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
    suggestions: ['Recommend something tasty', 'Any combos?', 'What categories are available?'],
  },
};

export const getAiCopy = (language) => AI_COPY[language] || AI_COPY.vi;

export const isWelcomeMessage = (message) =>
  Object.values(AI_COPY).some((copy) => copy.welcome === message);
