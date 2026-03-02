-- ============================================
-- 景区过度旅游预警系统 - 数据库初始化脚本
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `tourism_warning` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `tourism_warning`;

-- ============================================
-- 1. 管理员信息表
-- ============================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `role` VARCHAR(20) NOT NULL DEFAULT 'ADMIN' COMMENT '角色: ADMIN-管理员, SUPER_ADMIN-超级管理员',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员信息表';

-- ============================================
-- 2. 景区信息表
-- ============================================
DROP TABLE IF EXISTS `scenic_spot`;
CREATE TABLE `scenic_spot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '景区名称',
  `description` TEXT DEFAULT NULL COMMENT '景区描述',
  `province` VARCHAR(50) COMMENT '省份',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
  `longitude` DECIMAL(10, 7) NOT NULL COMMENT '经度',
  `latitude` DECIMAL(10, 7) NOT NULL COMMENT '纬度',
  `max_capacity` INT NOT NULL COMMENT '最大承载量(人)',
  `current_count` INT NOT NULL DEFAULT 0 COMMENT '当前人数',
  `level` VARCHAR(10) DEFAULT NULL COMMENT '景区等级: 5A, 4A, 3A等',
  `image_url` VARCHAR(255) DEFAULT NULL COMMENT '景区图片URL',
  `open_time` VARCHAR(50) DEFAULT NULL COMMENT '开放时间',
  `ticket_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '门票价格',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-关闭, 1-开放',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_city` (`city`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='景区信息表';

-- ============================================
-- 3. 客流流水表
-- ============================================
DROP TABLE IF EXISTS `flow_record`;
CREATE TABLE `flow_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scenic_id` BIGINT NOT NULL COMMENT '景区ID',
  `current_count` INT NOT NULL COMMENT '当前客流人数',
  `in_count` INT DEFAULT 0 COMMENT '进入人数',
  `out_count` INT DEFAULT 0 COMMENT '离开人数',
  `congestion_rate` DECIMAL(5, 2) DEFAULT NULL COMMENT '拥挤度百分比(%)',
  `record_time` DATETIME NOT NULL COMMENT '记录时间',
  `source` VARCHAR(20) DEFAULT 'SIMULATOR' COMMENT '数据来源: SIMULATOR-模拟, SENSOR-传感器, MANUAL-手动',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_scenic_id` (`scenic_id`),
  KEY `idx_record_time` (`record_time`),
  KEY `idx_scenic_time` (`scenic_id`, `record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客流流水表';

-- ============================================
-- 4. 报警历史记录表
-- ============================================
DROP TABLE IF EXISTS `warning_log`;
CREATE TABLE `warning_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scenic_id` BIGINT NOT NULL COMMENT '景区ID',
  `scenic_name` VARCHAR(100) DEFAULT NULL COMMENT '景区名称(冗余)',
  `warning_level` VARCHAR(10) NOT NULL COMMENT '预警级别: YELLOW-黄色预警, RED-红色预警',
  `current_count` INT NOT NULL COMMENT '当前客流人数',
  `max_capacity` INT NOT NULL COMMENT '最大承载量',
  `threshold_percent` DECIMAL(5, 2) NOT NULL COMMENT '触发阈值百分比(%)',
  `congestion_rate` DECIMAL(5, 2) NOT NULL COMMENT '当前拥挤度(%)',
  `message` VARCHAR(500) DEFAULT NULL COMMENT '预警消息内容',
  `handled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已处理: 0-未处理, 1-已处理',
  `handle_user` VARCHAR(50) DEFAULT NULL COMMENT '处理人',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  `warning_time` DATETIME NOT NULL COMMENT '预警时间',
  `plan` TEXT DEFAULT NULL COMMENT 'AI生成的应急预案',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_scenic_id` (`scenic_id`),
  KEY `idx_warning_level` (`warning_level`),
  KEY `idx_warning_time` (`warning_time`),
  KEY `idx_handled` (`handled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报警历史记录表';

-- ============================================
-- 5. 预警阈值配置表
-- ============================================
DROP TABLE IF EXISTS `threshold_config`;
CREATE TABLE `threshold_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `scenic_id` BIGINT NOT NULL COMMENT '景区ID',
  `yellow_percent` DECIMAL(5, 2) NOT NULL DEFAULT 80.00 COMMENT '黄色预警阈值百分比(%)',
  `red_percent` DECIMAL(5, 2) NOT NULL DEFAULT 100.00 COMMENT '红色预警阈值百分比(%)',
  `enable_warning` TINYINT NOT NULL DEFAULT 1 COMMENT '是否开启预警: 0-关闭, 1-开启',
  `notify_method` VARCHAR(50) DEFAULT 'WEBSOCKET' COMMENT '通知方式: WEBSOCKET, SMS, EMAIL',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scenic_id` (`scenic_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预警阈值配置表';

-- ============================================
-- 初始数据
-- ============================================

-- 默认管理员 (密码: admin123, BCrypt加密)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'SUPER_ADMIN');

