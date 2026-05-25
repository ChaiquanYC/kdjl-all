import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import styles from './MarryPanel.module.css';

export default function MarryPanel() {
  const [status, setStatus] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState<string | null>(null);
  const [targetId, setTargetId] = useState('');
  const [bagItemId, setBagItemId] = useState('');

  const fetchData = () => {
    apiGet<any>('/marriage/status').then((res) => {
      if (res.code === 0) setStatus(res.data);
      setLoading(false);
    });
  };
  useEffect(() => { fetchData(); }, []);
  const showMsg = (m: string) => { setMsg(m); setTimeout(() => setMsg(null), 2500); };

  const handlePropose = () => {
    apiPost('/marriage/propose', { targetPlayerId: Number(targetId), bagItemId: Number(bagItemId), count: 1 }).then((res: any) => {
      showMsg(res.code === 0 ? '已求婚!' : res.message); fetchData();
    });
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}
      {status?.married ? (
        <div className={styles.card}>
          <h3>💑 已婚</h3>
          <p>配偶: {status.spouseName || '#' + status.spouseId}</p>
          <p>水晶: {status.crystals || 0}</p>
          <button className={styles.btn} onClick={() => {
            if (confirm('离婚将消耗2000水晶, 确定?')) {
              apiPost('/marriage/divorce/request', {}).then((res: any) => {
                showMsg(res.code === 0 ? '已提出离婚' : res.message); fetchData();
              });
            }
          }}>提出离婚 (2000水晶)</button>
        </div>
      ) : (
        <div className={styles.card}>
          <h3>💔 未婚</h3>
          {status?.pendingProposal ? (
            <p>已向他人求婚, 等待回应...</p>
          ) : (
            <div className={styles.form}>
              <input className={styles.input} value={targetId} onChange={(e) => setTargetId(e.target.value)} placeholder="对方玩家ID" />
              <input className={styles.input} value={bagItemId} onChange={(e) => setBagItemId(e.target.value)} placeholder="定情信物(背包道具ID)" />
              <button className={styles.btn} onClick={handlePropose}>求婚</button>
            </div>
          )}
          {status?.divorceRequested && (
            <div className={styles.divorce}>
              <p>离婚请求中...</p>
              <button className={styles.btnSm} onClick={() => {
                apiPost('/marriage/divorce/cancel', {}).then((res: any) => {
                  showMsg(res.code === 0 ? '已取消离婚' : res.message); fetchData();
                });
              }}>取消离婚</button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
