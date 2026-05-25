import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import styles from './FriendPanel.module.css';

interface FriendInfo { id: number; nickname: string; level: number; online: boolean; }

export default function FriendPanel() {
  const setGameView = useGameStore((s) => s.setGameView);
  const [friends, setFriends] = useState<FriendInfo[]>([]);
  const [addId, setAddId] = useState('');
  const [msg, setMsg] = useState<string | null>(null);

  const fetchFriends = () => {
    apiGet<FriendInfo[]>('/friend/list').then((res) => {
      if (res.code === 0 && res.data) setFriends(res.data);
    }).catch(() => {});
  };

  useEffect(() => { fetchFriends(); }, []);

  const handleAdd = () => {
    const id = parseInt(addId);
    if (!id) { setMsg('请输入有效的玩家ID'); return; }
    apiPost<Record<string,unknown>>('/friend/add/' + id, {}).then((res) => {
      if (res.code === 0) { fetchFriends(); setAddId(''); setMsg('添加成功！'); }
      else setMsg(res.message || '添加失败');
    }).catch(() => setMsg('网络错误'));
  };

  const handleRemove = (id: number, name: string) => {
    if (!confirm('确定删除好友 ' + name + ' 吗？')) return;
    apiPost<Record<string,unknown>>('/friend/remove/' + id, {}).then((res) => {
      if (res.code === 0) { fetchFriends(); setMsg('已删除'); }
      else setMsg(res.message || '删除失败');
    }).catch(() => setMsg('网络错误'));
  };

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <span className={styles.title}>好友列表 ({friends.length}/50)</span>
        <button className={styles.backBtn} onClick={() => setGameView(null)}>返回</button>
      </div>
      <div className={styles.addRow}>
        <input className={styles.addInput} placeholder="输入玩家ID" value={addId}
          onChange={(e) => setAddId(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleAdd()} />
        <button className={styles.addBtn} onClick={handleAdd}>添加好友</button>
      </div>
      {msg && <div className={styles.msg} onClick={() => setMsg(null)}>{msg}</div>}
      <div className={styles.list}>
        {friends.length === 0 ? (
          <div className={styles.empty}>暂无好友</div>
        ) : (
          friends.map(f => (
            <div key={f.id} className={styles.friendRow}>
              <span className={`${styles.status} ${f.online ? styles.online : styles.offline}`}>
                {f.online ? '●' : '○'}
              </span>
              <span className={styles.name}>{f.nickname}</span>
              <span className={styles.level}>Lv.{f.level}</span>
              <span className={styles.state}>{f.online ? '在线' : '离线'}</span>
              <button className={styles.removeBtn} onClick={() => handleRemove(f.id, f.nickname)}>删除</button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
