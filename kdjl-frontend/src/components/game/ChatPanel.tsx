import { useState, useRef, useEffect, type ReactNode } from 'react';
import { useGameStore } from '@/stores/gameStore';
import { useSendMessage } from '@/hooks/useWebSocket';
import type { ChatMessage } from '@/types';
import styles from './ChatPanel.module.css';

type Channel = 'world' | 'guild' | 'team';
const CHANNELS: { key: Channel; label: string }[] = [
  { key: 'world', label: '世界' }, { key: 'guild', label: '公会' }, { key: 'team', label: '队伍' },
];

const CHANNEL_COLORS: Record<string, string> = {
  world: '#fff',
  private: '#B64ABA',
  guild: '#4E7BE7',
  team: '#009900',
  system: '#C95C14',
};

const EMOTION_RE = /\((\d+)\)/g;

function renderContent(text: string, color: string): ReactNode[] {
  const parts: ReactNode[] = [];
  let lastIndex = 0;
  let count = 0;
  let match: RegExpExecArray | null;

  EMOTION_RE.lastIndex = 0;
  while ((match = EMOTION_RE.exec(text)) !== null && count < 5) {
    const num = parseInt(match[1], 10);
    if (num < 1 || num > 36) continue;
    if (match.index > lastIndex) {
      parts.push(<span key={`t${lastIndex}`} style={{ color }}>{text.slice(lastIndex, match.index)}</span>);
    }
    parts.push(<img key={`e${match.index}`} src={`/images/ui/motion/${num}.gif`} alt={`(${num})`} />);
    lastIndex = match.index + match[0].length;
    count++;
  }
  if (lastIndex < text.length) {
    parts.push(<span key={`t${lastIndex}`} style={{ color }}>{text.slice(lastIndex)}</span>);
  }
  return parts.length > 0 ? parts : [<span key="0" style={{ color }}>{text}</span>];
}

function handleWhisper(name: string) {
  const inp = document.getElementById('chatmsg') as HTMLInputElement | null;
  if (inp) {
    inp.value = `//${name} `;
    inp.focus();
  }
}

export default function ChatPanel({ embedded, channelFilter }: { embedded?: boolean; channelFilter?: string }) {
  const messages = useGameStore((s) => s.chatMessages);
  const sendMessage = useSendMessage();
  const [input, setInput] = useState('');
  const [channel, setChannel] = useState<Channel>('world');
  const bottomRef = useRef<HTMLDivElement>(null);

  const filtered = messages.filter((m) => {
    if (embedded) {
      if (channelFilter) return m.channel === channelFilter;
      return true;
    }
    return m.channel === channel;
  });

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

  const renderMessage = (msg: ChatMessage) => {
    const time = new Date(msg.timestamp).toLocaleTimeString().slice(0, 5);
    const color = CHANNEL_COLORS[msg.channel] || '#fff';
    const isSystem = msg.channel === 'system';

    if (isSystem) {
      return (
        <div key={msg.id} className={styles.message}>
          <span className={styles.msgEmbedTime}>{time}</span>
          <span style={{ color: '#C95C14' }}>[系 统]</span>
          <span style={{ color }}>{msg.content}</span>
        </div>
      );
    }

    const showVip = msg.vip === 1 || msg.vip === 3;
    const showMarried = msg.vip != null && msg.vip >= 2;

    return (
      <div key={msg.id} className={styles.message}>
        <span className={styles.msgEmbedTime}>{time}</span>
        <span
          className={styles.clickableName}
          onClick={() => handleWhisper(msg.senderName)}
          title={`私聊 ${msg.senderName}`}
        >
          {msg.senderName}
        </span>
        {showMarried && <img src="/images/merge.gif" className={styles.mergeIcon} alt="" />}
        {showVip && <span className={styles.vipBadge}>(VIP)</span>}
        <span style={{ color: '#fff' }}>:</span>
        <span style={{ color }}>{renderContent(msg.content, color)}</span>
      </div>
    );
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
        {filtered.map(renderMessage)}
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
