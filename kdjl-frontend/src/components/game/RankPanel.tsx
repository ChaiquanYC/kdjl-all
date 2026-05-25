import { useEffect, useState } from 'react';
import { apiGet } from '@/api/client';
import styles from './RankPanel.module.css';

interface RankEntry { rank: number; playerId: number; nickname: string; value: number; }
type RankType = 'level' | 'money' | 'prestige';

export default function RankPanel() {
  const [type, setType] = useState<RankType>('level');
  const [data, setData] = useState<RankEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    apiGet<RankEntry[]>('/rank/' + type).then((res) => {
      if (res.code === 0 && res.data) setData(res.data);
      setLoading(false);
    });
  }, [type]);

  const TYPE_LABELS: Record<RankType, string> = { level: '宠物等级', money: '金币', prestige: '声望' };

  return (
    <div className={styles.container}>
      <div className={styles.tabs}>
        {(Object.keys(TYPE_LABELS) as RankType[]).map((t) => (
          <button key={t} className={type === t ? styles.tabActive : styles.tab} onClick={() => setType(t)}>
            {TYPE_LABELS[t]}
          </button>
        ))}
      </div>
      {loading ? <div className={styles.loading}>加载中...</div> : (
        <div className={styles.list}>
          {data.map((e) => (
            <div key={e.playerId} className={styles.row}>
              <span className={styles.rank}>#{e.rank}</span>
              <span className={styles.name}>{e.nickname}</span>
              <span className={styles.val}>{e.value}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
