-- =========================
-- 1) 创建数据库并切换
-- =========================
CREATE DATABASE IF NOT EXISTS service_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE service_db;

-- （可选）重建干净环境：解除注释会删表
-- DROP TABLE IF EXISTS service_order_express;
-- DROP TABLE IF EXISTS service_order;
-- DROP TABLE IF EXISTS service_provider_draft;
-- DROP TABLE IF EXISTS service_provider;

-- =========================
-- 2) 服务商表
-- =========================
CREATE TABLE IF NOT EXISTS service_provider (
                                                id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
                                                name VARCHAR(100) NOT NULL COMMENT '服务商名称',
    consignee VARCHAR(50) COMMENT '联系人',
    address VARCHAR(200) COMMENT '地址',
    mobile VARCHAR(20) COMMENT '电话',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE-活跃 INACTIVE-不活跃',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务商表';

-- =========================
-- 3) 服务商变更草稿表
-- =========================
CREATE TABLE IF NOT EXISTS service_provider_draft (
                                                      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '草稿ID',
                                                      service_provider_id BIGINT NOT NULL COMMENT '服务商ID',
                                                      provider_name VARCHAR(100) NOT NULL COMMENT '服务商名称',
    contact_person VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '地址',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING-待审核 APPROVED-审核通过 REJECTED-审核拒绝',
    opinion VARCHAR(500) COMMENT '审核意见',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_service_provider_id (service_provider_id),
    INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务商变更草稿表';

-- =========================
-- 4) 服务单表
-- =========================
CREATE TABLE IF NOT EXISTS `service_order` (
                                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '服务单主键',

                                               `type` TINYINT NOT NULL COMMENT '服务方式：0-上门服务，1-寄件服务',

                                               `consignee` VARCHAR(64) NOT NULL COMMENT '联系人姓名',
    `address` VARCHAR(255) NOT NULL COMMENT '联系人地址',
    `mobile` VARCHAR(32) NOT NULL COMMENT '联系电话',

    `status` INT NOT NULL COMMENT '服务单状态',
    `description` VARCHAR(255) NULL COMMENT '问题描述',

    `service_staff_id` BIGINT NULL COMMENT '维修师傅ID',
    `service_provider_id` BIGINT NULL COMMENT '服务商ID',
    `service_contract_id` BIGINT NULL COMMENT '服务合同ID',
    `service_id` BIGINT NULL COMMENT '服务ID',

    `customer_id` BIGINT NOT NULL COMMENT '顾客ID',
    `region_id` BIGINT NOT NULL COMMENT '地区ID',
    `product_id` BIGINT NOT NULL COMMENT '产品ID',
    `aftersale_id` BIGINT NOT NULL COMMENT '售后单ID',
    `express_id` BIGINT NULL COMMENT '运单ID(可选：你们原字段保留)',
    `return_express_id` BIGINT NULL COMMENT '返件运单ID',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_service_order_aftersale_id` (`aftersale_id`),
    KEY `idx_service_order_express_id` (`express_id`),
    KEY `idx_service_order_return_express_id` (`return_express_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务单表';

-- =========================
-- 5) 服务单-运单关联表（新增）
--    字段 = 创建运单API请求体字段 + service_order.id
--    +（建议）返回体字段，便于回填
-- =========================
CREATE TABLE IF NOT EXISTS `service_order_express` (
                                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联记录ID',
                                                       `service_order_id` BIGINT NOT NULL COMMENT '服务单ID(service_order.id)',
                                                       status ENUM('CREATED','CANCELED') NOT NULL DEFAULT 'CREATED' COMMENT '运单关联状态',


    -- ===== 请求体字段 =====
                                                       `contract_id` BIGINT NOT NULL COMMENT '物流合同ID(contractId)',
                                                       `pay_method`  INT    NOT NULL COMMENT '付费方式(payMethod)',

                                                       `name`        VARCHAR(64)  NOT NULL COMMENT '联系人(address.name)',
    `mobile`      VARCHAR(32)  NOT NULL COMMENT '电话(address.mobile)',
    `region_id`   BIGINT       NOT NULL COMMENT '地区ID(address.regionId)',
    `address`     VARCHAR(255) NOT NULL COMMENT '详细地址(address.address)',

    `cargo_details` JSON NULL COMMENT '货物明细(cargoDetails) JSON数组',

    -- ===== 返回体字段（回填用）=====
    `express_id`   BIGINT NULL COMMENT '运单ID(返回data.id)',
    `bill_code`    VARCHAR(64) NULL COMMENT '运单号(返回data.billCode)',
    `express_status` INT NULL COMMENT '运单状态(返回data.status)',

    -- ===== 内部调用辅助字段（建议保留，方便重试/排错）=====
    `call_status` INT NOT NULL DEFAULT 0 COMMENT '内部调用状态 0-未调用 1-成功 2-失败',
    `fail_reason` VARCHAR(255) NULL COMMENT '失败原因(记录errmsg/异常信息)',

    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_soe_service_order_id` (`service_order_id`),
    KEY `idx_soe_express_id` (`express_id`),
    KEY `idx_soe_bill_code` (`bill_code`),

    CONSTRAINT `fk_soe_service_order`
    FOREIGN KEY (`service_order_id`) REFERENCES `service_order`(`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务单-运单关联表';

-- =========================
-- 6) 测试数据（与你现有一致）
-- =========================
INSERT INTO service_provider (id, name, consignee, address, mobile, status, created_at, updated_at) VALUES
                                                                                                        (1, '张三维修服务', '张三', '北京市朝阳区XX街道', '13800138000', 'ACTIVE', NOW(), NOW()),
                                                                                                        (2, '李四售后服务', '李四', '上海市浦东新区YY路', '13900139000', 'ACTIVE', NOW(), NOW()),
                                                                                                        (3, '王五技术服务', '王五', '深圳市南山区ZZ大道', '13700137000', 'ACTIVE', NOW(), NOW())
    ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO service_provider_draft (id, service_provider_id, provider_name, contact_person, contact_phone, address, status, created_at, updated_at) VALUES
                                                                                                                                                        (1, 1, '张三维修服务', '张三', '13800138000', '北京市朝阳区XX街道', 'PENDING', NOW(), NOW()),
                                                                                                                                                        (2, 2, '李四售后服务', '李四', '13900139000', '上海市浦东新区YY路', 'PENDING', NOW(), NOW()),
                                                                                                                                                        (3, 3, '王五技术服务', '王五', '13700137000', '深圳市南山区ZZ大道', 'APPROVED', NOW(), NOW())
    ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO service_order
