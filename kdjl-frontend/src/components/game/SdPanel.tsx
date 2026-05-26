import { useEffect, useState, useCallback } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import styles from './SdPanel.module.css';

interface PetInfo {
  id: number; name: string; level: number; hp: number; maxHp: number;
  mp: number; maxMp: number; exp: number; maxExp: number;
  atk: number; def: number; speed: number; czl: number;
  wx: number; img?: string; cardImg?: string;
  remakelevel?: string; remakeid?: string; remakepid?: string; remaketimes?: number;
  remakeName?: string; remakePName?: string;
}

interface BagItem {
  id: number; propId: number; name?: string; sums: number;
  effect?: string; vary?: number; varyname?: number; usages?: string;
}

interface RebirthTarget {
  zsId: number; petId: number; name: string; img: string; cardImg?: string;
}

const NYI = () => { alert('开发中'); };

export default function SdPanel() {
  const setGameView = useGameStore((s) => s.setGameView);
  const player = useAuthStore((s) => s.player);
  const [tab, setTab] = useState(1);
  const [pets, setPets] = useState<PetInfo[]>([]);
  const [selPet, setSelPet] = useState<PetInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [msg, setMsg] = useState('');
  const [working, setWorking] = useState(false);

  // Tab 1 state
  const [keepCzlItemId, setKeepCzlItemId] = useState<number | null>(null);

  // Tab 2 state
  const [compMainId, setCompMainId] = useState<number | null>(null);
  const [compSubId, setCompSubId] = useState<number | null>(null);
  const [compItem1, setCompItem1] = useState<number | null>(null);
  const [compItem2, setCompItem2] = useState<number | null>(null);

  // Tab 3 state
  const [nirvMainId, setNirvMainId] = useState<number | null>(null);
  const [nirvSubId, setNirvSubId] = useState<number | null>(null);
  const [nirvBeastId, setNirvBeastId] = useState<number | null>(null);
  const [nirvItem1, setNirvItem1] = useState<number | null>(null);
  const [nirvItem2, setNirvItem2] = useState<number | null>(null);

  // Tab 4 state
  const [extractItem1, setExtractItem1] = useState<number | null>(null);
  const [extractItem2, setExtractItem2] = useState<number | null>(null);
  const [convertValue, setConvertValue] = useState('');
  // Tab 4 bag items for dropdowns
  const [bagItems, setBagItems] = useState<BagItem[]>([]);

  // Tab 5 state
  const [rebirthTargets, setRebirthTargets] = useState<RebirthTarget[]>([]);
  const [rebirthSelId, setRebirthSelId] = useState<number | null>(null);
  const [rebirthInfo, setRebirthInfo] = useState<Record<string, unknown> | null>(null);
  const [rebirthItem1, setRebirthItem1] = useState<number | null>(null);
  const [rebirthItem2, setRebirthItem2] = useState<number | null>(null);

  const refreshPets = useCallback(async () => {
    const res = await apiGet<PetInfo[]>('/pets');
    if (res.code === 0 && res.data) {
      setPets(res.data);
      if (res.data.length > 0 && !selPet) setSelPet(res.data[0]);
    }
  }, [selPet]);

  useEffect(() => {
    refreshPets().then(() => setLoading(false));
    // Load bag for Tab 4
    apiGet<BagItem[]>('/bag').then((res) => {
      if (res.code === 0 && res.data) setBagItems(res.data);
    });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const isDivine = (pet: PetInfo) => pet.wx === 7;
  const isSpirit = (pet: PetInfo) => pet.wx === 6;

  const showMsg = (m: string) => { setMsg(m); setTimeout(() => setMsg(''), 5000); };

  const doApi = async <T,>(fn: () => Promise<{ code: number; message?: string; data?: T }>) => {
    setWorking(true);
    try {
      const res = await fn();
      if (res.code === 0 && res.data) {
        const d = res.data as Record<string, unknown>;
        showMsg((d.message as string) || '操作成功');
        await refreshPets();
        return d;
      } else if (res.data) {
        const d = res.data as Record<string, unknown>;
        showMsg((d.message as string) || '操作失败');
        return d;
      } else {
        showMsg(res.message || '请求失败');
        return null;
      }
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      showMsg(err?.response?.data?.message || '网络错误');
      return null;
    } finally {
      setWorking(false);
    }
  };

  // ──────── Tab 1: Evolve ────────
  const handleEvolve = (style: number) => {
    if (!selPet || working) return;
    doApi(() => apiPost(`/pet/${selPet.id}/evolve`, { style }));
  };

  // ──────── Tab 2: Compose ────────
  const handleCompose = () => {
    if (!compMainId || !compSubId || working) return;
    doApi(() => apiPost('/pet/compose', {
      mainPetId: compMainId, subPetId: compSubId,
      item1Id: compItem1, item2Id: compItem2,
    }));
  };

  // ──────── Tab 3: Nirvana ────────
  const handleNirvana = () => {
    if (!nirvMainId || !nirvSubId || !nirvBeastId || working) return;
    doApi(() => apiPost('/pet/nirvana', {
      mainPetId: nirvMainId, subPetId: nirvSubId, beastId: nirvBeastId,
      item1Id: nirvItem1, item2Id: nirvItem2,
    }));
  };

  // ──────── Tab 4: Sacred Evolve ────────
  const handleSacredEvolve = () => {
    if (!selPet || working) return;
    doApi(() => apiPost(`/pet/${selPet.id}/sacred-evolve`, { keepCzlItemId }));
  };

  const handleExtractGrowth = () => {
    if (!selPet || working) return;
    doApi(() => apiPost(`/pet/${selPet.id}/extract-growth`, {
      item1Id: extractItem1, item2Id: extractItem2,
    }));
  };

  const handleConvertGrowth = () => {
    if (!selPet || working || !convertValue) return;
    doApi(() => apiPost(`/pet/${selPet.id}/convert-growth`, { value: parseInt(convertValue, 10) }));
  };

  // ──────── Tab 5: Sacred Rebirth ────────
  const loadRebirthImages = async (petId: number) => {
    const res = await apiGet<RebirthTarget[]>(`/pet/rebirth-images`, { petId });
    if (res.code === 0 && res.data) setRebirthTargets(res.data);
  };

  const loadRebirthInfo = async (zsId: number) => {
    if (!selPet) return;
    const res = await apiGet<Record<string, unknown>>(`/pet/${selPet.id}/rebirth-info`, { zsId });
    if (res.code === 0 && res.data) setRebirthInfo(res.data);
  };

  const handleSacredRebirth = () => {
    if (!selPet || !rebirthSelId || working) return;
    doApi(() => apiPost(`/pet/${selPet.id}/sacred-rebirth`, {
      zsId: rebirthSelId, item1Id: rebirthItem1, item2Id: rebirthItem2,
    }));
  };

  // Load rebirth targets when selecting a divine pet in Tab 5
  useEffect(() => {
    if (tab === 5 && selPet && isDivine(selPet)) {
      loadRebirthImages(selPet.id);
      setRebirthInfo(null);
      setRebirthSelId(null);
    }
  }, [tab, selPet]); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) return <div className={styles.container}>加载中...</div>;

  const compPets = pets.filter(p => p.level >= 40);
  const nirvPets = pets.filter(p => isSpirit(p) && p.level >= 60);
  const beastPets = pets.filter(p => p.level >= 60 && (p.name?.includes('涅磐兽') || p.name?.includes('涅槃兽')));
  return (
    <div className={styles.container}>
      <div className={styles.left}></div>
      <div className={styles.right}>
        {/* Message toast */}
        {msg && <div style={{position:'absolute',top:8,left:'50%',transform:'translateX(-50%)',background:'rgba(0,0,0,0.8)',color:'#FFD700',padding:'4px 16px',borderRadius:4,zIndex:10,fontSize:12}}>{msg}</div>}

        <ul className={styles.tabs}>
          <li className={tab === 1 ? styles.on : ''} onClick={() => setTab(1)}>
            <a className={styles.tab1} />
          </li>
          <li className={tab === 2 ? styles.on : ''} onClick={() => setTab(2)}>
            <a className={styles.tab2} />
          </li>
          <li className={tab === 3 ? styles.on : ''} onClick={() => setTab(3)}>
            <a className={styles.tab3} />
          </li>
          <li className={tab === 4 ? styles.on : ''} onClick={() => setTab(4)}>
            <a className={styles.tab4} />
          </li>
          <li className={tab === 5 ? styles.on : ''} onClick={() => setTab(5)}>
            <a className={styles.tab5} />
          </li>
        </ul>

        {/* Tab 1: 进化 */}
        {tab === 1 && (
          <div className={styles.tabContent}>
            <div className={styles.sdL}>
              <p>说明：进化需要满足条件，进化材料可以通过怪物掉落、开启进化箱、进化宝石兑换获得。</p>
              <div className={styles.petRow}>
                {pets.slice(0, 3).map((p) => (
                  <div key={p.id} className={`${styles.petItem} ${selPet?.id === p.id ? styles.petSel : ''}`}
                    onClick={() => setSelPet(p)} title={p.name}>
                    {p.cardImg ? <img src={`/images/bb/${p.cardImg}`} alt={p.name} /> : <span>{p.name}</span>}
                  </div>
                ))}
                {pets.length === 0 && <span>暂无宠物</span>}
              </div>
            </div>
            <div className={styles.sdR}>
              {selPet && (() => {
                const levels = selPet.remakelevel ? selPet.remakelevel.split(',') : [];
                const rnames = selPet.remakeName ? selPet.remakeName.split(',') : [];
                const pnames = selPet.remakePName ? selPet.remakePName.split(',') : [];
                return <>
                  <div className={styles.step}>
                    <p>
                      进化需求等级：{levels[0] || '不可进化'}<br />
                      当前等级：{selPet.level}<br />
                      进化所需金币：1000<br />
                      进化所需材料：{pnames[0] || '查看具体宠物'}<br />
                      进化后宠物：{rnames[0] || '查看具体宠物'}<br />
                      <a href="#" onClick={(e) => { e.preventDefault(); handleEvolve(1); }}>
                        <img src="/new_images/ui/sd04.jpg" alt="A进化" style={{opacity: working ? 0.5 : 1}} />
                      </a>
                    </p>
                  </div>
                  <div className={styles.step}>
                    <p>
                      进化需求等级：{levels[1] || '不可进化'}<br />
                      当前等级：{selPet.level}<br />
                      进化所需金币：1000<br />
                      进化所需材料：{pnames[1] || '查看具体宠物'}<br />
                      进化后宠物：{rnames[1] || '查看具体宠物'}<br />
                      <a href="#" onClick={(e) => { e.preventDefault(); handleEvolve(2); }}>
                        <img src="/new_images/ui/sd05.jpg" alt="B进化" style={{opacity: working ? 0.5 : 1}} />
                      </a>
                    </p>
                  </div>
                </>;
              })()}
              {!selPet && <p>请选择一只宠物</p>}
            </div>
          </div>
        )}

        {/* Tab 2: 合成 */}
        {tab === 2 && (
          <div className={styles.tabContent}>
            <div className={styles.composeL}>
              <div className={styles.composePets}>
                <div className={styles.composeSlot}>
                  {compMainId ? (pets.find(p => p.id === compMainId)?.cardImg && <img src={`/images/bb/${pets.find(p => p.id === compMainId)!.cardImg}`} alt="" />) : null}
                </div>
                <div className={styles.composeSlot}>
                  {compSubId ? (pets.find(p => p.id === compSubId)?.cardImg && <img src={`/images/bb/${pets.find(p => p.id === compSubId)!.cardImg}`} alt="" />) : null}
                </div>
              </div>
              <div className={styles.composeSelects}>
                <table width="280"><tbody>
                  <tr>
                    <td align="center" style={{paddingRight:20}}>
                      <select value={compMainId ?? ''} onChange={(e) => {
                        const v = e.target.value ? parseInt(e.target.value) : null;
                        setCompMainId(v);
                        if (v === compSubId) setCompSubId(null);
                      }}>
                        <option value="">请选择主宠物</option>
                        {compPets.filter(p => p.id !== compSubId).map(p => <option key={p.id} value={p.id}>{p.name}-Lv.{p.level}</option>)}
                      </select>
                    </td>
                    <td align="center">
                      <select value={compSubId ?? ''} onChange={(e) => {
                        const v = e.target.value ? parseInt(e.target.value) : null;
                        setCompSubId(v);
                        if (v === compMainId) setCompMainId(null);
                      }}>
                        <option value="">请选择副宠物</option>
                        {compPets.filter(p => p.id !== compMainId).map(p => <option key={p.id} value={p.id}>{p.name}-Lv.{p.level}</option>)}
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <td height="30" colSpan={2} align="center">合成宠物需要金币：<span>50000金币！</span></td>
                  </tr>
                </tbody></table>
              </div>
            </div>
            <div className={styles.composeR}>
              <b>合成等级限制：</b>主副宠物均需要40级<br />
              <b>合成失败惩罚：</b>副宠消失<br />
              <b>说明：</b><br />
              1）添加的道具可以通过神秘商店、副本等获得<br />
              2）合成时请先取下宠物装备，否则宠物消失时装备也会一起消失。<br />
              3）合成冷却为60秒，需等待冷却后才能继续合成。<br />
              添加<span style={{color:'red'}}>守护</span>材料：
              <select value={compItem1 ?? ''} onChange={(e) => setCompItem1(e.target.value ? parseInt(e.target.value) : null)}>
                <option value="">选择材料一</option>
                {bagItems.filter(b => b.varyname === 8 && b.effect && b.sums > 0 && (!b.usages || !b.usages.startsWith('涅盘'))).map(b => <option key={b.id} value={b.id}>{b.name} x{b.sums}</option>)}
              </select><br />
              添加<span style={{color:'red'}}>加成</span>材料：
              <select value={compItem2 ?? ''} onChange={(e) => setCompItem2(e.target.value ? parseInt(e.target.value) : null)}>
                <option value="">选择材料二</option>
                {bagItems.filter(b => b.varyname === 8 && b.effect && b.sums > 0 && (!b.usages || !b.usages.startsWith('涅盘'))).map(b => <option key={b.id} value={b.id}>{b.name} x{b.sums}</option>)}
              </select><br />
              <table width="300" style={{marginTop:10}}><tbody>
                <tr>
                  <td rowSpan={2}>
                    <a href="#" onClick={(e) => { e.preventDefault(); handleCompose(); }}>
                      <img src="/images/sdbtn.gif" alt="开始合成" style={{opacity: working ? 0.5 : 1}} />
                    </a>
                  </td>
                  <td style={{paddingLeft:20}}>合成幸运星：{player?.mergeCount ?? 0}</td>
                </tr>
                <tr><td style={{paddingLeft:20}}><a href="#" onClick={(e) => { e.preventDefault(); NYI(); }}><img src="/images/gm15.gif" alt="合成幸运星说明" /></a></td></tr>
              </tbody></table>
            </div>
          </div>
        )}

        {/* Tab 3: 涅磐 */}
        {tab === 3 && (
          <div className={styles.tabContent}>
            <div className={styles.composeL}>
              <div className={styles.composePets}>
                <div className={styles.composeSlot}>
                  {nirvMainId ? (pets.find(p => p.id === nirvMainId)?.cardImg && <img src={`/images/bb/${pets.find(p => p.id === nirvMainId)!.cardImg}`} alt="" />) : null}
                </div>
                <div className={styles.composeSlot}>
                  {nirvSubId ? (pets.find(p => p.id === nirvSubId)?.cardImg && <img src={`/images/bb/${pets.find(p => p.id === nirvSubId)!.cardImg}`} alt="" />) : null}
                </div>
              </div>
              <div className={styles.composeSelects}>
                <table><tbody>
                  <tr>
                    <td align="center">
                      <select value={nirvMainId ?? ''} onChange={(e) => {
                        const v = e.target.value ? parseInt(e.target.value) : null;
                        setNirvMainId(v);
                        if (v === nirvSubId) setNirvSubId(null);
                      }}>
                        <option value="">请选择主宠物</option>
                        {nirvPets.filter(p => p.id !== nirvSubId).map(p => <option key={p.id} value={p.id}>{p.name}-Lv.{p.level}</option>)}
                      </select>
                    </td>
                    <td align="center">
                      <select value={nirvSubId ?? ''} onChange={(e) => {
                        const v = e.target.value ? parseInt(e.target.value) : null;
                        setNirvSubId(v);
                        if (v === nirvMainId) setNirvMainId(null);
                      }}>
                        <option value="">请选择副宠物</option>
                        {nirvPets.filter(p => p.id !== nirvMainId).map(p => <option key={p.id} value={p.id}>{p.name}-Lv.{p.level}</option>)}
                      </select>
                    </td>
                  </tr>
                  <tr>
                    <td height="30" colSpan={2} align="center">
                      请选择涅磐兽：
                      <select value={nirvBeastId ?? ''} onChange={(e) => setNirvBeastId(e.target.value ? parseInt(e.target.value) : null)}>
                        <option value="">请选择涅磐兽</option>
                        {beastPets.map(p => <option key={p.id} value={p.id}>{p.name}-Lv.{p.level}</option>)}
                      </select>
                    </td>
                  </tr>
                </tbody></table>
              </div>
            </div>
            <div className={styles.composeR}>
              <b>涅磐限制：</b>主副宠,涅磐兽均需为神宠，60级以上<br />
              <b>涅磐失败惩罚：</b>涅磐兽消失<br />
              <b>说明：</b><br />
              1）添加的道具可以通过神秘商店、副本等获得<br />
              2）合成时请先取下宠物装备，否则宠物消失时装备也会一起消失。<br />
              3）合成冷却为60秒，需等待冷却后才能继续合成。<br />
              添加材料一：
              <select value={nirvItem1 ?? ''} onChange={(e) => setNirvItem1(e.target.value ? parseInt(e.target.value) : null)}>
                <option value="">选择材料一</option>
                {bagItems.filter(b => b.varyname === 8 && b.effect && b.sums > 0 && b.usages?.startsWith('涅盘')).map(b => <option key={b.id} value={b.id}>{b.name} x{b.sums}</option>)}
              </select><br />
              添加材料二：
              <select value={nirvItem2 ?? ''} onChange={(e) => setNirvItem2(e.target.value ? parseInt(e.target.value) : null)}>
                <option value="">涅槃加成材料</option>
                {bagItems.filter(b => b.varyname === 19 && b.sums > 0).map(b => <option key={b.id} value={b.id}>{b.name} x{b.sums}</option>)}
              </select><br />
              <table><tbody>
                <tr>
                  <td>
                    <a href="#" onClick={(e) => { e.preventDefault(); handleNirvana(); }}>
                      <img src="/images/sdbtn02.gif" alt="神宠涅磐" style={{opacity: working ? 0.5 : 1}} />
                    </a>
                  </td>
                  <td>每次涅槃消耗金币：50W</td>
                </tr>
              </tbody></table>
            </div>
          </div>
        )}

        {/* Tab 4: 神圣进化 */}
        {tab === 4 && (
          <div className={styles.tabContent}>
            <div className={styles.ssBg}>
              <div className={styles.ssL}>
                <div className={styles.petRow} style={{marginTop:55, marginLeft:35}}>
                  {pets.slice(0, 3).map((p) => (
                    <div key={p.id} className={`${styles.petItem} ${selPet?.id === p.id ? styles.petSel : ''}`}
                      onClick={() => setSelPet(p)} title={p.name}>
                      {p.cardImg ? <img src={`/images/bb/${p.cardImg}`} alt={p.name} /> : <span>{p.name}</span>}
                    </div>
                  ))}
                </div>
                {selPet && (
                  <div className={styles.ssInfo}>
                    <table><tbody>
                      <tr><td width="125">进化需要等级：</td><td width="85">当前等级：{isDivine(selPet) ? selPet.level : 'N/A'}</td></tr>
                      <tr><td colSpan={2}>进化所需材料：查看详细配置</td></tr>
                      <tr><td colSpan={2}>进化所需金币：根据阶段计算</td></tr>
                      <tr><td colSpan={2}>当前进化次数：{isDivine(selPet) ? selPet.remaketimes ?? 0 : 'N/A'}</td></tr>
                      <tr>
                        <td><img src="/images/sd_cion01.jpg" alt="查看说明" style={{cursor:'pointer'}} onClick={NYI} /></td>
                        <td>
                          <a href="#" onClick={(e) => { e.preventDefault(); handleSacredEvolve(); }}>
                            <img src="/images/sd_cion02.jpg" alt="进化" style={{cursor:'pointer', opacity: working ? 0.5 : 1}} />
                          </a>
                        </td>
                      </tr>
                    </tbody></table>
                    <div style={{marginTop: 8}}>
                      选择保护成长道具：
                      <select value={keepCzlItemId ?? ''} onChange={(e) => setKeepCzlItemId(e.target.value ? parseInt(e.target.value) : null)} style={{width: 160}}>
                        <option value="">选择保护成长道具</option>
                        {bagItems.filter(b => b.effect?.includes('keepczl:')).map(b => <option key={b.id} value={b.id}>{b.name} x{b.sums}</option>)}
                      </select>
                    </div>
                  </div>
                )}
              </div>
              <div className={styles.ssR}>
                <div className={styles.ssSection}>
                  <table><tbody>
                    <tr>
                      <td>选择道具一：
                        <select value={extractItem1 ?? ''} onChange={(e) => setExtractItem1(e.target.value ? parseInt(e.target.value) : null)}>
                          <option value="">增加抽取比例道具</option>
                          {bagItems.filter(b => b.effect?.includes('inczhl:')).map(b => <option key={b.id} value={b.id}>{b.name} x{b.sums}</option>)}
                        </select>
                      </td>
                      <td rowSpan={3} align="right">
                        <a href="#" onClick={(e) => { e.preventDefault(); handleExtractGrowth(); }}>
                          <img src="/images/sd_cion03.jpg" alt="抽取成长" style={{cursor:'pointer', opacity: working ? 0.5 : 1}} />
                        </a>
                      </td>
                    </tr>
                    <tr><td>选择道具二：
                      <select value={extractItem2 ?? ''} onChange={(e) => setExtractItem2(e.target.value ? parseInt(e.target.value) : null)}>
                        <option value="">增加抽取比例道具</option>
                        {bagItems.filter(b => b.effect?.includes('inczhl:')).map(b => <option key={b.id} value={b.id}>{b.name} x{b.sums}</option>)}
                      </select>
                    </td></tr>
                    <tr><td>本次抽取需要金币：<span>czl x 10000</span></td></tr>
                    <tr><td colSpan={2}><img src="/images/sd_cion05.jpg" alt="查看说明" style={{cursor:'pointer'}} onClick={NYI} /></td></tr>
                  </tbody></table>
                </div>
                <div className={styles.ssSection}>
                  <table><tbody>
                    <tr>
                      <td>当前拥有成长值：<span>{selPet && isDivine(selPet) ? selPet.czl : '不可转化'}</span></td>
                      <td rowSpan={3} align="right">
                        <a href="#" onClick={(e) => { e.preventDefault(); handleConvertGrowth(); }}>
                          <img src="/images/sd_cion04.jpg" alt="转化成长" style={{cursor:'pointer', opacity: working ? 0.5 : 1}} />
                        </a>
                      </td>
                    </tr>
                    <tr>
                      <td>输入你要转化的成长值：
                        <input name="zhvalue" type="text" size={8} value={convertValue}
                          onChange={(e) => setConvertValue(e.target.value)} />
                      </td>
                    </tr>
                    <tr><td><img src="/images/sd_cion07.jpg" alt="查看说明" style={{cursor:'pointer'}} onClick={NYI} /></td></tr>
                  </tbody></table>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 5: 神圣转生 */}
        {tab === 5 && (
          <div className={styles.tabContent}>
            <div className={styles.ssBg2}>
              <div className={styles.ssL}>
                <div className={styles.petRow} style={{marginTop:55, marginLeft:35}}>
                  {pets.slice(0, 3).map((p) => (
                    <div key={p.id} className={`${styles.petItem} ${selPet?.id === p.id ? styles.petSel : ''}`}
                      onClick={() => setSelPet(p)} title={p.name}>
                      {p.cardImg ? <img src={`/images/bb/${p.cardImg}`} alt={p.name} /> : <span>{p.name}</span>}
                    </div>
                  ))}
                </div>
                {selPet && (
                  <div className={styles.ssInfo}>
                    <table><tbody>
                      <tr><td colSpan={2}>宠物当前等级：<span>{isDivine(selPet) ? selPet.level : ''}</span></td></tr>
                      <tr><td colSpan={2}>宠物当前成长：<span>{isDivine(selPet) ? selPet.czl : ''}</span></td></tr>
                      <tr>
                        <td colSpan={2}>
                          选择转生目标：
                          <select value={rebirthSelId ?? ''} onChange={(e) => {
                            const v = e.target.value ? parseInt(e.target.value) : null;
                            setRebirthSelId(v);
                            if (v) loadRebirthInfo(v);
                          }}>
                            <option value="">请选择</option>
                            {rebirthTargets.map(t => <option key={t.zsId} value={t.zsId}>{t.name}</option>)}
                          </select>
                        </td>
                      </tr>
                      {rebirthInfo && (
                        <tr>
                          <td colSpan={2}>
                            需要等级：{String(rebirthInfo.needLevel || '-')} | 需要成长：{String(rebirthInfo.needCzl || '-')}<br />
                            需要金币：{String(rebirthInfo.goldCost || '-')} | 成功率：{String(rebirthInfo.baseSuccessRate || '-')}%
                          </td>
                        </tr>
                      )}
                      <tr>
                        <td width="125"><img src="/images/sd_cion08.jpg" alt="查看说明" style={{cursor:'pointer'}} onClick={NYI} /></td>
                        <td width="85">
                          <a href="#" onClick={(e) => { e.preventDefault(); handleSacredRebirth(); }}>
                            <img src="/images/sd_cion09.jpg" alt="转生" style={{cursor:'pointer', opacity: working ? 0.5 : 1}} />
                          </a>
                        </td>
                      </tr>
                    </tbody></table>
                  </div>
                )}
              </div>
              <div className={styles.ssR}>
                <div className={styles.ssItemBox}>
                  <table width="330"><tbody>
                    <tr>
                      <td rowSpan={4}>转生条件</td>
                      <td width="153" align="left">选择道具一：</td>
                    </tr>
                    <tr>
                      <td height="30" align="left">
                        <select value={rebirthItem1 ?? ''} onChange={(e) => setRebirthItem1(e.target.value ? parseInt(e.target.value) : null)}>
                          <option value="">增加道具</option>
                          {bagItems.filter(b => b.varyname === 23 && b.sums > 0).map(b => <option key={b.id} value={b.id}>{b.name} x{b.sums}</option>)}
                        </select>
                      </td>
                    </tr>
                    <tr><td align="left">选择道具二：</td></tr>
                    <tr>
                      <td height="20" align="left">
                        <select value={rebirthItem2 ?? ''} onChange={(e) => setRebirthItem2(e.target.value ? parseInt(e.target.value) : null)}>
                          <option value="">增加道具</option>
                          {bagItems.filter(b => b.varyname === 23 && b.sums > 0).map(b => <option key={b.id} value={b.id}>{b.name} x{b.sums}</option>)}
                        </select>
                      </td>
                    </tr>
                  </tbody></table>
                </div>
              </div>
            </div>
          </div>
        )}

      </div>

      {/* Back to city button */}
      <div className={styles.backBtn} onClick={() => setGameView('city')}>
        <label></label>
      </div>
    </div>
  );
}
