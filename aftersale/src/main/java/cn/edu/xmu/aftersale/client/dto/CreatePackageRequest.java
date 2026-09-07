package cn.edu.xmu.aftersale.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建运单请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePackageRequest {
    
    private Long contractId;         // 物流合同ID，0=自动选择
    private Integer payMethod;        // 支付方式：1-寄方付 2-收方付
    private AddressInfo address;      // 收件人信息
    private List<CargoDetail> cargoDetails;  // 托寄物信息
    
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

