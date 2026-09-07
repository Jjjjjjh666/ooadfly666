package cn.edu.xmu.aftersale.service;

import cn.edu.xmu.aftersale.client.LogisticsClient;
import cn.edu.xmu.aftersale.client.ServiceClient;
import cn.edu.xmu.aftersale.client.dto.CreatePackageRequest;
import cn.edu.xmu.aftersale.client.dto.CreatePackageResponse;
import cn.edu.xmu.aftersale.dao.AftersaleOrderRepository;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleStatus;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.impl.*;
import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import cn.edu.xmu.javaee.core.model.ReturnObject;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AftersaleServiceIntegrationTest {
    @Mock private LogisticsClient logisticsClient;
    @Mock private ServiceClient serviceClient;
    @Mock private AftersaleOrderRepository repository;
    private AftersaleService aftersaleService;

    @BeforeEach
    void setUp() {
        ReturnConfirmStrategy returnConfirmStrategy = new ReturnConfirmStrategy(logisticsClient);
        ExchangeConfirmStrategy exchangeConfirmStrategy = new ExchangeConfirmStrategy(logisticsClient);
        RepairConfirmStrategy repairConfirmStrategy = new RepairConfirmStrategy(serviceClient);
        ReturnCancelStrategy returnCancelStrategy = new ReturnCancelStrategy(logisticsClient);
        ExchangeCancelStrategy exchangeCancelStrategy = new ExchangeCancelStrategy(logisticsClient);
        RepairCancelStrategy repairCancelStrategy = new RepairCancelStrategy(serviceClient);
        ReturnAcceptStrategy returnAcceptStrategy = new ReturnAcceptStrategy(logisticsClient);
        ExchangeAcceptStrategy exchangeAcceptStrategy = new ExchangeAcceptStrategy(logisticsClient);
        ReturnReceiveStrategy returnProcessStrategy = new ReturnReceiveStrategy(logisticsClient);
        ExchangeReceiveStrategy exchangeProcessStrategy = new ExchangeReceiveStrategy(logisticsClient);

        aftersaleService = new AftersaleService(
                repository,
                List.of(returnConfirmStrategy, exchangeConfirmStrategy, repairConfirmStrategy),
                List.of(returnCancelStrategy, exchangeCancelStrategy, repairCancelStrategy),
                List.of(returnAcceptStrategy, exchangeAcceptStrategy),
                List.of(returnProcessStrategy, exchangeProcessStrategy)
        );
    }

    @Test
    void confirmReturnAftersale_Approve_ShouldCreatePackage() {
        AftersaleOrder order = AftersaleOrder.builder().id(1L).shopId(10L).type(AftersaleType.RETURN.getCode()).status(AftersaleStatus.PENDING).build();
        when(repository.findById(10L, 1L)).thenReturn(order);
        InternalReturnObject<CreatePackageResponse> ret = new InternalReturnObject<>(); ret.setErrno(0); ret.setData(new CreatePackageResponse(888L, "R001", 2, 0));
        when(logisticsClient.createPackage(anyLong(), anyString(), any())).thenReturn(ret);
        String status = aftersaleService.confirmAftersale(10L, 1L, true, "同意");
        assertEquals(AftersaleStatus.TO_BE_RECEIVED.getCode(), status);
    }

    @Test
    void acceptAftersale_ReturnAccepted_ShouldBeReceived() {
        AftersaleOrder order = AftersaleOrder.builder().id(20L).shopId(10L).type(AftersaleType.RETURN.getCode()).status(AftersaleStatus.TO_BE_RECEIVED).build();
        when(repository.findById(10L, 20L)).thenReturn(order);
        String status = aftersaleService.acceptAftersale(10L, 20L, true, "验收通过");
        assertEquals(AftersaleStatus.RECEIVED.getCode(), status);
    }

    @Test
    void acceptAftersale_ReturnRejected_ShouldCreateReturnPackage() {
        AftersaleOrder order = AftersaleOrder.builder().id(21L).shopId(10L).type(AftersaleType.RETURN.getCode()).status(AftersaleStatus.TO_BE_RECEIVED).build();
        when(repository.findById(10L, 21L)).thenReturn(order);
        InternalReturnObject<CreatePackageResponse> ret = new InternalReturnObject<>(); ret.setErrno(0); ret.setData(new CreatePackageResponse(901L, "RT", 2, 0));
        when(logisticsClient.createPackage(anyLong(), anyString(), any())).thenReturn(ret);
        String status = aftersaleService.acceptAftersale(10L, 21L, false, "包装破损");
        assertEquals(AftersaleStatus.REJECTED.getCode(), status);
    }

    @Test
    void processReceivedAftersale_ReturnCompleted() {
        AftersaleOrder order = AftersaleOrder.builder().id(22L).shopId(10L).type(AftersaleType.RETURN.getCode()).status(AftersaleStatus.RECEIVED).build();
        when(repository.findById(10L, 22L)).thenReturn(order);
        String status = aftersaleService.processReceivedAftersale(10L, 22L, "退款完成");
        assertEquals(AftersaleStatus.COMPLETED.getCode(), status);
    }

    @Test
    void processReceivedAftersale_ExchangeShipped() {
        AftersaleOrder order = AftersaleOrder.builder().id(23L).shopId(10L).type(AftersaleType.EXCHANGE.getCode()).status(AftersaleStatus.RECEIVED).build();
        when(repository.findById(10L, 23L)).thenReturn(order);
        InternalReturnObject<CreatePackageResponse> ret = new InternalReturnObject<>(); ret.setErrno(0); ret.setData(new CreatePackageResponse(777L, "EX", 2, 0));
        when(logisticsClient.createPackage(anyLong(), anyString(), any())).thenReturn(ret);
        String status = aftersaleService.processReceivedAftersale(10L, 23L, "补发完成");
        assertEquals(AftersaleStatus.COMPLETED.getCode(), status);
    }

    @Test
    void acceptAftersale_InvalidStatus_ShouldThrow() {
        AftersaleOrder order = AftersaleOrder.builder().id(25L).shopId(10L).type(AftersaleType.EXCHANGE.getCode()).status(AftersaleStatus.PENDING).build();
        when(repository.findById(10L, 25L)).thenReturn(order);
        assertThrows(BusinessException.class, () -> aftersaleService.acceptAftersale(10L, 25L, true, "x"));
    }

    @Test
    void processReceivedAftersale_InvalidStatus_ShouldThrow() {
        AftersaleOrder order = AftersaleOrder.builder().id(26L).shopId(10L).type(AftersaleType.RETURN.getCode()).status(AftersaleStatus.PENDING).build();
        when(repository.findById(10L, 26L)).thenReturn(order);
        assertThrows(BusinessException.class, () -> aftersaleService.processReceivedAftersale(10L, 26L, "x"));
    }
}

