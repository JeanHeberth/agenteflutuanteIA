import { useEffect, useState } from 'react';
import { ChatBubble } from './ChatBubble';
import { ChatDialog } from './ChatDialog';

function createSessionId() {
  const generated = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `session-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

  if (typeof window === 'undefined') {
    return generated;
  }

  try {
    const saved = window.localStorage.getItem('agenteflutuanteia.sessionId');
    if (saved) {
      return saved;
    }

    window.localStorage.setItem('agenteflutuanteia.sessionId', generated);
  } catch {
    // Ignora restrições de storage do navegador e segue com ID em memória.
  }

  return generated;
}

export function ChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [sessionId] = useState(createSessionId);
  const [bubbleInDom, setBubbleInDom] = useState(false);

  useEffect(() => {
    if (typeof document === 'undefined') {
      setBubbleInDom(false);
      return;
    }

    const updateBubbleState = () => {
      setBubbleInDom(Boolean(document.querySelector('[data-chat-bubble="true"]')));
    };

    updateBubbleState();

    const timeoutId = window.setTimeout(updateBubbleState, 0);
    return () => window.clearTimeout(timeoutId);
  }, [isOpen]);

  return (
    <>
      {isOpen && (
        <ChatDialog sessionId={sessionId} onClose={() => setIsOpen(false)} />
      )}
      <ChatBubble onClick={() => setIsOpen(prev => !prev)} isOpen={isOpen} />
      {import.meta.env.DEV && (
        <div
          style={{
            position: 'fixed',
            top: 16,
            left: 16,
            zIndex: 2147483647,
            padding: '10px 12px',
            borderRadius: 10,
            background: 'rgba(17, 24, 39, 0.9)',
            color: '#fff',
            fontSize: 12,
            fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace',
            boxShadow: '0 12px 32px rgba(0, 0, 0, 0.25)',
          }}
        >
          widget: mounted | bubbleInDom: {bubbleInDom ? 'true' : 'false'}
        </div>
      )}
    </>
  );
}

