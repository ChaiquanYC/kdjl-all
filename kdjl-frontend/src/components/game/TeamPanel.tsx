import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import styles from './TeamPanel.module.css';

interface TeamInfo { id: number; name: string; creatorId: number; members: {playerId:number;nickname:string;leader:number}[]; }
interface TeamBrief { id: number; name: string; creatorId: number; memberCount: number; }

export default function TeamPanel() {
  const [myTeam, setMyTeam] = useState<TeamInfo | null>(null);
  const [teams, setTeams] = useState<TeamBrief[]>([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState<string | null>(null);

  const fetchData = () => {
    Promise.all([
      apiGet<TeamInfo>('/team/my').catch(() => ({ code: -1 } as any)),
      apiGet<TeamBrief[]>('/team/list'),
    ]).then(([myRes, listRes]) => {
      if (myRes.code === 0 && myRes.data) setMyTeam(myRes.data);
      if (listRes.code === 0 && listRes.data) setTeams(listRes.data);
      setLoading(false);
    });
  };

  useEffect(() => { fetchData(); }, []);

  const showMsg = (m: string) => { setMsg(m); setTimeout(() => setMsg(null), 2500); };

  const handleCreate = () => {
    apiPost('/team/create', { name: '队伍' }).then((res: any) => {
      if (res.code === 0) { showMsg('队伍已创建'); fetchData(); }
      else showMsg(res.message);
    });
  };

  const handleJoin = (teamId: number) => {
    apiPost('/team/join/' + teamId, {}).then((res: any) => {
      if (res.code === 0) { showMsg('加入成功'); fetchData(); }
      else showMsg(res.message);
    });
  };

  const handleLeave = () => {
    apiPost('/team/leave', {}).then((res: any) => {
      if (res.code === 0) { showMsg('已退出'); fetchData(); }
      else showMsg(res.message);
    });
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}
      {myTeam ? (
        <div className={styles.card}>
          <h3>{myTeam.name}</h3>
          <div className={styles.members}>
            {myTeam.members.map((m) => (
              <div key={m.playerId} className={styles.member}>
                <span>{m.nickname}</span>
                {m.leader === 1 && <span className={styles.leader}>队长</span>}
              </div>
            ))}
          </div>
          <button className={styles.leaveBtn} onClick={handleLeave}>退出队伍</button>
        </div>
      ) : (
        <div>
          <button className={styles.createBtn} onClick={handleCreate}>创建队伍</button>
          <h3>队伍列表</h3>
          {teams.map((t) => (
            <div key={t.id} className={styles.row}>
              <span className={styles.tname}>{t.name}</span>
              <span className={styles.tcnt}>{t.memberCount}/4人</span>
              <button className={styles.joinBtn} onClick={() => handleJoin(t.id)}>加入</button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
