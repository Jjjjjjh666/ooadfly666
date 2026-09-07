package cn.edu.xmu.aftersale.model.strategy;

import cn.edu.xmu.aftersale.client.LogisticsClient;
import cn.edu.xmu.aftersale.client.ServiceClient;
import cn.edu.xmu.aftersale.client.dto.CreatePackageResponse;
import cn.edu.xmu.aftersale.client.dto.CreateServiceOrderRequest;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleStatus;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.impl.*;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AftersaleStrategyTest {

    @Mock
    private LogisticsClient logisticsClient;
    @Mock
    private ServiceClient serviceClient;

    private ReturnConfirmStrategy returnConfirmStrategy;
    private ExchangeConfirmStrategy exchangeConfirmStrategy;
    private ReturnCancelStrategy returnCancelStrategy;
    private ExchangeCancelStrategy exchangeCancelStrategy;
    private RepairConfirmStrategy repairConfirmStrategy;
    private RepairCancelStrategy repairCancelStrategy;
    private ReturnAcceptStrategy returnAcceptStrategy;
    private ExchangeAcceptStrategy exchangeAcceptStrategy;
    private ReturnReceiveStrategy returnProcessStrategy;
    private ExchangeReceiveStrategy exchangeProcessStrategy;

    @BeforeEach
    void init() {
        returnConfirmStrategy = new ReturnConfirmStrategy(logisticsClient);
        exchangeConfirmStrategy = new ExchangeConfirmStrategy(logisticsClient);
        returnCancelStrategy = new ReturnCancelStrategy(logisticsClient);
        exchangeCancelStrategy = new ExchangeCancelStrategy(logisticsClient);
        repairConfirmStrategy = new RepairConfirmStrategy(serviceClient);
        repairCancelStrategy = new RepairCancelStrategy(serviceClient);
        returnAcceptStrategy = new ReturnAcceptStrategy(logisticsClient);
        exchangeAcceptStrategy = new ExchangeAcceptStrategy(logisticsClient);
        returnProcessStrategy = new ReturnReceiveStrategy(logisticsClient);
        exchangeProcessStrategy = new ExchangeReceiveStrategy(logisticsClient);
    }

    @Test
    void returnConfirmStrategyShouldHandleApproveAndReject() {
        AftersaleOrder order = buildOrder(AftersaleType.RETURN);
        when(logisticsClient.createPackage(any(), any(), any()))
                .thenReturn(successPackage(100L));

        returnConfirmStrategy.confirm(order, true, "同意退货");
        assertEquals(AftersaleStatus.TO_BE_RECEIVED, order.getStatus());
        assertEquals(100L, order.getExpressId());

        order.setStatus(AftersaleStatus.PENDING);
        returnConfirmStrategy.confirm(order, false, "拒绝退货");
        assertEquals(AftersaleStatus.REJECTED, order.getStatus());
    }

    @Test
    void exchangeConfirmStrategyShouldMoveToPendingReceive() {
        AftersaleOrder order = buildOrder(AftersaleType.EXCHANGE);
        when(logisticsClient.createPackage(any(), any(), any()))
                .thenReturn(successPackage(200L));

        exchangeConfirmStrategy.confirm(order, true, null);
        assertEquals(AftersaleStatus.TO_BE_RECEIVED, order.getStatus());
        assertEquals(200L, order.getExpressId());

        order.setStatus(AftersaleStatus.PENDING);
        exchangeConfirmStrategy.confirm(order, false, "库存不足");
        assertEquals(AftersaleStatus.REJECTED, order.getStatus());
    }

    @Test
    void acceptStrategiesShouldUpdateStatus() {
        AftersaleOrder returnOrder = buildOrder(AftersaleType.RETURN);
        returnOrder.setStatus(AftersaleStatus.TO_BE_RECEIVED);
        when(logisticsClient.createPackage(any(), any(), any()))
                .thenReturn(successPackage(300L));

        returnAcceptStrategy.accept(returnOrder, false, "验收失败");
        assertEquals(AftersaleStatus.REJECTED, returnOrder.getStatus());
        assertEquals(300L, returnOrder.getReturnExpressId());

        AftersaleOrder exchangeOrder = buildOrder(AftersaleType.EXCHANGE);
        exchangeOrder.setStatus(AftersaleStatus.TO_BE_RECEIVED);
        exchangeAcceptStrategy.accept(exchangeOrder, true, "验收通过");
        assertEquals(AftersaleStatus.RECEIVED, exchangeOrder.getStatus());
    }

    @Test
    void processStrategiesShouldComplete() {
        AftersaleOrder returnOrder = buildOrder(AftersaleType.RETURN);
        returnOrder.setStatus(AftersaleStatus.RECEIVED);
        returnProcessStrategy.process(returnOrder, "退款完成");
        assertEquals(AftersaleStatus.COMPLETED, returnOrder.getStatus());

        AftersaleOrder exchangeOrder = buildOrder(AftersaleType.EXCHANGE);
        exchangeOrder.setStatus(AftersaleStatus.RECEIVED);
        when(logisticsClient.createPackage(any(), any(), any()))
                .thenReturn(successPackage(400L));
        exchangeProcessStrategy.process(exchangeOrder, "补发完成");
        assertEquals(AftersaleStatus.COMPLETED, exchangeOrder.getStatus());
        assertEquals(400L, exchangeOrder.getReturnExpressId());
    }

    @Test
    void returnAndExchangeCancelShouldCancelOrder() {
        when(logisticsClient.cancelPackage(any(), any(), any()))
                .thenReturn(successVoid());

        AftersaleOrder returnOrder = buildOrder(AftersaleType.RETURN);
        returnOrder.setStatus(AftersaleStatus.TO_BE_RECEIVED);
        returnOrder.setExpressId(500L);
        returnCancelStrategy.cancel(returnOrder, "客户取消");
        assertEquals(AftersaleStatus.CANCELLED, returnOrder.getStatus());
        verify(logisticsClient).cancelPackage(any(), eq(500L), any());

        AftersaleOrder exchangeOrder = buildOrder(AftersaleType.EXCHANGE);
        exchangeOrder.setStatus(AftersaleStatus.TO_BE_COMPLETED);
        exchangeOrder.setExpressId(600L);
        exchangeCancelStrategy.cancel(exchangeOrder, "无货");
        assertEquals(AftersaleStatus.CANCELLED, exchangeOrder.getStatus());
        verify(logisticsClient).cancelPackage(any(), eq(600L), any());
    }

    @Test
    void repairConfirmShouldInvokeServiceClient() {
        AftersaleOrder order = buildOrder(AftersaleType.REPAIR);

        repairConfirmStrategy.confirm(order, true, "维修");

        ArgumentCaptor<CreateServiceOrderRequest> captor = ArgumentCaptor.forClass(CreateServiceOrderRequest.class);
        verify(serviceClient).createServiceOrder(eq(order.getShopId()), eq(order.getId()), captor.capture());
        assertEquals(AftersaleStatus.TO_BE_COMPLETED, order.getStatus());
        assertEquals("维修", order.getConclusion());
    }

    @Test
    void repairCancelShouldInvokeServiceModule() {
        AftersaleOrder order = buildOrder(AftersaleType.REPAIR);
        repairCancelStrategy.cancel(order, "原因");
        verify(serviceClient).cancelServiceOrder(order.getShopId(), order.getId(), "原因");
        assertEquals(AftersaleStatus.CANCELLED, order.getStatus());
    }

    private InternalReturnObject<CreatePackageResponse> successPackage(Long id) {
        InternalReturnObject<CreatePackageResponse> ret = new InternalReturnObject<>();
        ret.setErrno(0);
        ret.setData(new CreatePackageResponse(id, "BILL" + id, 2, 0));
        return ret;
    }

    private InternalReturnObject<Void> successVoid() {
        InternalReturnObject<Void> ret = new InternalReturnObject<>();
        ret.setErrno(0);
        return ret;
    }

    private AftersaleOrder buildOrder(AftersaleType type) {
        return AftersaleOrder.builder()
                .id(10L)
                .shopId(20L)
                .type(type.getCode())
                .status(AftersaleStatus.PENDING)
                .build();
    }
}

