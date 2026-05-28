import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './TowerPanel.module.css';

interface PetBrief { id: number; name: string; level: number; cardImg?: string; }
interface TowerState { currentFloor: number; maxFloor: number; canChallenge: boolean; }
interface LbEntry { rank: number; playerId: number; nickname: string; floor: number; }

interface Props {
  onChallenge: (monsterId: number, monsterName: string, mapId?: number) => void;
  onLeave: () => void;
}

const STARS = ['','★','★★','★★★','★★★★','★★★★★'];

export default function TowerPanel({ onChallenge, onLeave }: Props) {
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const player = useAuthStore((s) => s.player);
  const [tower, setTower] = useState<TowerState | null>(null);
  const [leaderboard, setLeaderboard] = useState<LbEntry[]>([]);
  const [pets, setLocalPets] = useState<PetBrief[]>([]);
  const selectedPetId = useGameStore((s) => s.selectedPetId);
  const setSelectedPetId = useGameStore((s) => s.setSelectedPetId);
  const [difficulty, setDifficulty] = useState(1);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    Promise.all([
      apiGet<TowerState>('/tower/state'),
      apiGet<LbEntry[]>('/tower/leaderboard'),
      apiGet<PetBrief[]>('/pets'),
    ]).then(([tRes, lRes, pRes]) => {
      if (tRes.code === 0 && tRes.data) setTower(tRes.data);
      if (lRes.code === 0 && lRes.data) setLeaderboard(lRes.data);
      if (pRes.code === 0 && pRes.data) {
        setLocalPets(pRes.data);
        if (!selectedPetId && pRes.data.length > 0) {
          const mainPet = player?.mbid ? pRes.data.find(p => p.id === player.mbid) : null;
          setSelectedPetId(mainPet ? mainPet.id : pRes.data[0].id);
        }
      }
    }).catch(() => {});
  }, []);

  const handleEnter = () => {
    const petId = selectedPetId || pets[0]?.id;
    if (!petId) { alert('请选择一只宠物！'); return; }
    setLoading(true);
    apiPost<Record<string,unknown>>('/tower/challenge', { difficulty }).then((res) => {
      if (res.code === 0 && res.data) {
        const d = res.data as Record<string,unknown>;
        triggerRefresh();
        onChallenge(d.monsterId as number, d.monsterName as string, 126);
      } else alert(res.message || '挑战失败');
      setLoading(false);
    }).catch(() => { setLoading(false); });
  };

  return (
    <div className={styles.container}>
      {/* COL 1: 292px — zdy01 header + zdy02 desc + zdy03 bottom + pets */}
      <div className={styles.col1}>
        <img src="/images/ui/team/zdy01.jpg" className={styles.headerImg} alt="" onClick={onLeave} />
        <div className={styles.descBox}>
          <table className={styles.descTable}><tbody>
            <tr><td>当前层数：{tower?.currentFloor ?? 0} / {tower?.maxFloor ?? 55}</td></tr>
            <tr><td>难度选择：{STARS[difficulty]}</td></tr>
            {tower && tower.currentFloor >= tower.maxFloor && <tr><td style={{color:'#52c41a'}}>已通关全部楼层！</td></tr>}
          </tbody></table>
        </div>
        <img src="/images/ui/team/zdy03.jpg" className={styles.bottomImg} alt="" />
        <img src="/images/ui/team/zdy04.jpg" className={styles.petHeader} alt="" />
        <div className={styles.petArea}>
          {pets.slice(0, 3).map((p, i) => (
            <img key={p.id}
              src={`/images/bb/${p.cardImg || 'g1.gif'}`}
              alt={p.name} title={`${p.name} Lv.${p.level}`}
              className={styles.petImg}
              style={{opacity: selectedPetId ? (selectedPetId === p.id ? 1 : 0.5) : (i === 0 ? 1 : 0.5)}}
              onClick={() => setSelectedPetId(p.id)}
            />
          ))}
        </div>
      </div>

      {/* COL 2: separator */}
      <div className={styles.sep} />

      {/* COL 3: 241px — team info + buttons */}
      <div className={styles.col3}>
        <div className={styles.teamHeader}>
          <img src="/images/ui/team/zdy07.jpg" alt="" /><img src="/images/ui/team/zdy08.jpg" alt="" /><img src="/images/ui/team/zdy09.jpg" alt="" />
        </div>
        <div className={styles.teamArea}><span>暂未组队</span></div>
        <div className={styles.actions}>
          <img src="/images/ui/team/ann06.gif" className={styles.btn} onClick={handleEnter} alt="战斗" style={{opacity: loading ? 0.5 : 1}} />
          <img src="/images/ui/team/ann02.gif" className={styles.btn} onClick={onLeave} alt="离开" />
        </div>
      </div>

      {/* COL 4: separator */}
      <div className={styles.sep} />

      {/* COL 5: leaderboard */}
      <div className={styles.col5}>
        <div className={styles.teamHeader}>
          <img src="/images/ui/team/zdy10.jpg" alt="" /><img src="/images/ui/team/zdy08.jpg" alt="" /><img src="/images/ui/team/zdy09.jpg" alt="" />
        </div>
        <div className={styles.rankList}>
          {leaderboard.map(r => (
            <div key={r.rank} className={styles.rankRow}>
              <span className={styles.rn}>#{r.rank}</span>
              <span className={styles.rnm}>{r.nickname}</span>
              <span className={styles.rf}>{r.floor}层</span>
            </div>
          ))}
        </div>
        <img src="/images/ui/team/zdy11.jpg" alt="" />
        <div className={styles.diffRow}>
          {[1,2,3].map(n => (
            <span key={n} className={n===difficulty?styles.diffOn:styles.diffOff}
              onClick={() => setDifficulty(n)}>{['普通','困难','冒险'][n-1]}</span>
          ))}
        </div>
      </div>
    </div>
  );
}
