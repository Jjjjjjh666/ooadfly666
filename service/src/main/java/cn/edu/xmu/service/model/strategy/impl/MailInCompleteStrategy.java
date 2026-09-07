package cn.edu.xmu.service.model.strategy.impl;

import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderStatus;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.ServiceOrderCompleteStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 寄修完成策略
 * 只有type=1且status=3（已收件）可以进行完成服务单
 */
@Slf4j
@Component
public class MailInCompleteStrategy implements ServiceOrderCompleteStrategy {

    @Override
    public void complete(ServiceOrder order) {
        log.info("执行寄修完成策略: orderId={}", order.getId());
        
        // 完成服务单
        order.complete();
        
        log.info("寄修服务单已完成: orderId={}", order.getId());
        log.info("后续需要: 1.安排寄回商品 2.生成物流单 3.通知客户 4.更新售后单状态 5.结算费用");
    }

    @Override
    public boolean support(Integer type, Integer status) {
        // type=1（寄修）且status=3（已收件）
        return ServiceOrderType.MAIL_IN_REPAIR.getCode().equals(type) 
                && ServiceOrderStatus.RECEIVED.ordinal() == status;
    }
}

