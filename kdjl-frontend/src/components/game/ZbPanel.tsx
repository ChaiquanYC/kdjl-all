import { useEffect, useState, type ReactNode } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import ShopLayout from './ShopLayout';
import ConfirmDialog from './ConfirmDialog';
import BagColumn from './BagColumn';
import layoutStyles from './ShopLayout.module.css';
import styles from './ZbPanel.module.css';

interface ShopItem {
  id: number; name: string; buy: number; prestige: number;
  img?: string; varyname?: number; requires?: string; postion?: number;
}
interface BagItem {
  id: number; propId: number; count: number; sell: number;
  name?: string; varyname?: number; category?: string;
}

const CATEGORIES = [
  { label: '全部道具', vary: [] },
  { label: '辅助道具', vary: [1] }, { label: '增益道具', vary: [2] }, { label: '捕捉道具', vary: [3] },
  { label: '收集道具', vary: [4] }, { label: '技能书',   vary: [5] }, { label: '卡片道具', vary: [6] },
  { label: '进化道具', vary: [7] }, { label: '合体道具', vary: [8] }, { label: '装备道具', vary: [9] },
  { label: '精练道具', vary: [10] },{ label: '宝箱道具', vary: [11] },{ label: '特殊道具', vary: [12] },
  { label: '功能道具', vary: [13] },{ label: '宠物卵',   vary: [14] },{ label: '合成道具', vary: [15] },
];

function filterByCat<T extends { category?: string }>(items: T[], cat: number) {
  if (cat === 0) return items;
  const label = CATEGORIES[cat]?.label ?? '';
  return items.filter(i => (i.category ?? '') === label);
}

const POS_FILTERS = [
  { label: '全部', pos: [] },
  { label: '头部', pos: [1] },
  { label: '身体', pos: [2] },
  { label: '脚部', pos: [3] },
  { label: '武器', pos: [4] },
  { label: '项链', pos: [5] },
  { label: '戒指', pos: [6] },
  { label: '翅膀', pos: [0,7] },
  { label: '手镯', pos: [8] },
  { label: '宝石', pos: [9] },
  { label: '道具', pos: [10] },
  { label: '其他', pos: [11] },
];

