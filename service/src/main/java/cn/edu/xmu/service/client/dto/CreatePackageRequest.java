package cn.edu.xmu.service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建运单请求DTO
 * 对应oomall API: POST /internal/shops/{shopId}/packages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePackageRequest {
    
    /**
     * 物流合同ID，0由仓库优先级和物流优先级决定
     */
    private Long contractId;
    
    /**
     * 支付方式：1-寄方付 2-收方付
     */
    private Integer payMethod;
    
    /**
     * 收件人信息
     */
    private AddressInfo address;
    
    /**
     * 托寄物信息列表
     */
    private List<CargoDetail> cargoDetails;
    
    /**
     * 收发件人信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressInfo {
        private String name;      // 姓名
        private String mobile;    // 电话
        private Long regionId;    // 地区id
        private String address;   // 详细地址
    }
    
    /**
     * 托寄物信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CargoDetail {
        private Long id;          // 货物ID
        private String name;      // 货物名称
        private Integer count;    // 数量
        private String unit;      // 单位
        private Integer weight;   // 重量（克）
        private Integer amount;   // 单价（分）
    }
}

