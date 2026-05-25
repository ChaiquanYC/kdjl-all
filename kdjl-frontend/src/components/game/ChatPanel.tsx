import { useState, useRef, useEffect } from 'react';
import { useGameStore } from '@/stores/gameStore';
import { useSendMessage } from '@/hooks/useWebSocket';
import styles from './ChatPanel.module.css';

type Channel = 'world' | 'guild' | 'team';
const CHANNELS: { key: Channel; label: string }[] = [
  { key: 'world', label: '世界' }, { key: 'guild', label: '公会' }, { key: 'team', label: '队伍' },
];

export default function ChatPanel({ embedded }: { embedded?: boolean }) {
  const messages = useGameStore((s) => s.chatMessages);
  const sendMessage = useSendMessage();
  const [input, setInput] = useState('');
  const [channel, setChannel] = useState<Channel>('world');
  const bottomRef = useRef<HTMLDivElement>(null);

  const filtered = messages.filter((m) => m.channel === channel);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [filtered]);

  const handleSend = () => {
    if (!input.trim()) return;
    sendMessage(input.trim(), channel);
    setInput('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSend();
  };

  return (
    <div className={styles.chatPanel}>
      {!embedded && <div className={styles.header}>
        {CHANNELS.map((ch) => (
          <button key={ch.key} className={channel === ch.key ? styles.chActive : styles.chBtn}
            onClick={() => setChannel(ch.key)}>{ch.label}</button>
        ))}
      </div>}
      <div className={embedded ? styles.msgEmbed : styles.messages}>
        {filtered.map((msg) => (
          <div key={msg.id} className={styles.message}>
            <span className={styles.time}>{new Date(msg.timestamp).toLocaleTimeString().slice(0,5)}</span>
            <span className={styles.sender}>{msg.senderName}:</span>
            <span className={styles.content}>{msg.content}</span>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>
      {!embedded && <div className={styles.inputArea}>
        <input type="text" value={input} onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown} placeholder={`${CHANNELS.find(c=>c.key===channel)?.label}频道...`}
          className={styles.input} />
        <button onClick={handleSend} className={styles.sendBtn}>发送</button>
      </div>}
    </div>
  );
}
