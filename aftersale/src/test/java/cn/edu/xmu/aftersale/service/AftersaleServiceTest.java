package cn.edu.xmu.aftersale.service;

import cn.edu.xmu.aftersale.dao.AftersaleOrderRepository;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleStatus;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.*;
import cn.edu.xmu.javaee.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 售后服务单元测试，验证策略选择、状态校验与返回值
 */
class AftersaleServiceTest {

    @Mock
    private AftersaleOrderRepository repository;
    @Mock
    private AftersaleConfirmStrategy exchangeConfirmStrategy;
    @Mock
    private AftersaleConfirmStrategy returnConfirmStrategy;
    @Mock
    private AftersaleConfirmStrategy repairConfirmStrategy;
    @Mock
    private AftersaleAcceptStrategy exchangeAcceptStrategy;
    @Mock
    private AftersaleAcceptStrategy returnAcceptStrategy;
    @Mock
    private AftersaleProcessStrategy exchangeProcessStrategy;
    @Mock
    private AftersaleProcessStrategy returnProcessStrategy;
    @Mock
    private AftersaleCancelStrategy exchangeCancelStrategy;
    @Mock
    private AftersaleCancelStrategy returnCancelStrategy;
    @Mock
    private AftersaleCancelStrategy repairCancelStrategy;

    private AftersaleService aftersaleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(exchangeConfirmStrategy.support(AftersaleType.EXCHANGE.getCode())).thenReturn(true);
        when(returnConfirmStrategy.support(AftersaleType.RETURN.getCode())).thenReturn(true);
        when(repairConfirmStrategy.support(AftersaleType.REPAIR.getCode())).thenReturn(true);
        when(exchangeCancelStrategy.support(AftersaleType.EXCHANGE.getCode())).thenReturn(true);
        when(returnCancelStrategy.support(AftersaleType.RETURN.getCode())).thenReturn(true);
        when(repairCancelStrategy.support(AftersaleType.REPAIR.getCode())).thenReturn(true);
        when(exchangeAcceptStrategy.support(AftersaleType.EXCHANGE.getCode())).thenReturn(true);
        when(returnAcceptStrategy.support(AftersaleType.RETURN.getCode())).thenReturn(true);
        when(exchangeProcessStrategy.support(AftersaleType.EXCHANGE.getCode())).thenReturn(true);
        when(returnProcessStrategy.support(AftersaleType.RETURN.getCode())).thenReturn(true);

        stubConfirmStrategy(exchangeConfirmStrategy, AftersaleStatus.TO_BE_RECEIVED);
        stubConfirmStrategy(returnConfirmStrategy, AftersaleStatus.TO_BE_RECEIVED);
        stubConfirmStrategy(repairConfirmStrategy, AftersaleStatus.TO_BE_COMPLETED);

        stubCancelStrategy(exchangeCancelStrategy);
        stubCancelStrategy(returnCancelStrategy);
        stubCancelStrategy(repairCancelStrategy);

        stubAcceptStrategy(exchangeAcceptStrategy, true);
        stubAcceptStrategy(returnAcceptStrategy, false);

        doAnswer(invocation -> {
            AftersaleOrder order = invocation.getArgument(0);
            order.complete(invocation.getArgument(1));
            return null;
        }).when(exchangeProcessStrategy).process(any(), any());

        doAnswer(invocation -> {
            AftersaleOrder order = invocation.getArgument(0);
            order.complete(invocation.getArgument(1));
            return null;
        }).when(returnProcessStrategy).process(any(), any());

