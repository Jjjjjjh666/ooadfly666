package cn.edu.xmu.aftersale.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 售后单类型枚举
 * 对应数据库：0-换货 1-退货 2-维修
 */
@Getter
@AllArgsConstructor
public enum AftersaleType {
    EXCHANGE(0, "换货"),
    RETURN(1, "退货"),
    REPAIR(2, "维修");

    private final Integer code;
    private final String description;

    public static AftersaleType valueOf(Integer code) {
        for (AftersaleType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的售后类型: " + code);
    }
}

