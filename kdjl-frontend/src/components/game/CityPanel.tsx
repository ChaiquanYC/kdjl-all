import { useState } from 'react';
import { useGameStore, type Panel } from '@/stores/gameStore';
import styles from './CityPanel.module.css';

interface Building {
  id: string; label: string; panel: Panel;
  left: number; top: number; width: number; height: number;
  page: number;
  spriteClass?: string;
  imgIcon?: string;
}

const BUILDINGS: Building[] = [
  // Page 1
  { id: 'mc',      label: '牧场',     panel: 'ranch',   left: 40,  top: 55,  width: 38,  height: 40,  page: 1, spriteClass: 'mc' },
  { id: 'jiaoyi',  label: '交易所',   panel: 'auction',  left: 50,  top: 180, width: 52,  height: 40,  page: 1, spriteClass: 'jiaoyisuo' },
  { id: 'smsd',    label: '神秘商店', panel: 'smshop',  left: 260, top: 70,  width: 70,  height: 40,  page: 1, spriteClass: 'smsd' },
  { id: 'ck',      label: '仓库',     panel: 'depot',    left: 430, top: 30,  width: 38,  height: 40,  page: 1, spriteClass: 'ck' },
  { id: 'ggp',     label: '公告牌',   panel: 'tasks',    left: 460, top: 160, width: 52,  height: 40,  page: 1, spriteClass: 'ggp' },
  { id: 'djsd',    label: '道具店',   panel: 'shop',     left: 530, top: 50,  width: 70,  height: 40,  page: 1, spriteClass: 'djsd' },
  { id: 'tjp',     label: '铁匠铺',   panel: 'zb',       left: 690, top: 60,  width: 52,  height: 40,  page: 1, spriteClass: 'tjp' },
  { id: 'hg',      label: '皇宫',     panel: 'tasks',    left: 640, top: 160, width: 38,  height: 40,  page: 1, spriteClass: 'hg' },
  { id: 'zhanbu', label: '占卜屋', panel: 'zhanbu', left: 180, top: 100, width: 70, height: 30, page: 1, imgIcon: '/images/city/mfwu.png' },
  { id: 'slei',   label: '扫雷',   panel: null, left: 560, top: 260, width: 50, height: 30, page: 1, imgIcon: '/images/city/slei.png' },
  { id: 'mtlun',  label: '摩天轮', panel: null, left: 690, top: 260, width: 55, height: 30, page: 1, imgIcon: '/images/city/mtlun.png' },
  // Page 2
  { id: 'cwsd',    label: '宠物神殿', panel: 'sd',       left: 10,  top: 150, width: 70,  height: 30,  page: 2, imgIcon: '/images/city/cwsdian.png' },
  { id: 'jzsd',    label: '家族商店', panel: null,       left: 430, top: 145, width: 70,  height: 30,  page: 2, imgIcon: '/images/city/jzsdian.png' },
  { id: 'mkzhi',   label: '卡牌',     panel: null,       left: 630, top: 215, width: 50,  height: 30,  page: 2, imgIcon: '/images/city/mkzhi.png' },
  { id: 'qrdao',   label: '情人岛',   panel: null,       left: 600, top: 40,  width: 70,  height: 30,  page: 2, imgIcon: '/images/city/qrdao.png' },
];

export default function CityPanel() {
  const [page, setPage] = useState(1);
  const setActivePanel = useGameStore((s) => s.setActivePanel);
  const setGameView = useGameStore((s) => s.setGameView);

  const handleBuildingClick = (b: Building) => {
    if (b.panel) {
      // Game views use setGameView, overlays use setActivePanel
      if (b.panel === 'depot' || b.panel === 'shop' || b.panel === 'zb' || b.panel === 'pets' || b.panel === 'smshop' || b.panel === 'auction' || b.panel === 'ranch' || b.panel === 'zhanbu' || b.panel === 'sd') {
        setGameView(b.panel);
      } else {
        setActivePanel(b.panel);
      }
    } else {
      alert(b.label + '暂未开放');
    }
  };

  const pageBuildings = BUILDINGS.filter((b) => b.page === page);

  return (
    <div className={styles.city}>
      <img
        src={page === 1 ? '/images/city/bitmap1.png' : '/images/city/bitmap2_1.png'}
        alt="城镇"
        className={styles.cityBg}
      />

      {pageBuildings.map((b) => (
        <div
          key={b.id}
          className={`${styles.building} ${b.spriteClass ? styles[b.spriteClass] : ''}`}
          style={{ left: b.left, top: b.top, width: b.width, height: b.height }}
          onClick={() => handleBuildingClick(b)}
          title={b.label}
        >
          {b.imgIcon && (
            <img src={b.imgIcon} alt={b.label} className={styles.buildingImg} />
          )}
          {!b.spriteClass && !b.imgIcon && (
            <span className={styles.buildingLabel}>{b.label}</span>
          )}
        </div>
      ))}

      {/* Page navigation arrows */}
      {page === 1 && (
        <div className={`${styles.arrow} ${styles.arrowRight}`} onClick={() => setPage(2)}>
          <img src="/images/city/left.png" alt="下一页" style={{ transform: 'rotateY(180deg)' }} />
        </div>
      )}
      {page === 2 && (
        <div className={`${styles.arrow} ${styles.arrowLeft}`} onClick={() => setPage(1)}>
          <img src="/images/city/left.png" alt="上一页" />
        </div>
      )}
    </div>
  );
}
