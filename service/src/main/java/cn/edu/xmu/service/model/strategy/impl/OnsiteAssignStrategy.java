package cn.edu.xmu.service.model.strategy.impl;

import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.ServiceOrderAssignStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 上门维修派工策略
 * 上门维修派工时只需要分配师傅即可
 */
@Slf4j
@Component
public class OnsiteAssignStrategy implements ServiceOrderAssignStrategy {

    @Override
    public void assign(ServiceOrder order, Long serviceStaffId) {
        log.info("执行上门维修派工策略: orderId={}, serviceStaffId={}", order.getId(), serviceStaffId);
        
        // 上门维修：只需要分配师傅
        order.assign(serviceStaffId);
        
        log.info("上门维修派工完成: orderId={}, serviceStaffId={}", order.getId(), serviceStaffId);
    }

    @Override
    public boolean support(Integer type) {
        return ServiceOrderType.ONSITE_REPAIR.getCode().equals(type);
    }
}

