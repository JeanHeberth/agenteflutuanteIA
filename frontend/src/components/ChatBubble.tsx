import type { CSSProperties } from 'react';
import './ChatBubble.css';

const bubbleStyle: CSSProperties = {
  position: 'fixed',
  right: '24px',
  bottom: '24px',
  width: '68px',
  height: '68px',
  borderRadius: '9999px',
  border: 'none',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: 'linear-gradient(135deg, #6c63ff, #48bfe3)',
  color: '#ffffff',
  boxShadow: '0 12px 30px rgba(108, 99, 255, 0.35)',
  cursor: 'pointer',
  zIndex: 2147483647,
  padding: 0,
  fontFamily: 'inherit',
};

const bubbleOpenStyle: CSSProperties = {
  background: 'linear-gradient(135deg, #f87171, #ef4444)',
  boxShadow: '0 12px 30px rgba(239, 68, 68, 0.35)',
};

interface Props {
  onClick: () => void;
  isOpen: boolean;
}

export function ChatBubble({ onClick, isOpen }: Props) {
  return (
    <button
      type="button"
      data-chat-bubble="true"
      className={`chat-bubble ${isOpen ? 'open' : ''}`}
      onClick={onClick}
      title={isOpen ? 'Fechar assistente' : 'Abrir assistente'}
      aria-label={isOpen ? 'Fechar assistente' : 'Abrir assistente'}
      aria-expanded={isOpen}
      style={isOpen ? { ...bubbleStyle, ...bubbleOpenStyle } : bubbleStyle}
    >
      <span className="bubble-icon" aria-hidden="true">
        {isOpen ? '✕' : 'IA'}
      </span>
    </button>
  );
}

