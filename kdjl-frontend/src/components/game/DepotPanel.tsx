import { useEffect, useState, useMemo } from 'react';
import { apiGet, apiPost } from '@/api/client';
import { useGameStore } from '@/stores/gameStore';
import { useAuthStore } from '@/stores/authStore';
import ShopLayout from './ShopLayout';
import BagColumn from './BagColumn';
import ResourceBar from './ResourceBar';
import ShopFooter, { FooterBtn } from './ShopFooter';
import CategorySelect from './CategorySelect';
import ItemTooltip, { useTooltipProps } from './ItemTooltip';
import BagTooltip from './BagTooltip';
import { CATEGORIES } from './shopConstants';
import { filterByCat } from './shopUtils';
import type { BagItemBase } from './ShopTypes';
import layoutStyles from './ShopLayout.module.css';
import styles from './DepotPanel.module.css';

interface ItemRaw extends BagItemBase {
  varyname?: number; name?: string; img?: string; category?: string;
  effect?: string; effectDesc?: string; requires?: string; requiresDesc?: string;
  pluseffect?: string; pluseffectDesc?: string; usages?: string;
  propsColor?: number; postion?: number; plusflag?: number; propslock?: number;
  expire?: string; series?: string; serieseffect?: string;
}

function mergeItems(raw: BagItemBase[], propsMap: Record<number, import('@/types').PropsItem>): ItemRaw[] {
  return raw.map(r => {
    const p = propsMap[r.propId];
    return {
      ...r,
      name: p?.name,
      img: p?.img,
      varyname: p?.varyname,
      category: p?.category,
      effect: p?.effect,
      effectDesc: p?.effectDesc,
      requires: p?.requires,
      requiresDesc: p?.requiresDesc,
      pluseffect: p?.pluseffect,
      pluseffectDesc: p?.pluseffectDesc,
      usages: p?.usages,
      propsColor: p?.propscolor ? Number(p.propscolor) : undefined,
      postion: p?.postion,
      plusflag: p?.plusflag,
      propslock: p?.propslock,
      expire: p?.expire ? String(p.expire) : undefined,
      series: p?.series,
      serieseffect: p?.serieseffect,
    };
  });
}

