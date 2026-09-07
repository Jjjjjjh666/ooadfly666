package cn.edu.xmu.service.model.strategy.impl;

import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import cn.edu.xmu.service.client.LogisticsClient;
import cn.edu.xmu.service.client.dto.CreatePackageRequest;
import cn.edu.xmu.service.client.dto.CreatePackageResponse;
import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.ServiceOrderAssignStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 寄修派工策略
 * 寄修派工时需要生成运单，通知客户寄送商品
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailInAssignStrategy implements ServiceOrderAssignStrategy {

    private final LogisticsClient logisticsClient;

    @Override
    public void assign(ServiceOrder order, Long serviceStaffId) {
        log.info("执行寄修派工策略: orderId={}, serviceStaffId={}", order.getId(), serviceStaffId);
        
        // 1. 分配维修师傅
        order.assign(serviceStaffId);
        
        // 2. 生成运单（客户寄送商品到维修点）
        try {
            Long expressId = createPackageForMailIn(order);
            order.setExpressId(expressId);
            log.info("寄修运单创建成功: orderId={}, expressId={}", order.getId(), expressId);
        } catch (Exception e) {
            log.error("创建寄修运单失败: orderId={}", order.getId(), e);
            throw new RuntimeException("创建寄修运单失败: " + e.getMessage(), e);
        }
        
        log.info("寄修派工完成: orderId={}, serviceStaffId={}, expressId={}", 
                 order.getId(), serviceStaffId, order.getExpressId());
    }

    @Override
    public boolean support(Integer type) {
        return ServiceOrderType.MAIL_IN_REPAIR.getCode().equals(type);
    }
    
    /**
     * 创建寄修运单
     * 客户寄送商品到维修点
     */
    private Long createPackageForMailIn(ServiceOrder order) {
        // 构建发件地址（客户地址 - 从服务单获取）
        CreatePackageRequest.AddressInfo address = 
            CreatePackageRequest.AddressInfo.builder()
                .name(order.getConsignee())      // 客户姓名
                .mobile(order.getMobile())       // 客户电话
                .regionId(order.getRegionId())   // 客户地区
                .address(order.getAddress())     // 客户详细地址
                .build();
        
        // 构建托寄物信息
        CreatePackageRequest.CargoDetail cargo = 
            CreatePackageRequest.CargoDetail.builder()
                .id(order.getProductId())
                .name("待维修产品")
                .count(1)
                .unit("件")
                .weight(1000)  // 1kg，应该从产品信息获取
                .amount(0)     // 无价值申报
                .build();
        
        List<CreatePackageRequest.CargoDetail> cargoList = new ArrayList<>();
        cargoList.add(cargo);
        
        // 构建完整请求
        CreatePackageRequest request = CreatePackageRequest.builder()
                .contractId(0L)          // 0表示自动选择物流合同
                .payMethod(2)            // 2-收方付（服务商承担运费）
                .address(address)
                .cargoDetails(cargoList)
                .build();
        
        // 调用物流API
        InternalReturnObject<CreatePackageResponse> response = 
            logisticsClient.createPackage(
                1L,  // shopId，实际应该从服务单获取
                "Bearer token",  // 实际应该从上下文获取
                request
            );
        
        CreatePackageResponse packageData = response.getData();
        log.info("物流运单创建成功: packageId={}, billCode={}", 
                 packageData.getId(), packageData.getBillCode());
        
        return packageData.getId();
    }
}

