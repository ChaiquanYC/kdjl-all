-- Migration: Replace gonggao table with announcement table
-- Date: 2026-06-02
-- Description: Redesign announcement system with proper field types, status, type, sorting

DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id` int NOT NULL AUTO_INCREMENT,
  `msg` text NOT NULL COMMENT '公告内容',
  `href` varchar(255) DEFAULT NULL COMMENT '链接地址',
  `type` tinyint NOT NULL DEFAULT 1 COMMENT '类型: 1=系统, 2=活动, 3=维护',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0=停用, 1=启用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序权重，越小越靠前',
  `start_time` datetime DEFAULT NULL COMMENT '生效开始时间，null=不限',
  `end_time` datetime DEFAULT NULL COMMENT '生效结束时间，null=不限',
  `broadcast_interval` int NOT NULL DEFAULT 0 COMMENT '广播间隔(分钟)，0=不广播',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏公告';

-- Seed data (replaces old gonggao data)
INSERT INTO `announcement` (`msg`, `type`, `status`, `sort_order`) VALUES
('欢迎来到口袋精灵2！', 1, 1, 0),
('新服开启，精彩活动等你来！', 2, 1, 1),
('维护公告：服务器将于凌晨维护', 3, 1, 2);

-- Drop old table (uncomment after verifying)
-- DROP TABLE IF EXISTS `gonggao`;
