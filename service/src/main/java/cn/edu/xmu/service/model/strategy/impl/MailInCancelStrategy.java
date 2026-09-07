package cn.edu.xmu.service.model.strategy.impl;

import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import cn.edu.xmu.service.client.LogisticsClient;
import cn.edu.xmu.service.client.dto.CreatePackageRequest;
import cn.edu.xmu.service.client.dto.CreatePackageResponse;
import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderStatus;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.ServiceOrderCancelStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 寄修取消策略
 * 取消时需要取消对应的运单；若已收件，还需生成返件运单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailInCancelStrategy implements ServiceOrderCancelStrategy {

    private final LogisticsClient logisticsClient;

    @Override
    public void cancel(ServiceOrder order) {
        log.info("执行寄修取消策略: orderId={}, status={}", order.getId(), order.getStatus());
        ServiceOrderStatus beforeStatus = order.getStatus();

        // 取消服务单
        order.cancel();
        
        // 寄修取消特定逻辑
        log.info("寄修服务单已取消: orderId={}", order.getId());
        
        // 根据当前状态判断是否需要额外操作
        if (ServiceOrderStatus.RECEIVED.equals(beforeStatus)) {
            log.info("商品已收件，后续需要: 1.安排返件 2.通知客户");
            // 已收件：先生成返件运单
            Long returnExpressId = createReturnPackage(order);
            order.setReturnExpressId(returnExpressId);
            log.info("返件运单创建成功: orderId={}, returnExpressId={}", order.getId(), returnExpressId);

            // 仍尝试取消原寄件运单（如果有）
            if (order.getExpressId() != null) {
                cancelExpressPackage(order);
            }
        } else if (order.getExpressId() != null) {
            // 商品在途中，需要取消运单
            log.info("商品在途中，运单ID: {}, 需要取消运单", order.getExpressId());
            cancelExpressPackage(order);
        } else {
            log.info("商品未寄出，直接通知客户取消即可");
        }
    }

    @Override
    public boolean support(Integer type) {
        return ServiceOrderType.MAIL_IN_REPAIR.getCode().equals(type);
    }
    
    /**
     * 取消运单
     */
    private void cancelExpressPackage(ServiceOrder order) {
        try {
            log.info("准备取消运单: expressId={}", order.getExpressId());
            
            // 调用物流API取消运单
            logisticsClient.cancelPackage(
                    1L,  // shopId，实际应该从服务单获取
                    order.getExpressId(),
                    "Bearer token"  // 实际应该从上下文获取
            );
            
            log.info("运单取消成功: expressId={}", order.getExpressId());
        } catch (Exception e) {
            log.error("取消运单失败: expressId={}", order.getExpressId(), e);
            throw new RuntimeException("取消运单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 已收件时创建返件运单（服务商寄回给客户）
     */
    private Long createReturnPackage(ServiceOrder order) {
        try {
            CreatePackageRequest.AddressInfo address = CreatePackageRequest.AddressInfo.builder()
                    .name(order.getConsignee())
                    .mobile(order.getMobile())
                    .regionId(order.getRegionId())
                    .address(order.getAddress())
                    .build();

            CreatePackageRequest.CargoDetail cargo = CreatePackageRequest.CargoDetail.builder()
                    .id(order.getProductId())
                    .name("返件商品")
                    .count(1)
                    .unit("件")
                    .weight(1000)
                    .amount(0)
                    .build();

            CreatePackageRequest request = CreatePackageRequest.builder()
                    .contractId(0L)
                    .payMethod(2) // 收方付，服务商承担运费
                    .address(address)
                    .cargoDetails(List.of(cargo))
                    .build();

            Long shopId = order.getServiceProviderId() != null ? order.getServiceProviderId() : 1L;
            InternalReturnObject<CreatePackageResponse> response =
                    logisticsClient.createPackage(shopId, "Bearer token", request);
            CreatePackageResponse data = response.getData();
            return data.getId();
        } catch (Exception e) {
            log.error("创建返件运单失败: orderId={}", order.getId(), e);
            throw new RuntimeException("创建返件运单失败: " + e.getMessage(), e);
        }
    }
}
