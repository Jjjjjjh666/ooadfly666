package cn.edu.xmu.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简化的服务商信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProviderBrief {
    private Long id;
    private String name;

    @JsonProperty("providername")
    private String providerName;

    private String phone;
}