(type, consignee, address, mobile, status, description,
 service_staff_id, service_provider_id, service_contract_id, service_id,
 customer_id, region_id, product_id, aftersale_id, express_id)
VALUES
    (0, '张三', '厦门市思明区软件园一期', '13800000001', 0, '上门服务-已创建',
     NULL, 101, 201, 301, 1001, 350203, 5001, 6001, NULL),

    (0, '张三', '厦门市思明区软件园一期', '13800000001', 1, '上门服务-已派单',
     9001, 101, 201, 301, 1001, 350203, 5001, 6002, NULL),

    (0, '李四', '厦门市湖里区高新园区', '13800000002', 2, '上门服务-服务中',
     9002, 102, 202, 302, 1002, 350206, 5002, 6003, NULL),

    (0, '李四', '厦门市湖里区高新园区', '13800000002', 3, '上门服务-已完成',
     9002, 102, 202, 302, 1002, 350206, 5002, 6004, NULL),

    (0, '王五', '厦门市集美区大学城', '13800000003', 4, '上门服务-已取消',
     NULL, 103, 203, 303, 1003, 350211, 5003, 6005, NULL),

    (1, '赵六', '福州市鼓楼区五一广场', '13800000004', 0, '寄件服务-已创建',
     NULL, 104, 204, 304, 1004, 350102, 5004, 6006, 7001),

    (1, '赵六', '福州市鼓楼区五一广场', '13800000004', 1, '寄件服务-已派单',
     9003, 104, 204, 304, 1004, 350102, 5004, 6007, 7002),

    (1, '孙七', '泉州市丰泽区东海街道', '13800000005', 2, '寄件服务-服务中',
     9004, 105, 205, 305, 1005, 350503, 5005, 6008, 7003),

    (1, '孙七', '泉州市丰泽区东海街道', '13800000005', 3, '寄件服务-已完成',
     9004, 105, 205, 305, 1005, 350503, 5005, 6009, 7004),

    (1, '周八', '漳州市芗城区胜利路', '13800000006', 4, '寄件服务-已取消',
     NULL, 106, 206, 306, 1006, 350602, 5006, 6010, 7005);
