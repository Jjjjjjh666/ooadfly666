package cn.edu.xmu.service.model.strategy;

import cn.edu.xmu.service.client.LogisticsClient;
import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderStatus;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.impl.MailInCancelStrategy;
import cn.edu.xmu.service.model.strategy.impl.MailInCompleteStrategy;
import cn.edu.xmu.service.model.strategy.impl.OnsiteCancelStrategy;
import cn.edu.xmu.service.model.strategy.impl.OnsiteCompleteStrategy;
import cn.edu.xmu.service.client.dto.CreatePackageResponse;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderStrategyTest {

    @Mock
    private LogisticsClient logisticsClient;

    private OnsiteCompleteStrategy onsiteCompleteStrategy;
    private MailInCompleteStrategy mailInCompleteStrategy;
    private OnsiteCancelStrategy onsiteCancelStrategy;
    private MailInCancelStrategy mailInCancelStrategy;

    @BeforeEach
    void setUp() {
        onsiteCompleteStrategy = new OnsiteCompleteStrategy();
        mailInCompleteStrategy = new MailInCompleteStrategy();
        onsiteCancelStrategy = new OnsiteCancelStrategy();
        mailInCancelStrategy = new MailInCancelStrategy(logisticsClient);  // 注入Mock
    }

    @Test
    void onsiteCompleteStrategyShouldCompleteWhenSupported() {
        ServiceOrder order = ServiceOrder.builder()
                .id(1L)
                .type(ServiceOrderType.ONSITE_REPAIR.getCode())
                .status(ServiceOrderStatus.ASSIGNED)
                .build();

        onsiteCompleteStrategy.complete(order);

        assertEquals(ServiceOrderStatus.COMPLETED, order.getStatus());
        assertTrue(onsiteCompleteStrategy.support(ServiceOrderType.ONSITE_REPAIR.getCode(),
                ServiceOrderStatus.ASSIGNED.ordinal()));
        assertFalse(onsiteCompleteStrategy.support(ServiceOrderType.MAIL_IN_REPAIR.getCode(),
                ServiceOrderStatus.ASSIGNED.ordinal()));
    }

    @Test
    void mailInCompleteStrategyShouldCompleteWhenReceived() {
        ServiceOrder order = ServiceOrder.builder()
                .id(2L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.RECEIVED)
                .build();

        mailInCompleteStrategy.complete(order);

        assertEquals(ServiceOrderStatus.COMPLETED, order.getStatus());
        assertTrue(mailInCompleteStrategy.support(ServiceOrderType.MAIL_IN_REPAIR.getCode(),
                ServiceOrderStatus.RECEIVED.ordinal()));
        assertFalse(mailInCompleteStrategy.support(ServiceOrderType.ONSITE_REPAIR.getCode(),
                ServiceOrderStatus.RECEIVED.ordinal()));
    }

    @Test
    void onsiteCancelStrategyShouldCancelRegardlessOfStaffAssignment() {
        ServiceOrder withStaff = ServiceOrder.builder()
                .id(3L)
                .type(ServiceOrderType.ONSITE_REPAIR.getCode())
                .status(ServiceOrderStatus.ASSIGNED)
                .serviceStaffId(9L)
                .build();
        ServiceOrder withoutStaff = ServiceOrder.builder()
                .id(4L)
                .type(ServiceOrderType.ONSITE_REPAIR.getCode())
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .build();

        onsiteCancelStrategy.cancel(withStaff);
        onsiteCancelStrategy.cancel(withoutStaff);

        assertEquals(ServiceOrderStatus.CANCELED, withStaff.getStatus());
        assertEquals(ServiceOrderStatus.CANCELED, withoutStaff.getStatus());
        assertTrue(onsiteCancelStrategy.support(ServiceOrderType.ONSITE_REPAIR.getCode()));
        assertFalse(onsiteCancelStrategy.support(ServiceOrderType.MAIL_IN_REPAIR.getCode()));
    }

    @Test
    void mailInCancelStrategyShouldCoverAllBranches() {
        ServiceOrder receivedOrder = ServiceOrder.builder()
                .id(5L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.RECEIVED)
                .build();
        ServiceOrder shippingOrder = ServiceOrder.builder()
                .id(6L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.ASSIGNED)
                .expressId(66L)
                .build();
        ServiceOrder idleOrder = ServiceOrder.builder()
                .id(7L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .build();

        InternalReturnObject<CreatePackageResponse> createReturn = new InternalReturnObject<>();
        createReturn.setErrno(ReturnNo.OK.getErrNo());
        createReturn.setData(new CreatePackageResponse(904L, "RT-BRANCH", 2, 0));
        when(logisticsClient.createPackage(anyLong(), anyString(), any())).thenReturn(createReturn);

        mailInCancelStrategy.cancel(receivedOrder);
        mailInCancelStrategy.cancel(shippingOrder);
        mailInCancelStrategy.cancel(idleOrder);

        assertEquals(ServiceOrderStatus.CANCELED, receivedOrder.getStatus());
        assertEquals(ServiceOrderStatus.CANCELED, shippingOrder.getStatus());
        assertEquals(ServiceOrderStatus.CANCELED, idleOrder.getStatus());
        assertTrue(mailInCancelStrategy.support(ServiceOrderType.MAIL_IN_REPAIR.getCode()));
        assertFalse(mailInCancelStrategy.support(ServiceOrderType.ONSITE_REPAIR.getCode()));
    }
}
