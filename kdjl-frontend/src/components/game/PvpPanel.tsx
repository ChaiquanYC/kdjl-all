import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import styles from './PvpPanel.module.css';

interface Opponent { id: number; nickname: string; level: number; petName: string; petLevel: number; prestige: number; }
interface PvpResult { won: boolean; rounds: Array<Record<string,unknown>>; atkPetName: string; defPetName: string;
  expGained: number; prestigeGained?: number; newPrestige: number; }

export default function PvpPanel() {
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [opponents, setOpponents] = useState<Opponent[]>([]);
  const [result, setResult] = useState<PvpResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [leaderboard, setLeaderboard] = useState<Array<Record<string,unknown>>>([]);
  const [tab, setTab] = useState<'fight' | 'rank'>('fight');

  const fetchOpponents = () => {
    apiGet<Opponent[]>('/pvp/opponents').then((res) => {
      if (res.code === 0 && res.data) setOpponents(res.data);
    }).catch(() => {});
  };

  useEffect(() => {
    fetchOpponents();
    apiGet<Array<Record<string,unknown>>>('/pvp/leaderboard').then((res) => {
      if (res.code === 0 && res.data) setLeaderboard(res.data);
    }).catch(() => {});
  }, []);

  const handleChallenge = (opponentId: number, name: string) => {
    if (!confirm(`确定挑战 ${name} 吗？`)) return;
    setLoading(true);
    apiPost<PvpResult>('/pvp/challenge/' + opponentId, {}).then((res) => {
      if (res.code === 0 && res.data) { setResult(res.data); triggerRefresh(); }
      else alert(res.message || '挑战失败');
      setLoading(false);
    }).catch(() => { setLoading(false); alert('挑战失败'); });
  };

  if (result) {
    return (
      <div className={styles.container}>
        <div className={styles.header}>
          <span className={styles.title}>PvP 战斗结果</span>
          <button className={styles.backBtn} onClick={() => setResult(null)}>返回</button>
        </div>
        <div className={styles.resultBox}>
          <div className={result.won ? styles.win : styles.lose}>
            {result.won ? '胜利！' : '失败！'}
          </div>
          <p>{result.atkPetName} VS {result.defPetName}</p>
          <p>获得经验：+{result.expGained}</p>
          {result.prestigeGained && <p>获得威望：+{result.prestigeGained}</p>}
          <p>当前威望：{result.newPrestige}</p>
          <div className={styles.rounds}>
            {(result.rounds || []).map((r: any, i: number) => (
              <div key={i} className={styles.roundRow}>
                <span>回合{r.round}</span>
                <span>{r.attacker} 造成 {r.damage} 伤害</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div className={styles.tabs}>
          <button className={`${styles.tab} ${tab==='fight'?styles.tabActive:''}`} onClick={()=>setTab('fight')}>挑战</button>
          <button className={`${styles.tab} ${tab==='rank'?styles.tabActive:''}`} onClick={()=>setTab('rank')}>排行</button>
        </div>
        <button className={styles.backBtn} onClick={() => setGameView(null)}>返回</button>
      </div>
      {tab === 'fight' ? (
        <div className={styles.list}>
          {opponents.length === 0 ? (
            <div className={styles.empty}>暂无在线对手</div>
          ) : (
            opponents.map(o => (
              <div key={o.id} className={styles.oppRow}>
                <span className={styles.name}>{o.nickname}</span>
                <span className={styles.lv}>Lv.{o.level}</span>
                <span className={styles.pet}>{o.petName} Lv.{o.petLevel}</span>
                <span className={styles.pres}>威望:{o.prestige}</span>
                <button className={styles.challengeBtn} onClick={() => handleChallenge(o.id, o.nickname)}
                  disabled={loading}>{loading ? '...' : '挑战'}</button>
              </div>
            ))
          )}
        </div>
      ) : (
        <div className={styles.list}>
          {leaderboard.map((r: any, i: number) => (
            <div key={i} className={styles.rankRow}>
              <span className={styles.rank}>#{r.rank}</span>
              <span className={styles.name}>{r.nickname}</span>
              <span className={styles.pres}>威望:{r.prestige}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
