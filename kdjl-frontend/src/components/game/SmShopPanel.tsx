import { useEffect, useState, type ReactNode } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import { systips } from '@/stores/systipsStore';
import ShopLayout from './ShopLayout';
import ConfirmDialog from './ConfirmDialog';
import layoutStyles from './ShopLayout.module.css';
import styles from './SmShopPanel.module.css';

interface ShopItem {
  id: number; name: string; buy: number; yb: number; sj?: number; prestige?: number;
  img?: string; varyname?: number; timelimit?: number; stime?: number;
  effect?: string; requires?: string; propsColor?: number;
  vary?: number; postion?: number; plusflag?: number; category?: string;
}
interface BagItem {
  id: number; propId: number; count: number; sell: number; zbing?: number;
  name?: string; img?: string; varyname?: number;
  effect?: string; effectDesc?: string; requires?: string; requiresDesc?: string;
  propsColor?: number; vary?: number; usages?: string;
}

const SUB_CATS = [
  { label: '热卖', style: 1 }, { label: '进化合成', style: 2 },
  { label: '宠物相关', style: 3 }, { label: '装备相关', style: 4 },
];

const PROPS_COLORS: Record<number, string> = {
  1: '#FEFDFA', 2: '#0067CB', 3: '#9833DC', 4: '#14FD10', 5: '#FED625', 6: '#ED9037',
};
const SLOT_NAMES = ['翅膀','头部','身体','脚部','武器','项链','戒指','翅膀','手镯','宝石','道具','特殊'];

const ATTR_KEYS: Record<string, string> = {
  ac:'攻击', mc:'防御', hp:'生命', mp:'魔法', speed:'速度', hits:'命中', miss:'闪避',
  addmoney:'获得金币', time:'时间', acrate:'攻击%', mcrate:'防御%', hprate:'生命%',
  mprate:'魔法%', speedrate:'速度%', hitsrate:'命中%', missrate:'闪避%',
  hitshp:'吸血%', hitsmp:'吸蓝%', dxsh:'多行伤害', shjs:'伤害减少', szmp:'数值魔法', sdmp:'速度魔法', crit:'暴击率',
  srchp:'生命上限', srcmp:'魔法上限', addhp:'生命', addmp:'魔法',
};

function resolveEffect(effect?: string): string {
  if (!effect) return '';
  return effect.split('|').map(p => {
    const [k, v] = p.split(',');
    if (!k || !v) return '';
    const label = ATTR_KEYS[k] || k;
    if (k.endsWith('rate') || k === 'hitshp' || k === 'hitsmp' || k === 'crit' || k === 'dxsh')
      return `+${v}% ${label}`;
    return `+${v} ${label}`;
  }).filter(Boolean).join(' ');
}

const WX_NAMES = ['所有','金','木','水','火','土','神','神圣'];

function parseRequires(requires?: string) {
  if (!requires) return null;
  const parts = requires.split(',');
  let lv: string | null = null;
  let wx: string | null = null;
  for (const p of parts) {
    const [k, v] = p.split(':');
    if (k === 'lv') lv = v;
    else if (k === 'wx') wx = WX_NAMES[Number(v)] ?? v;
  }
  return { lv, wx };
}

const EP_BASE = '#FEFDFA';
const EP_PLUS = '#0067CB';
const EP_GRAY = '#A8A7A4';

