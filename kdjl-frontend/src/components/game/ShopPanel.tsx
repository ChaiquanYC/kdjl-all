import { useEffect, useState, useMemo, type ReactNode } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import { systips } from '@/stores/systipsStore';
import ShopLayout from './ShopLayout';
import ConfirmDialog from './ConfirmDialog';
import BagColumn from './BagColumn';
import ResourceBar from './ResourceBar';
import ShopFooter, { FooterBtn } from './ShopFooter';
import CategorySelect from './CategorySelect';
import { CATEGORIES } from './shopConstants';
import { filterByCat } from './shopUtils';
import type { ShopItem, BagItemBase, BagItemMerged } from './ShopTypes';
import layoutStyles from './ShopLayout.module.css';
import styles from './ShopPanel.module.css';

function mergeWithProps(base: BagItemBase, propsMap: Record<number, import('@/types').PropsItem>): BagItemMerged {
  const p = propsMap[base.propId];
  return {
    ...base,
    name: p?.name ?? `道具#${base.propId}`,
    img: p?.img,
    varyname: p?.varyname,
    category: p?.category,
    effect: p?.effect,
    effectDesc: p?.effectDesc,
    requires: p?.requires,
    requiresDesc: p?.requiresDesc,
    pluseffect: p?.pluseffect,
    usages: p?.usages,
    propsColor: p?.propscolor ? Number(p.propscolor) : undefined,
    postion: p?.postion,
  };
}

export default function ShopPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const [tab, setTab] = useState(1);
  const [shopItems, setShopItems] = useState<ShopItem[]>([]);
  const [rawBagItems, setRawBagItems] = useState<BagItemBase[]>([]);
  const [loading, setLoading] = useState(true);
  const propsMap = useGameStore((s) => s.propsMap);

  const bagItems: BagItemMerged[] = useMemo(() => rawBagItems.map(b => mergeWithProps(b, propsMap)), [rawBagItems, propsMap]);
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
      apiGet<BagItemBase[]>('/bag'),
    ]).then(([s, b]) => {
      if (s.code === 0 && s.data) setShopItems(s.data);
      if (b.code === 0 && b.data) setRawBagItems(b.data);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  const goldItems = shopItems.filter(i => (i.buy ?? 0) > 0 && (i.yb ?? 0) === 0 && (i.prestige ?? 0) === 0);
  const prestigeItems = shopItems.filter(i => (i.prestige ?? 0) > 0);

  const doBuy = (item: ShopItem, currency: 'money' | 'prestige') => {
    apiPost('/shop/buy/' + item.id, { count, currency }).then((res: any) => {
      if (res.code === 0) {
        const d = res.data;
        const m = (d?.message as string) ?? `购买了${count}个 ${item.name}`;
        setMsg(m); systips(m);
        apiGet<BagItemBase[]>('/bag').then(r => { if (r.code === 0 && r.data) setRawBagItems(r.data); });
        triggerRefresh();
      } else {
        setMsg(res.message ?? '购买失败'); systips(res.message ?? '购买失败');
      }
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const doSell = (item: BagItemMerged) => {
    apiPost('/bag/sell/' + item.id, { count }).then((res: any) => {
      if (res.code === 0) {
        const m = `卖出成功，获得${res.data?.goldGained ?? 0}金币`;
        setMsg(m); systips(m);
        apiGet<BagItemBase[]>('/bag').then(r => { if (r.code === 0 && r.data) setRawBagItems(r.data); });
        triggerRefresh();
      } else {
        setMsg(res.message ?? '卖出失败'); systips(res.message ?? '卖出失败');
      }
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const handleBuy = (item: ShopItem, currency: 'money' | 'prestige') => {
    const cost = (currency === 'money' ? item.buy : (item.prestige ?? 0)) * count;
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

  const displayItems = filterByCat(tab === 1 ? goldItems : prestigeItems, shopCat, CATEGORIES);
  const curLabel = tab === 1 ? '金币商店' : '威望商店';
  const filterBag = filterByCat(bagItems, bagCat, CATEGORIES);

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
          <ResourceBar items={[
            { icon: '/images/ui/icon02.jpg', label: tab === 1 ? '金币' : '威望', value: tab === 1 ? (player?.money ?? 0) : (player?.prestige ?? 0) },
          ]} />
        </>
      }
    >
      <div className={layoutStyles.column}>
        <div className={styles.colTitle}>
          <img src="/images/ui/shop03.jpg" alt={curLabel} />
          <CategorySelect value={shopCat} onChange={setShopCat} />
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
        <ShopFooter count={count} onCountChange={setCount}>
          <FooterBtn onClick={() => { const item = displayItems.find(i => i.id === selShop); item ? handleBuy(item, tab === 1 ? 'money' : 'prestige') : setMsg('请先选择商品'); }}>购买</FooterBtn>
          {tab === 1 && <FooterBtn onClick={handleSell}>卖出</FooterBtn>}
        </ShopFooter>
      </div>

      <BagColumn items={filterBag} selId={selBag}
        onSelect={item => { setSelBag(item.id); setSelShop(null); }}
        extraHeader={<CategorySelect value={bagCat} onChange={setBagCat} />}
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
