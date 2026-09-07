package cn.edu.xmu.service.controller;

import cn.edu.xmu.javaee.core.model.ReturnObject;
import cn.edu.xmu.service.controller.dto.CreateServiceOrderRequest;
import cn.edu.xmu.service.service.ServiceOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOrderInternalControllerTest {

    @Mock
    private ServiceOrderService serviceOrderService;

    private ServiceOrderInternalController controller;

    @BeforeEach
    void setUp() {
        controller = new ServiceOrderInternalController(serviceOrderService);
    }

    @Test
    void createServiceOrderShouldUseRequestPayload() {
        CreateServiceOrderRequest request = new CreateServiceOrderRequest();
        request.setType(1);
        CreateServiceOrderRequest.ConsigneeInfo consignee = new CreateServiceOrderRequest.ConsigneeInfo();
        consignee.setName("Alice");
        consignee.setMobile("13900000000");
        request.setConsignee(consignee);
        request.setAddress("地址");

        when(serviceOrderService.createServiceOrder(1L, 2L, 1, "Alice", "地址", "13900000000"))
                .thenReturn(88L);

        ReturnObject result = controller.createServiceOrder(1L, 2L, request);

        verify(serviceOrderService).createServiceOrder(1L, 2L, 1, "Alice", "地址", "13900000000");
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(88L, data.get("id"));
        assertEquals(1, data.get("type"));
    }

    @Test
    void createServiceOrderShouldFallbackWhenRequestMissing() {
        when(serviceOrderService.createServiceOrder(5L, 6L, 0, "", "", ""))
                .thenReturn(99L);

        ReturnObject result = controller.createServiceOrder(5L, 6L, null);

        verify(serviceOrderService).createServiceOrder(5L, 6L, 0, "", "", "");
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(99L, data.get("id"));
        assertEquals(0, data.get("type"));
    }

    @Test
    void cancelServiceOrderShouldDelegateToService() {
        ReturnObject result = controller.cancelServiceOrder(1L, 9L, "no longer needed");

        verify(serviceOrderService).cancelServiceOrderByAftersale(9L, "no longer needed");
        assertEquals(0, result.getErrno());
    }
}
