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
 * 已验收退货的处理策略：完成退款
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnReceiveStrategy implements AftersaleProcessStrategy {

    private final LogisticsClient logisticsClient;

    @Override
    public void process(AftersaleOrder order, String conclusion) {
        log.info("处理已验收退货: orderId={}", order.getId());
        // 退货：直接完成并模拟退款（此处仅记录结论）
        order.complete(conclusion != null ? conclusion : "退货已验收，退款完成");
    }

    @Override
    public boolean support(Integer type) {
        return AftersaleType.RETURN.getCode().equals(type);
    }
}

