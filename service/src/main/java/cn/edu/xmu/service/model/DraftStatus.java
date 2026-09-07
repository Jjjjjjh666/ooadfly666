package cn.edu.xmu.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 服务商草稿状态枚举
 */
@Getter
@AllArgsConstructor
public enum DraftStatus {
    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "审核通过"),
    REJECTED("REJECTED", "审核拒绝");

    private final String code;
    private final String description;

    public static DraftStatus of(String code) {
        for (DraftStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的草稿状态: " + code);
    }
}

