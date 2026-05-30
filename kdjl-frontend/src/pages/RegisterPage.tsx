import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { apiPost, apiGet } from '@/api/client';
import styles from './RegisterPage.module.css';

const HEAD_OPTIONS = [1, 3, 5, 2, 4, 6]; // PHP order: lr1,lr3,lr5,lr2,lr4,lr6

const PET_OPTIONS = [
  { bc: 2, img: '/images/login/zc17.jpg' },
  { bc: 4, img: '/images/login/zc19.jpg' },
  { bc: 1, img: '/images/login/zc21.jpg' },
  { bc: 5, img: '/images/login/zc23.jpg' },
  { bc: 3, img: '/images/login/zc25.jpg' },
];

export default function RegisterPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState('');
  const [headImg, setHeadImg] = useState(5); // PHP default: lr5
  const [showPetModal, setShowPetModal] = useState(false);
  const [regError, setRegError] = useState<string | null>(null);
  const [regLoading, setRegLoading] = useState(false);
  const [nicknameStatus, setNicknameStatus] = useState<string | null>(null);
  const { token, setAuth, fetchPlayer } = useAuthStore();
  const navigate = useNavigate();
  const overlayRef = useRef<HTMLDivElement>(null);
  const modalRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (token) navigate('/', { replace: true });
  }, [token, navigate]);

  // Position modal at center when shown
  useEffect(() => {
    if (showPetModal && overlayRef.current && modalRef.current) {
      const scrollX = window.pageXOffset || document.documentElement.scrollLeft;
      const scrollY = window.pageYOffset || document.documentElement.scrollTop;
      const viewW = document.documentElement.clientWidth;
      const viewH = document.documentElement.clientHeight;
      overlayRef.current.style.width = document.body.scrollWidth + 'px';
      overlayRef.current.style.height = Math.max(document.body.scrollHeight, viewH) + 'px';
      modalRef.current.style.left = (viewW / 2 + scrollX - 267) + 'px';
      modalRef.current.style.top = (viewH / 2 + scrollY - 170) + 'px';
    }
  }, [showPetModal]);

  const getSex = () => headImg % 2 === 1 ? '1' : '2';

  const checkNickname = useCallback(async (name: string) => {
    if (name.length < 4) { setNicknameStatus(null); return; }
    try {
      const res = await apiGet<void>('/auth/check-nickname', { nickname: name });
      setNicknameStatus(res.code === 0 ? 'ok' : (res.message || '不可用'));
    } catch { setNicknameStatus(null); }
  }, []);

  // Step 1: validate inputs and show pet selection popup
  const handleNext = async () => {
    setRegError(null);
    if (!username.trim()) { setRegError('请输入用户名'); return; }
    if (!/^[A-Za-z0-9]+$/.test(username)) { setRegError('用户名仅允许字母和数字'); return; }
    if (/^[0-9]+$/.test(username)) { setRegError('用户名不能全为数字'); return; }
    if (password.length < 4) { setRegError('密码至少4位'); return; }
    const finalNickname = nickname.trim() || username;
    if (finalNickname.length < 4 || finalNickname.length > 21) { setRegError('角色名长度需为4-21字符'); return; }

    // Check nickname availability
    try {
      const res = await apiGet<void>('/auth/check-nickname', { nickname: finalNickname });
      if (res.code !== 0) { setRegError(res.message || '角色名不可用'); return; }
    } catch { setRegError('网络错误'); return; }

    setShowPetModal(true);
  };

  // Step 2: register with selected pet
  const handleRegister = async (bc: number) => {
    setRegLoading(true);
    setRegError(null);
    const finalNickname = nickname.trim() || username;
    const sexText = getSex() === '1' ? '帅哥' : '美女';

    apiPost<{ token: string; uid: number; username: string; nickname: string }>(
      '/auth/register', { username, password, nickname: finalNickname, petChoice: bc, sex: sexText, headImg }
    ).then(async (res: any) => {
      if (res.code === 0 && res.data?.token) {
        setAuth(res.data.token, {
          id: res.data.uid ?? 0,
          username: res.data.username ?? username,
          nickname: res.data.nickname ?? finalNickname,
          money: 0, yb: 0, vip: 0, score: 0, prestige: 0,
          inMap: 0, openMap: '', fightTop: 0, maxBag: 30, sex: sexText,
          headImg, onlineTime: 0, newGuideStep: 0,
        } as any);
        await fetchPlayer();
        navigate('/', { replace: true });
      } else {
        setRegError(res.message || '注册失败');
        setShowPetModal(false);
      }
      setRegLoading(false);
    }).catch(() => { setRegError('网络错误'); setShowPetModal(false); setRegLoading(false); });
  };

  return (
    <>
      <div className={styles.layout}>
        <div className={styles.wrap}>
          {/* Top banner */}
          <img src="/images/login/top.jpg" alt="" className={styles.topBanner} />

          {/* Role selection area */}
          <div className={styles.roleArea}>
            <img src="/images/login/r01.jpg" alt="" className={styles.sideImg} />
            <div className={styles.roleBg}>
              <div className={styles.roleRow}>
                {HEAD_OPTIONS.map((id) => (
                  <div key={id}
                    className={`${styles.roleSlot} ${headImg === id ? styles.roleActive : ''}`}
                    onClick={() => setHeadImg(id)}
                  >
                    <div className={styles.roleSprite} data-role={id} />
                  </div>
                ))}
              </div>
            </div>
            <img src="/images/login/r02.jpg" alt="" className={styles.sideImg} />
          </div>

          {/* Bottom input bar */}
          <div className={styles.bottomBar}>
            <img src="/images/login/r03.jpg" alt="" className={styles.sideImg} />
            <div className={styles.inputBar}>
              {regError && <div className={styles.errorTip}>{regError}</div>}
              <div className={styles.inputRow}>
                <span>用户名：</span>
                <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} className={styles.input} />
                <span>密码：</span>
                <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className={styles.input} />
                <span>角色名：</span>
                <input type="text" value={nickname} onChange={(e) => { setNickname(e.target.value); checkNickname(e.target.value); }} className={styles.inputWide} placeholder={username || '4-21字符'} />
                {nicknameStatus && nickname.length >= 4 && (
                  <span className={nicknameStatus === 'ok' ? styles.statusOk : styles.statusFail}>
                    {nicknameStatus === 'ok' ? 'OK' : nicknameStatus}
                  </span>
                )}
                <button type="button" className={styles.okBtn} onClick={handleNext} disabled={regLoading} />
              </div>
            </div>
            <img src="/images/login/r04.jpg" alt="" className={styles.sideImg} />
          </div>
        </div>
      </div>

      {/* Pet selection modal */}
      {showPetModal && (
        <>
          <div className={styles.overlay} ref={overlayRef} onClick={() => !regLoading && setShowPetModal(false)} />
          <div className={styles.petModal} ref={modalRef}>
            {/* Top */}
            <table className={styles.petTable}><tbody><tr>
              <td><img src="/images/login/zc06.gif" width="112" height="47" /></td>
              <td className={styles.petTopMid} />
              <td><img src="/images/login/zc08.gif" width="66" height="47" style={{ cursor: 'pointer' }} onClick={() => !regLoading && setShowPetModal(false)} /></td>
            </tr></tbody></table>
            {/* Pets */}
            <table className={styles.petTable}><tbody><tr>
              <td><img src="/images/login/zc16.jpg" width="15" height="224" /></td>
              {PET_OPTIONS.map((p) => (
                <td key={p.bc}>
                  <img src={p.img} width="92" height="224" style={{ cursor: regLoading ? 'default' : 'pointer' }}
                    onClick={() => !regLoading && handleRegister(p.bc)} />
                </td>
              ))}
              <td><img src="/images/login/zc26.jpg" width="14" height="224" /></td>
            </tr></tbody></table>
            {/* Bottom */}
            <table className={styles.petTable}><tbody><tr>
              <td><img src="/images/login/zc09.gif" width="15" height="26" /></td>
              <td className={styles.petBotMid} />
              <td><img src="/images/login/zc10.gif" width="15" height="26" /></td>
            </tr></tbody></table>
          </div>
        </>
      )}
    </>
  );
}
