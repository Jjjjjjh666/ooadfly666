package cn.edu.xmu.service.controller;

import cn.edu.xmu.javaee.core.model.ReturnObject;
import cn.edu.xmu.service.controller.dto.ReviewDraftRequest;
import cn.edu.xmu.service.controller.dto.ServiceProviderDraftHistoryResponse;
import cn.edu.xmu.service.controller.dto.ServiceProviderDraftListResult;
import cn.edu.xmu.service.service.ServiceProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceProviderControllerTest {

    @Mock
    private ServiceProviderService serviceProviderService;

    private ServiceProviderController controller;

    @BeforeEach
    void setUp() {
        controller = new ServiceProviderController(serviceProviderService);
    }

    @Test
    void reviewDraftShouldDelegateToServiceAndReturnOk() {
        ReviewDraftRequest request = new ReviewDraftRequest();
        request.setConclusion(1);
        request.setOpinion("通过");

        ReturnObject result = controller.reviewDraft(10L, request);

        verify(serviceProviderService).reviewDraft(10L, 1, "通过");
        assertEquals(0, result.getErrno());
        assertEquals("ReturnNo.OK", result.getErrMsg());
    }

    @Test
    void listDraftsShouldDelegateToService() {
        ServiceProviderDraftListResult expected = ServiceProviderDraftListResult.builder().build();
        when(serviceProviderService.listDrafts(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(expected);

        ReturnObject ret = controller.listDrafts(null, null, null, null, null, null, null);

        verify(serviceProviderService).listDrafts(null, null, null, null, 1, 10);
        assertSame(expected, ret.getData());
    }

    @Test
    void getDraftHistoryShouldDelegateToService() {
        ServiceProviderDraftHistoryResponse history = new ServiceProviderDraftHistoryResponse();
        when(serviceProviderService.getDraftHistory(5L)).thenReturn(history);

        ReturnObject ret = controller.getDraftHistory(5L);

        verify(serviceProviderService).getDraftHistory(5L);
        assertSame(history, ret.getData());
    }
}

