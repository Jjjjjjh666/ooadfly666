package cn.edu.xmu.service.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 草稿历史查询返回体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProviderDraftHistoryResponse {
    private List<String> history;
}

