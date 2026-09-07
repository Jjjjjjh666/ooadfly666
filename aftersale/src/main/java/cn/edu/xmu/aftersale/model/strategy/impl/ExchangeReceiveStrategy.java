package cn.edu.xmu.aftersale.model.strategy.impl;

import cn.edu.xmu.aftersale.client.LogisticsClient;
import cn.edu.xmu.aftersale.client.dto.CreatePackageRequest;
import cn.edu.xmu.aftersale.client.dto.CreatePackageResponse;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.AftersaleProcessStrategy;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 已验收换货的处理策略：发出替换商品并完成
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeReceiveStrategy implements AftersaleProcessStrategy {

    private final LogisticsClient logisticsClient;

    @Override
    public void process(AftersaleOrder order, String conclusion) {
        log.info("处理已验收换货: orderId={}", order.getId());
        Long expressId = createOutboundPackage(order);
        order.setReturnExpressId(expressId);
        order.complete(conclusion != null ? conclusion : "换货已验收，发货完成");
        log.info("换货处理完成: orderId={}, outboundExpressId={}", order.getId(), expressId);
    }

    @Override
    public boolean support(Integer type) {
        return AftersaleType.EXCHANGE.getCode().equals(type);
    }

    /** 创建换货发货运单（商家发货给顾客） */
    private Long createOutboundPackage(AftersaleOrder order) {
        CreatePackageRequest.AddressInfo address = CreatePackageRequest.AddressInfo.builder()
                .name("商家仓库")
                .mobile("400-888-8888")
                .regionId(100000L)
                .address("商家仓库地址")
                .build();

        CreatePackageRequest.CargoDetail cargo = CreatePackageRequest.CargoDetail.builder()
                .id(order.getProductId())
                .name("换货商品")
                .count(1)
                .unit("件")
                .weight(1000)
                .amount(0)
                .build();

        CreatePackageRequest request = CreatePackageRequest.builder()
                .contractId(0L)
                .payMethod(2) // 收方付
                .address(address)
                .cargoDetails(List.of(cargo))
                .build();

        InternalReturnObject<CreatePackageResponse> response =
                logisticsClient.createPackage(order.getShopId(), "Bearer token", request);
        return response.getData().getId();
    }
}

