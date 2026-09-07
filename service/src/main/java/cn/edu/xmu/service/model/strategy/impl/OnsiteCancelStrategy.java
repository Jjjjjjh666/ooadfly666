package cn.edu.xmu.service.model.strategy.impl;

import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.ServiceOrderCancelStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 上门维修取消策略
 * 取消时需要通知客户和技师
 */
@Slf4j
@Component
public class OnsiteCancelStrategy implements ServiceOrderCancelStrategy {

    @Override
    public void cancel(ServiceOrder order) {
        log.info("执行上门维修取消策略: orderId={}, status={}", order.getId(), order.getStatus());
        
        // 取消服务单
        order.cancel();
        
        // 上门维修取消特定逻辑
        log.info("上门维修服务单已取消: orderId={}", order.getId());
        
        // 根据当前状态执行不同的取消逻辑
        if (order.getServiceStaffId() != null) {
            log.info("后续需要: 1.通知客户取消 2.通知已分配的技师 3.释放预约时间");
        } else {
            log.info("后续需要: 1.通知客户取消");
        }
        
        // 实际业务中可能需要：
        // 1. 通知客户服务已取消
        // 2. 通知已分配的技师
        // 3. 释放预约的时间段
        // 4. 记录取消原因
    }

    @Override
    public boolean support(Integer type) {
        return ServiceOrderType.ONSITE_REPAIR.getCode().equals(type);
    }
}
