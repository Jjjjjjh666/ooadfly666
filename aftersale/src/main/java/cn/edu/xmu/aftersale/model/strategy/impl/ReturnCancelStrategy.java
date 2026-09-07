package cn.edu.xmu.aftersale.model.strategy.impl;

import cn.edu.xmu.aftersale.client.LogisticsClient;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.AftersaleCancelStrategy;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 退货取消策略
 * 取消时需要取消对应的运单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnCancelStrategy implements AftersaleCancelStrategy {

    private final LogisticsClient logisticsClient;

    @Override
    public void cancel(AftersaleOrder order, String reason) {
        log.info("执行退货取消策略: orderId={}, reason={}", order.getId(), reason);
        
        // 取消售后单
        order.cancel();
        log.info("退货售后单已取消: orderId={}", order.getId());
        
        // 如果有运单ID，需要取消运单
        if (order.getExpressId() != null) {
            try {
                log.info("准备取消退货运单: expressId={}", order.getExpressId());
                
                InternalReturnObject<Void> response = 
                    logisticsClient.cancelPackage(
                        order.getShopId(),
                        order.getExpressId(),
                        "Bearer token"  // 实际应该从上下文获取
                    );
                
                log.info("退货运单取消成功: expressId={}", order.getExpressId());
            } catch (Exception e) {
                log.error("取消退货运单失败: expressId={}", order.getExpressId(), e);
                throw new RuntimeException("取消退货运单失败: " + e.getMessage(), e);
            }
        } else {
            log.info("售后单没有运单，无需取消运单");
        }
    }

    @Override
    public boolean support(Integer type) {
        return AftersaleType.RETURN.getCode().equals(type);
    }
}
