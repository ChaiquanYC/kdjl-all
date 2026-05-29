import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import { systips } from '@/stores/systipsStore';
import { parseEffects } from '@/utils/equipEffect';
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
  effect?: string; effectDesc?: string; requires?: string; sell?: number; buy?: number;
  usages?: string; expire?: string; propsColor?: number;
  // Equipment fields (matching BagPanel)
  varyname?: number; postion?: number;
  series?: string; serieseffect?: string; serieseffectDesc?: string;
  seriesPieces?: { id: number; name: string }[];
  seriesTotalPieces?: number;
  seriesBonusConfig?: Record<number, Record<number, number>>;
  pluseffect?: string; pluseffectDesc?: string;
  plusflag?: number; pluspid?: number; plusget?: string; plusnum?: number;
  plusTimesEffect?: string;
  holeInfo?: string; holeInfoDesc?: string;
  prestige?: number; yb?: number;
}

interface SkillInfo {
  id: number; name: string; level: number; element: string;
}

interface LearnableSkill {
  id: number; name: string; element: string; requires: string; wx: number; pid: number;
}

const ELE_RESIST = ['金抗','木抗','水抗','火抗','土抗'];
const SLOT_NAMES = ['武器','衣服','头盔','鞋子','项链','戒指左','戒指右','护腕','腰带','特殊','翅膀'];
const PROPS_COLORS: Record<number, string> = {
  1: '#FEFDFA', 2: '#0067CB', 3: '#9833DC', 4: '#14FD10', 5: '#FED625', 6: '#ED9037',
};
// Maps CSS visual position (s0-s11) → DB postion value (matching PHP tpl_bb.html layout)
const SLOT_MAP = [
  { dbPos: 0,  label: '' },       // s0  (200,5)   = PHP .i11 = zbsx装备属性汇总
  { dbPos: 2,  label: '身体' },   // s1  (4,5)     = PHP .i1
  { dbPos: 1,  label: '头部' },   // s2  (144,5)   = PHP .i2
  { dbPos: 5,  label: '项链' },   // s3  (282,5)   = PHP .i3
  { dbPos: 4,  label: '武器' },   // s4  (4,69)    = PHP .i4
  { dbPos: 8,  label: '手镯' },   // s5  (282,69)  = PHP .i5
  { dbPos: 3,  label: '脚部' },   // s6  (4,132)   = PHP .i6
  { dbPos: 6,  label: '戒指' },   // s7  (282,132) = PHP .i7
  { dbPos: 7,  label: '翅膀' },   // s8  (4,196)   = PHP .i8
  { dbPos: 9,  label: '宝石' },   // s9  (144,196) = PHP .i9
  { dbPos: 10, label: '道具' },   // s10 (282,196) = PHP .i10
  { dbPos: 11, label: '' },       // s11 (200,5)   = PHP .i12 = extra
];

