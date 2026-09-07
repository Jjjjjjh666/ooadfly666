package cn.edu.xmu.service.dao.po;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 服务商变更草稿持久化对象
 */
@Data
public class ServiceProviderDraftPo {
    private Long id;
    private Long serviceProviderId;
    private String providerName;
    private String contactPerson;
    private String contactPhone;
    private String address;
    private String status; // PENDING-待审核 APPROVED-审核通过 REJECTED-审核拒绝
    private String opinion; // 审核意见
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

