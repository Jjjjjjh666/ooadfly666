package cn.edu.xmu.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 服务单类型枚举
 */
@Getter
@AllArgsConstructor
public enum ServiceOrderType {
    ONSITE_REPAIR(0, "上门维修"),
    MAIL_IN_REPAIR(1, "寄修"),
    OTHER(2, "其他");

    private final Integer code;
    private final String description;

    public static ServiceOrderType valueOf(Integer code) {
        for (ServiceOrderType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的服务类型: " + code);
    }
}
