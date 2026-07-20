package com.hotel.module.coupon.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class CouponSchemaBootstrapRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS t_coupon (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
                    receive_code VARCHAR(10) UNIQUE COMMENT '兑换码',
                    description VARCHAR(255) COMMENT '说明',
                    discount_amount DECIMAL(10,2) NOT NULL COMMENT '优惠金额',
                    threshold_amount DECIMAL(10,2) DEFAULT 0 COMMENT '使用门槛',
                    total_num INT NOT NULL COMMENT '总库存',
                    issue_num INT DEFAULT 0 NOT NULL COMMENT '已发放数量',
                    per_user_limit INT DEFAULT 1 NOT NULL COMMENT '每人限领数量',
                    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
                    receive_start_time DATETIME COMMENT '领取开始时间',
                    receive_end_time DATETIME COMMENT '领取结束时间',
                    valid_start_time DATETIME COMMENT '生效开始时间',
                    valid_end_time DATETIME COMMENT '生效结束时间',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板'
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS t_user_coupon (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL COMMENT '用户ID',
                    coupon_id BIGINT NOT NULL COMMENT '优惠券ID',
                    coupon_name VARCHAR(100) NOT NULL COMMENT '优惠券名称快照',
                    receive_code VARCHAR(10) NOT NULL COMMENT '兑换码快照',
                    description VARCHAR(255) COMMENT '说明快照',
                    discount_amount DECIMAL(10,2) NOT NULL COMMENT '优惠金额快照',
                    threshold_amount DECIMAL(10,2) DEFAULT 0 COMMENT '门槛快照',
                    status TINYINT DEFAULT 0 COMMENT '状态：0-未使用 1-已使用 2-已过期',
                    receive_time DATETIME NOT NULL COMMENT '领取时间',
                    valid_start_time DATETIME COMMENT '生效开始时间',
                    valid_end_time DATETIME COMMENT '生效结束时间',
                    use_time DATETIME COMMENT '使用时间',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_user_coupon_user (user_id),
                    INDEX idx_user_coupon_coupon (coupon_id),
                    CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES t_user(id),
                    CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES t_coupon(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券'
                """);

        jdbcTemplate.update("""
                INSERT IGNORE INTO t_coupon
                (id, name, receive_code, description, discount_amount, threshold_amount, total_num, issue_num, per_user_limit, status, receive_start_time, receive_end_time, valid_start_time, valid_end_time)
                VALUES
                (1, '新客立减券', NULL, '满 300 元可用，立减 50 元', 50.00, 300.00, 5000, 0, 1, 1, '2026-01-01 00:00:00', '2030-12-31 23:59:59', '2026-01-01 00:00:00', '2030-12-31 23:59:59'),
                (2, '周末精选券', NULL, '满 500 元可用，立减 80 元', 80.00, 500.00, 3000, 0, 2, 1, '2026-01-01 00:00:00', '2030-12-31 23:59:59', '2026-01-01 00:00:00', '2030-12-31 23:59:59')
                """);

        Integer orderTableExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 't_order'
                """,
                Integer.class
        );
        if (orderTableExists != null && orderTableExists > 0) {
            addColumnIfMissing("t_order", "user_coupon_id", "ALTER TABLE t_order ADD COLUMN user_coupon_id BIGINT NULL COMMENT '使用的用户优惠券ID'");
            addColumnIfMissing("t_order", "coupon_name", "ALTER TABLE t_order ADD COLUMN coupon_name VARCHAR(100) NULL COMMENT '优惠券名称快照'");
            addColumnIfMissing("t_order", "original_amount", "ALTER TABLE t_order ADD COLUMN original_amount DECIMAL(10,2) NULL COMMENT '优惠前金额'");
            addColumnIfMissing("t_order", "discount_amount", "ALTER TABLE t_order ADD COLUMN discount_amount DECIMAL(10,2) NULL COMMENT '优惠金额'");
        }

        log.info("Coupon schema bootstrap checked");
    }

    private void addColumnIfMissing(String tableName, String columnName, String alterSql) {
        Integer columnExists = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        if (columnExists == null || columnExists == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