function ShopTooltip({ item, x, y }: { item: ShopItem | BagItem; x: number; y: number }) {
  const propsColor = (item as any).propsColor ?? 1;
  const nameColor = PROPS_COLORS[propsColor] ?? '#FEFDFA';
  const effect = (item as any).effectDesc || resolveEffect((item as any).effect);
  const pluseffect = resolveEffect((item as any).pluseffect);
  const req = parseRequires((item as any).requiresDesc || (item as any).requires);
  const slotName = (item as any).postion != null ? SLOT_NAMES[(item as any).postion] : null;
  const sell = (item as any).sell ?? (item as any).buy;
  const varyname = (item as any).varyname;

  const tipImg = (name: string) => `/images/ui/tips/border4_${name}.gif`;

  const content = (
    <>
      {varyname === 9 ? (
        <>
          <div className={styles.tipName} style={{ color: nameColor }}><b>{item.name}</b></div>
          <div style={{ color: EP_GRAY }}>可交易</div>
          {slotName && <div style={{ color: EP_BASE }}>{slotName}装备 ({((item as any).plusflag ?? 0) === 1 ? '可强化' : '不可强化'})</div>}
          {effect && <div style={{ color: EP_BASE }}>{effect}</div>}
          {req && (req.wx || req.lv) && (
            <>
              {req.wx && <div style={{ color: EP_BASE }}>五行需求：{req.wx}系</div>}
              {req.lv && <div style={{ color: EP_BASE }}>需求等级：{req.lv}级</div>}
            </>
          )}
          {pluseffect && <div style={{ color: EP_PLUS }}>{pluseffect}</div>}
          {(item as any).usages && <div style={{ color: EP_BASE }}>{(item as any).usages}</div>}
        </>
      ) : (
        <>
          <div className={styles.tipName} style={{ color: nameColor }}><b>{item.name}</b></div>
          {effect && <div style={{ color: EP_BASE }}>{effect}</div>}
          {req && (req.wx || req.lv) && (
            <>
              {req.wx && <div style={{ color: EP_BASE }}>五行需求：{req.wx}系</div>}
              {req.lv && <div style={{ color: EP_BASE }}>需求等级：{req.lv}级</div>}
            </>
          )}
          {(item as any).usages && <div style={{ color: EP_BASE }}>{(item as any).usages}</div>}
        </>
      )}
      {sell != null && <div style={{ color: EP_BASE }}>售价：{sell}金</div>}
      {(item as any).expire && <div style={{ color: EP_BASE }}>{(item as any).expire}</div>}
    </>
  );

  return (
    <table className={styles.tooltip} style={{ left: x + 12, top: Math.max(0, y - 160) }} cellPadding={0} cellSpacing={0} border={0}>
      <tbody>
        <tr>
          <td className={styles.tipCorner}><img src={tipImg('tl')} alt="" /></td>
          <td className={styles.tipEdge} style={{ backgroundImage: `url(${tipImg('t')})` }} />
          <td className={styles.tipCorner}><img src={tipImg('tr')} alt="" /></td>
        </tr>
        <tr>
          <td className={styles.tipEdgeL} style={{ backgroundImage: `url(${tipImg('l')})` }} />
          <td className={styles.tipBorderTd} />
          <td className={styles.tipEdgeL} style={{ backgroundImage: `url(${tipImg('r')})` }} />
        </tr>
        <tr>
          <td className={styles.tipEdgeL} style={{ backgroundImage: `url(${tipImg('l')})` }} />
          <td className={styles.tipBorderTd}>{content}</td>
          <td className={styles.tipEdgeL} style={{ backgroundImage: `url(${tipImg('r')})` }} />
        </tr>
        <tr>
          <td className={styles.tipCorner}><img src={tipImg('bl')} alt="" /></td>
          <td className={styles.tipEdge} style={{ backgroundImage: `url(${tipImg('b')})` }} />
          <td className={styles.tipCorner}><img src={tipImg('br')} alt="" /></td>
        </tr>
      </tbody>
    </table>
  );
}

