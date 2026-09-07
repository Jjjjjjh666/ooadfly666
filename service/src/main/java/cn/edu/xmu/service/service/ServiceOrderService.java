package cn.edu.xmu.service.service;

import cn.edu.xmu.service.dao.ServiceOrderRepository;
import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderStatus;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.ServiceOrderAssignStrategy;
import cn.edu.xmu.service.model.strategy.ServiceOrderCancelStrategy;
import cn.edu.xmu.service.model.strategy.ServiceOrderCompleteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceOrderService {

    private final ServiceOrderRepository repository;
    private final List<ServiceOrderAssignStrategy> assignStrategies;
    private final List<ServiceOrderCancelStrategy> cancelStrategies;
    private final List<ServiceOrderCompleteStrategy> completeStrategies;

    /**
     * 创建服务单（被aftersale模块调用）
     */
    @Transactional
    public Long createServiceOrder(Long shopId, Long aftersalesId, Integer type, 
                                    String consignee, String address, String mobile) {
        log.info("创建服务单: shopId={}, aftersalesId={}, type={}, consignee={}, address={}, mobile={}", 
                shopId, aftersalesId, type, consignee, address, mobile);
        
        // 设置默认值，确保必填字段不为空
        String finalConsignee = (consignee != null && !consignee.isEmpty()) ? consignee : "客户";
        String finalAddress = (address != null && !address.isEmpty()) ? address : "待填写地址";
        String finalMobile = (mobile != null && !mobile.isEmpty()) ? mobile : "待填写电话";
        
        ServiceOrder order = ServiceOrder.builder()
                .aftersaleId(aftersalesId)
                .type(type != null ? type : 0)
                .status(ServiceOrderStatus.PENDING)
                .consignee(finalConsignee)
                .address(finalAddress)
                .mobile(finalMobile)
                .customerId(0L) // 默认值，实际应该从售后单获取
                .regionId(0L)  // 默认值，实际应该从订单获取
                .productId(0L) // 默认值，实际应该从售后单获取
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        repository.create(order);
        
        log.info("服务单创建成功: id={}", order.getId());
        return order.getId();
    }

    /**
     * 服务商审核服务单
     * 审核通过 → TO_BE_ASSIGNED
     * 审核不通过 → REJECTED
     */
    @Transactional
    public void confirmServiceOrder(Long serviceProviderId, Long orderId, Boolean confirm) {
        log.info("开始审核服务单: serviceProviderId={}, orderId={}, confirm={}", serviceProviderId, orderId, confirm);
        
        // 1. 查询服务单
        ServiceOrder order = repository.findById(orderId);
        
        // 2. 检查状态
        order.checkPendingStatus();
        
        // 3. 根据审核结果更新状态
        if (Boolean.TRUE.equals(confirm)) {
            // 审核通过，转换为待派工状态
            order.approve(serviceProviderId);
            log.info("服务单审核通过: orderId={}, 状态转换为待派工", orderId);
        } else {
            // 审核不通过，转换为已拒绝状态
            order.reject();
            log.info("服务单审核不通过: orderId={}, 状态转换为已拒绝", orderId);
        }
        
        // 4. 保存更新
        repository.save(order);
        
        log.info("服务单审核完成: orderId={}, status={}", orderId, order.getStatus());
    }

    /**
     * 服务商派工（使用策略模式）
     * TO_BE_ASSIGNED → ASSIGNED
     * 寄修类型会生成运单
     */
    @Transactional
    public void assignServiceOrder(Long serviceProviderId, Long orderId, Long serviceStaffId) {
        log.info("开始派工: serviceProviderId={}, orderId={}, serviceStaffId={}", 
                serviceProviderId, orderId, serviceStaffId);
        
        // 1. 查询服务单
        ServiceOrder order = repository.findById(orderId);
        
        // 2. 检查状态
        order.checkToBeAssignedStatus();
        
        // 3. 根据类型选择派工策略（多态）
        ServiceOrderAssignStrategy strategy = assignStrategies.stream()
                .filter(s -> s.support(order.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的服务类型: " + order.getType()));
        
        // 4. 执行派工策略
        strategy.assign(order, serviceStaffId);
        
        // 5. 保存更新
        repository.save(order);
        
        log.info("服务单派工完成: orderId={}, serviceStaffId={}, status={}, expressId={}", 
                orderId, serviceStaffId, order.getStatus(), order.getExpressId());
    }

    /**
     * 验收寄修商品
     * ASSIGNED → RECEIVED（仅type=1）
     */
    @Transactional
    public void receiveServiceOrder(Long serviceProviderId, Long orderId) {
        log.info("开始验收寄修商品: serviceProviderId={}, orderId={}", serviceProviderId, orderId);
        
        // 1. 查询服务单
        ServiceOrder order = repository.findById(orderId);
        
        // 2. 检查类型（只有寄修才能验收收件）
        if (!ServiceOrderType.MAIL_IN_REPAIR.getCode().equals(order.getType())) {
            throw new IllegalArgumentException("只有寄修类型的服务单才能验收收件");
        }
        
        // 3. 检查状态
        order.checkAssignedStatus();
        
        // 4. 验收收件
        order.receive();
        
        // 5. 保存更新
        repository.save(order);
        
        log.info("寄修商品验收完成: orderId={}, status={}", orderId, order.getStatus());
    }

    /**
     * 完成服务单（使用策略模式）
     * type=0且status=2 → COMPLETED
     * type=1且status=3 → COMPLETED
     */
    @Transactional
    public void completeServiceOrder(Long serviceProviderId, Long orderId) {
        log.info("开始完成服务单: serviceProviderId={}, orderId={}", serviceProviderId, orderId);
        
        // 1. 查询服务单
        ServiceOrder order = repository.findById(orderId);
        
        // 2. 根据类型和状态选择策略（多态）
        ServiceOrderCompleteStrategy strategy = completeStrategies.stream()
                .filter(s -> s.support(order.getType(), order.getStatus().ordinal()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "当前状态和类型不支持完成操作: type=" + order.getType() + ", status=" + order.getStatus()));
        
        // 3. 执行策略
        strategy.complete(order);
        
        // 4. 保存更新
        repository.save(order);
        
        log.info("服务单完成: orderId={}, status={}", orderId, order.getStatus());
    }

    /**
     * 服务商取消服务单（多态实现）
     */
    @Transactional
    public void cancelServiceOrder(Long serviceProviderId, Long orderId) {
        log.info("开始取消服务单: serviceProviderId={}, orderId={}", serviceProviderId, orderId);
        
        // 1. 查询服务单
        ServiceOrder order = repository.findById(orderId);
        
        // 2. 检查状态（允许待派工、已派工、已收件状态取消）
        order.checkCanCancel();
        
        // 3. 根据类型选择策略（多态）
        ServiceOrderCancelStrategy strategy = cancelStrategies.stream()
                .filter(s -> s.support(order.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的服务类型: " + order.getType()));
        
        // 4. 执行策略
        strategy.cancel(order);
        
        // 5. 保存更新
        repository.save(order);
        
        log.info("服务单取消完成: orderId={}, status={}", orderId, order.getStatus());
    }

    /**
     * 内部接口 - 取消服务单（被aftersale模块调用）
     */
    @Transactional
    public void cancelServiceOrderByAftersale(Long aftersalesId, String reason) {
        log.info("售后模块取消服务单: aftersalesId={}, reason={}", aftersalesId, reason);
        
        // 1. 根据售后单ID查询服务单
        ServiceOrder order = repository.findByAftersaleId(aftersalesId);
        if (order == null) {
            log.warn("未找到对应的服务单: aftersalesId={}", aftersalesId);
            return;
        }
        
        // 2. 检查状态（允许待接收、待派工、已派工、已收件状态取消）
        if (!order.getStatus().canCancel()) {
            log.warn("服务单状态不允许取消: orderId={}, status={}", order.getId(), order.getStatus());
            throw new IllegalArgumentException("服务单状态不允许取消: " + order.getStatus());
        }
        
        // 3. 根据类型选择策略（多态）
        ServiceOrderCancelStrategy strategy = cancelStrategies.stream()
                .filter(s -> s.support(order.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的服务类型: " + order.getType()));
        
        // 4. 执行策略
        strategy.cancel(order);
        
        // 5. 保存更新
        repository.save(order);
        
        log.info("服务单取消完成: orderId={}, aftersalesId={}, status={}", 
                order.getId(), aftersalesId, order.getStatus());
    }
}

