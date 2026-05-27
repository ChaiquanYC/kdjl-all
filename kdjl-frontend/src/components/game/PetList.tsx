import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './PetList.module.css';

interface PetData {
  id: number; name: string; level: number;
  hp: number; mp: number; ac: number; mc: number;
  speed: number; hits: number; miss: number;
  element: string; nowexp: number; lexp: number;
  skillList: string; czl: string;
  img?: string; cardImg?: string; zb?: string;
  srchp?: number; srcmp?: number; addhp?: number; addmp?: number;
  wx?: number; kx?: string;
  subyl?: number; subsl?: number; subxl?: number;
  subdl?: number; subfl?: number; subhl?: number; subkl?: number;
  remaketimes?: number; remakelevel?: string;
}

interface BagItem {
  id: number; name?: string; img?: string; propId: number;
  effect?: string; requires?: string; sell?: number; buy?: number;
  usages?: string; expire?: string; propsColor?: number;
}

interface SkillInfo {
  id: number; name: string; level: number; element: string;
}

interface LearnableSkill {
  id: number; name: string; element: string; requires: string; wx: number; pid: number;
}

const ELE_RESIST = ['金抗','木抗','水抗','火抗','土抗'];
const SLOT_LABELS = ['翅膀','头部','身体','脚部','武器','项链','戒指','翅膀','手镯','宝石','道具','特殊'];

