package cn.edu.xmu.service.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 派工服务单请求
 */
@Data
public class AssignServiceOrderRequest {
    
    @NotNull(message = "serviceStaffId不能为空")
    private Long serviceStaffId;
}