export default function ZbPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [tab, setTab] = useState(1);
  const [shopItems, setShopItems] = useState<ShopItem[]>([]);
  const [bagItems, setBagItems] = useState<BagItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [posFilter, setPosFilter] = useState(0);
  const [selShop, setSelShop] = useState<number | null>(null);
  const [selBag, setSelBag] = useState<number | null>(null);
  const [count, setCount] = useState(1);
  const [msg, setMsg] = useState<string | null>(null);
  const [bagCat, setBagCat] = useState(0);
  const [confirmDialog, setConfirmDialog] = useState<{ message: ReactNode; onConfirm: () => void } | null>(null);
  useEffect(() => {
    Promise.all([
      apiGet<ShopItem[]>('/shop/list?type=equip'),
      apiGet<BagItem[]>('/bag'),
    ]).then(([s, b]) => {
      if (s.code === 0 && s.data) setShopItems(s.data);
      if (b.code === 0 && b.data) setBagItems(b.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  const equipItems = shopItems.filter(i => i.varyname === 9 || i.postion != null);
  const goldItems = equipItems.filter(i => (i.buy ?? 0) > 0 && (i.prestige ?? 0) === 0);
  const prestigeItems = equipItems.filter(i => (i.prestige ?? 0) > 0);

  const displayItems = (tab === 1 ? goldItems : prestigeItems).filter(i => {
    if (posFilter === 0) return true;
    return POS_FILTERS[posFilter].pos.includes(i.postion ?? -1);
  });

  const doBuy = (item: ShopItem, currency: 'money' | 'prestige') => {
    apiPost('/shop/buy/' + item.id, { count, currency }).then((res: any) => {
      if (res.code === 0) {
        setMsg(`购买了${count}个 ${item.name}`);
        apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); });
        triggerRefresh();
      } else setMsg(res.message ?? '购买失败');
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const doSell = () => {
    apiPost('/bag/sell/' + selBag, { count }).then((res: any) => {
      if (res.code === 0) {
        setMsg(`卖出成功，获得${res.data?.goldGained ?? 0}金币`);
        apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); });
        triggerRefresh();
      } else setMsg(res.message ?? '卖出失败');
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const handleBuy = (item: ShopItem, currency: 'money' | 'prestige') => {
    setConfirmDialog({ message: `确定购买 ${count}个 ${item.name}？`, onConfirm: () => { doBuy(item, currency); setConfirmDialog(null); } });
  };

  const handleSell = () => {
    if (!selBag) { setMsg('请先选择要卖出的物品'); return; }
    setConfirmDialog({ message: `确定卖出 ${count}个物品？`, onConfirm: () => { doSell(); setConfirmDialog(null); } });
  };

  if (loading) return <div className={layoutStyles.loading}>加载中...</div>;

  const curLabel = tab === 1 ? '装备商店' : '威望装备';

  return (
    <>
    <ShopLayout
      leftBg="/images/ui/zb01.jpg"
      onReturn={() => setGameView('city')}
      toast={msg}
      topArea={
        <>
          <ul className={styles.tabs}>
            <li className={tab === 1 ? styles.tabOn : ''} onClick={() => setTab(1)}><span className={styles.t1} /></li>
            <li className={tab === 2 ? styles.tabOn : ''} onClick={() => setTab(2)}><span className={styles.t2} /></li>
          </ul>
          <div className={styles.resBar}>
            {tab === 1 ? <><img src="/images/ui/icon02.jpg" alt="" /> 金币：{player?.money ?? 0}</>
              : <><img src="/images/ui/icon02.jpg" alt="" /> 威望：{player?.prestige ?? 0}</>}
          </div>
        </>
      }
    >
      <div className={layoutStyles.column}>
        <div className={styles.colHead}>
          <span className={styles.colTitle}>{curLabel}</span>
          <select className={styles.posFilter} value={posFilter} onChange={e => setPosFilter(Number(e.target.value))}>
            {POS_FILTERS.map((f, i) => <option key={i} value={i}>{f.label}</option>)}
          </select>
        </div>
        <div className={layoutStyles.itemList}>
          <table className={layoutStyles.table}>
            <thead><tr><th className={layoutStyles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>{tab===1?'价格':'威望'}</th><th className={styles.thReq}>需求</th></tr></thead>
            <tbody>
              {displayItems.length === 0 ? (
                <tr><td colSpan={4} className={layoutStyles.empty}>暂无装备</td></tr>
              ) : displayItems.map(item => (
                <tr key={item.id} className={`${layoutStyles.row} ${selShop === item.id ? layoutStyles.rowSel : ''}`}
                  onClick={() => { setSelShop(item.id); setSelBag(null); }}>
                  <td className={layoutStyles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                  <td className={styles.tdName}>{item.name}</td>
                  <td className={styles.tdPrice}>{tab === 1 ? item.buy : item.prestige}</td>
                  <td className={styles.tdReq}>{item.requires ?? '无'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className={layoutStyles.colFoot}>
          数量：<input className={layoutStyles.numInput} type="text" value={count} onChange={e => setCount(Number(e.target.value) || 1)} />
          <button className={layoutStyles.btn} onClick={() => selShop ? handleBuy(displayItems.find(i => i.id === selShop)!, tab === 1 ? 'money' : 'prestige') : setMsg('请先选择装备')}>购买</button>
          {tab === 1 && <button className={layoutStyles.btn} onClick={handleSell}>卖出</button>}
        </div>
      </div>

      <BagColumn items={filterByCat(bagItems, bagCat)} selId={selBag}
        onSelect={item => { setSelBag(item.id); setSelShop(null); setCount(item.count); }}
        title={<span className={styles.colTitle}>我的背包</span>}
        extraHeader={
          <select className={styles.posFilter} value={bagCat} onChange={e => setBagCat(Number(e.target.value))}>
            {CATEGORIES.map((c, i) => <option key={i} value={i}>{c.label}</option>)}
          </select>
        }
      />
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
