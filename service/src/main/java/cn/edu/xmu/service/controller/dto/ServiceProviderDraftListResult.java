package cn.edu.xmu.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 草稿列表查询返回体（放在ReturnObject.data内）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProviderDraftListResult {

    private List<ServiceProviderDraftListItem> list;

    private Integer page;

    @JsonProperty("pagesize")
    private Integer pageSize;

    private Long total;
}

