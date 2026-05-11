import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { MessageBubble } from './MessageBubble';
import { useWebSocket } from '../hooks/useWebSocket';
import './ChatDialog.css';

interface Props {
  sessionId: string;
  onClose: () => void;
}

export function ChatDialog({ sessionId, onClose }: Props) {
  const { connected, messages, loading, sendMessage, clearMessages } = useWebSocket(sessionId);
  const [input, setInput] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const handleSend = () => {
    const trimmed = input.trim();
    if (!trimmed || !connected || loading) return;
    sendMessage(trimmed);
    setInput('');
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="chat-dialog">
      {/* Header */}
      <div className="chat-header">
        <div className="chat-header-info">
          <span className={`status-dot ${connected ? 'online' : 'offline'}`} />
          <span className="chat-title">Assistente IA</span>
          <span className="chat-subtitle">{connected ? 'Online' : 'Conectando...'}</span>
        </div>
        <div className="chat-header-actions">
          <button className="btn-icon" onClick={clearMessages} title="Limpar conversa">🗑️</button>
          <button className="btn-icon" onClick={onClose} title="Fechar">✕</button>
        </div>
      </div>

      {/* Messages */}
      <div className="chat-messages">
        {messages.length === 0 && (
          <div className="empty-state">
            <p>👋 Olá! Como posso te ajudar hoje?</p>
          </div>
        )}
        {messages.map(msg => (
          <MessageBubble key={msg.id} message={msg} />
        ))}
        {loading && (
          <div className="message-row assistant">
            <div className="avatar">🤖</div>
            <div className="bubble assistant typing">
              <span /><span /><span />
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="chat-input-area">
        <textarea
          className="chat-input"
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Digite sua mensagem... (Enter para enviar)"
          rows={1}
          disabled={!connected}
        />
        <button
          className="btn-send"
          onClick={handleSend}
          disabled={!connected || !input.trim() || loading}
        >
          ➤
        </button>
      </div>
    </div>
  );
}

