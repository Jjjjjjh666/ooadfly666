package cn.edu.xmu.service.controller;

import cn.edu.xmu.javaee.core.model.ReturnObject;
import cn.edu.xmu.service.controller.dto.AssignServiceOrderRequest;
import cn.edu.xmu.service.controller.dto.ConfirmServiceOrderRequest;
import cn.edu.xmu.service.service.ServiceOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServiceProviderOrderControllerTest {

    @Mock
    private ServiceOrderService serviceOrderService;

    private ServiceProviderOrderController controller;

    @BeforeEach
    void setUp() {
        controller = new ServiceProviderOrderController(serviceOrderService);
    }

    @Test
    void confirmEndpointShouldDelegateToService() {
        ConfirmServiceOrderRequest request = new ConfirmServiceOrderRequest();
        request.setConfirm(Boolean.TRUE);

        ReturnObject result = controller.confirmServiceOrder(3L, 9L, request);

        verify(serviceOrderService).confirmServiceOrder(3L, 9L, true);
        assertEquals(0, result.getErrno());
    }

    @Test
    void assignEndpointShouldPassPayload() {
        AssignServiceOrderRequest request = new AssignServiceOrderRequest();
        request.setServiceStaffId(66L);

        ReturnObject result = controller.assignServiceOrder(1L, 2L, request);

        verify(serviceOrderService).assignServiceOrder(1L, 2L, 66L);
        assertEquals(0, result.getErrno());
    }

    @Test
    void receiveEndpointShouldDelegate() {
        ReturnObject result = controller.receiveServiceOrder(1L, 5L);

        verify(serviceOrderService).receiveServiceOrder(1L, 5L);
        assertEquals(0, result.getErrno());
    }

    @Test
    void completeEndpointShouldDelegate() {
        ReturnObject result = controller.completeServiceOrder(8L, 18L);

        verify(serviceOrderService).completeServiceOrder(8L, 18L);
        assertEquals(0, result.getErrno());
    }

    @Test
    void cancelEndpointShouldDelegate() {
        ReturnObject result = controller.cancelServiceOrder(9L, 19L);

        verify(serviceOrderService).cancelServiceOrder(9L, 19L);
        assertEquals(0, result.getErrno());
    }
}
