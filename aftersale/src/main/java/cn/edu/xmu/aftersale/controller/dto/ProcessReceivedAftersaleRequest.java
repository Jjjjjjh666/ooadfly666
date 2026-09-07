package cn.edu.xmu.aftersale.controller.dto;

import lombok.Data;

/**
 * 商户处理已验收商品请求
 */
@Data
public class ProcessReceivedAftersaleRequest {
    /** 处理备注/结论，可选 */
    private String conclusion;
}

