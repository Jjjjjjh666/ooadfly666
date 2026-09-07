package cn.edu.xmu.aftersale.model.strategy.impl;

import cn.edu.xmu.aftersale.client.LogisticsClient;
import cn.edu.xmu.aftersale.client.dto.CreatePackageRequest;
import cn.edu.xmu.aftersale.client.dto.CreatePackageResponse;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.AftersaleConfirmStrategy;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 换货审核策略
 * 审核通过后需要创建换货运单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeConfirmStrategy implements AftersaleConfirmStrategy {

    private final LogisticsClient logisticsClient;

    @Override
    public void confirm(AftersaleOrder order, Boolean confirm, String conclusion) {
        log.info("执行换货审核策略: orderId={}, confirm={}", order.getId(), confirm);
        
        if (Boolean.TRUE.equals(confirm)) {
            // 审核通过（换货），转换为待验收状态
            order.approveToBeReceived(conclusion != null ? conclusion : "同意换货");
            log.info("换货审核通过: orderId={}, 状态转换为待验收", order.getId());
            
            // 创建换货运单（客户寄回商品到商家）
            try {
                Long expressId = createExchangePackage(order);
                order.setExpressId(expressId);
                log.info("换货运单创建成功: aftersaleId={}, expressId={}", order.getId(), expressId);
            } catch (Exception e) {
                log.error("创建换货运单失败: aftersaleId={}", order.getId(), e);
                throw new RuntimeException("创建换货运单失败: " + e.getMessage(), e);
            }
        } else {
            // 审核不通过，转换为已拒绝状态
            order.reject(conclusion != null ? conclusion : "拒绝换货");
            log.info("换货审核拒绝: orderId={}, reason={}", order.getId(), conclusion);
        }
    }

    @Override
    public boolean support(Integer type) {
        return AftersaleType.EXCHANGE.getCode().equals(type);
    }
    
    /**
     * 创建换货运单
     * 客户寄回商品到商家仓库
     */
    private Long createExchangePackage(AftersaleOrder order) {
        // 构建收件地址（商家仓库地址）
        CreatePackageRequest.AddressInfo address = 
            CreatePackageRequest.AddressInfo.builder()
                .name("商家仓库")  // 实际应该从商家信息获取
                .mobile("400-888-8888")
                .regionId(100000L)  // 实际应该从商家仓库信息获取
                .address("商家仓库地址")
                .build();
        
        // 构建托寄物信息（换货商品）
        CreatePackageRequest.CargoDetail cargo = 
            CreatePackageRequest.CargoDetail.builder()
                .id(order.getId())
                .name("换货商品")
                .count(1)
                .unit("件")
                .weight(1000)  // 1kg，实际应该从产品信息获取
                .amount(0)     // 无价值申报
                .build();
        
        List<CreatePackageRequest.CargoDetail> cargoList = new ArrayList<>();
        cargoList.add(cargo);
        
        // 构建完整请求
        CreatePackageRequest request = CreatePackageRequest.builder()
                .contractId(0L)          // 0表示自动选择物流合同
                .payMethod(2)            // 2-收方付（商家承担运费）
                .address(address)
                .cargoDetails(cargoList)
                .build();
        
        // 调用物流API
        InternalReturnObject<CreatePackageResponse> response = 
            logisticsClient.createPackage(
                order.getShopId(),
                "Bearer token",  // 实际应该从上下文获取
                request
            );
        
        CreatePackageResponse packageData = response.getData();
        log.info("物流运单创建成功: packageId={}, billCode={}", 
                 packageData.getId(), packageData.getBillCode());
        
        return packageData.getId();
    }
}
