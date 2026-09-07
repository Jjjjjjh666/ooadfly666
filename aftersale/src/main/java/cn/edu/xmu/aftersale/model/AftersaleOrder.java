package cn.edu.xmu.aftersale.model;

import cn.edu.xmu.aftersale.dao.po.AftersaleOrderPo;
import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 售后单领域对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AftersaleOrder {
    
    private Long id;
    private Long shopId;
    private Long orderId;
    private Long customerId;
    private Long productId;
    private Integer type; // 0-退货 1-换货 2-维修
    private AftersaleStatus status;
    private String reason;
    private String conclusion;
    /** 客户寄回商品的运单ID */
    private Long expressId;
    /** 商家发货（换货/返件）的运单ID */
    private Long returnExpressId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从PO构建领域对象
     */
    public static AftersaleOrder fromPo(AftersaleOrderPo po) {
        if (po == null) {
            return null;
        }
        return AftersaleOrder.builder()
                .id(po.getId())
                .shopId(po.getShopId())
                .orderId(po.getOrderId())
                .customerId(po.getCustomerId())
                .productId(po.getProductId())
                .type(po.getType())
                .status(convertStatus(po.getStatus()))
                .reason(po.getReason())
                .conclusion(po.getConclusion())
                .expressId(po.getExpressId())
                .returnExpressId(po.getReturnExpressId())
                .createdAt(po.getGmtCreate())
                .updatedAt(po.getGmtModified())
                .build();
    }

    /**
     * 转换为PO
     */
    public AftersaleOrderPo toPo() {
        AftersaleOrderPo po = new AftersaleOrderPo();
        po.setId(this.id);
        po.setShopId(this.shopId);
        po.setOrderId(this.orderId);
        po.setCustomerId(this.customerId);
        po.setProductId(this.productId);
        po.setType(this.type);
        po.setStatus(convertStatusToInt(this.status));
        po.setReason(this.reason);
        po.setConclusion(this.conclusion);
        po.setExpressId(this.expressId);
        po.setReturnExpressId(this.returnExpressId);
        po.setGmtCreate(this.createdAt);
        po.setGmtModified(this.updatedAt);
        return po;
    }

    /**
     * 将数据库状态码转换为状态枚举
     */
    private static AftersaleStatus convertStatus(Integer statusCode) {
        if (statusCode == null) {
            return AftersaleStatus.PENDING;
        }
        return switch (statusCode) {
            case 0 -> AftersaleStatus.PENDING;           // 待审核
            case 1 -> AftersaleStatus.TO_BE_RECEIVED;    // 待验收
            case 2 -> AftersaleStatus.TO_BE_COMPLETED;   // 待完成
            case 3 -> AftersaleStatus.RECEIVED;          // 已验收
            case 4 -> AftersaleStatus.REJECTED;          // 已拒绝
            case 5 -> AftersaleStatus.COMPLETED;         // 已完成
            case 6 -> AftersaleStatus.CANCELLED;         // 已取消
            default -> AftersaleStatus.PENDING;
        };
    }

    /**
     * 将状态枚举转换为数据库状态码
     */
    private static Integer convertStatusToInt(AftersaleStatus status) {
        if (status == null) {
            return 0;
        }
        return switch (status) {
            case PENDING -> 0;
            case TO_BE_RECEIVED -> 1;
            case TO_BE_COMPLETED -> 2;
            case RECEIVED -> 3;
            case REJECTED -> 4;
            case COMPLETED -> 5;
            case CANCELLED -> 6;
        };
    }

    /** 检查是否为待审核 */
    public void checkPendingStatus() {
        if (!AftersaleStatus.PENDING.equals(this.status)) {
            throw new BusinessException(ReturnNo.AFTERSALE_STATE_INVALID,
                    "只有待审核状态的售后单才能进行审核");
        }
    }

    /** 检查是否可以取消（待验收或待完成状态） */
    public void checkCanCancel() {
        if (!this.status.canCancel()) {
            throw new BusinessException(ReturnNo.AFTERSALE_STATE_INVALID,
                    "只有待验收或待完成状态的售后单才能取消");
        }
    }

    /** 审核通过（退货/换货），转换为待验收 */
    public void approveToBeReceived(String conclusion) {
        this.status = AftersaleStatus.TO_BE_RECEIVED;
        this.conclusion = conclusion;
        this.updatedAt = LocalDateTime.now();
    }

    /** 审核通过（维修），转换为待完成 */
    public void approveToBeCompleted(String conclusion) {
        this.status = AftersaleStatus.TO_BE_COMPLETED;
        this.conclusion = conclusion;
        this.updatedAt = LocalDateTime.now();
    }

    /** 更新为已取消 */
    public void cancel() {
        this.status = AftersaleStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /** 审核拒绝 */
    public void reject(String conclusion) {
        this.status = AftersaleStatus.REJECTED;
        this.conclusion = conclusion;
        this.updatedAt = LocalDateTime.now();
    }

    /** 验收前状态校验 */
    public void checkCanReceive() {
        if (!AftersaleStatus.TO_BE_RECEIVED.equals(this.status)) {
            throw new BusinessException(ReturnNo.AFTERSALE_STATE_INVALID,
                    "只有待验收状态的售后单才能进行验收");
        }
    }

    /** 已验收处理前状态校验 */
    public void checkCanProcessReceived() {
        if (!AftersaleStatus.RECEIVED.equals(this.status)) {
            throw new BusinessException(ReturnNo.AFTERSALE_STATE_INVALID,
                    "只有已验收状态的售后单才能进行后续处理");
        }
    }

    /** 验收通过，转为已验收 */
    public void accept(String conclusion) {
        this.status = AftersaleStatus.RECEIVED;
        this.conclusion = conclusion;
        this.updatedAt = LocalDateTime.now();
    }

    /** 完成售后单 */
    public void complete(String conclusion) {
        this.status = AftersaleStatus.COMPLETED;
        this.conclusion = conclusion;
        this.updatedAt = LocalDateTime.now();
    }

    /** 获取售后类型 */
    public AftersaleType getAftersaleType() {
        return AftersaleType.valueOf(this.type);
    }
}

