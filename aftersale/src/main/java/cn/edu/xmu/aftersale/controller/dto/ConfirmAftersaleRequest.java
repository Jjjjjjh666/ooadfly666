package cn.edu.xmu.aftersale.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核售后请求
 */
@Data
public class ConfirmAftersaleRequest {
    
    @NotNull(message = "confirm不能为空")
    private Boolean confirm;
    
    private String conclusion;
}

