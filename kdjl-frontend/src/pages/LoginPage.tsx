import { useState, FormEvent, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { apiPost } from '@/api/client';
import styles from './LoginPage.module.css';

const STARTER_PETS = [
  { id: 1, name: '金波姆', wx: '金' }, { id: 2, name: '木波姆', wx: '木' },
  { id: 3, name: '水波姆', wx: '水' }, { id: 4, name: '火波姆', wx: '火' },
  { id: 5, name: '土波姆', wx: '土' }, { id: 6, name: '暗波姆', wx: '暗' },
];

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [petChoice, setPetChoice] = useState(1);
  const [isRegister, setIsRegister] = useState(false);
  const [regError, setRegError] = useState<string | null>(null);
  const [regLoading, setRegLoading] = useState(false);
  const { login, loading, error, token, setAuth } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (token) navigate('/', { replace: true });
  }, [token, navigate]);

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    await login(username, password);
  };

  const handleRegister = async (e: FormEvent) => {
    e.preventDefault();
    setRegLoading(true); setRegError(null);
    apiPost<{ token: string; uid: number; username: string; nickname: string; petName?: string; petId?: number }>('/auth/register', { username, password, nickname: nickname || username, petChoice }).then((res: any) => {
      if (res.code === 0 && res.data?.token) {
        setAuth(res.data.token, {
          id: res.data.uid ?? 0,
          username: res.data.username ?? username,
          nickname: res.data.nickname ?? (nickname || username),
          money: 0, yb: 0, vip: 0, score: 0, prestige: 0,
          inMap: 0, openMap: '', fightTop: 0, maxBag: 30, sex: '',
          onlineTime: 0, newGuideStep: 0,
        } as any);
        navigate('/', { replace: true });
      } else {
        setRegError(res.message || '注册失败');
      }
      setRegLoading(false);
    }).catch(() => { setRegError('网络错误'); setRegLoading(false); });
  };

  return (
    <div className={styles.container}>
      {isRegister ? (
        <form className={styles.form} onSubmit={handleRegister}>
          <h1 className={styles.title}>创建角色</h1>
          {regError && <div className={styles.error}>{regError}</div>}
          <input type="text" placeholder="登录用户名" value={username} onChange={(e) => setUsername(e.target.value)} className={styles.input} required />
          <input type="password" placeholder="密码(至少4位)" value={password} onChange={(e) => setPassword(e.target.value)} className={styles.input} required />
          <input type="text" placeholder="游戏昵称" value={nickname} onChange={(e) => setNickname(e.target.value)} className={styles.input} />
          <div className={styles.petGrid}>
            {STARTER_PETS.map((p) => (
              <div key={p.id} className={`${styles.petOption} ${petChoice === p.id ? styles.petSelected : ''}`}
                onClick={() => setPetChoice(p.id)}>
                <span>{p.name}</span><small>{p.wx}</small>
              </div>
            ))}
          </div>
          <button type="submit" disabled={regLoading} className={styles.submitBtn}>
            {regLoading ? '创建中...' : '创建角色'}
          </button>
          <button type="button" className={styles.switchBtn} onClick={() => setIsRegister(false)}>已有账号？去登录</button>
        </form>
      ) : (
        <form className={styles.form} onSubmit={handleLogin}>
          <h1 className={styles.title}>口袋精灵</h1>
          <p className={styles.subtitle}>KDJL — 在线宠物养成游戏</p>
          {error && <div className={styles.error}>{error}</div>}
          <input type="text" placeholder="用户名" value={username} onChange={(e) => setUsername(e.target.value)} className={styles.input} required />
          <input type="password" placeholder="密码" value={password} onChange={(e) => setPassword(e.target.value)} className={styles.input} required />
          <button type="submit" disabled={loading} className={styles.submitBtn}>{loading ? '登录中...' : '登录'}</button>
          <button type="button" className={styles.switchBtn} onClick={() => setIsRegister(true)}>没有账号？注册新角色</button>
        </form>
      )}
    </div>
  );
}
