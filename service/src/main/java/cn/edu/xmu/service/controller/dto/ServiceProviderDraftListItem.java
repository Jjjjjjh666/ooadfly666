package cn.edu.xmu.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 服务商草稿列表项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProviderDraftListItem {

    @JsonProperty("draftid")
    private Long draftId;

    @JsonProperty("serviceprovider")
    private ServiceProviderBrief serviceProvider;

    @JsonProperty("servicearea")
    private String serviceArea;

    @JsonProperty("changetime")
    private LocalDateTime changeTime;

    private String change;

    private String operation;
}