export default function DepotPanel() {
  const player = useAuthStore((s) => s.player);
  const setGameView = useGameStore((s) => s.setGameView);
  const triggerRefresh = useGameStore((s) => s.triggerRefresh);
  const propsMap = useGameStore((s) => s.propsMap);
  const [rawBag, setRawBag] = useState<BagItemBase[]>([]);
  const [rawDepot, setRawDepot] = useState<BagItemBase[]>([]);
  const [loading, setLoading] = useState(true);

  const bag: ItemRaw[] = useMemo(() => mergeItems(rawBag, propsMap), [rawBag, propsMap]);
  const depot: ItemRaw[] = useMemo(() => mergeItems(rawDepot, propsMap), [rawDepot, propsMap]);
  const [bagCat, setBagCat] = useState(0);
  const [depotCat, setDepotCat] = useState(0);
  const [selBag, setSelBag] = useState<number | null>(null);
  const [selDepot, setSelDepot] = useState<number | null>(null);
  const [count, setCount] = useState(1);
  const [msg, setMsg] = useState<string | null>(null);
  const [tooltip, setTooltip] = useState<{ item: any; x: number; y: number } | null>(null);
  const [bagTooltip, setBagTooltip] = useState<any>(null);

  const maxBag = player?.maxBag ?? 30;
  const maxDepot = 50;

  const fetchData = () => {
    setLoading(true);
    apiGet<BagItemBase[]>('/bag').then(bagRes => {
      if (bagRes.code === 0 && bagRes.data) setRawBag(bagRes.data);
      return apiGet<BagItemBase[]>('/depot');
    }).then(depRes => {
      if (depRes.code === 0 && depRes.data) setRawDepot(depRes.data);
    }).catch(() => {}).finally(() => setLoading(false));
  };

  useEffect(() => { fetchData(); }, []);

  const filterBag = filterByCat(bag.filter(i => i.count > 0 && i.zbing !== 1), bagCat, CATEGORIES);
  const filterDepot = filterByCat(depot.filter(i => i.count > 0), depotCat, CATEGORIES);
  const bagTotal = bag.filter(i => i.count > 0 && i.zbing !== 1).length;
  const depotTotal = depot.filter(i => i.count > 0).length;

  const handleDeposit = () => {
    if (!selBag) { setMsg('请先在背包中选择物品'); return; }
    apiPost('/depot/deposit/' + selBag, { count }).then((res: any) => {
      if (res.code === 0) { setMsg('存放成功'); fetchData(); triggerRefresh(); setSelBag(null); setCount(1); }
      else setMsg(res.message);
      setTimeout(() => setMsg(null), 2000);
    });
  };

  const handleWithdraw = () => {
    if (!selDepot) { setMsg('请先在仓库中选择物品'); return; }
    apiPost('/depot/withdraw/' + selDepot, { count }).then((res: any) => {
      if (res.code === 0) { setMsg('取出成功'); fetchData(); triggerRefresh(); setSelDepot(null); setCount(1); }
      else setMsg(res.message);
      setTimeout(() => setMsg(null), 2000);
    });
  };

  if (loading) return <div className={layoutStyles.loading}>加载中...</div>;

  return (
    <ShopLayout
      leftBg="/images/ui/cangku01.jpg"
      onReturn={() => setGameView('city')}
      toast={msg}
      topArea={
        <>
          <div className={styles.topBar}>
            <div className={styles.topBtn} />
          </div>
          <ResourceBar items={[
            { icon: '/images/ui/icon01.jpg', label: '元宝', value: player?.yb ?? 0 },
            { icon: '/images/ui/icon02.jpg', label: '金币', value: player?.money ?? 0 },
          ]} />
          {tooltip && <ItemTooltip item={tooltip.item} x={tooltip.x} y={tooltip.y} />}
        </>
      }
    >
      <div className={layoutStyles.column}>
        <div className={styles.colHeader}>
          <img src="/images/ui/icon03.jpg" alt="仓库物品" className={styles.colIcon} />
          <CategorySelect value={depotCat} onChange={setDepotCat} showLabel />
        </div>
        <div className={`${layoutStyles.itemList} ${styles.itemListH}`}>
          <table className={layoutStyles.table}>
            <thead><tr><th className={layoutStyles.thIcon}></th><th className={styles.thName}>名称</th><th className={styles.thPrice}>价格</th><th className={styles.thCount}>数量</th></tr></thead>
            <tbody>
              {filterDepot.length === 0 ? (
                <tr><td colSpan={4} className={layoutStyles.empty}>仓库中没有物品</td></tr>
              ) : filterDepot.map(item => (
                <tr key={item.id} className={`${layoutStyles.row} ${selDepot === item.id ? layoutStyles.rowSel : ''}`}
                  onClick={() => { setSelDepot(item.id); setSelBag(null); setCount(item.count); }}
                  {...useTooltipProps(setTooltip, item)}>
                  <td className={layoutStyles.tdIcon}>{item.varyname ? <img src={`/images/ui/bag/${item.varyname}.gif`} alt="" /> : null}</td>
                  <td className={styles.tdName}>{item.name ?? `道具#${item.propId}`}</td>
                  <td className={styles.tdPrice}>{item.sell ?? 0}</td>
                  <td className={styles.tdCount}>{item.count}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className={`${layoutStyles.colFoot} ${styles.colFoot}`}>
          仓库空间：{depotTotal}/{maxDepot}
          <ShopFooter count={count} onCountChange={setCount}>
            <FooterBtn disabled={!selDepot} onClick={handleWithdraw}>取出</FooterBtn>
            <FooterBtn disabled={!selBag} onClick={handleDeposit}>存放</FooterBtn>
          </ShopFooter>
        </div>
      </div>

      <BagColumn items={filterBag} selId={selBag}
        onSelect={item => { setSelBag(item.id); setSelDepot(null); setCount(item.count); }}
        onTooltipChange={setBagTooltip}
        extraHeader={<CategorySelect value={bagCat} onChange={setBagCat} showLabel />}
        listClassName={styles.itemListH}
        footer={<div className={`${layoutStyles.colFoot} ${styles.colFoot}`}>背包空间：{bagTotal}/{maxBag}</div>}
      />
      <BagTooltip item={bagTooltip} onTimeout={() => setBagTooltip(null)} />
    </ShopLayout>
  );
}
