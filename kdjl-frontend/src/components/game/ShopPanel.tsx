import { useEffect, useState, type ReactNode } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import { systips } from '@/stores/systipsStore';
import ShopLayout from './ShopLayout';
import ConfirmDialog from './ConfirmDialog';
import BagColumn from './BagColumn';
import layoutStyles from './ShopLayout.module.css';
import styles from './ShopPanel.module.css';

interface ShopItem {
  id: number; name: string; buy: number; yb: number; prestige: number;
  img?: string; effect?: string; varyname?: number; category?: string;
}
interface BagItem {
  id: number; propId: number; count: number; sell: number; zbing?: number;
  name?: string; img?: string; varyname?: number; category?: string;
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

export default function ShopPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [tab, setTab] = useState(1);
  const [shopItems, setShopItems] = useState<ShopItem[]>([]);
  const [bagItems, setBagItems] = useState<BagItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [selShop, setSelShop] = useState<number | null>(null);
  const [selBag, setSelBag] = useState<number | null>(null);
  const [count, setCount] = useState(1);
  const [msg, setMsg] = useState<string | null>(null);
  const [shopCat, setShopCat] = useState(0);
  const [bagCat, setBagCat] = useState(0);
  const [confirmDialog, setConfirmDialog] = useState<{ message: ReactNode; onConfirm: () => void } | null>(null);

  useEffect(() => {
    Promise.all([
      apiGet<ShopItem[]>('/shop/list?type=props'),
      apiGet<BagItem[]>('/bag'),
    ]).then(([s, b]) => {
      if (s.code === 0 && s.data) setShopItems(s.data);
      if (b.code === 0 && b.data) setBagItems(b.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  const goldItems = shopItems.filter(i => (i.buy ?? 0) > 0 && (i.yb ?? 0) === 0 && (i.prestige ?? 0) === 0);
  const prestigeItems = shopItems.filter(i => (i.prestige ?? 0) > 0);

  const doBuy = (item: ShopItem, currency: 'money' | 'prestige') => {
    apiPost('/shop/buy/' + item.id, { count, currency }).then((res: any) => {
      if (res.code === 0) {
        const d = res.data;
        const msg = (d?.message as string) ?? `购买了${count}个 ${item.name}`;
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

  const doSell = (item: BagItem) => {
    apiPost('/bag/sell/' + item.id, { count }).then((res: any) => {
      if (res.code === 0) {
        const msg = `卖出成功，获得${res.data?.goldGained ?? 0}金币`;
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

  const handleBuy = (item: ShopItem, currency: 'money' | 'prestige') => {
    const cost = (currency === 'money' ? item.buy : item.prestige) * count;
    setConfirmDialog({
      message: `确定购买 ${count}个 ${item.name}？共${cost}${currency === 'money' ? '金币' : '威望'}`,
      onConfirm: () => { doBuy(item, currency); setConfirmDialog(null); },
    });
  };

  const handleSell = () => {
    if (!selBag) { setMsg('请先选择要卖出的物品'); return; }
    const item = bagItems.find(i => i.id === selBag);
    if (!item) return;
    setConfirmDialog({
      message: `确定卖出 ${count}个 ${item.name}？共${(item.sell ?? 0) * count}金币`,
      onConfirm: () => { doSell(item); setConfirmDialog(null); },
    });
  };

  if (loading) return <div className={layoutStyles.loading}>加载中...</div>;

  const displayItems = filterByCat(tab === 1 ? goldItems : prestigeItems, shopCat);
  const curLabel = tab === 1 ? '金币商店' : '威望商店';
  const filterBag = filterByCat(bagItems, bagCat);

  return (
    <>
    <ShopLayout
      leftBg="/images/ui/shop01.jpg"
      onReturn={() => setGameView('city')}
      toast={msg}
      topArea={
        <>
          <ul className={styles.tabs}>
            <li className={tab === 1 ? styles.tabOn : ''} onClick={() => setTab(1)}><span className={styles.t1} /></li>
            <li className={tab === 2 ? styles.tabOn : ''} onClick={() => setTab(2)}><span className={styles.t2} /></li>
          </ul>
          <div className={styles.resBar}>
            {tab === 1 ? (
              <><img src="/images/ui/icon02.jpg" alt="" /> 金币：{player?.money ?? 0}</>
            ) : (
              <><img src="/images/ui/icon02.jpg" alt="" /> 威望：{player?.prestige ?? 0}</>
            )}
          </div>
        </>
      }
    >
      <div className={layoutStyles.column}>
        <div className={styles.colTitle}>
          <img src="/images/ui/shop03.jpg" alt={curLabel} />
          <select className={styles.catSelect} value={shopCat} onChange={e => setShopCat(Number(e.target.value))}>
            {CATEGORIES.map((c, i) => <option key={i} value={i}>{c.label}</option>)}
          </select>
        </div>
        <div className={layoutStyles.itemList}>
          <table className={layoutStyles.table}>
            <thead><tr><th className={layoutStyles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>{tab === 1 ? '价格' : '威望'}</th><th className={styles.thType}>属性</th></tr></thead>
            <tbody>
              {displayItems.length === 0 ? (
                <tr><td colSpan={4} className={layoutStyles.empty}>暂无商品</td></tr>
              ) : displayItems.map(item => (
                <tr key={item.id} className={`${layoutStyles.row} ${selShop === item.id ? layoutStyles.rowSel : ''}`}
                  onClick={() => { setSelShop(item.id); setSelBag(null); }}>
                  <td className={layoutStyles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                  <td className={styles.tdName}>{item.name}</td>
                  <td className={styles.tdPrice}>{tab === 1 ? item.buy : item.prestige}</td>
                  <td className={styles.tdType}>{item.category ?? ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className={layoutStyles.colFoot}>
          数量：<input className={layoutStyles.numInput} type="text" value={count} onChange={e => setCount(Number(e.target.value) || 1)} />
          <button className={layoutStyles.btn} onClick={() => selShop ? handleBuy(displayItems.find(i => i.id === selShop)!, tab === 1 ? 'money' : 'prestige') : setMsg('请先选择商品')}>购买</button>
          {tab === 1 && <button className={layoutStyles.btn} onClick={handleSell}>卖出</button>}
        </div>
      </div>

      <BagColumn items={filterBag} selId={selBag}
        onSelect={item => { setSelBag(item.id); setSelShop(null); }}
        extraHeader={
          <select className={styles.catSelect} value={bagCat} onChange={e => setBagCat(Number(e.target.value))}>
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