        aftersaleService = new AftersaleService(
                repository,
                List.of(exchangeConfirmStrategy, returnConfirmStrategy, repairConfirmStrategy),
                List.of(exchangeCancelStrategy, returnCancelStrategy, repairCancelStrategy),
                List.of(exchangeAcceptStrategy, returnAcceptStrategy),
                List.of(exchangeProcessStrategy, returnProcessStrategy));
    }

    @Test
    void confirmAftersaleShouldReturnUpdatedStatus() {
        AftersaleOrder order = buildOrder(AftersaleType.RETURN, AftersaleStatus.PENDING);
        when(repository.findById(order.getShopId(), order.getId())).thenReturn(order);

        String status = aftersaleService.confirmAftersale(order.getShopId(), order.getId(), true, "同意退货");

        assertEquals(AftersaleStatus.TO_BE_RECEIVED.getCode(), status);
        verify(returnConfirmStrategy).confirm(eq(order), eq(true), eq("同意退货"));
        verify(repository).save(order);
    }

    @Test
    void acceptAftersaleShouldRouteToAcceptStrategy() {
        AftersaleOrder order = buildOrder(AftersaleType.EXCHANGE, AftersaleStatus.TO_BE_RECEIVED);
        when(repository.findById(order.getShopId(), order.getId())).thenReturn(order);

        String status = aftersaleService.acceptAftersale(order.getShopId(), order.getId(), true, "验收通过");

        assertEquals(AftersaleStatus.RECEIVED.getCode(), status);
        verify(exchangeAcceptStrategy).accept(eq(order), eq(true), eq("验收通过"));
    }

    @Test
    void processAfterReceivedShouldInvokeProcessStrategy() {
        AftersaleOrder order = buildOrder(AftersaleType.RETURN, AftersaleStatus.RECEIVED);
        when(repository.findById(order.getShopId(), order.getId())).thenReturn(order);

        String status = aftersaleService.processReceivedAftersale(order.getShopId(), order.getId(), "处理完成");

        assertEquals(AftersaleStatus.COMPLETED.getCode(), status);
        verify(returnProcessStrategy).process(eq(order), eq("处理完成"));
    }

    @Test
    void confirmAftersaleShouldFailWhenStateIllegal() {
        AftersaleOrder order = buildOrder(AftersaleType.RETURN, AftersaleStatus.CANCELLED);
        when(repository.findById(order.getShopId(), order.getId())).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> aftersaleService.confirmAftersale(order.getShopId(), order.getId(), true, "invalid"));
    }

    @Test
    void cancelAftersaleShouldReturnNewStatus() {
        AftersaleOrder order = buildOrder(AftersaleType.EXCHANGE, AftersaleStatus.TO_BE_RECEIVED);
        when(repository.findById(order.getShopId(), order.getId())).thenReturn(order);

        String status = aftersaleService.cancelAftersale(order.getShopId(), order.getId(), "库存不足");

        assertEquals(AftersaleStatus.CANCELLED.getCode(), status);
        verify(exchangeCancelStrategy).cancel(eq(order), eq("库存不足"));
        verify(repository).save(order);
    }

    private void stubConfirmStrategy(AftersaleConfirmStrategy strategy, AftersaleStatus passStatus) {
        doAnswer(invocation -> {
            AftersaleOrder order = invocation.getArgument(0);
            Boolean confirm = invocation.getArgument(1);
            String conclusion = invocation.getArgument(2);
            if (Boolean.TRUE.equals(confirm)) {
                if (passStatus == AftersaleStatus.TO_BE_COMPLETED) {
                    order.approveToBeCompleted(conclusion);
                } else {
                    order.approveToBeReceived(conclusion);
                }
            } else {
                order.reject(conclusion);
            }
            return null;
        }).when(strategy).confirm(any(), any(), any());
    }

    private void stubCancelStrategy(AftersaleCancelStrategy strategy) {
        doAnswer(invocation -> {
            AftersaleOrder order = invocation.getArgument(0);
            order.cancel();
            return null;
        }).when(strategy).cancel(any(), any());
    }

    private void stubAcceptStrategy(AftersaleAcceptStrategy strategy, boolean acceptedResult) {
        doAnswer(invocation -> {
            AftersaleOrder order = invocation.getArgument(0);
            Boolean accept = invocation.getArgument(1);
            String conclusion = invocation.getArgument(2);
            if (Boolean.TRUE.equals(accept)) {
                order.accept(conclusion);
            } else {
                order.reject(conclusion);
            }
            return null;
        }).when(strategy).accept(any(), any(), any());
        when(strategy.support(any())).thenReturn(acceptedResult);
    }

    private AftersaleOrder buildOrder(AftersaleType type, AftersaleStatus status) {
        return buildOrder(type.getCode(), status);
    }

    private AftersaleOrder buildOrder(int type, AftersaleStatus status) {
        return AftersaleOrder.builder()
                .id(100L + type)
                .shopId(200L)
                .type(type)
                .status(status)
                .build();
    }
}

