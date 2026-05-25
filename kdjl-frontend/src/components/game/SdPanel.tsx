import { useEffect, useState } from 'react';
import { apiGet } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import styles from './SdPanel.module.css';

interface PetInfo {
  id: number; name: string; level: number; hp: number; maxHp: number;
  mp: number; maxMp: number; exp: number; maxExp: number;
  atk: number; def: number; speed: number; czl: number;
  wx: number; img?: string; cardImg?: string;
  remakelevel?: string; remaketimes?: number;
}

const NYI = () => { alert('开发中'); };

export default function SdPanel() {
  const setGameView = useGameStore((s) => s.setGameView);
  const [tab, setTab] = useState(1);
  const [pets, setPets] = useState<PetInfo[]>([]);
  const [selPet, setSelPet] = useState<PetInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiGet<PetInfo[]>('/pets').then((res) => {
      if (res.code === 0 && res.data) {
        setPets(res.data);
        if (res.data.length > 0) setSelPet(res.data[0]);
      }
      setLoading(false);
    });
  }, []);

  const isDivine = (pet: PetInfo) => pet.wx === 7;
  const isSpirit = (pet: PetInfo) => pet.wx === 6;

  if (loading) return <div className={styles.container}>加载中...</div>;

  return (
    <div className={styles.container}>
      <div className={styles.left}></div>
      <div className={styles.right}>
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
              {selPet && (
                <>
                  <div className={styles.step}>
                    <p>
                      进化需求等级：{selPet.remakelevel ? selPet.remakelevel.split(',')[0] : 'N/A'}<br />
                      当前等级：{selPet.level}<br />
                      进化所需金币：1000<br />
                      进化所需材料：查看具体宠物<br />
                      进化后宠物：查看具体宠物<br />
                      <a href="#" onClick={(e) => { e.preventDefault(); NYI(); }}><img src="/new_images/ui/sd04.jpg" alt="进化" /></a>
                    </p>
                  </div>
                  <div className={styles.step}>
                    <p>
                      进化需求等级：{selPet.remakelevel ? (selPet.remakelevel.split(',')[1] || 'N/A') : 'N/A'}<br />
                      当前等级：{selPet.level}<br />
                      进化所需金币：1000<br />
                      进化所需材料：查看具体宠物<br />
                      进化后宠物：查看具体宠物<br />
                      <a href="#" onClick={(e) => { e.preventDefault(); NYI(); }}><img src="/new_images/ui/sd05.jpg" alt="进化" /></a>
                    </p>
                  </div>
                </>
              )}
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
                  {pets[0]?.cardImg && <img src={`/images/bb/${pets[0].cardImg}`} alt="" />}
                </div>
                <div className={styles.composeSlot}>
                  {pets[1]?.cardImg && <img src={`/images/bb/${pets[1].cardImg}`} alt="" />}
                </div>
              </div>
              <div className={styles.composeSelects}>
                <table width="280"><tbody>
                  <tr>
                    <td align="center" style={{paddingRight:20}}>
                      <select defaultValue=""><option value="">请选择主宠物</option>{pets.filter(p => p.level >= 40).map(p => <option key={p.id} value={p.id}>{p.name}-{p.level}</option>)}</select>
                    </td>
                    <td align="center">
                      <select defaultValue=""><option value="">请选择副宠物</option>{pets.filter(p => p.level >= 40).map(p => <option key={p.id} value={p.id}>{p.name}-{p.level}</option>)}</select>
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
              <select defaultValue=""><option value="">选择材料一</option></select><br />
              添加<span style={{color:'red'}}>加成</span>材料：
              <select defaultValue=""><option value="">选择材料二</option></select><br />
              <table width="300" style={{marginTop:10}}><tbody>
                <tr>
                  <td rowSpan={2}><a href="#" onClick={(e) => { e.preventDefault(); NYI(); }}><img src="/images/sdbtn.gif" alt="开始合成" /></a></td>
                  <td style={{paddingLeft:20}}>合成幸运星：0</td>
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
                  {pets[0]?.cardImg && <img src={`/images/bb/${pets[0].cardImg}`} alt="" />}
                </div>
                <div className={styles.composeSlot}>
                  {pets[1]?.cardImg && <img src={`/images/bb/${pets[1].cardImg}`} alt="" />}
                </div>
              </div>
              <div className={styles.composeSelects}>
                <table><tbody>
                  <tr>
                    <td align="center">
                      <select defaultValue=""><option value="">请选择主宠物</option>{pets.filter(p => isSpirit(p) && p.level >= 60).map(p => <option key={p.id} value={p.id}>{p.name}-{p.level}</option>)}</select>
                    </td>
                    <td align="center">
                      <select defaultValue=""><option value="">请选择副宠物</option>{pets.filter(p => isSpirit(p) && p.level >= 60).map(p => <option key={p.id} value={p.id}>{p.name}-{p.level}</option>)}</select>
                    </td>
                  </tr>
                  <tr>
                    <td height="30" colSpan={2} align="center">请选择涅磐兽：<select defaultValue=""><option value="">请选择涅磐兽</option></select></td>
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
              <select defaultValue=""><option value="">选择材料一</option></select><br />
              添加材料二：
              <select defaultValue=""><option value="">涅槃加成材料</option></select><br />
              <table><tbody>
                <tr>
                  <td><a href="#" onClick={(e) => { e.preventDefault(); NYI(); }}><img src="/images/sdbtn02.gif" alt="神宠涅磐" /></a></td>
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
                      <tr><td colSpan={2}>进化所需材料：</td></tr>
                      <tr><td colSpan={2}>进化所需金币：</td></tr>
                      <tr><td colSpan={2}>当前进化次数：{isDivine(selPet) ? selPet.remaketimes ?? 0 : 'N/A'}</td></tr>
                      <tr>
                        <td><img src="/images/sd_cion01.jpg" alt="查看说明" style={{cursor:'pointer'}} onClick={NYI} /></td>
                        <td><img src="/images/sd_cion02.jpg" alt="进化" style={{cursor:'pointer'}} onClick={NYI} /></td>
                      </tr>
                    </tbody></table>
                  </div>
                )}
              </div>
              <div className={styles.ssR}>
                <div className={styles.ssSection}>
                  <table><tbody>
                    <tr>
                      <td>选择道具一：<select defaultValue=""><option>增加抽取比例道具</option></select></td>
                      <td rowSpan={3} align="right"><img src="/images/sd_cion03.jpg" alt="抽取成长" style={{cursor:'pointer'}} onClick={NYI} /></td>
                    </tr>
                    <tr><td>选择道具二：<select defaultValue=""><option>增加抽取比例道具</option></select></td></tr>
                    <tr><td>本次抽取需要金币：<span>0</span></td></tr>
                    <tr><td colSpan={2}><img src="/images/sd_cion05.jpg" alt="查看说明" style={{cursor:'pointer'}} onClick={NYI} /></td></tr>
                  </tbody></table>
                </div>
                <div className={styles.ssSection}>
                  <table><tbody>
                    <tr>
                      <td>当前拥有成长值：<span>{isDivine(selPet) ? selPet.czl : '不可转化'}</span></td>
                      <td rowSpan={3} align="right"><img src="/images/sd_cion04.jpg" alt="转化成长" style={{cursor:'pointer'}} onClick={NYI} /></td>
                    </tr>
                    <tr><td>输入你要转化的成长值：<input name="zhvalue" type="text" size={8} /></td></tr>
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
                      <tr><td colSpan={2}>&nbsp;</td></tr>
                      <tr><td colSpan={2}>&nbsp;</td></tr>
                      <tr>
                        <td width="125"><img src="/images/sd_cion08.jpg" alt="查看说明" style={{cursor:'pointer'}} onClick={NYI} /></td>
                        <td width="85"><img src="/images/sd_cion09.jpg" alt="转生" style={{cursor:'pointer'}} onClick={NYI} /></td>
                      </tr>
                    </tbody></table>
                  </div>
                )}
              </div>
              <div className={styles.ssR}>
                <div className={styles.ssImgBox} align="center"></div>
                <div className={styles.ssItemBox}>
                  <table width="330"><tbody>
                    <tr>
                      <td width="177" rowSpan={4}></td>
                      <td width="153" align="left">选择道具一：</td>
                    </tr>
                    <tr><td height="30" align="left"><select defaultValue=""><option value="">增加道具</option></select></td></tr>
                    <tr><td align="left">选择道具二：</td></tr>
                    <tr><td height="20" align="left"><select defaultValue=""><option value="">增加道具</option></select></td></tr>
                  </tbody></table>
                </div>
              </div>
            </div>
          </div>
        )}

      </div>

      {/* Back to city button — absolute positioned relative to container */}
      <div className={styles.backBtn} onClick={() => setGameView('city')}>
        <label></label>
      </div>
    </div>
  );
}
