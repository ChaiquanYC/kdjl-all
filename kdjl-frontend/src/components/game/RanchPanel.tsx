import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './RanchPanel.module.css';

interface PetBrief { id: number; name: string; level: number; wx?: number; czl?: string; img?: string; cardImg?: string; }
interface PetDetail extends PetBrief {
  hp: number; mp: number; ac: number; mc: number; hits: number; miss: number; speed: number;
  nowexp: number; lexp: number; element: string; srchp?: number; srcmp?: number;
}

const ELE = ['？','金','木','水','火','土','神','神圣'];

export default function RanchPanel() {
  const [carriedPets, setCarriedPets] = useState<PetDetail[]>([]);
  const [ranchPets, setRanchPets] = useState<PetBrief[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [showDetail, setShowDetail] = useState<any | null>(null);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const mainPetId = useAuthStore((s) => s.player?.mbid);
  const maxMc = useAuthStore((s) => s.player?.maxMc ?? 10);

  const fetchData = () => {
    Promise.all([
      apiGet<PetDetail[]>('/pets'),
      apiGet<PetBrief[]>('/pets/ranch'),
    ]).then(([pRes, rRes]) => {
      if (pRes.code === 0 && pRes.data) {
        setCarriedPets(pRes.data);
        // Set indicator to main pet on first load
        const idx = pRes.data.findIndex(p => p.id === mainPetId);
        if (idx >= 0) setIndicatorIdx(idx);
      }
      if (rRes.code === 0 && rRes.data) setRanchPets(rRes.data);
    });
  };
  useEffect(() => { fetchData(); }, []);

  // PHP mcbbshow — show pet detail popup on click
  const showPetDetail = (petId: number) => {
    apiGet<any>('/pets/' + petId).then((res) => {
      if (res.code === 0 && res.data) {
        setShowDetail(res.data);
        setTimeout(() => setShowDetail(null), 50000); // auto-close 5s
      }
    });
  };

  const doApi = (url: string, okMsg: string) => {
    apiPost<Record<string, unknown>>(url, {}).then((res: any) => {
      if (res.code === 0) { setMsg(okMsg); fetchData(); triggerRefresh(); }
      else setMsg(res.message);
      setTimeout(() => setMsg(null), 2500);
    });
  };

  const petSlots: (PetDetail | null)[] = [null, null, null];
  carriedPets.forEach((p, i) => { if (i < 3) petSlots[i] = p; });
  const mainPetIdx = petSlots.findIndex(p => p?.id === mainPetId);
  // PHP zhixiang: clicked pet gets ch01 indicator (separate from main pet status)
  const [indicatorIdx, setIndicatorIdx] = useState(0);

  return (
    <div className={styles.task}>
      {msg && <div className={styles.msg}>{msg}</div>}

      {/* PHP mcbbshow — pet detail popup */}
      {showDetail && (
        <div className={styles.detailPopup}>
          <div className={styles.detailClose} onClick={() => setShowDetail(null)}>关闭</div>
          <div className={styles.detailBg}>
            <div className={styles.detailLeft}>
              <img src={`/images/bb/${showDetail.effectImg || showDetail.img || 't1.gif'}`} alt="" />
            </div>
            <div className={styles.detailRight}>
              <div className={styles.detailName}>{showDetail.name}</div>
              <div className={styles.detailStats}>
                五行：{showDetail.element}<br/>
                生命：{showDetail.srchp ?? showDetail.hp}<br/>
                魔法：{showDetail.srcmp ?? showDetail.mp}<br/>
                攻击：{showDetail.ac}<br/>
                防御：{showDetail.mc}<br/>
                命中：{showDetail.hits}<br/>
                闪避：{showDetail.miss}<br/>
                成长：{showDetail.czl ?? '-'}<br/>
                等级：{showDetail.level}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* PHP #Layer1 — close/return button */}
      <div className={styles.layer1} onClick={() => setGameView('city')} />

      {/* PHP div.task_left — decorative */}
      <div className={styles.taskLeft} />

      {/* PHP div.task_right */}
      <div className={styles.taskRight}>
        {/* PHP ul.task_nav — tab bar using muchang_06.jpg sprite */}
        <ul className={styles.tabBar}>
          <li className={styles.tabOn}><a className={styles.tab1} onClick={() => {}} /></li>
          <li><a className={styles.tab2} style={{opacity:0.4,cursor:'default'}} /></li>
        </ul>

        <div className={styles.clear} />

        {/* PHP div.dt_task#con_tab_1 */}
        <div className={styles.tabContent}>
          {/* LEFT COLUMN — PHP div.box01 */}
          <div className={styles.box}>
            {/* Header */}
            <div className={styles.boxHd}>
              <img src="/new_images/ui/muchang_09.jpg" alt="" className={styles.hdImg} />
            </div>
            {/* Pet list table — PHP div.box03 + div.dt_list */}
            <div className={styles.petListBox}>
              <table className={styles.petTable}><tbody>
                <tr className={styles.tableHd}>
                  <td style={{width:130}}>名称</td>
                  <td style={{width:70}}>五行</td>
                  <td style={{width:70}}>等级</td>
                </tr>
              </tbody></table>
              <div className={styles.petScroll}>
                <table className={styles.petTable}><tbody>
                  {ranchPets.length === 0 ? (
                    <tr><td colSpan={3} className={styles.empty}>牧场里面还没有宝贝！</td></tr>
                  ) : ranchPets.map(p => (
                    <tr key={p.id} className={selectedId === p.id ? styles.sel : ''}
                      onClick={() => { setSelectedId(p.id); showPetDetail(p.id); }}>
                      <td className={styles.nameTd}>
                        <img src="/images/ui/muchang/mc05.gif" alt="" />{p.name}
                      </td>
                      <td>{ELE[p.wx ?? 0]}</td>
                      <td>LV {p.level}</td>
                    </tr>
                  ))}
                </tbody></table>
              </div>
            </div>
            {/* Bottom bar — PHP div.box04 */}
            <div className={styles.btmBar}>
              牧场宠物数量：{ranchPets.length}/{maxMc}
              <button className={styles.conbtn} onClick={() => selectedId && doApi('/pets/' + selectedId + '/withdraw', '取出成功')}>携带</button>
              <button className={styles.conbtn} onClick={() => selectedId && doApi('/pets/set-main/' + selectedId, '主战设置成功')}>主战</button>
            </div>
          </div>

          {/* RIGHT COLUMN — PHP div.box01 */}
          <div className={styles.box}>
            {/* Header */}
            <div className={styles.boxHd}>
              <img src="/new_images/ui/muchang_11.jpg" alt="" className={styles.hdImg} />
            </div>
            {/* Pet cards — PHP div.box06 */}
            <div className={styles.petCardBox}>
              <table className={styles.petab}><tbody>
                {/* PHP #mainbb# — 3 td, clicked pet gets zz.gif bg (zhixiang function) */}
                <tr>
                  {petSlots.map((p, i) => (
                    <td key={i} className={i === indicatorIdx ? styles.ch01 : ''}>&nbsp;</td>
                  ))}
                </tr>
                {/* PHP #one/#two/#three — pet cards, main pet full size, others ch02 (70px) */}
                <tr>
                  {petSlots.map((p, i) => (
                    <td key={i}>
                      <div className={styles.pet}>
                        {p ? (
                          <img src={`/images/bb/${p.cardImg || p.img || 't1.gif'}`} alt={p.name}
                            className={i === mainPetIdx ? styles.mainPetImg : styles.ch02}
                            onClick={() => { setSelectedId(p.id); setIndicatorIdx(i); }} />
                        ) : <div style={{width:65,height:70}} />}
                      </div>
                    </td>
                  ))}
                </tr>
              </tbody></table>
            </div>
            {/* PHP p — instructions */}
            <p className={styles.desc}>
              <b>说明：</b><br />
              1）设置主战宠物后，该宠物可获得装备道具、以及获得任务经验、道具使用效果等。<br />
              2）主战宠物无法寄养，请设置其他宠物后再寄养 。
            </p>
            {/* PHP div.plus — discard + lock icon */}
            <div className={styles.plus}>
              <button className={styles.conbtn} onClick={() => {
                if (!selectedId) return;
                if (selectedId === mainPetId) { alert('主战宠物不能寄养！请先设置其他宠物为主战'); return; }
                doApi('/pets/' + selectedId + '/deposit', '寄养成功');
              }}>寄养</button>
              <button className={styles.conbtn} onClick={() => {
                if (!selectedId) return;
                if (selectedId === mainPetId) { alert('主战宠物不能丢弃！请先设置其他宠物为主战'); return; }
                if (!confirm('您一旦丢弃宝宝，就再也找不回来了，并且宝宝穿戴的装备也会一起消失。\n确定要丢弃吗？')) return;
                doApi('/pets/' + selectedId + '/discard', '丢弃成功');
              }}>丢弃</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
