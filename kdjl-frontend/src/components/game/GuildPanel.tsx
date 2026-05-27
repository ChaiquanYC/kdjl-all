import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import styles from './GuildPanel.module.css';

interface GuildInfo { id: number; name: string; level: number; memberCount: number; honor: number; info: string; }
interface GuildDetail extends GuildInfo { members: {playerId:number;nickname:string;priv:number;contribution:number}[]; }

export default function GuildPanel() {
  const [myGuild, setMyGuild] = useState<GuildDetail | null>(null);
  const [guilds, setGuilds] = useState<GuildInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [guildName, setGuildName] = useState('');
  const [guildInfo, setGuildInfo] = useState('');

  const fetchData = () => {
    Promise.all([
      apiGet<GuildDetail>('/guild/my').catch(() => ({ code: -1 } as any)),
      apiGet<GuildInfo[]>('/guild/list'),
    ]).then(([myRes, listRes]) => {
      if (myRes.code === 0 && myRes.data) setMyGuild(myRes.data);
      if (listRes.code === 0 && listRes.data) setGuilds(listRes.data);
      setLoading(false);
    });
  };

  useEffect(() => { fetchData(); }, []);

  const handleCreate = () => {
    apiPost('/guild/create', { name: guildName, info: guildInfo }).then((res: any) => {
      if (res.code === 0) { setMsg('公会创建成功!'); fetchData(); setShowCreate(false); }
      else setMsg(res.message);
      setTimeout(() => setMsg(null), 2500);
    }).catch((e: any) => {
      setMsg(e?.response?.data?.message || '创建失败');
      setTimeout(() => setMsg(null), 2500);
    });
  };

  const handleJoin = (guildId: number) => {
    apiPost('/guild/join/' + guildId, {}).then((res: any) => {
      setMsg(res.code === 0 ? '加入成功!' : res.message);
      setTimeout(() => setMsg(null), 2500);
      fetchData();
    }).catch((e: any) => {
      setMsg(e?.response?.data?.message || '加入失败');
      setTimeout(() => setMsg(null), 2500);
    });
  };

  const handleLeave = () => {
    if (!confirm('确定离开/解散公会?')) return;
    apiPost('/guild/leave', {}).then((res: any) => {
      setMsg(res.code === 0 ? '已退出' : res.message);
      setTimeout(() => setMsg(null), 2500);
      fetchData();
    }).catch((e: any) => {
      setMsg(e?.response?.data?.message || '退出失败');
      setTimeout(() => setMsg(null), 2500);
    });
  };

  if (loading) return <div className={styles.loading}>加载中...</div>;

  const privLabel = (p: number) => p === 3 ? '会长' : p === 2 ? '长老' : '成员';

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}

      {myGuild ? (
        <div className={styles.myGuild}>
          <h3>{myGuild.name} Lv.{myGuild.level}</h3>
          <p>{myGuild.info || '无简介'}</p>
          <p>荣誉: {myGuild.honor} | 成员: {myGuild.memberCount}</p>
          <h4>成员列表</h4>
          {myGuild.members.map((m) => (
            <div key={m.playerId} className={styles.member}>
              <span>{m.nickname}</span>
              <span className={styles.priv}>{privLabel(m.priv)}</span>
              <span>贡献: {m.contribution}</span>
            </div>
          ))}
          <button className={styles.leaveBtn} onClick={handleLeave}>退出公会</button>
        </div>
      ) : (
        <div className={styles.noGuild}>
          <p>你还没有加入公会</p>
          <button className={styles.createBtn} onClick={() => setShowCreate(!showCreate)}>创建公会 (1万金币)</button>

          {showCreate && (
            <div className={styles.createForm}>
              <input className={styles.input} value={guildName} onChange={(e) => setGuildName(e.target.value)} placeholder="公会名称" />
              <input className={styles.input} value={guildInfo} onChange={(e) => setGuildInfo(e.target.value)} placeholder="公会简介" />
              <button className={styles.submitBtn} onClick={handleCreate}>创建</button>
            </div>
          )}

          <h3>公会列表</h3>
          {guilds.map((g) => (
            <div key={g.id} className={styles.guildCard}>
              <span className={styles.guildName}>{g.name} Lv.{g.level}</span>
              <span className={styles.guildInfo}>{g.memberCount}人 | 荣誉{g.honor}</span>
              <button className={styles.joinBtn} onClick={() => handleJoin(g.id)}>加入</button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
