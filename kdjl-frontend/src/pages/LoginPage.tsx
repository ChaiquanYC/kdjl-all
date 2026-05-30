import { useState, FormEvent, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import styles from './LoginPage.module.css';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { login, loading, error, token } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (token) navigate('/', { replace: true });
  }, [token, navigate]);

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();
    await login(username, password);
  };

  return (
    <div className={styles.container}>
      <form className={styles.form} onSubmit={handleLogin}>
        <h1 className={styles.title}>口袋精灵</h1>
        <p className={styles.subtitle}>KDJL — 在线宠物养成游戏</p>
        {error && <div className={styles.error}>{error}</div>}
        <input type="text" placeholder="用户名" value={username} onChange={(e) => setUsername(e.target.value)} className={styles.input} required />
        <input type="password" placeholder="密码" value={password} onChange={(e) => setPassword(e.target.value)} className={styles.input} required />
        <button type="submit" disabled={loading} className={styles.submitBtn}>{loading ? '登录中...' : '登录'}</button>
        <Link to="/register" className={styles.switchBtn}>没有账号？注册新角色</Link>
      </form>
    </div>
  );
}
