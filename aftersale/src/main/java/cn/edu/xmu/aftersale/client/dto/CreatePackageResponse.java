package cn.edu.xmu.aftersale.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建运单响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePackageResponse {
    private Long id;           // 运单ID
    private String billCode;   // 运单号
    private Integer payMethod; // 支付方式
    private Integer status;    // 订单状态
}

