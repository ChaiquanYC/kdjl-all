import { useEffect, useState, type ReactNode } from 'react';
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
import ItemTooltip, { useTooltipProps } from './ItemTooltip';
import { CATEGORIES } from './shopConstants';
import { filterByCat } from './shopUtils';
import type { ShopItem, BagItemMerged } from './ShopTypes';
import layoutStyles from './ShopLayout.module.css';
import styles from './SmShopPanel.module.css';

const SUB_CATS = [
  { label: '热卖', style: 1 }, { label: '进化合成', style: 2 },
  { label: '宠物相关', style: 3 }, { label: '装备相关', style: 4 },
];

type BagItem = BagItemMerged;

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
  const [bagCat, setBagCat] = useState(0);
  const [confirmDialog, setConfirmDialog] = useState<{ message: ReactNode; onConfirm: () => void } | null>(null);
  const [tooltip, setTooltip] = useState<{ item: any; x: number; y: number } | null>(null);

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
  const vipItems = allItems.filter(i => (i.prestige ?? 0) > 0);
  const limitItems = allItems.filter(i => (i.timelimit ?? 0) > 0);

  const displayItems = tab === 1 ? ybItems : tab === 2 ? sjItems : tab === 3 ? vipItems : limitItems;
  const currency = tab === 1 ? 'yb' : tab === 2 ? 'sj' : tab === 3 ? 'vip' : 'yb';
  const curLabel = tab === 1 ? '元宝' : tab === 2 ? '水晶' : tab === 3 ? 'VIP积分' : '元宝';
  const filterBag = filterByCat(bagItems, bagCat, CATEGORIES);

  const doBuy = () => {
    apiPost('/shop/buy/' + selShop, { count, currency }).then((res: any) => {
      if (res.code === 0) {
        const m = `购买了${count}个商品`;
        setMsg(m); systips(m);
        apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); });
        triggerRefresh();
      } else {
        setMsg(res.message ?? '购买失败'); systips(res.message ?? '购买失败');
      }
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const doSell = () => {
    apiPost('/bag/sell/' + selBag, { count }).then((res: any) => {
      if (res.code === 0) {
        const m = '卖出成功';
        setMsg(m); systips(m);
        apiGet<BagItem[]>('/bag').then(r => { if (r.code === 0 && r.data) setBagItems(r.data); });
        triggerRefresh();
      } else {
        setMsg(res.message ?? '卖出失败'); systips(res.message ?? '卖出失败');
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
            <li className={tab===4?styles.tabOn:''} onClick={()=>setTab(4)}><span className={styles.t4}/></li>
          </ul>
          <ResourceBar items={[
            { icon: '/images/ui/icon02.jpg', label: '金币', value: player?.money ?? 0 },
            { icon: '/images/ui/icon06.jpg', label: '水晶', value: player?.sj ?? 0 },
            { icon: '/images/ui/icon01.jpg', label: '元宝', value: player?.yb ?? 0 },
          ]} />
          {tooltip && <ItemTooltip item={tooltip.item} x={tooltip.x} y={tooltip.y} />}
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
                  {...useTooltipProps(setTooltip, item)}>
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
        <ShopFooter count={count} onCountChange={setCount}>
          <FooterBtn onClick={handleBuy}>购买</FooterBtn>
          <FooterBtn onClick={handleSell}>卖出</FooterBtn>
        </ShopFooter>
      </div>

      {/* Right column — bag */}
      <BagColumn items={filterBag} selId={selBag}
        onSelect={item => { setSelBag(item.id); setSelShop(null); setCount(item.count); }}
        listVariant="fixed" listClassName={styles.itemListSize}
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
