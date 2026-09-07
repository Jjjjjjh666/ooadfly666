package cn.edu.xmu.aftersale.service;

import cn.edu.xmu.aftersale.dao.AftersaleOrderRepository;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.strategy.AftersaleAcceptStrategy;
import cn.edu.xmu.aftersale.model.strategy.AftersaleCancelStrategy;
import cn.edu.xmu.aftersale.model.strategy.AftersaleConfirmStrategy;
import cn.edu.xmu.aftersale.model.strategy.AftersaleProcessStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 售后服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AftersaleService {

    private final AftersaleOrderRepository repository;
    private final List<AftersaleConfirmStrategy> confirmStrategies;
    private final List<AftersaleCancelStrategy> cancelStrategies;
    private final List<AftersaleAcceptStrategy> acceptStrategies;
    private final List<AftersaleProcessStrategy> processStrategies;

    /** 商户审核售后单 */
    @Transactional
    public String confirmAftersale(Long shopId, Long id, Boolean confirm, String conclusion) {
        log.info("开始审核售后单: shopId={}, id={}, confirm={}", shopId, id, confirm);

        AftersaleOrder order = repository.findById(shopId, id);
        order.checkPendingStatus();

        AftersaleConfirmStrategy strategy = confirmStrategies.stream()
                .filter(s -> s.support(order.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的售后类型: " + order.getType()));

        strategy.confirm(order, confirm, conclusion);

        repository.save(order);
        log.info("售后单审核完成: id={}, status={}", id, order.getStatus());
        return order.getStatus().getCode();
    }

    /** 商户验收（待验收 -> 已验收/已拒绝） */
    @Transactional
    public String acceptAftersale(Long shopId, Long id, Boolean accept, String conclusion) {
        log.info("开始验收售后单: shopId={}, id={}, accept={}", shopId, id, accept);

        AftersaleOrder order = repository.findById(shopId, id);
        order.checkCanReceive();

        AftersaleAcceptStrategy strategy = acceptStrategies.stream()
                .filter(s -> s.support(order.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的售后类型: " + order.getType()));

        strategy.accept(order, accept, conclusion);

        repository.save(order);
        log.info("售后单验收完成: id={}, status={}", id, order.getStatus());
        return order.getStatus().getCode();
    }

    /** 商户处理已验收商品（已验收 -> 完成或发货） */
    @Transactional
    public String processReceivedAftersale(Long shopId, Long id, String conclusion) {
        log.info("处理已验收售后单: shopId={}, id={}", shopId, id);

        AftersaleOrder order = repository.findById(shopId, id);
        order.checkCanProcessReceived();

        AftersaleProcessStrategy strategy = processStrategies.stream()
                .filter(s -> s.support(order.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的售后类型: " + order.getType()));

        strategy.process(order, conclusion);

        repository.save(order);
        log.info("已验收处理完成: id={}, status={}", id, order.getStatus());
        return order.getStatus().getCode();
    }

    /** 商户取消售后单 */
    @Transactional
    public String cancelAftersale(Long shopId, Long id, String reason) {
        log.info("开始取消售后单: shopId={}, id={}, reason={}", shopId, id, reason);

        AftersaleOrder order = repository.findById(shopId, id);
        order.checkCanCancel();

        AftersaleCancelStrategy strategy = cancelStrategies.stream()
                .filter(s -> s.support(order.getType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的售后类型: " + order.getType()));

        strategy.cancel(order, reason);

        repository.save(order);
        log.info("售后单取消完成: id={}, status={}", id, order.getStatus());
        return order.getStatus().getCode();
    }
}

