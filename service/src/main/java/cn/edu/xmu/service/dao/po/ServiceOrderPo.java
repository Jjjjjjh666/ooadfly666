package cn.edu.xmu.service.dao.po;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 服务单持久化对象 - 对应数据库表 service_order
 * 注意：数据库字段是 created_at/updated_at，status是INT类型
 */
@Data
public class ServiceOrderPo {
    private Long id;
    private Integer type; // 服务方式：0-上门服务，1-寄件服务
    private String consignee; // 联系人姓名
    private String address; // 联系人地址
    private String mobile; // 联系电话
    private Integer status; // 服务单状态（INT类型）
    private String description; // 问题描述
    private Long serviceStaffId; // 维修师傅ID
    private Long serviceProviderId; // 服务商ID
    private Long serviceContractId; // 服务合同ID
    private Long serviceId; // 服务ID
    private Long customerId; // 顾客ID
    private Long regionId; // 地区ID
    private Long productId; // 产品ID
    private Long aftersaleId; // 售后单ID
    private Long expressId; // 运单ID（寄出）
    private Long returnExpressId; // 返件运单ID
    private LocalDateTime createdAt; // 对应数据库 created_at
    private LocalDateTime updatedAt; // 对应数据库 updated_at
}

