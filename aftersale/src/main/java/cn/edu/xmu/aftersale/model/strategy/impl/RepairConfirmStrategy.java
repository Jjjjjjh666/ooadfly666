package cn.edu.xmu.aftersale.model.strategy.impl;

import cn.edu.xmu.aftersale.client.ServiceClient;
import cn.edu.xmu.aftersale.client.dto.CreateServiceOrderRequest;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.AftersaleConfirmStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 维修审核策略
 * 审核通过后需要调用服务模块创建服务单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairConfirmStrategy implements AftersaleConfirmStrategy {

    private final ServiceClient serviceClient;

    @Override
    public void confirm(AftersaleOrder order, Boolean confirm, String conclusion) {
        log.info("执行维修审核策略: orderId={}, confirm={}", order.getId(), confirm);
        
        if (Boolean.TRUE.equals(confirm)) {
            // 审核通过（维修），转换为待完成状态
            order.approveToBeCompleted(conclusion != null ? conclusion : "同意维修");
            log.info("维修审核通过: orderId={}, 状态转换为待完成", order.getId());
            
            // 调用服务模块创建服务单（跨模块调用）
            try {
                CreateServiceOrderRequest request = new CreateServiceOrderRequest();
                // 服务单类型：0-上门服务，1-寄件服务（默认使用上门服务）
                request.setType(0);
                // 设置联系人信息（如果后续需要从订单获取客户信息，可以在这里优化）
                CreateServiceOrderRequest.ConsigneeInfo consignee = new CreateServiceOrderRequest.ConsigneeInfo();
                consignee.setName("客户"); // 默认名称，实际应该从订单获取
                consignee.setMobile("13800138000"); // 默认手机号，实际应该从订单获取
                request.setConsignee(consignee);
                // 设置地址信息（如果后续需要从订单获取地址信息，可以在这里优化）
                request.setAddress("待填写地址"); // 默认地址，实际应该从订单获取
                
                serviceClient.createServiceOrder(order.getShopId(), order.getId(), request);
                log.info("维修服务单创建成功: aftersaleId={}", order.getId());
            } catch (Exception e) {
                log.error("创建维修服务单失败: aftersaleId={}", order.getId(), e);
                throw new RuntimeException("创建服务单失败: " + e.getMessage(), e);
            }
        } else {
            // 审核不通过，转换为已拒绝状态
            order.reject(conclusion != null ? conclusion : "拒绝维修");
            log.info("维修审核拒绝: orderId={}, reason={}", order.getId(), conclusion);
        }
    }

    @Override
    public boolean support(Integer type) {
        return AftersaleType.REPAIR.getCode().equals(type);
    }
}

