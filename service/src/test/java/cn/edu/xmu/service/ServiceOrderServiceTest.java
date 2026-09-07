package cn.edu.xmu.service;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.service.dao.ServiceOrderRepository;
import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderStatus;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.ServiceOrderAssignStrategy;
import cn.edu.xmu.service.model.strategy.ServiceOrderCancelStrategy;
import cn.edu.xmu.service.model.strategy.ServiceOrderCompleteStrategy;
import cn.edu.xmu.service.service.ServiceOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServiceOrderServiceTest {

    @Mock
    private ServiceOrderRepository repository;
    @Mock
    private ServiceOrderAssignStrategy onsiteAssignStrategy;
    @Mock
    private ServiceOrderAssignStrategy mailInAssignStrategy;
    @Mock
    private ServiceOrderCancelStrategy onsiteCancelStrategy;
    @Mock
    private ServiceOrderCancelStrategy mailInCancelStrategy;
    @Mock
    private ServiceOrderCompleteStrategy onsiteCompleteStrategy;
    @Mock
    private ServiceOrderCompleteStrategy mailInCompleteStrategy;

    private ServiceOrderService serviceOrderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock派工策略
        when(onsiteAssignStrategy.support(ServiceOrderType.ONSITE_REPAIR.getCode())).thenReturn(true);
        when(mailInAssignStrategy.support(ServiceOrderType.MAIL_IN_REPAIR.getCode())).thenReturn(true);
        
        doAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            Long staffId = invocation.getArgument(1);
            order.assign(staffId);
            return null;
        }).when(onsiteAssignStrategy).assign(any(), any());
        
        doAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            Long staffId = invocation.getArgument(1);
            order.assign(staffId);
            return null;
        }).when(mailInAssignStrategy).assign(any(), any());

        // Mock取消策略
        when(onsiteCancelStrategy.support(ServiceOrderType.ONSITE_REPAIR.getCode())).thenReturn(true);
        when(mailInCancelStrategy.support(ServiceOrderType.MAIL_IN_REPAIR.getCode())).thenReturn(true);

        doAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            order.cancel();
            return null;
        }).when(onsiteCancelStrategy).cancel(any());
        doAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            order.cancel();
            return null;
        }).when(mailInCancelStrategy).cancel(any());

        // Mock完成策略
        when(onsiteCompleteStrategy.support(eq(ServiceOrderType.ONSITE_REPAIR.getCode()),
                eq(ServiceOrderStatus.ASSIGNED.ordinal()))).thenReturn(true);
        when(mailInCompleteStrategy.support(eq(ServiceOrderType.MAIL_IN_REPAIR.getCode()),
                eq(ServiceOrderStatus.RECEIVED.ordinal()))).thenReturn(true);

        doAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            order.complete();
            return null;
        }).when(onsiteCompleteStrategy).complete(any());
        doAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            order.complete();
            return null;
        }).when(mailInCompleteStrategy).complete(any());

        serviceOrderService = new ServiceOrderService(
                repository,
                List.of(onsiteAssignStrategy, mailInAssignStrategy),
                List.of(onsiteCancelStrategy, mailInCancelStrategy),
                List.of(onsiteCompleteStrategy, mailInCompleteStrategy));
    }

    @Test
    void createServiceOrderShouldFillDefaults() {
        ArgumentCaptor<ServiceOrder> captor = ArgumentCaptor.forClass(ServiceOrder.class);
        doAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            order.setId(88L);
            return order;
        }).when(repository).create(any(ServiceOrder.class));

        Long id = serviceOrderService.createServiceOrder(1L, 2L, null, "", "", "");

        assertEquals(88L, id);
        verify(repository).create(captor.capture());
        ServiceOrder created = captor.getValue();
        assertEquals("客户", created.getConsignee());
        assertEquals("待填写地址", created.getAddress());
        assertEquals("待填写电话", created.getMobile());
        assertEquals(ServiceOrderStatus.PENDING, created.getStatus());
    }

    @Test
    void confirmServiceOrderShouldApproveWhenConfirmed() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.PENDING, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(3L)).thenReturn(order);

        serviceOrderService.confirmServiceOrder(10L, 3L, true);

        assertEquals(ServiceOrderStatus.TO_BE_ASSIGNED, order.getStatus());
        assertEquals(10L, order.getServiceProviderId());
        verify(repository).save(order);
    }

    @Test
    void confirmServiceOrderShouldRejectWhenFlagFalse() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.PENDING, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(4L)).thenReturn(order);

        serviceOrderService.confirmServiceOrder(10L, 4L, false);

        assertEquals(ServiceOrderStatus.REJECTED, order.getStatus());
        verify(repository).save(order);
    }

    @Test
    void assignServiceOrderShouldUpdateStatus() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.TO_BE_ASSIGNED, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(5L)).thenReturn(order);

        serviceOrderService.assignServiceOrder(1L, 5L, 9L);

        assertEquals(ServiceOrderStatus.ASSIGNED, order.getStatus());
        assertEquals(9L, order.getServiceStaffId());
        verify(repository).save(order);
    }

    @Test
    void receiveServiceOrderShouldValidateType() {
        ServiceOrder mailInOrder = buildOrder(ServiceOrderStatus.ASSIGNED, ServiceOrderType.MAIL_IN_REPAIR);
        when(repository.findById(6L)).thenReturn(mailInOrder);

        serviceOrderService.receiveServiceOrder(2L, 6L);
        assertEquals(ServiceOrderStatus.RECEIVED, mailInOrder.getStatus());

        ServiceOrder onsiteOrder = buildOrder(ServiceOrderStatus.ASSIGNED, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(7L)).thenReturn(onsiteOrder);
        assertThrows(IllegalArgumentException.class,
                () -> serviceOrderService.receiveServiceOrder(2L, 7L));
    }

    @Test
    void completeServiceOrderShouldInvokeMatchingStrategy() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.ASSIGNED, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(8L)).thenReturn(order);

        serviceOrderService.completeServiceOrder(1L, 8L);

        verify(onsiteCompleteStrategy).complete(order);
        verify(repository).save(order);
    }

    @Test
    void completeServiceOrderShouldThrowWhenNoStrategyMatches() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.PENDING, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(9L)).thenReturn(order);

        assertThrows(IllegalArgumentException.class,
                () -> serviceOrderService.completeServiceOrder(1L, 9L));
    }

    @Test
    void cancelServiceOrderShouldUseStrategy() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.ASSIGNED, ServiceOrderType.MAIL_IN_REPAIR);
        when(repository.findById(10L)).thenReturn(order);

        serviceOrderService.cancelServiceOrder(2L, 10L);

        verify(mailInCancelStrategy).cancel(order);
        verify(repository).save(order);
    }

    @Test
    void cancelServiceOrderShouldValidateState() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.COMPLETED, ServiceOrderType.MAIL_IN_REPAIR);
        when(repository.findById(11L)).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> serviceOrderService.cancelServiceOrder(2L, 11L));
        verify(mailInCancelStrategy, never()).cancel(any());
    }

    @Test
    void cancelServiceOrderByAftersaleShouldSkipWhenMissing() {
        when(repository.findByAftersaleId(100L)).thenReturn(null);

        assertDoesNotThrow(() -> serviceOrderService.cancelServiceOrderByAftersale(100L, "reason"));
    }

    @Test
    void cancelServiceOrderByAftersaleShouldValidateStatus() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.COMPLETED, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findByAftersaleId(200L)).thenReturn(order);

        assertThrows(IllegalArgumentException.class,
                () -> serviceOrderService.cancelServiceOrderByAftersale(200L, "reason"));
    }

    @Test
    void cancelServiceOrderByAftersaleShouldInvokeStrategy() {
        ServiceOrder order = buildOrder(ServiceOrderStatus.RECEIVED, ServiceOrderType.MAIL_IN_REPAIR);
        when(repository.findByAftersaleId(201L)).thenReturn(order);

        serviceOrderService.cancelServiceOrderByAftersale(201L, "原因");

        verify(mailInCancelStrategy).cancel(order);
        verify(repository).save(order);
    }

    private ServiceOrder buildOrder(ServiceOrderStatus status, ServiceOrderType type) {
        return ServiceOrder.builder()
                .id(999L)
                .type(type.getCode())
                .status(status)
                .aftersaleId(300L)
                .build();
    }
}
