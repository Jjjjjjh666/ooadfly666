package cn.edu.xmu.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 服务单状态枚举
 * 0-PENDING-待接收
 * 1-TO_BE_ASSIGNED-待派工
 * 2-ASSIGNED-已派工
 * 3-RECEIVED-已收件
 * 4-REJECTED-已拒绝
 * 5-COMPLETED-已完成
 * 6-CANCELED-已取消
 * 7-RETURNED-已退回
 */
@Getter
@AllArgsConstructor
public enum ServiceOrderStatus {
    PENDING("PENDING", "待接收"),
    TO_BE_ASSIGNED("TO_BE_ASSIGNED", "待派工"),
    ASSIGNED("ASSIGNED", "已派工"),
    RECEIVED("RECEIVED", "已收件"),
    REJECTED("REJECTED", "已拒绝"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELED("CANCELED", "已取消"),
    RETURNED("RETURNED", "已退回");

    private final String code;
    private final String description;

    public static ServiceOrderStatus of(String code) {
        for (ServiceOrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的服务单状态: " + code);
    }
    
    /**
     * 判断是否为终态
     */
    public boolean isTerminal() {
        return this == REJECTED || this == COMPLETED || this == CANCELED || this == RETURNED;
    }
    
    /**
     * 判断是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING || this == TO_BE_ASSIGNED || this == ASSIGNED || this == RECEIVED;
    }
}

