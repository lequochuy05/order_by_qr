import { MessageCircle } from 'lucide-react';

const AiChatLauncher = ({ copy, launcher }) => (
  <div className="fixed z-[60]" style={{ left: launcher.position.x, top: launcher.position.y }}>
    <button
      id="ai-chat-toggle"
      type="button"
      onPointerDown={launcher.handlePointerDown}
      onPointerMove={launcher.handlePointerMove}
      onPointerUp={launcher.finishPointer}
      onPointerCancel={launcher.finishPointer}
      onKeyDown={launcher.handleKeyDown}
      className={`ai-chat-launcher ${launcher.isDragging ? 'ai-chat-launcher-dragging' : ''}`}
      aria-label={copy.openLabel}
      title={copy.openTitle}
    >
      <span className="ai-chat-launcher-halo" />
      <span className="ai-chat-launcher-face">
        <MessageCircle size={26} />
      </span>
      <span className="ai-chat-launcher-status" aria-hidden="true">
        <span />
      </span>
    </button>
  </div>
);

export default AiChatLauncher;
