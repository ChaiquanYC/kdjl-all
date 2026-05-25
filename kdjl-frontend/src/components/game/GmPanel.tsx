import { useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import styles from './GmPanel.module.css';

export default function GmPanel() {
  const [searchName, setSearchName] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [msg, setMsg] = useState<string | null>(null);
  const [givePlayerId, setGivePlayerId] = useState('');
  const [givePropId, setGivePropId] = useState('1');
  const [giveCount, setGiveCount] = useState('1');
  const [giveMoney, setGiveMoney] = useState('0');
  const [giveYb, setGiveYb] = useState('0');

  const showMsg = (m: string) => { setMsg(m); setTimeout(() => setMsg(null), 2500); };

  const handleSearch = () => {
    apiGet<any[]>('/gm/player/search?name=' + encodeURIComponent(searchName)).then((res) => {
      if (res.code === 0) setSearchResults(res.data || []);
    });
  };

  const handleGiveItem = () => {
    apiPost('/gm/give-item', { playerId: Number(givePlayerId), propId: Number(givePropId), count: Number(giveCount) }).then((res: any) => {
      showMsg(res.code === 0 ? '已发放: ' + (res.data?.given || '') : res.message);
    });
  };

  const handleGiveMoney = () => {
    apiPost('/gm/give-money', { playerId: Number(givePlayerId), money: Number(giveMoney), yb: Number(giveYb) }).then((res: any) => {
      showMsg(res.code === 0 ? '已发放' : res.message);
    });
  };

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}

      <div className={styles.section}>
        <h3>搜索玩家</h3>
        <div className={styles.row}>
          <input className={styles.input} value={searchName} onChange={(e) => setSearchName(e.target.value)} placeholder="昵称" />
          <button className={styles.btn} onClick={handleSearch}>搜索</button>
        </div>
        {searchResults.map((p: any) => (
          <div key={p.id} className={styles.result} onClick={() => setGivePlayerId(String(p.id))}>
            <span>#{p.id} {p.nickname}</span>
            <span>金币{p.money} 元宝{p.yb}</span>
          </div>
        ))}
      </div>

      {givePlayerId && (
        <>
          <div className={styles.section}>
            <h3>发放道具 (玩家#{givePlayerId})</h3>
            <div className={styles.row}>
              <input className={styles.inputSm} value={givePropId} onChange={(e) => setGivePropId(e.target.value)} placeholder="道具ID" />
              <input className={styles.inputSm} value={giveCount} onChange={(e) => setGiveCount(e.target.value)} placeholder="数量" />
              <button className={styles.btn} onClick={handleGiveItem}>发放</button>
            </div>
          </div>
          <div className={styles.section}>
            <h3>发放金币/元宝</h3>
            <div className={styles.row}>
              <input className={styles.inputSm} value={giveMoney} onChange={(e) => setGiveMoney(e.target.value)} placeholder="金币" />
              <input className={styles.inputSm} value={giveYb} onChange={(e) => setGiveYb(e.target.value)} placeholder="元宝" />
              <button className={styles.btn} onClick={handleGiveMoney}>发放</button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
