-- ============================================================
-- 酒店辅助订购系统 — 数据库初始化脚本
-- 目标库: hotel_db (application-dev.yml 中 createDatabaseIfNotExist=true 会自动创建)
-- ============================================================

-- ============================================================
-- 1. 字典/基础数据表
-- ============================================================

CREATE TABLE IF NOT EXISTS t_country (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    code        VARCHAR(10)  NOT NULL COMMENT '国家代号，如CN',
    name_cn     VARCHAR(100) NOT NULL COMMENT '国家中文名',
    name_en     VARCHAR(100) COMMENT '国家英文名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国家';

CREATE TABLE IF NOT EXISTS t_city (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    country_id  BIGINT       NOT NULL COMMENT '国家ID',
    name_cn     VARCHAR(100) NOT NULL COMMENT '城市中文名',
    name_en     VARCHAR(100) COMMENT '城市英文名',
    code        VARCHAR(20)  COMMENT '城市代码',
    hot         TINYINT(1) DEFAULT 0 COMMENT '是否热门城市',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES t_country(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='城市';

CREATE TABLE IF NOT EXISTS t_bed_type (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '床型名称（大床/双床/单人床等）',
    code        VARCHAR(20)  COMMENT '床型代码',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='床型字典';

CREATE TABLE IF NOT EXISTS t_breakfast (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '早餐类型（无早/单早/双早等）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='早餐字典';

-- ============================================================
-- 2. 酒店核心表
-- ============================================================

CREATE TABLE IF NOT EXISTS t_hotel (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    city_id     BIGINT       NOT NULL COMMENT '城市ID',
    name_cn     VARCHAR(200) NOT NULL COMMENT '酒店中文名',
    name_en     VARCHAR(200) COMMENT '酒店英文名',
    address     VARCHAR(500) COMMENT '详细地址',
    longitude   DECIMAL(10,7) COMMENT '经度',
    latitude    DECIMAL(10,7) COMMENT '纬度',
    star_level  TINYINT      COMMENT '星级（1-5）',
    brand       VARCHAR(100) COMMENT '品牌',
    description TEXT         COMMENT '酒店描述',
    score       DECIMAL(2,1) DEFAULT 0 COMMENT '综合评分',
    status      TINYINT(1) DEFAULT 1 COMMENT '状态 1-上架 0-下架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_city (city_id),
    INDEX idx_score (score),
    INDEX idx_star (star_level),
    FOREIGN KEY (city_id) REFERENCES t_city(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店';

CREATE TABLE IF NOT EXISTS t_hotel_image (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id    BIGINT       NOT NULL COMMENT '酒店ID',
    url         VARCHAR(500) NOT NULL COMMENT '图片URL',
    type        TINYINT      DEFAULT 1 COMMENT '1-主图 2-外观 3-大堂 4-其他',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hotel_id) REFERENCES t_hotel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店图片';

CREATE TABLE IF NOT EXISTS t_hotel_facility (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT,
    hotel_id    BIGINT      NOT NULL COMMENT '酒店ID',
    name        VARCHAR(100) NOT NULL COMMENT '设施名称（WiFi/停车场/泳池等）',
    icon        VARCHAR(200) COMMENT '设施图标',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hotel_id) REFERENCES t_hotel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店设施';

-- ============================================================
-- 3. 房间相关表
-- ============================================================

CREATE TABLE IF NOT EXISTS t_room (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    hotel_id        BIGINT        NOT NULL COMMENT '酒店ID',
    name            VARCHAR(200)  NOT NULL COMMENT '房型名称',
    bed_type_id     BIGINT        COMMENT '床型ID',
    breakfast_id    BIGINT        COMMENT '早餐ID',
    max_guests      INT DEFAULT 2 COMMENT '最大入住人数',
    area            VARCHAR(50)   COMMENT '房间面积',
    floor           VARCHAR(50)   COMMENT '楼层',
    price           DECIMAL(10,2) NOT NULL COMMENT '标准价格',
    cancelable      TINYINT(1) DEFAULT 1 COMMENT '是否可取消 1-是 0-否',
    cancel_penalty  DECIMAL(10,2) COMMENT '取消手续费',
    status          TINYINT(1) DEFAULT 1 COMMENT '1-可用 0-不可用',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_hotel (hotel_id),
    FOREIGN KEY (hotel_id) REFERENCES t_hotel(id),
    FOREIGN KEY (bed_type_id) REFERENCES t_bed_type(id),
    FOREIGN KEY (breakfast_id) REFERENCES t_breakfast(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房型';

CREATE TABLE IF NOT EXISTS t_room_image (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id     BIGINT       NOT NULL COMMENT '房型ID',
    url         VARCHAR(500) NOT NULL COMMENT '图片URL',
    sort_order  INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES t_room(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房型图片';

CREATE TABLE IF NOT EXISTS t_room_facility (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id     BIGINT       NOT NULL COMMENT '房型ID',
    name        VARCHAR(100) NOT NULL COMMENT '设施（空调/电视/浴缸等）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES t_room(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房型设施';

-- ============================================================
-- 4. 用户与权限表
-- ============================================================

CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone       VARCHAR(20) UNIQUE COMMENT '手机号',
    email       VARCHAR(100) UNIQUE COMMENT '邮箱',
    password    VARCHAR(200) COMMENT '密码（BCrypt加密）',
    nickname    VARCHAR(50)  COMMENT '昵称',
    avatar      VARCHAR(500) COMMENT '头像URL',
    status      TINYINT(1) DEFAULT 1 COMMENT '1-正常 0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS t_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '角色名（admin/supplier/user）',
    description VARCHAR(200) COMMENT '角色描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE IF NOT EXISTS t_permission (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL COMMENT '权限标识（如 order:list）',
    description VARCHAR(200) COMMENT '权限描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限';

CREATE TABLE IF NOT EXISTS t_user_role (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (role_id) REFERENCES t_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS t_role_permission (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES t_role(id),
    FOREIGN KEY (permission_id) REFERENCES t_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

-- ============================================================
-- 5. 订单表
-- ============================================================

CREATE TABLE IF NOT EXISTS t_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(32)   NOT NULL UNIQUE COMMENT '订单号',
    user_id         BIGINT        NOT NULL COMMENT '用户ID',
    hotel_id        BIGINT        NOT NULL COMMENT '酒店ID',
    check_in_date   DATE          NOT NULL COMMENT '入住日期',
    check_out_date  DATE          NOT NULL COMMENT '退房日期',
    room_count      INT DEFAULT 1 COMMENT '房间数量',
    guest_name      VARCHAR(50)   COMMENT '入住人姓名',
    guest_phone     VARCHAR(20)   COMMENT '入住人手机号',
    total_amount    DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    status          TINYINT       NOT NULL COMMENT '状态：0-待支付 1-已支付 2-已取消 3-已入住 4-退房申请中 5-已退房 6-已完成',
    pay_time        DATETIME      COMMENT '支付时间',
    cancel_time     DATETIME      COMMENT '取消时间',
    cancel_amount   DECIMAL(10,2) COMMENT '预取消返回金额',
    cancel_confirm_id VARCHAR(50)  COMMENT '取消确认ID',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_order_no (order_no),
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (hotel_id) REFERENCES t_hotel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

CREATE TABLE IF NOT EXISTS t_order_item (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT        NOT NULL COMMENT '订单ID',
    room_id         BIGINT        NOT NULL COMMENT '房型ID',
    room_name       VARCHAR(200)  COMMENT '房型名称（冗余）',
    price           DECIMAL(10,2) NOT NULL COMMENT '单价',
    quantity        INT DEFAULT 1 COMMENT '数量',
    subtotal        DECIMAL(10,2) NOT NULL COMMENT '小计',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES t_order(id),
    FOREIGN KEY (room_id) REFERENCES t_room(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

CREATE TABLE IF NOT EXISTS t_payment (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT        NOT NULL COMMENT '订单ID',
    trade_no        VARCHAR(64)   COMMENT '支付宝交易号',
    amount          DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status          TINYINT       COMMENT '0-待支付 1-成功 2-失败',
    pay_method      VARCHAR(20) DEFAULT 'ALIPAY' COMMENT '支付方式',
    pay_time        DATETIME      COMMENT '支付时间',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES t_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录';

-- ============================================================
-- 6. 优惠券表
-- ============================================================

CREATE TABLE IF NOT EXISTS t_coupon (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    name              VARCHAR(100)   NOT NULL COMMENT '优惠券名称',
    receive_code      VARCHAR(10)    UNIQUE COMMENT '兑换码',
    description       VARCHAR(255)   COMMENT '说明',
    discount_amount   DECIMAL(10,2)  NOT NULL COMMENT '优惠金额',
    threshold_amount  DECIMAL(10,2) DEFAULT 0 COMMENT '使用门槛',
    total_num         INT            NOT NULL COMMENT '总库存',
    issue_num         INT DEFAULT 0  NOT NULL COMMENT '已发放数量',
    per_user_limit    INT DEFAULT 1  NOT NULL COMMENT '每人限领数量',
    status            TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    receive_start_time DATETIME      COMMENT '领取开始时间',
    receive_end_time   DATETIME      COMMENT '领取结束时间',
    valid_start_time   DATETIME      COMMENT '生效开始时间',
    valid_end_time     DATETIME      COMMENT '生效结束时间',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS t_user_coupon (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL COMMENT '用户ID',
    coupon_id         BIGINT        NOT NULL COMMENT '优惠券ID',
    coupon_name       VARCHAR(100)  NOT NULL COMMENT '优惠券名称快照',
    receive_code      VARCHAR(10)   NOT NULL COMMENT '兑换码快照',
    description       VARCHAR(255)  COMMENT '说明快照',
    discount_amount   DECIMAL(10,2) NOT NULL COMMENT '优惠金额快照',
    threshold_amount  DECIMAL(10,2) DEFAULT 0 COMMENT '门槛快照',
    status            TINYINT DEFAULT 0 COMMENT '状态：0-未使用 1-已使用 2-已过期',
    receive_time      DATETIME      NOT NULL COMMENT '领取时间',
    valid_start_time  DATETIME      COMMENT '生效开始时间',
    valid_end_time    DATETIME      COMMENT '生效结束时间',
    use_time          DATETIME      COMMENT '使用时间',
    create_time       DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_coupon_user (user_id),
    INDEX idx_user_coupon_coupon (coupon_id),
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (coupon_id) REFERENCES t_coupon(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';

-- ============================================================
-- 7. 评价表
-- ============================================================

CREATE TABLE IF NOT EXISTS t_review (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    hotel_id    BIGINT       NOT NULL COMMENT '酒店ID',
    order_id    BIGINT       COMMENT '关联订单ID',
    score       TINYINT      NOT NULL COMMENT '评分（1-5）',
    content     TEXT         COMMENT '评价内容',
    anonymous   TINYINT(1) DEFAULT 0 COMMENT '是否匿名评价',
    images      VARCHAR(1000) COMMENT '评价图片（JSON数组）',
    reply       TEXT         COMMENT '商家回复',
    reply_time  DATETIME     COMMENT '回复时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (hotel_id) REFERENCES t_hotel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店评价';

-- ============================================================
-- 8. 初始数据
-- ============================================================

-- 国家
INSERT IGNORE INTO t_country (id, code, name_cn, name_en) VALUES
(1, 'CN', '中国', 'China'),
(2, 'JP', '日本', 'Japan'),
(3, 'TH', '泰国', 'Thailand');

-- 城市
INSERT IGNORE INTO t_city (id, country_id, name_cn, name_en, code, hot) VALUES
(1, 1, '西安', 'Xi''an', 'XA', 1),
(2, 1, '北京', 'Beijing', 'BJ', 1),
(3, 1, '上海', 'Shanghai', 'SH', 1),
(4, 1, '成都', 'Chengdu', 'CD', 1),
(5, 2, '东京', 'Tokyo', 'TYO', 1),
(6, 3, '曼谷', 'Bangkok', 'BKK', 1);

-- 床型
INSERT IGNORE INTO t_bed_type (id, name, code) VALUES
(1, '大床', 'KING'),
(2, '双床', 'TWIN'),
(3, '单人床', 'SINGLE');

-- 早餐
INSERT IGNORE INTO t_breakfast (id, name) VALUES
(1, '无早'),
(2, '单早'),
(3, '双早');

-- 角色
INSERT IGNORE INTO t_role (id, name, description) VALUES
(1, 'admin', '超级管理员'),
(2, 'user', '普通用户');

-- 权限
INSERT IGNORE INTO t_permission (id, name, description) VALUES
(1, '*:*', '全部权限'),
(2, 'hotel:search', '酒店搜索'),
(3, 'hotel:detail', '酒店详情'),
(4, 'order:create', '创建订单'),
(5, 'order:cancel', '取消订单'),
(6, 'order:refund', '退房申请'),
(7, 'review:create', '发表评价'),
(8, 'user:info', '用户信息'),
(9, 'user:order', '我的订单');

-- 角色-权限关联
INSERT IGNORE INTO t_role_permission (role_id, permission_id) VALUES
(1, 1);  -- admin 拥有全部权限 (*:*)

INSERT IGNORE INTO t_role_permission (role_id, permission_id) VALUES
(2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9);

-- 管理员用户
INSERT IGNORE INTO t_user (id, phone, email, password, nickname, status) VALUES
(1, '17727974960', 'admin@hotel.com', '$2a$10$sW2vrKcSznSW6T7Nnmc0CenrVzscSkIoSjJrujdMu93gRPu1fsaoy', '系统管理员', 1),
(2, '13800000000', 'admin2@hotel.com', '$2a$10$hvKRGAa3izteCsWtRLQ7Q.Vzp0uKoTcBRFmdXj7NQWC6D5ot97E5i', '默认管理员', 1);

-- 管理员角色分配
INSERT IGNORE INTO t_user_role (user_id, role_id) VALUES (1, 1), (2, 1);

-- 示例优惠券（启动后会自动生成 receive_code）
INSERT IGNORE INTO t_coupon (id, name, receive_code, description, discount_amount, threshold_amount, total_num, issue_num, per_user_limit, status, receive_start_time, receive_end_time, valid_start_time, valid_end_time) VALUES
(1, '新客立减券', NULL, '满 300 元可用，立减 50 元', 50.00, 300.00, 5000, 0, 1, 1, '2026-01-01 00:00:00', '2030-12-31 23:59:59', '2026-01-01 00:00:00', '2030-12-31 23:59:59'),
(2, '周末精选券', NULL, '满 500 元可用，立减 80 元', 80.00, 500.00, 3000, 0, 2, 1, '2026-01-01 00:00:00', '2030-12-31 23:59:59', '2026-01-01 00:00:00', '2030-12-31 23:59:59');

SELECT id, name, receive_code, total_num, issue_num, per_user_limit, status
FROM t_coupon;

SHOW PROCESSLIST;
