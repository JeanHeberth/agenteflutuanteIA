import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { Message } from '../types/chat';

export function useWebSocket(sessionId: string) {
  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/chat/${sessionId}`, (msg: IMessage) => {
          const response: Message = JSON.parse(msg.body);
          setMessages(prev => [...prev, response]);
          setLoading(false);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [sessionId]);

  const sendMessage = useCallback((content: string) => {
    if (!clientRef.current?.connected) return;

    const userMsg: Message = {
      id: crypto.randomUUID(),
      role: 'user',
      content,
      timestamp: new Date().toISOString(),
    };

    setMessages(prev => [...prev, userMsg]);
    setLoading(true);

    clientRef.current.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ sessionId, content }),
    });
  }, [sessionId]);

  const clearMessages = useCallback(() => {
    setMessages([]);
    fetch(`/api/chat/session/${sessionId}`, { method: 'DELETE' }).catch(console.error);
  }, [sessionId]);

  return { connected, messages, loading, sendMessage, clearMessages };
}

