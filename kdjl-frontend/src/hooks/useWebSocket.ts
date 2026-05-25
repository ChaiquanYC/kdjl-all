import { useEffect, useCallback } from 'react';
import { getWsClient, isWsReady, onWsReady } from '@/api/websocket';
import type { ChatMessage } from '@/types';

export function useChat(onMessage: (msg: ChatMessage) => void) {
  useEffect(() => {
    const doSubscribe = () => {
      const client = getWsClient();
      if (!client.active) return;
      const sub = client.subscribe('/topic/chat', (msg) => {
        try {
          const body = JSON.parse(msg.body) as ChatMessage;
          onMessage(body);
        } catch { /* ignore malformed messages */ }
      });
      return () => { sub.unsubscribe(); };
    };

    if (isWsReady()) {
      return doSubscribe();
    } else {
      onWsReady(doSubscribe);
    }
  }, [onMessage]);
}

export function useSendMessage() {
  return useCallback((content: string, channel: string = 'world') => {
    const client = getWsClient();
    if (client.active) {
      client.publish({ destination: '/app/chat', body: JSON.stringify({ content, channel }) });
    }
  }, []);
}
