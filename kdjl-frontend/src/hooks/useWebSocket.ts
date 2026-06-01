import { useEffect, useCallback, useRef } from 'react';
import { getWsClient, isWsReady, onWsReady } from '@/api/websocket';
import type { ChatMessage } from '@/types';

export function useChat(onMessage: (msg: ChatMessage) => void) {
  const cbRef = useRef(onMessage);
  cbRef.current = onMessage;

  useEffect(() => {
    const doSubscribe = () => {
      const client = getWsClient();
      if (!client.active) return;
      const sub = client.subscribe('/topic/chat', (msg) => {
        try {
          const body = JSON.parse(msg.body) as ChatMessage;
          cbRef.current(body);
        } catch { /* ignore malformed messages */ }
      });
      return () => { sub.unsubscribe(); };
    };

    if (isWsReady()) {
      return doSubscribe();
    } else {
      onWsReady(doSubscribe);
    }
  }, []);
}

export function useChatAll(onMessage: (msg: ChatMessage) => void) {
  const cbRef = useRef(onMessage);
  cbRef.current = onMessage;

  useEffect(() => {
    const topics = ['/topic/chat', '/topic/guild', '/topic/team'];
    const doSubscribe = () => {
      const client = getWsClient();
      if (!client.active) return;
      const subs = topics.map((topic) =>
        client.subscribe(topic, (msg) => {
          try {
            const body = JSON.parse(msg.body) as ChatMessage;
            cbRef.current(body);
          } catch { /* ignore */ }
        })
      );
      return () => { subs.forEach((s) => s.unsubscribe()); };
    };

    if (isWsReady()) {
      return doSubscribe();
    } else {
      onWsReady(doSubscribe);
    }
  }, []);
}

export function useSendMessage() {
  return useCallback((content: string, channel: string = 'world') => {
    const client = getWsClient();
    if (client.active) {
      client.publish({ destination: '/app/chat', body: JSON.stringify({ content, channel }) });
    }
  }, []);
}
