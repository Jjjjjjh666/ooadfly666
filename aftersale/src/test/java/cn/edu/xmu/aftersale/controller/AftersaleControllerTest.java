package cn.edu.xmu.aftersale.controller;

import cn.edu.xmu.aftersale.controller.dto.AcceptAftersaleRequest;
import cn.edu.xmu.aftersale.controller.dto.CancelAftersaleRequest;
import cn.edu.xmu.aftersale.controller.dto.ConfirmAftersaleRequest;
import cn.edu.xmu.aftersale.controller.dto.ProcessReceivedAftersaleRequest;
import cn.edu.xmu.aftersale.model.AftersaleStatus;
import cn.edu.xmu.aftersale.service.AftersaleService;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.javaee.core.model.ReturnObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AftersaleControllerTest {

    @Mock
    private AftersaleService aftersaleService;

    private AftersaleController controller;

    @BeforeEach
    void setUp() {
        controller = new AftersaleController(aftersaleService);
    }

    @Test
    void confirmAftersaleShouldReturnStatusFromService() {
        ConfirmAftersaleRequest request = new ConfirmAftersaleRequest();
        request.setConfirm(true);
        request.setConclusion("同意");
        when(aftersaleService.confirmAftersale(1L, 2L, true, "同意"))
                .thenReturn(AftersaleStatus.TO_BE_RECEIVED.getCode());

        ReturnObject result = controller.confirmAftersale(1L, 2L, request);

        verify(aftersaleService).confirmAftersale(1L, 2L, true, "同意");
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(ReturnNo.OK.getCode(), data.get("errno"));
        assertEquals(AftersaleStatus.TO_BE_RECEIVED.getCode(), data.get("status"));
    }

    @Test
    void acceptAftersaleShouldReturnStatusFromService() {
        AcceptAftersaleRequest request = new AcceptAftersaleRequest();
        request.setAccept(true);
        request.setConclusion("验收通过");
        when(aftersaleService.acceptAftersale(1L, 2L, true, "验收通过"))
                .thenReturn(AftersaleStatus.RECEIVED.getCode());

        ReturnObject result = controller.acceptAftersale(1L, 2L, request);

        verify(aftersaleService).acceptAftersale(1L, 2L, true, "验收通过");
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(AftersaleStatus.RECEIVED.getCode(), data.get("status"));
    }

    @Test
    void processReceivedAftersaleShouldReturnStatusFromService() {
        ProcessReceivedAftersaleRequest request = new ProcessReceivedAftersaleRequest();
        request.setConclusion("处理完毕");
        when(aftersaleService.processReceivedAftersale(1L, 2L, "处理完毕"))
                .thenReturn(AftersaleStatus.COMPLETED.getCode());

        ReturnObject result = controller.processReceivedAftersale(1L, 2L, request);

        verify(aftersaleService).processReceivedAftersale(1L, 2L, "处理完毕");
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(AftersaleStatus.COMPLETED.getCode(), data.get("status"));
    }

    @Test
    void cancelAftersaleShouldValidateConfirmFlag() {
        CancelAftersaleRequest request = new CancelAftersaleRequest();
        request.setConfirm(false);
        request.setReason("客户取消");

        ReturnObject result = controller.cancelAftersale(1L, 2L, request);

        assertEquals(ReturnNo.BAD_REQUEST.getCode(), result.getErrno());
        assertEquals("confirm必须为true", result.getErrMsg());
    }

    @Test
    void cancelAftersaleShouldInvokeServiceWhenConfirmed() {
        CancelAftersaleRequest request = new CancelAftersaleRequest();
        request.setConfirm(true);
        request.setReason("客户取消");
        when(aftersaleService.cancelAftersale(1L, 2L, "客户取消"))
                .thenReturn(AftersaleStatus.CANCELLED.getCode());

        ReturnObject result = controller.cancelAftersale(1L, 2L, request);

        verify(aftersaleService).cancelAftersale(1L, 2L, "客户取消");
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(ReturnNo.OK.getCode(), data.get("errno"));
        assertEquals(AftersaleStatus.CANCELLED.getCode(), data.get("status"));
        assertEquals(ReturnNo.OK.getMessage(), data.get("errmsg"));
    }
}

