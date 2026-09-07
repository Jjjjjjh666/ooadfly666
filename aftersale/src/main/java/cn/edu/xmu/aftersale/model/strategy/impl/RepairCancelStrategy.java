package cn.edu.xmu.aftersale.model.strategy.impl;

import cn.edu.xmu.aftersale.client.ServiceClient;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.AftersaleCancelStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 维修取消策略
 * 取消维修需要同时取消对应的服务单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairCancelStrategy implements AftersaleCancelStrategy {

    private final ServiceClient serviceClient;

    @Override
    public void cancel(AftersaleOrder order, String reason) {
        log.info("执行维修取消策略: orderId={}, reason={}", order.getId(), reason);
        
        // 先取消服务单
        try {
            serviceClient.cancelServiceOrder(order.getShopId(), order.getId(), reason);
            log.info("维修服务单已取消: aftersaleId={}", order.getId());
        } catch (Exception e) {
            log.error("取消维修服务单失败: aftersaleId={}", order.getId(), e);
            // 如果服务单取消失败，抛出异常阻止售后单取消
            // 这样可以保证数据一致性：要么都取消，要么都不取消
            throw new RuntimeException("取消服务单失败: " + e.getMessage(), e);
        }
        
        // 再取消售后单
        order.cancel();
        log.info("维修售后单已取消: orderId={}", order.getId());
    }

    @Override
    public boolean support(Integer type) {
        return AftersaleType.REPAIR.getCode().equals(type);
    }
}

