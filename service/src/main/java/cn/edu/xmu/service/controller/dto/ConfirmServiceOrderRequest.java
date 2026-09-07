package cn.edu.xmu.service.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核服务单请求
 */
@Data
public class ConfirmServiceOrderRequest {
    
    @NotNull(message = "confirm不能为空")
    private Boolean confirm;
}