-- 湖北省主要景区数据（含真实经纬度）
INSERT INTO `scenic_spot` (`name`, `description`, `city`, `longitude`, `latitude`, `max_capacity`, `level`, `open_time`, `ticket_price`) VALUES
-- 武汉市 (4个景区)
('黄鹤楼', '天下江山第一楼，国家5A级旅游景区', '武汉市', 114.3054720, 30.5491060, 25000, '5A', '08:00-18:00', 70.00),
('东湖风景区', '中国最大城中湖', '武汉市', 114.4013620, 30.5559720, 80000, '5A', '全天开放', 0.00),
('木兰天池', '木兰文化生态旅游区', '武汉市', 114.3170310, 31.0548820, 15000, '5A', '07:30-17:30', 70.00),
('武汉欢乐谷', '华中地区首座欢乐谷主题公园', '武汉市', 114.4206250, 30.6148900, 30000, '4A', '09:00-21:00', 230.00),
-- 宜昌市 (3个景区)
('三峡大坝', '世界最大水利枢纽工程', '宜昌市', 111.0037170, 30.8235910, 35000, '5A', '08:00-17:00', 0.00),
('长阳清江画廊', '八百里清江美如画', '宜昌市', 111.1619530, 30.3030850, 15000, '5A', '08:00-16:30', 150.00),
('三峡人家', '原汁原味的三峡风情画廊', '宜昌市', 111.0513450, 30.7637820, 18000, '5A', '08:00-17:30', 150.00),
-- 十堰市 (2个景区)
('武当山', '道教圣地，世界文化遗产', '十堰市', 111.0049740, 32.4001230, 60000, '5A', '07:00-17:30', 235.00),
('房县野人谷', '华中地区生态探险景区', '十堰市', 110.7406200, 32.0532100, 10000, '4A', '08:00-17:00', 90.00),
-- 恩施州 (3个景区)
('恩施大峡谷', '地球最美丽的伤痕', '恩施州', 109.2520510, 30.4021370, 20000, '5A', '08:00-16:00', 170.00),
('腾龙洞', '亚洲最大溶洞', '恩施州', 109.0301320, 30.2862510, 10000, '4A', '08:30-17:00', 150.00),
('恩施土司城', '全国规模最大的土家族地区土司文化标志性工程', '恩施州', 109.4790230, 30.3078900, 8000, '4A', '08:00-17:30', 50.00),
-- 襄阳市 (2个景区)
('古隆中', '三国文化圣地', '襄阳市', 112.0782310, 32.0016830, 20000, '5A', '08:00-17:30', 87.00),
('襄阳古城', '中国最完整的古城墙防御建筑', '襄阳市', 112.1788250, 32.0409660, 25000, '4A', '全天开放', 0.00),
-- 神农架林区 (2个景区)
('神农架', '华中屋脊，自然生态保护区', '神农架林区', 110.6758730, 31.7445820, 30000, '5A', '08:30-17:30', 269.00),
('神农顶', '华中第一峰，金丝猴栖息地', '神农架林区', 110.3125600, 31.4450100, 12000, '5A', '07:00-17:00', 140.00),
-- 黄冈市 (2个景区)
('东坡赤壁', '苏东坡赤壁赋诞生地', '黄冈市', 114.8788540, 30.4430020, 8000, '4A', '08:00-17:30', 40.00),
('天堂寨', '大别山主峰之一，避暑胜地', '黄冈市', 115.7750230, 31.1427890, 15000, '4A', '07:00-17:00', 100.00),
-- 荆州市 (2个景区)
('荆州古城', '三国文化名城', '荆州市', 112.1910140, 30.3508100, 20000, '4A', '全天开放', 0.00),
('荆州方特', '东方神画主题乐园', '荆州市', 112.2480500, 30.2953700, 25000, '4A', '09:30-21:00', 280.00),
-- 咸宁市 (2个景区)
('赤壁古战场', '三国赤壁大战遗址', '咸宁市', 113.9520300, 29.7211400, 12000, '5A', '08:00-17:00', 150.00),
('九宫山', '道教名山，避暑胜地', '咸宁市', 114.5570120, 29.4162800, 10000, '4A', '全天开放', 75.00);

-- 为每个景区配置默认预警阈值
INSERT INTO `threshold_config` (`scenic_id`, `yellow_percent`, `red_percent`) VALUES
(1, 80.00, 100.00),
(2, 85.00, 100.00),
(3, 80.00, 100.00),
(4, 80.00, 100.00),
(5, 80.00, 100.00),
(6, 80.00, 100.00),
(7, 80.00, 100.00),
(8, 80.00, 100.00),
(9, 80.00, 100.00),
(10, 80.00, 100.00),
(11, 80.00, 100.00),
(12, 80.00, 100.00),
(13, 80.00, 100.00),
(14, 80.00, 100.00),
(15, 75.00, 95.00),
(16, 75.00, 95.00),
(17, 80.00, 100.00),
(18, 80.00, 100.00),
(19, 80.00, 100.00),
(20, 80.00, 100.00),
(21, 80.00, 100.00),
(22, 80.00, 100.00);
