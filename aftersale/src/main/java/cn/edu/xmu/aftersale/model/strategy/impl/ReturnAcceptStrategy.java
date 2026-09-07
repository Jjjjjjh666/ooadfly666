package cn.edu.xmu.aftersale.model.strategy.impl;

import cn.edu.xmu.aftersale.client.LogisticsClient;
import cn.edu.xmu.aftersale.client.dto.CreatePackageRequest;
import cn.edu.xmu.aftersale.client.dto.CreatePackageResponse;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.AftersaleAcceptStrategy;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 退货验收策略：通过->已验收；不通过->已拒绝并生成返件运单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnAcceptStrategy implements AftersaleAcceptStrategy {

    private final LogisticsClient logisticsClient;

    @Override
    public void accept(AftersaleOrder order, Boolean accept, String conclusion) {
        log.info("执行退货验收策略: orderId={}, accept={}", order.getId(), accept);

        if (Boolean.TRUE.equals(accept)) {
            order.accept(conclusion != null ? conclusion : "验收通过");
            log.info("退货验收通过: orderId={}, 状态->已验收", order.getId());
        } else {
            // 验收不通过：生成返货运单，再置为已拒绝
            Long expressId = createReturnPackage(order);
            order.setReturnExpressId(expressId);
            order.reject(conclusion != null ? conclusion : "验收不通过，已安排返件");
            log.info("退货验收不通过: orderId={}, expressId={}", order.getId(), expressId);
        }
    }

    @Override
    public boolean support(Integer type) {
        return AftersaleType.RETURN.getCode().equals(type);
    }

    /** 创建返件运单（商家寄回给客户） */
    private Long createReturnPackage(AftersaleOrder order) {
        CreatePackageRequest.AddressInfo address = CreatePackageRequest.AddressInfo.builder()
                .name("客户收件人")
                .mobile("000-0000-0000")
                .regionId(100000L)
                .address("客户收件地址")
                .build();

        CreatePackageRequest.CargoDetail cargo = CreatePackageRequest.CargoDetail.builder()
                .id(order.getProductId())
                .name("退货返件")
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

