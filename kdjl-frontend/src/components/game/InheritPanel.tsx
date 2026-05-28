import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import styles from './InheritPanel.module.css';

interface InheritPet { id: number; name: string; muchang: number; chchengbb: number; level: number; czl: string; remainingSeconds?: number; }
interface AvailablePet { id: number; name: string; level: number; czl: string; ownerId: number; }

export default function InheritPanel() {
  const [myPets, setMyPets] = useState<InheritPet[]>([]);
  const [available, setAvailable] = useState<AvailablePet[]>([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState<string | null>(null);
  const fetchData = () => {
    Promise.all([
      apiGet<InheritPet[]>('/inherit/mine'),
      apiGet<AvailablePet[]>('/inherit/available'),
    ]).then(([myRes, avRes]) => {
      if (myRes.code === 0 && myRes.data) setMyPets(myRes.data);
      if (avRes.code === 0 && avRes.data) setAvailable(avRes.data);
      setLoading(false);
    });
  };

  useEffect(() => { fetchData(); }, []);

  const showMsg = (m: string) => { setMsg(m); setTimeout(() => setMsg(null), 2500); };

  const stateLabel = (mc: number) => {
    switch (mc) {
      case 3: return '配对池中'; case 4: return '已配对'; case 5: return '等待确认';
      case 6: return '培育中'; case 7: return '完成可取回'; default: return '未知';
    }
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}

      <div className={styles.section}>
        <h3>我的传承宠物</h3>
        {myPets.length === 0 ? (
          <div className={styles.empty}>暂无宠物在传承中。<br/>选择一只神宠(wx=6, Lv≥90, czl≥60)加入配对池。</div>
        ) : (
          myPets.map((p) => (
            <div key={p.id} className={styles.card}>
              <div className={styles.info}>
                <span className={styles.name}>{p.name} Lv.{p.level}</span>
                <span className={styles.czl}>czl: {p.czl}</span>
                <span className={styles.state}>{stateLabel(p.muchang)}</span>
                {p.remainingSeconds !== undefined && p.remainingSeconds > 0 && (
                  <span className={styles.timer}>剩余 {Math.floor(p.remainingSeconds / 60)} 分钟</span>
                )}
              </div>
              <div className={styles.actions}>
                {p.muchang === 6 && (
                  <button className={styles.completeBtn} onClick={() => {
                    apiPost('/inherit/complete/' + p.id, { useCrystals: false }).then((res: any) => {
                      if (res.data?.notReady) {
                        if (confirm(res.data.message)) {
                          apiPost('/inherit/complete/' + p.id, { useCrystals: true }).then((r: any) => {
                            showMsg(r.code === 0 ? '传承完成!' : r.message); fetchData();
                          }).catch((e: any) => showMsg(e?.response?.data?.message || '传承失败'));
                        }
                      } else {
                        showMsg(res.code === 0 ? '传承完成!' : res.message); fetchData();
                      }
                    }).catch((e: any) => showMsg(e?.response?.data?.message || '传承请求失败'));
                  }}>取回</button>
                )}
                {(p.muchang === 3 || p.muchang === 4 || p.muchang === 5) && (
                  <button className={styles.cancelBtn} onClick={() => {
                    apiPost('/inherit/cancel/' + p.id, {}).then((res: any) => {
                      showMsg(res.code === 0 ? '已取消' : res.message); fetchData();
                    }).catch((e: any) => showMsg(e?.response?.data?.message || '取消失败'));
                  }}>取消</button>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      <div className={styles.section}>
        <h3>可配对宠物</h3>
        {available.length === 0 ? <div className={styles.empty}>暂无其他玩家的宠物在配对池</div> : (
          available.map((p) => (
            <div key={p.id} className={styles.card}>
              <span className={styles.name}>{p.name} Lv.{p.level} czl:{p.czl}</span>
              <button className={styles.pairBtn} onClick={() => {
                const myPet = myPets.find(mp => mp.muchang === 3);
                if (!myPet) { showMsg('请先将宠物加入配对池'); return; }
                apiPost('/inherit/pair/' + myPet.id + '/' + p.id, {}).then((res: any) => {
                  showMsg(res.code === 0 ? '配对成功!' : res.message); fetchData();
                }).catch((e: any) => showMsg(e?.response?.data?.message || '配对失败'));
              }}>配对</button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
