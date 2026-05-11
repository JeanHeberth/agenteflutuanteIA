import type { Message } from '../types/chat';
import './MessageBubble.css';

interface Props {
  message: Message;
}

export function MessageBubble({ message }: Props) {
  const isUser = message.role === 'user';
  const time = new Date(message.timestamp).toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
  });

  return (
    <div className={`message-row ${isUser ? 'user' : 'assistant'}`}>
      {!isUser && <div className="avatar">🤖</div>}
      <div className={`bubble ${isUser ? 'user' : 'assistant'}`}>
        <p>{message.content}</p>
        <span className="time">{time}</span>
      </div>
      {isUser && <div className="avatar">👤</div>}
    </div>
  );
}

