import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import styles from './ZhanBuPanel.module.css';

interface StoneType { id: number; name: string; effect: string; }

export default function ZhanBuPanel() {
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [allStones, setAllStones] = useState<StoneType[]>([]);
  const [bagMap, setBagMap] = useState<Record<number, number>>({});
  const [selectedPid, setSelectedPid] = useState<number | null>(null);
  const [result, setResult] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchData = () => {
    apiGet<any>('/bag/stone-types').then((res) => {
      if (res.code === 0 && res.data) {
        setAllStones(Array.isArray(res.data) ? res.data as StoneType[] : []);
      }
    }).catch(() => {});
    apiGet<any>('/bag').then((res) => {
      if (res.code === 0 && res.data) {
        const map: Record<number, number> = {};
        (res.data as any[]).forEach((i: any) => {
          if (i.varyname === 22 && i.count > 0) map[i.propId] = i.count;
        });
        setBagMap(map);
      }
    }).catch(() => {});
  };

  useEffect(() => { fetchData(); }, []);

  const handleSacrifice = () => {
    if (!selectedPid) { alert('请先选择一个魔法石！'); return; }
    if (!bagMap[selectedPid]) { alert('无相关魔法石，无法满足释放魔法需要的魔力T_T下次再来吧。'); return; }
    setLoading(true);
    apiPost<Record<string, unknown>>('/bag/use-by-pid', { pid: selectedPid, js: true }).then((res) => {
      if (res.code === 0 && res.data) {
        const d = res.data as Record<string, unknown>;
        setResult((d.message as string) || '占卜完成');
        triggerRefresh();
        fetchData();
      } else setResult(res.message || '占卜失败');
      setLoading(false);
      setTimeout(() => setResult(null), 3000);
    }).catch(() => { setLoading(false); setResult('网络错误'); });
  };

  return (
    <div className={styles.container}>
      <img src="/images/fly.jpg" className={styles.leftImg} alt="" />
      <div className={styles.rightArea}>
        <div className={styles.descCol}>
          <div className={styles.descText}>
            欢迎来到魔法屋，我是魔法屋的芙蕾娅，我可以帮您使用您携带的魔法石。每种不同的魔法石将触发对应的魔法，每次使用将消耗一个魔法石。
          </div>
          <img src="/images/fly_cion.jpg" className={styles.useBtn}
            onClick={handleSacrifice} alt="使用"
            style={{opacity: loading ? 0.5 : 1}} />
        </div>
        <div className={styles.stoneGrid}>
          {result && <div className={styles.toast}>{result}</div>}
          <div className={styles.gridTable}>
            {allStones.map((s) => {
              const count = bagMap[s.id] || 0;
              return (
                <div key={s.id}
                  className={`${styles.stoneCard} ${selectedPid === s.id ? styles.selected : ''} ${count === 0 ? styles.noStock : ''}`}
                  onClick={() => count > 0 && setSelectedPid(s.id)}
                  onDoubleClick={() => { setSelectedPid(s.id); handleSacrifice(); }}
                >
                  <img src={`/images/pai/${s.id}.gif`} alt="" />
                  <span>{s.name.replace('石', '')}</span>
                  <span className={styles.count}>{count > 0 ? `x${count}` : '无'}</span>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
