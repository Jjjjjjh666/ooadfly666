package cn.edu.xmu.service.dao.po;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 服务商持久化对象 - 对应数据库表 service_provider
 */
@Data
public class ServiceProviderPo {
    private Long id;
    private String name; // 服务商名称
    private String consignee; // 联系人
    private String address; // 地址
    private String mobile; // 电话
    private String status; // 状态：ACTIVE-活跃 INACTIVE-不活跃
    private LocalDateTime createdAt; // 对应数据库 created_at
    private LocalDateTime updatedAt; // 对应数据库 updated_at
}