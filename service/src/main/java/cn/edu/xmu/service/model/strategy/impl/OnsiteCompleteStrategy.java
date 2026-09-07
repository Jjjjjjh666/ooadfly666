package cn.edu.xmu.service.model.strategy.impl;

import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderStatus;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.ServiceOrderCompleteStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 上门维修完成策略
 * 只有type=0且status=2（已派工）可以进行完成服务单
 */
@Slf4j
@Component
public class OnsiteCompleteStrategy implements ServiceOrderCompleteStrategy {

    @Override
    public void complete(ServiceOrder order) {
        log.info("执行上门维修完成策略: orderId={}", order.getId());
        
        // 完成服务单
        order.complete();
        
        log.info("上门维修服务单已完成: orderId={}", order.getId());
        log.info("后续需要: 1.通知客户服务完成 2.更新售后单状态 3.结算费用");
    }

    @Override
    public boolean support(Integer type, Integer status) {
        // type=0（上门服务）且status=2（已派工）
        return ServiceOrderType.ONSITE_REPAIR.getCode().equals(type) 
                && ServiceOrderStatus.ASSIGNED.ordinal() == status;
    }
}