export default function PetList() {
  const [pets, setPets] = useState<PetData[]>([]);
  const [bagItems, setBagItems] = useState<BagItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState(1);
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
        systips('已设为主战宠物'); triggerRefresh();
        setSelectedId(petId);
      });
    });
  };

  const handleLearn = (skillSysId: number) => {
    if (!selectedPet) return;
    apiPost('/pets/' + selectedPet.id + '/skills/learn/' + skillSysId, {}).then((res: any) => {
      if (res.code === 0 && res.data) {
        systips('学会了 ' + (res.data.learned ?? ''));
        apiGet<SkillInfo[]>('/pets/' + selectedPet.id + '/skills').then(r => {
          if (r.code === 0 && r.data) setSkills(r.data);
        });
      } else systips(res.message ?? '学习失败');
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
        systips('装备取下成功');
        Promise.all([apiGet<PetData[]>('/pets'), apiGet<BagItem[]>('/bag')]).then(([p, b]) => {
          if (p.code === 0 && p.data) setPets(p.data);
          if (b.code === 0 && b.data) setBagItems(b.data);
        });
      } else systips(res.message ?? '取下失败');
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
      {etip && (
        <div className={styles.equipTip} style={{ left: etip.x + 12, top: Math.max(0, etip.y - 160) }}>
          {(() => {
            const item = bagItems.find(b => b.id === etip.itemId);
            if (!item) return <span>装备 #{etip.itemId}</span>;
            const vn = item.varyname;
            const slotName = item.postion != null ? SLOT_NAMES[item.postion] ?? '未知' : null;
            const requiresText = (item as any).requiresDesc || item.requires || '';
            const requiresLines: string[] = requiresText ? requiresText.split('，') : [];
            const holeLines: string[] = item.holeInfoDesc ? item.holeInfoDesc.split('\n') : [];
            // Get currently equipped propIds for this pet
            const equippedPropIds = new Set<number>();
            zbMap.forEach((bagId) => {
              const bagItem = bagItems.find(b => b.id === bagId);
              if (bagItem?.propId) equippedPropIds.add(bagItem.propId);
            });
            return (
              <>
                {item.img && <img src={`/images/props/${item.img}`} className={styles.tipImg} alt="" />}
                <div className={styles.tipName} style={{ color: PROPS_COLORS[item.propsColor ?? 1] }}>{item.name}</div>
                {vn === 9 && slotName && (
                  <div className={styles.tipRow}>位置：{slotName}装备{item.plusflag === 1 ? '(可强化)' : '(不可强化)'}</div>
                )}
                {(item.effectDesc || item.effect) && <div className={styles.tipRow}>效果：{item.effectDesc || item.effect}</div>}
                {requiresText && requiresText !== '0' && (
                  <>
                    <div className={styles.tipRow}>需求：</div>
                    {requiresLines.map((line, i) => <div key={i} className={styles.tipRow} style={{ paddingLeft: 12 }}>{line}</div>)}
                  </>
                )}
                {item.plusTimesEffect && item.plusTimesEffect !== '0' && item.plusget && <div className={styles.tipRow}>强化：{item.plusget}</div>}
                {item.plusnum != null && item.plusnum > 0 && <div className={styles.tipRow}>镶嵌孔：{item.plusnum}</div>}
                {holeLines.length > 0 && (
                  <>
                    <div className={styles.tipRow}>已镶嵌：</div>
                    {holeLines.map((line, i) => <div key={i} className={styles.tipRow} style={{ paddingLeft: 12, color: '#14FD10' }}>{line}</div>)}
                  </>
                )}
                {(item.pluseffectDesc || item.pluseffect) && <div className={styles.tipRow} style={{ color: '#9833DC' }}>附加：{item.pluseffectDesc || item.pluseffect}</div>}
                {item.series && (
                  <div className={styles.tipRow}>
                    <span style={{ color: '#FED625' }}>套装：{item.series}{item.serieseffectDesc ? '（' + item.serieseffectDesc + '）' : ''}</span>
                    {item.seriesPieces && item.seriesPieces.length > 0 && (
                      <div style={{ paddingLeft: 12, marginTop: 2 }}>
                        {item.seriesPieces.map((piece, i) => (
                          <div key={i} style={{ color: equippedPropIds.has(piece.id) ? '#FED625' : '#888' }}>
                            {equippedPropIds.has(piece.id) ? '✓' : '○'} {piece.name}
                          </div>
                        ))}
                      </div>
                    )}
                    {item.seriesBonusConfig && (
                      <div style={{ paddingLeft: 12, marginTop: 4, fontSize: 11, color: '#aaa' }}>
                        阶段加成：
                        {Object.entries(item.seriesBonusConfig).map(([effectIdx, thresholds]) => (
                          <div key={effectIdx}>
                            {Object.entries(thresholds as Record<number, number>)
                              .sort(([a], [b]) => Number(a) - Number(b))
                              .map(([pieces, mult]) => `${pieces}件×${mult}`)
                              .join(' → ')}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
                {item.usages && <div className={styles.tipRow}>{item.usages}</div>}
                {item.prestige != null && item.prestige > 0 && <div className={styles.tipRow}>威望：{item.prestige}</div>}
                {item.expire && <div className={styles.tipRow}>{item.expire}</div>}
                <div className={styles.tipRow}>售价：{item.sell ?? 0}金{item.buy ? ' / 买价：' + item.buy + '金' : ''}{item.yb ? ' / ' + item.yb + '元宝' : ''}</div>
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
            <div key={p.id} className={`${styles.petCard} ${p.id === mainPetId ? styles.petCardActive : ''}`}
              onClick={() => { setSelectedId(p.id); if (p.id !== mainPetId) handleSetMain(p.id); }}>
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
                {SLOT_MAP.map((slot, i) => {
                  // pos 0 and 11 are zbsx/extra placeholders, not real equipment slots
                  if (slot.dbPos === 0 || slot.dbPos === 11) return <div key={i} className={`${styles.slot} ${styles['s' + i]}`} />;
                  const img = getEquipImg(slot.dbPos);
                  const equipItem = getEquipItem(slot.dbPos);
                  return (
                    <div key={i} className={`${styles.slot} ${styles['s' + i]}`}
                      onMouseEnter={(e) => equipItem && setEtip({ itemId: equipItem.id, x: e.clientX, y: e.clientY })}
                      onMouseMove={(e) => setEtip(prev => prev ? { ...prev, x: e.clientX, y: e.clientY } : null)}
                      onMouseLeave={() => setEtip(null)}
                      onDoubleClick={() => equipItem && handleUnequip(slot.dbPos)}>
                      {img ? <img src={img} alt={equipItem?.name ?? slot.label} className={styles.slotImg} onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
                        : <span>{slot.label}</span>}
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
              {(() => {
                const allEffects: Record<string, number> = {};
                zbMap.forEach((bagId) => {
                  const item = bagItems.find(b => b.id === bagId);
                  if (item?.effect) {
                    item.effect.split(',').forEach((eff) => {
                      const [k, v] = eff.split(':');
                      if (k && v) allEffects[k] = (allEffects[k] || 0) + (Number(v) || 0);
                    });
                  }
                });
                const formatted = parseEffects(Object.entries(allEffects).map(([k, v]) => `${k}:${v}`).join(','));
                if (formatted.length === 0) return null;
                return (
                  <div className={styles.equipBonus}>
                    <div className={styles.equipBonusTitle}>装备加成</div>
                    {formatted.map((e, i) => (
                      <div key={i} className={styles.equipBonusItem}>{e.value > 0 ? '+' : ''}{e.value}{e.isPercent ? '%' : ''}{e.label}</div>
                    ))}
                  </div>
                );
              })()}
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
                              systips(res.data?.upgraded + ' 升级到 Lv.' + res.data?.newLevel);
                              apiGet<SkillInfo[]>('/pets/' + selectedPet.id + '/skills').then(r => {
                                if (r.code === 0 && r.data) setSkills(r.data);
                              });
                            } else systips(res.message);
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