export default function PetList() {
  const [pets, setPets] = useState<PetData[]>([]);
  const [bagItems, setBagItems] = useState<BagItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState(1);
  const [msg, setMsg] = useState<string | null>(null);
  const [etip, setEtip] = useState<{ itemId: number; x: number; y: number } | null>(null);
  const [skills, setSkills] = useState<SkillInfo[]>([]);
  const [learnable, setLearnable] = useState<LearnableSkill[]>([]);
  const [ranchCount, setRanchCount] = useState<number>(0);
  const player = useAuthStore((s) => s.player);
  const fetchPlayer = useAuthStore((s) => s.fetchPlayer);
  const mainPetId = player?.mbid;
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const refreshTrigger = useGameStore((s) => s.refreshTrigger);
  const storeSelectedId = useGameStore((s) => s.selectedPetId);
  const setStoreSelectedId = useGameStore((s) => s.setSelectedPetId);

  const [localSelectedId, setLocalSelectedId] = useState<number>(mainPetId ?? 0);
  const selectedId = storeSelectedId ?? localSelectedId;
  const selectedPet = pets.find(p => p.id === selectedId) ?? pets[0];
  const setSelectedId = (id: number) => { setLocalSelectedId(id); setStoreSelectedId(id); };

  useEffect(() => {
    Promise.all([
      apiGet<PetData[]>('/pets'),
      apiGet<BagItem[]>('/bag'),
      apiGet<any[]>('/pets/ranch'),
    ]).then(([petRes, bagRes, ranchRes]) => {
      if (ranchRes.code === 0 && ranchRes.data) setRanchCount(ranchRes.data.length);
      if (petRes.code === 0 && petRes.data) {
        setPets(petRes.data);
        if (!selectedId || selectedId === 0 || !petRes.data.find(p => p.id === selectedId)) {
          const main = petRes.data.find(p => p.id === mainPetId) ?? petRes.data[0];
          setSelectedId(main?.id ?? 0);
        }
      }
      if (bagRes.code === 0 && bagRes.data) setBagItems(bagRes.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, [mainPetId, refreshTrigger]);

  useEffect(() => {
    if (selectedPet?.id) {
      Promise.all([
        apiGet<SkillInfo[]>('/pets/' + selectedPet.id + '/skills'),
        apiGet<LearnableSkill[]>('/pets/' + selectedPet.id + '/skills/learnable'),
      ]).then(([sRes, lRes]) => {
        if (sRes.code === 0 && sRes.data) setSkills(sRes.data); else setSkills([]);
        if (lRes.code === 0 && lRes.data) setLearnable(lRes.data); else setLearnable([]);
      }).catch(() => {});
    }
  }, [selectedPet?.id]);

  const handleSetMain = (petId: number) => {
    apiPost('/pets/set-main/' + petId, {}).then(() => {
      fetchPlayer().then(() => {
        setMsg('已设为主战宠物'); triggerRefresh();
        setSelectedId(petId);
        setTimeout(() => setMsg(null), 2000);
      });
    });
  };

  const handleLearn = (skillSysId: number) => {
    if (!selectedPet) return;
    apiPost('/pets/' + selectedPet.id + '/skills/learn/' + skillSysId, {}).then((res: any) => {
      if (res.code === 0 && res.data) {
        setMsg('学会了 ' + (res.data.learned ?? ''));
        apiGet<SkillInfo[]>('/pets/' + selectedPet.id + '/skills').then(r => {
          if (r.code === 0 && r.data) setSkills(r.data);
        });
      } else setMsg(res.message ?? '学习失败');
      setTimeout(() => setMsg(null), 2500);
    });
  };

  // Parse zb string "pos:bagId,pos:bagId" → Map<pos, bagId>
  const parseZb = (zb?: string) => {
    const map = new Map<number, number>();
    if (!zb) return map;
    zb.split(',').forEach(pair => {
      const [pos, bagId] = pair.split(':').map(Number);
      if (!isNaN(pos) && !isNaN(bagId)) map.set(pos, bagId);
    });
    return map;
  };
  const zbMap = parseZb(selectedPet?.zb);
  const getEquipItem = (pos: number) => {
    const bagId = zbMap.get(pos);
    if (!bagId) return null;
    return bagItems.find(b => b.id === bagId) ?? null;
  };
  const getEquipImg = (pos: number) => {
    const item = getEquipItem(pos);
    return item?.img ? `/images/props/${item.img}` : null;
  };
  const handleUnequip = (pos: number) => {
    const item = getEquipItem(pos);
    if (!item) return;
    if (!confirm(`确定取下 ${item.name} 吗？`)) return;
    apiPost('/bag/unequip/' + item.id, {}).then((res: any) => {
      if (res.code === 0) {
        setMsg('装备取下成功');
        Promise.all([apiGet<PetData[]>('/pets'), apiGet<BagItem[]>('/bag')]).then(([p, b]) => {
          if (p.code === 0 && p.data) setPets(p.data);
          if (b.code === 0 && b.data) setBagItems(b.data);
        });
      } else setMsg(res.message ?? '取下失败');
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const parseKx = (kx?: string) => {
    if (!kx) return [0,0,0,0,0];
    return kx.split(',').map(Number);
  };
  const kxArr = parseKx(selectedPet?.kx);

  if (loading) return <div className={styles.loading}>加载中...</div>;
  if (pets.length === 0) return <div className={styles.container}><div className={styles.empty}><p>暂无宠物，去野外捕捉吧</p></div></div>;
  if (!selectedPet) return <div className={styles.loading}>加载中...</div>;

  const petCards = pets.slice(0, 3);

  return (
    <div className={styles.container}>
      {msg && <div className={styles.toast}>{msg}</div>}
      {etip && (
        <div className={styles.equipTip} style={{ left: etip.x + 12, top: etip.y - 40 }}>
          {(() => {
            const item = bagItems.find(b => b.id === etip.itemId);
            if (!item) return <span>装备 #{etip.itemId}</span>;
            return (
              <>
                {item.img && <img src={`/images/props/${item.img}`} className={styles.tipImg} alt="" />}
                <div className={styles.tipName}>{item.name}</div>
                {item.effect && <div className={styles.tipRow}>效果：{item.effect}</div>}
                {item.requires && <div className={styles.tipRow}>需要：{item.requires}</div>}
                {item.usages && <div className={styles.tipRow}>{item.usages}</div>}
                {item.expire && <div className={styles.tipRow}>{item.expire}</div>}
                {item.sell != null && <div className={styles.tipRow}>售价：{item.sell}金</div>}
              </>
            );
          })()}
        </div>
      )}

      {/* Left panel */}
      <div className={styles.left}>
        <div className={styles.userInfo}>
          <div>{player?.nickname ?? '冒险者'}<br />宝贝：<span style={{color:'green'}}>{selectedPet?.name}</span></div>
          <div>宝贝：{pets.length + (ranchCount ?? 0)}只</div>
          <div>VIP：{player?.vip ?? 0}</div>
          <div>性别：{player?.sex === '1' ? '男' : '女'}</div>
          <div>金币：{player?.money ?? 0}</div>
          <div>元宝：{player?.yb ?? 0}</div>
        </div>
        <div className={styles.avatar}>
          <img src={`/images/head/2${player?.headImg || 1}.gif`} alt="" />
        </div>
        <div className={styles.petCards}>
          {petCards.map((p) => (
            <div key={p.id} className={`${styles.petCard} ${p.id === selectedId ? styles.petCardActive : ''}`}
              onClick={() => setSelectedId(p.id)}
              onDoubleClick={() => { if (p.id !== mainPetId) handleSetMain(p.id); }}>
              {p.cardImg ? <img src={`/images/bb/${p.cardImg}`} alt="" /> : <div className={styles.noCard}>{p.name}</div>}
              <span>{p.name}<br />Lv.{p.level}</span>
            </div>
          ))}
          {petCards.length < 3 && <div className={styles.petCard}><div className={styles.noCard}>--</div></div>}
        </div>
      </div>

      {/* Right panel */}
      <div className={styles.right}>
        <ul className={styles.tabs}>
          <li className={tab === 1 ? styles.tabOn : ''} onClick={() => setTab(1)}><span className={styles.tab1} /></li>
          <li className={tab === 2 ? styles.tabOn : ''} onClick={() => setTab(2)}><span className={styles.tab2} /></li>
          <li className={tab === 3 ? styles.tabOn : ''} onClick={() => setTab(3)}><span className={styles.tab3} /></li>
        </ul>

        {/* Tab 1: Equipment */}
        {tab === 1 && (
          <div className={styles.tabEq}>
            <div className={styles.equipPanel}>
              <div className={styles.equipSlots}>
                {SLOT_LABELS.map((label, i) => {
                  const img = getEquipImg(i);
                  const equipItem = getEquipItem(i);
                  return (
                    <div key={i} className={`${styles.slot} ${styles['s' + i]}`}
                      onMouseEnter={(e) => equipItem && setEtip({ itemId: equipItem.id, x: e.clientX, y: e.clientY })}
                      onMouseMove={(e) => setEtip(prev => prev ? { ...prev, x: e.clientX, y: e.clientY } : null)}
                      onMouseLeave={() => setEtip(null)}
                      onDoubleClick={() => equipItem && handleUnequip(i)}>
                      {img ? <img src={img} alt={equipItem?.name ?? label} className={styles.slotImg} onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                        : <span>{label}</span>}
                    </div>
                  );
                })}
              </div>
              <div className={styles.petBig}>
                {selectedPet.img ? <img src={`/images/bb/${selectedPet.img}`} alt="" /> : <div className={styles.noImg}>{selectedPet.element}</div>}
              </div>
            </div>
            <div className={styles.statCol}>
              <div>等级：{selectedPet.level}</div>
              <div>五行：{selectedPet.element}</div>
              <div>生命：{selectedPet.hp}/{selectedPet.srchp ?? '?'}</div>
              <div>魔法：{selectedPet.mp}/{selectedPet.srcmp ?? '?'}</div>
              <div>攻击：{selectedPet.ac}</div>
              <div>防御：{selectedPet.mc}</div>
              <div>命中：{selectedPet.hits}</div>
              <div>闪避：{selectedPet.miss}</div>
              <div>速度：{selectedPet.speed}</div>
              <div>成长：{selectedPet.czl ?? '-'}</div>
            </div>
          </div>
        )}

        {/* Tab 2: Attributes */}
        {tab === 2 && (
          <div className={styles.tabAttr}>
            <div className={styles.attrCol}>
              <div className={styles.petBig2}>
                {selectedPet.img ? <img src={`/images/bb/${selectedPet.img}`} alt="" /> : <div className={styles.noImg}>{selectedPet.element}</div>}
              </div>
              <ul className={styles.attrList}>
                <li>等级：{selectedPet.level}<br />当前经验：{selectedPet.nowexp ?? 0}<br />升级经验：{selectedPet.lexp ?? 0}<br />五行：{selectedPet.element}</li>
                <li>生命：{selectedPet.hp}/{selectedPet.srchp ?? '?'}<br />魔法：{selectedPet.mp}/{selectedPet.srcmp ?? '?'}<br />攻击：{selectedPet.ac}<br />防御：{selectedPet.mc}</li>
                <li>命中：{selectedPet.hits}<br />闪避：{selectedPet.miss}<br />速度：{selectedPet.speed}<br />成长：{selectedPet.czl ?? '-'}</li>
              </ul>
            </div>
            <div className={styles.attrCol}>
              {ELE_RESIST.map((label, i) => (
                <div key={label}>{label}：{kxArr[i] ?? 0}</div>
              ))}
              <div>减晕率：{selectedPet.subyl ?? 0}%</div>
              <div>减睡率：{selectedPet.subsl ?? 0}%</div>
              <div>减毒率：{selectedPet.subdl ?? 0}%</div>
              <div>减虚率：{selectedPet.subxl ?? 0}%</div>
              <div>减缓率：{selectedPet.subhl ?? 0}%</div>
              <div>减防率：{selectedPet.subfl ?? 0}%</div>
              <div>减抗率：{selectedPet.subkl ?? 0}%</div>
              <div>进化次数：{selectedPet.remaketimes ?? 0}</div>
            </div>
          </div>
        )}

        {/* Tab 3: Skills */}
        {tab === 3 && (
          <div className={styles.tabSkill}>
            <div className={styles.skillLeft}>
              <div className={styles.petBig2}>
                {selectedPet.img ? <img src={`/images/bb/${selectedPet.img}`} alt="" /> : <div className={styles.noImg}>{selectedPet.element}</div>}
              </div>
              {learnable.length > 0 && (
                <div className={styles.learnRow}>
                  <select id="skillSel">
                    {learnable.map(s => <option key={s.id} value={s.id}>{s.name} (需#{s.pid})</option>)}
                  </select>
                  <button onClick={() => {
                    const sel = (document.getElementById('skillSel') as HTMLSelectElement)?.value;
                    if (sel) handleLearn(Number(sel));
                  }}>学习</button>
                </div>
              )}
            </div>
            <div className={styles.skillRight}>
              <h4>已学技能</h4>
              {skills.length === 0 ? (
                <div className={styles.noSkills}>宝宝还没有学习技能！</div>
              ) : (
                <ul className={styles.skillList}>
                  {skills.map(s => (
                    <li key={s.id}>
                      {s.name} Lv.{s.level} <span className={styles.elTag}>{s.element}</span>
                      {s.level < 10 && (
                        <button className={styles.upgradeBtn} onClick={(e) => {
                          e.stopPropagation();
                          apiPost('/pets/' + selectedPet.id + '/skills/upgrade/' + s.id, {}).then((res: any) => {
                            if (res.code === 0) {
                              setMsg(res.data?.upgraded + ' 升级到 Lv.' + res.data?.newLevel);
                              apiGet<SkillInfo[]>('/pets/' + selectedPet.id + '/skills').then(r => {
                                if (r.code === 0 && r.data) setSkills(r.data);
                              });
                            } else setMsg(res.message);
                            setTimeout(() => setMsg(null), 2000);
                          });
                        }}>升级</button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
