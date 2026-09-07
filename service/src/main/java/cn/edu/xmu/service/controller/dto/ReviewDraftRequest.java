package cn.edu.xmu.service.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核服务商变更请求
 */
@Data
public class ReviewDraftRequest {
    
    @NotNull(message = "conclusion不能为空")
    private Integer conclusion; // 0-拒绝 1-通过
    
    private String opinion; // 审核意见
}

