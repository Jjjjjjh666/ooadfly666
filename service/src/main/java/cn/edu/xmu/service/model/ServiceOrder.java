package cn.edu.xmu.service.model;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.service.dao.po.ServiceOrderPo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 服务单领域对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrder {
    
    private Long id;
    private Integer type; // 服务方式：0-上门服务，1-寄件服务
    private String consignee; // 联系人姓名
    private String address; // 联系人地址
    private String mobile; // 联系电话
    private ServiceOrderStatus status; // 服务单状态
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从PO构建领域对象
     */
    public static ServiceOrder fromPo(ServiceOrderPo po) {
        if (po == null) {
            return null;
        }
        return ServiceOrder.builder()
                .id(po.getId())
                .type(po.getType())
                .consignee(po.getConsignee())
                .address(po.getAddress())
                .mobile(po.getMobile())
                .status(convertStatusFromInt(po.getStatus()))
                .description(po.getDescription())
                .serviceStaffId(po.getServiceStaffId())
                .serviceProviderId(po.getServiceProviderId())
                .serviceContractId(po.getServiceContractId())
                .serviceId(po.getServiceId())
                .customerId(po.getCustomerId())
                .regionId(po.getRegionId())
                .productId(po.getProductId())
                .aftersaleId(po.getAftersaleId())
                .expressId(po.getExpressId())
                .returnExpressId(po.getReturnExpressId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    /**
     * 转换为PO
     */
    public ServiceOrderPo toPo() {
        ServiceOrderPo po = new ServiceOrderPo();
        po.setId(this.id);
        po.setType(this.type);
        po.setConsignee(this.consignee);
        po.setAddress(this.address);
        po.setMobile(this.mobile);
        po.setStatus(convertStatusToInt(this.status));
        po.setDescription(this.description);
        po.setServiceStaffId(this.serviceStaffId);
        po.setServiceProviderId(this.serviceProviderId);
        po.setServiceContractId(this.serviceContractId);
        po.setServiceId(this.serviceId);
        po.setCustomerId(this.customerId);
        po.setRegionId(this.regionId);
        po.setProductId(this.productId);
        po.setAftersaleId(this.aftersaleId);
        po.setExpressId(this.expressId);
        po.setReturnExpressId(this.returnExpressId);
        po.setCreatedAt(this.createdAt);
        po.setUpdatedAt(this.updatedAt);
        return po;
    }

    /**
     * 将数据库状态码转换为状态枚举
     */
    private static ServiceOrderStatus convertStatusFromInt(Integer statusCode) {
        if (statusCode == null) {
            return ServiceOrderStatus.PENDING;
        }
        switch (statusCode) {
            case 0: return ServiceOrderStatus.PENDING;          // 待接收
            case 1: return ServiceOrderStatus.TO_BE_ASSIGNED;   // 待派工
            case 2: return ServiceOrderStatus.ASSIGNED;         // 已派工
            case 3: return ServiceOrderStatus.RECEIVED;         // 已收件
            case 4: return ServiceOrderStatus.REJECTED;         // 已拒绝
            case 5: return ServiceOrderStatus.COMPLETED;        // 已完成
            case 6: return ServiceOrderStatus.CANCELED;         // 已取消
            case 7: return ServiceOrderStatus.RETURNED;         // 已退回
            default: return ServiceOrderStatus.PENDING;
        }
    }

    /**
     * 将状态枚举转换为数据库状态码
     */
    private static Integer convertStatusToInt(ServiceOrderStatus status) {
        if (status == null) {
            return 0;
        }
        switch (status) {
            case PENDING: return 0;
            case TO_BE_ASSIGNED: return 1;
            case ASSIGNED: return 2;
            case RECEIVED: return 3;
            case REJECTED: return 4;
            case COMPLETED: return 5;
            case CANCELED: return 6;
            case RETURNED: return 7;
            default: return 0;
        }
    }

    /**
     * 检查是否为待接收状态
     */
    public void checkPendingStatus() {
        if (!ServiceOrderStatus.PENDING.equals(this.status)) {
            throw new BusinessException(ReturnNo.STATENOTALLOW, 
                    "只有待接收状态的服务单才能进行审核");
        }
    }

    /**
     * 检查是否为待派工状态
     */
    public void checkToBeAssignedStatus() {
        if (!ServiceOrderStatus.TO_BE_ASSIGNED.equals(this.status)) {
            throw new BusinessException(ReturnNo.STATENOTALLOW, 
                    "只有待派工状态的服务单才能派工");
        }
    }

    /**
     * 检查是否为已派工状态（寄修场景）
     */
    public void checkAssignedStatus() {
        if (!ServiceOrderStatus.ASSIGNED.equals(this.status)) {
            throw new BusinessException(ReturnNo.STATENOTALLOW, 
                    "只有已派工状态的服务单才能验收收件");
        }
    }

    /**
     * 检查是否可以取消
     */
    public void checkCanCancel() {
        if (!this.status.canCancel()) {
            throw new BusinessException(ReturnNo.STATENOTALLOW, 
                    "只有待派工、已派工或已收件状态的服务单才能取消");
        }
    }

    /**
     * 审核通过，转换为待派工状态
     */
    public void approve(Long serviceProviderId) {
        this.serviceProviderId = serviceProviderId;
        this.status = ServiceOrderStatus.TO_BE_ASSIGNED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 审核不通过，转换为已拒绝状态
     */
    public void reject() {
        this.status = ServiceOrderStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 派工给维修师傅
     */
    public void assign(Long serviceStaffId) {
        this.serviceStaffId = serviceStaffId;
        this.status = ServiceOrderStatus.ASSIGNED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 验收收件（寄修场景）
     */
    public void receive() {
        this.status = ServiceOrderStatus.RECEIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 完成服务单
     */
    public void complete() {
        this.status = ServiceOrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消服务单
     */
    public void cancel() {
        this.status = ServiceOrderStatus.CANCELED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取服务类型枚举
     */
    public ServiceOrderType getServiceOrderType() {
        return ServiceOrderType.valueOf(this.type);
    }
}

