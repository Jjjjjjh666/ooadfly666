package cn.edu.xmu.aftersale.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商户验收请求
 */
@Data
public class AcceptAftersaleRequest {

    /** 是否验收通过 */
    @NotNull(message = "accept不能为空")
    private Boolean accept;

    /** 验收结论/备注 */
    private String conclusion;
}

