package cn.edu.xmu.aftersale.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 售后单状态枚举
 * 0-PENDING-待审核
 * 1-TO_BE_RECEIVED-待验收
 * 2-TO_BE_COMPLETED-待完成
 * 3-RECEIVED-已验收
 * 4-REJECTED-已拒绝
 * 5-COMPLETED-已完成
 * 6-CANCELLED-已取消
 */
@Getter
@AllArgsConstructor
public enum AftersaleStatus {
    PENDING("PENDING", "待审核"),
    TO_BE_RECEIVED("TO_BE_RECEIVED", "待验收"),
    TO_BE_COMPLETED("TO_BE_COMPLETED", "待完成"),
    RECEIVED("RECEIVED", "已验收"),
    REJECTED("REJECTED", "已拒绝"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    public static AftersaleStatus of(String code) {
        for (AftersaleStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的售后状态: " + code);
    }

    /**
     * 是否终态
     */
    public boolean isTerminal() {
        return this == REJECTED || this == COMPLETED || this == CANCELLED;
    }

    /**
     * 是否可取消
     */
    public boolean canCancel() {
        return this == TO_BE_RECEIVED || this == TO_BE_COMPLETED;
    }
}

