import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import styles from './ChallengePanel.module.css';

interface PetBrief { id: number; name: string; level: number; cardImg?: string; }
interface CState { maxAttempts: number; usedAttempts: number; remainingAttempts: number; difficulty: number; }

interface Props {
  onChallenge: (monsterId: number, monsterName: string, mapId?: number) => void;
  onLeave: () => void;
}

export default function ChallengePanel({ onChallenge, onLeave }: Props) {
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [state, setState] = useState<CState | null>(null);
  const [pets, setLocalPets] = useState<PetBrief[]>([]);
  const selectedPetId = useGameStore((s) => s.selectedPetId);
  const setSelectedPetId = useGameStore((s) => s.setSelectedPetId);
  const [difficulty, setDifficulty] = useState(1);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    Promise.all([
      apiGet<CState>('/challenge/state'),
      apiGet<PetBrief[]>('/pets'),
    ]).then(([sRes, pRes]) => {
      if (sRes.code === 0 && sRes.data) { setState(sRes.data); setDifficulty(sRes.data.difficulty || 1); }
      if (pRes.code === 0 && pRes.data) setLocalPets(pRes.data);
    }).catch(() => {});
  }, []);

  const handleEnter = () => {
    const petId = selectedPetId || pets[0]?.id;
    if (!petId) { alert('请选择一只宠物！'); return; }
    setLoading(true);
    apiPost<Record<string,unknown>>('/challenge/enter', { difficulty }).then((res) => {
      if (res.code === 0 && res.data) {
        const d = res.data as Record<string,unknown>;
        triggerRefresh();
        onChallenge(d.monsterId as number, d.monsterName as string, 125);
      } else alert(res.message || '挑战失败');
      setLoading(false);
    }).catch(() => { setLoading(false); });
  };

  return (
    <div className={styles.container}>
      {/* COL 1: 292px */}
      <div className={styles.col1}>
        <img src="/images/ui/team/zdy01.jpg" className={styles.headerImg} alt="" onClick={onLeave} />
        <div className={styles.descBox}>
          <table className={styles.descTable}><tbody>
            <tr><td>挑战模式 — 琥珀屋</td></tr>
            <tr><td>剩余次数：{state?.remainingAttempts ?? 0}/{state?.maxAttempts ?? 3}</td></tr>
            {state?.remainingAttempts === 0 && <tr><td style={{color:'#ff4d4f'}}>今日次数已用完</td></tr>}
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

      {/* COL 3: 241px — team + buttons */}
      <div className={styles.col3}>
        <div className={styles.teamHeader}>
          <img src="/images/ui/team/zdy07.jpg" alt="" /><img src="/images/ui/team/zdy08.jpg" alt="" /><img src="/images/ui/team/zdy09.jpg" alt="" />
        </div>
        <div className={styles.teamArea}><span>暂未组队</span></div>
        <div className={styles.actions}>
          {state && state.remainingAttempts > 0 && (
            <img src="/images/ui/team/ann06.gif" className={styles.btn} onClick={handleEnter} alt="战斗" />
          )}
          <img src="/images/ui/team/ann02.gif" className={styles.btn} onClick={onLeave} alt="离开" />
        </div>
      </div>

      {/* COL 4: separator */}
      <div className={styles.sep} />

      {/* COL 5: monster info + difficulty */}
      <div className={styles.col5}>
        <div className={styles.teamHeader}>
          <img src="/images/ui/team/zdy10.jpg" alt="" /><img src="/images/ui/team/zdy08.jpg" alt="" /><img src="/images/ui/team/zdy09.jpg" alt="" />
        </div>
        <div className={styles.monsterArea}>
          <p>选择星级难度挑战不同等级的怪物</p>
          <div className={styles.stars}>
            {[1,2,3,4,5].map(n => (
              <span key={n} className={n <= difficulty ? styles.starOn : styles.starOff}
                onClick={() => setDifficulty(n)}>
                {'★'.repeat(n)}
              </span>
            ))}
          </div>
        </div>
        <img src="/images/ui/team/zdy11.jpg" alt="" />
      </div>
    </div>
  );
}
