-- =========================
-- 1) 创建数据库并切换
-- =========================
CREATE DATABASE IF NOT EXISTS `aftersale_db`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `aftersale_db`;

-- （可选）重建表：解除注释会清空历史数据
-- DROP TABLE IF EXISTS `aftersale_packages`;
-- DROP TABLE IF EXISTS `aftersales`;

-- =========================
-- 2) 售后单表 aftersales
--    含新增字段：contact/mobile/region_id/address
-- =========================
CREATE TABLE IF NOT EXISTS `aftersales` (
    `id`               BIGINT NOT NULL AUTO_INCREMENT COMMENT '售后单ID',
    `shop_id`          BIGINT NOT NULL COMMENT '店铺ID',
    `order_id`         BIGINT NOT NULL COMMENT '订单ID',
    `order_item_id`    BIGINT NULL COMMENT '订单明细ID(可选)',
    `customer_id`      BIGINT NOT NULL COMMENT '顾客ID',
    `product_id`       BIGINT NOT NULL COMMENT '商品ID',
    `type`             INT NOT NULL COMMENT '售后类型 0-换货 1-退货 2-维修',
    `reason`           VARCHAR(255) NULL COMMENT '原因',
    -- 状态：0-待审核 1-待寄回 2-待完成 3-待收货 4-待处理 5-已拒绝 6-已完成 7-已取消
    `status`           INT NOT NULL DEFAULT 0 COMMENT '状态',
    `conclusion`       VARCHAR(255) NULL COMMENT '审核结论/备注',
    `express_id`       BIGINT NULL COMMENT '客户寄回商品的运单ID',
    `return_express_id` BIGINT NULL COMMENT '商家发货（换货）的运单ID',

    -- 新增：取货/联系信息
    `contact`       VARCHAR(64)  NULL COMMENT '联系人',
    `mobile`        VARCHAR(32)  NULL COMMENT '联系电话',
    `region_id`     BIGINT       NULL COMMENT '取货地区id',
    `address`       VARCHAR(255) NULL COMMENT '取货地址',

    `gmt_create`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_shop_id` (`shop_id`),
    KEY `idx_shop_order` (`shop_id`, `order_id`),
    KEY `idx_aftersales_region_id` (`region_id`),
    KEY `idx_aftersales_mobile` (`mobile`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='售后单表';

-- =========================
-- 3) 售后单-运单关联表 aftersale_express
--    字段 = 创建运单内部API要求字段 + aftersale_id
-- =========================
CREATE TABLE IF NOT EXISTS `aftersale_express` (
                                     `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联记录ID',
                                     `aftersale_id` BIGINT NOT NULL COMMENT '售后单ID(aftersales.id)',
                                     status ENUM('CREATED','CANCELED') NOT NULL DEFAULT 'CREATED' COMMENT '运单关联状态',


    -- ===== 请求体：contractId / payMethod =====
                                     `contract_id`  BIGINT NOT NULL COMMENT '物流合同ID(contractId)',
                                     `pay_method`   INT    NOT NULL COMMENT '付费方式(payMethod)',

    -- ===== 请求体：address =====
                                     `name`         VARCHAR(64)  NOT NULL COMMENT '联系人(address.name)',
                                     `mobile`       VARCHAR(32)  NOT NULL COMMENT '电话(address.mobile)',
                                     `region_id`    BIGINT       NOT NULL COMMENT '地区ID(address.regionId)',
                                     `address`      VARCHAR(255) NOT NULL COMMENT '详细地址(address.address)',

    -- ===== 请求体：cargoDetails（数组）=====
                                     `cargo_details` JSON NULL COMMENT '货物明细(cargoDetails) JSON数组',

    -- ===== 返回体 data：id / billCode / payMethod / status =====
                                     `express_id`   BIGINT NULL COMMENT '运单ID(返回data.id)',
                                     `bill_code`    VARCHAR(64) NULL COMMENT '运单号(返回data.billCode)',
                                     `express_status` INT NULL COMMENT '运单状态(返回data.status)',

    -- 内部调用的状态（你们自己用，便于重试/排错）
                                     `call_status`  INT NOT NULL DEFAULT 0 COMMENT '内部调用状态 0-未调用 1-成功 2-失败',
                                     `fail_reason`  VARCHAR(255) NULL COMMENT '失败原因(记录errmsg/异常信息)',

                                     `gmt_create`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                                     PRIMARY KEY (`id`),
                                     KEY `idx_ae_aftersale_id` (`aftersale_id`),
                                     KEY `idx_ae_express_id` (`express_id`),
                                     KEY `idx_ae_bill_code` (`bill_code`),

                                     CONSTRAINT `fk_ae_aftersale`
                                         FOREIGN KEY (`aftersale_id`) REFERENCES `aftersales`(`id`)
                                             ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='售后单-运单关联表(请求体+返回体+aftersale_id)';

-- =========================
-- 4) （可选）插入测试数据
-- =========================
-- TRUNCATE TABLE `aftersale_packages`;
-- TRUNCATE TABLE `aftersales`;

INSERT INTO `aftersales`
(`shop_id`,`order_id`,`order_item_id`,`customer_id`,`product_id`,`type`,`reason`,`status`,`conclusion`,
 `contact`,`mobile`,`region_id`,`address`)
VALUES
    (1, 20001, 60001, 21001, 31001, 0, '尺码不合适，申请换货', 0, NULL, '张三', '13800000001', 6, '福建省厦门市思明区xx路1号'),
    (1, 20002, 60002, 21002, 31002, 1, '商品与描述不符，申请退货', 0, NULL, '李四', '13800000002', 6, '福建省厦门市思明区xx路2号'),
    (1, 20003, 60003, 21003, 31003, 2, '设备无法开机，申请维修', 0, NULL, '王五', '13800000003', 6, '福建省厦门市思明区xx路3号'),
    (2, 20004, 60004, 21004, 31004, 1, '商品破损', 1, '同意退货退款', '赵六', '13800000004', 8, '福建省厦门市湖里区yy路4号'),
    (2, 20005, NULL,  21005, 31005, 0, '不喜欢款式', 2, '非质量问题不支持换货', '孙七', '13800000005', 8, '福建省厦门市湖里区yy路5号');