export default function SmShopPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [tab, setTab] = useState(1);
  const [subCat, setSubCat] = useState(1);
  const [shopItems, setShopItems] = useState<ShopItem[]>([]);
  const [bagItems, setBagItems] = useState<BagItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selShop, setSelShop] = useState<number | null>(null);
  const [selBag, setSelBag] = useState<number | null>(null);
  const [count, setCount] = useState(1);
  const [msg, setMsg] = useState<string | null>(null);
  const [confirmDialog, setConfirmDialog] = useState<{ message: ReactNode; onConfirm: () => void } | null>(null);
  const [tooltip, setTooltip] = useState<{ item: ShopItem | BagItem; x: number; y: number } | null>(null);
  const maxBag = player?.maxBag ?? 30;

  useEffect(() => {
    Promise.all([
      apiGet<ShopItem[]>('/shop/list?type=smshop'),
      apiGet<BagItem[]>('/bag'),
    ]).then(([s, b]) => {
      if (s.code === 0 && s.data) setShopItems(s.data);
      if (b.code === 0 && b.data) setBagItems(b.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  const allItems = shopItems.filter(i => `${i.stime ?? 1}`.startsWith(`${subCat}`));
  const ybItems = allItems.filter(i => (i.yb ?? 0) > 0);
  const sjItems = allItems.filter(i => (i.sj ?? 0) > 0);
  const vipItems = allItems.filter(i => (i.yb ?? 0) > 0).slice(0, 5);

  const displayItems = tab === 1 ? ybItems : tab === 2 ? sjItems : vipItems;
  const currency = tab === 1 ? 'yb' : tab === 2 ? 'sj' : 'vip';
  const curLabel = tab === 1 ? '元宝' : tab === 2 ? '水晶' : 'VIP积分';
  const bagTotal = bagItems.filter(i => i.count > 0 && i.zbing !== 1).length;

  const doBuy = () => {
    apiPost('/shop/buy/' + selShop, { count, currency }).then((res: any) => {
      if (res.code === 0) {
        const msg = `购买了${count}个商品`;
        setMsg(msg);
        systips(msg);
        apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); });
        triggerRefresh();
      } else {
        setMsg(res.message ?? '购买失败');
        systips(res.message ?? '购买失败');
      }
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const doSell = () => {
    apiPost('/bag/sell/' + selBag, { count }).then((res: any) => {
      if (res.code === 0) {
        const msg = '卖出成功';
        setMsg(msg);
        systips(msg);
        apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); });
        triggerRefresh();
      } else {
        setMsg(res.message ?? '卖出失败');
        systips(res.message ?? '卖出失败');
      }
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const handleBuy = () => {
    if (!selShop) { setMsg('请先选择商品'); return; }
    setConfirmDialog({ message: `确定购买 ${count}个商品？`, onConfirm: () => { doBuy(); setConfirmDialog(null); } });
  };

  const handleSell = () => {
    if (!selBag) { setMsg('请先选择要卖出的物品'); return; }
    setConfirmDialog({ message: `确定卖出 ${count}个物品？`, onConfirm: () => { doSell(); setConfirmDialog(null); } });
  };

  const hoverProps = (item: ShopItem | BagItem) => ({
    onMouseEnter: (e: React.MouseEvent) => setTooltip({ item, x: e.clientX, y: e.clientY }),
    onMouseMove: (e: React.MouseEvent) => setTooltip(prev => prev ? { ...prev, x: e.clientX, y: e.clientY } : null),
    onMouseLeave: () => setTooltip(null),
  });

  if (loading) return <div className={layoutStyles.loading}>加载中...</div>;

  return (
    <>
    <ShopLayout
      leftBg="/images/ui/smshop01.jpg"
      onReturn={() => setGameView('city')}
      toast={msg}
      className={styles.containerBg}
      topArea={
        <>
          <ul className={styles.tabs}>
            <li className={tab===1?styles.tabOn:''} onClick={()=>setTab(1)}><span className={styles.t1}/></li>
            <li className={tab===2?styles.tabOn:''} onClick={()=>setTab(2)}><span className={styles.t2}/></li>
            <li className={tab===3?styles.tabOn:''} onClick={()=>setTab(3)}><span className={styles.t3}/></li>
          </ul>
          <div className={styles.resBar}>
            <div className={styles.resRow}><img src="/images/ui/icon02.jpg" alt="" /> 金币：{player?.money ?? 0}</div>
            <div className={styles.resRow}><img src="/images/ui/icon06.jpg" alt="" /> 水晶：{player?.sj ?? 0}</div>
            {tab === 1 && <div className={styles.resRow}><img src="/images/ui/icon01.jpg" alt="" /> 元宝：{player?.yb ?? 0}</div>}
            {tab === 2 && <div className={styles.resRow}><img src="/images/ui/icon01.jpg" alt="" /> 元宝：{player?.yb ?? 0}</div>}
            {tab === 3 && <div className={styles.resRow}><img src="/images/ui/icon05.jpg" alt="" /> VIP：{player?.vip ?? 0}</div>}
          </div>
          {tooltip && <ShopTooltip item={tooltip.item} x={tooltip.x} y={tooltip.y} />}
        </>
      }
    >
      {/* Left column — shop items */}
      <div className={layoutStyles.column}>
        <div className={styles.subCats}>
          {SUB_CATS.map(c => (
            <img key={c.style} src={`/images/ui/smshop_0${c.style}.jpg`} alt={c.label}
              style={subCat===c.style?{filter:'brightness(1.2)',borderBottom:'2px solid #FED625'}:{}}
              onClick={() => setSubCat(c.style)} />
          ))}
        </div>
        <div className={`${layoutStyles.itemListFixed} ${styles.itemListSize}`}>
          <table className={layoutStyles.table}>
            <thead><tr><th className={layoutStyles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>{curLabel}</th><th className={styles.tdType}>属性</th></tr></thead>
          </table>
          <div className={layoutStyles.itemBody}>
            <table className={layoutStyles.table}>
              <tbody>
              {displayItems.length === 0 ? (
                <tr><td colSpan={4} className={layoutStyles.empty}>暂无商品</td></tr>
              ) : displayItems.map(item => (
                <tr key={item.id} className={`${layoutStyles.row} ${selShop===item.id?layoutStyles.rowSel:''}`}
                  onClick={() => { setSelShop(item.id); setSelBag(null); }}
                  {...hoverProps(item)}>
                  <td className={layoutStyles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                  <td className={styles.tdName}>{item.name}</td>
                  <td className={styles.tdPrice}>{tab===1?item.yb:tab===2?item.sj:item.yb}</td>
                  <td className={styles.tdType}>{item.category || ''}</td>
                </tr>
              ))}
              </tbody>
            </table>
          </div>
        </div>
        <div className={styles.colFoot}>
          数量：<input className={styles.numInp} type="text" value={count} onChange={e => setCount(Number(e.target.value)||1)} />
          <button className={styles.btn} onClick={handleBuy}>购买</button>
          <button className={styles.btn} onClick={handleSell}>卖出</button>
        </div>
      </div>

      {/* Right column — bag */}
      <div className={`${layoutStyles.column} ${styles.column2}`}>
        <div className={styles.colTitle}>
          <img src="/images/ui/icon04.jpg" alt="背包物品" />
        </div>
        <div className={`${layoutStyles.itemListFixed} ${styles.itemListSize}`}>
          <table className={layoutStyles.table}>
            <thead><tr><th className={layoutStyles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>卖价</th><th className={styles.tdType}>数量</th></tr></thead>
          </table>
          <div className={layoutStyles.itemBody}>
            <table className={layoutStyles.table}>
              <tbody>
              {bagItems.filter(i => i.count > 0 && i.zbing !== 1).length === 0 ? (
                <tr><td colSpan={4} className={layoutStyles.empty}>背包空空</td></tr>
              ) : bagItems.filter(i => i.count > 0 && i.zbing !== 1).map(item => (
                <tr key={item.id} className={`${layoutStyles.row} ${selBag===item.id?layoutStyles.rowSel:''}`}
                  onClick={() => { setSelBag(item.id); setSelShop(null); setCount(item.count); }}
                  {...hoverProps(item)}>
                  <td className={layoutStyles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                  <td className={styles.tdName}>{item.name ?? `道具#${item.propId}`}</td>
                  <td className={styles.tdPrice}>{item.sell ?? 0}</td>
                  <td className={styles.tdType}>{item.count}</td>
                </tr>
              ))}
              </tbody>
            </table>
          </div>
        </div>
        <div className={styles.bagFoot}>背包空间：{bagTotal}/{maxBag}</div>
      </div>
    </ShopLayout>
      <ConfirmDialog
        open={confirmDialog !== null}
        message={confirmDialog?.message ?? ''}
        onConfirm={() => confirmDialog?.onConfirm()}
        onCancel={() => setConfirmDialog(null)}
      />
    </>
  );
}
