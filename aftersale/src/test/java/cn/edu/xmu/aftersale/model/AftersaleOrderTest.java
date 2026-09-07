package cn.edu.xmu.aftersale.model;

import cn.edu.xmu.aftersale.dao.po.AftersaleOrderPo;
import cn.edu.xmu.javaee.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AftersaleOrderTest {

    @Test
    void fromPoShouldConvertFields() {
        AftersaleOrderPo po = buildPo();
        po.setStatus(2);

        AftersaleOrder order = AftersaleOrder.fromPo(po);

        assertEquals(po.getId(), order.getId());
        assertEquals(AftersaleStatus.TO_BE_COMPLETED, order.getStatus());
    }

    @Test
    void toPoShouldMirrorDomainValues() {
        AftersaleOrder order = AftersaleOrder.builder()
                .id(2L)
                .shopId(3L)
                .orderId(4L)
                .type(AftersaleType.EXCHANGE.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED)
                .reason("reason")
                .conclusion("done")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AftersaleOrderPo po = order.toPo();

        assertEquals(order.getType(), po.getType());
        assertEquals(Integer.valueOf(1), po.getStatus());
        assertEquals(order.getConclusion(), po.getConclusion());
    }

    @Test
    void checkPendingStatusShouldThrowWhenNotPending() {
        AftersaleOrder order = AftersaleOrder.builder()
                .status(AftersaleStatus.CANCELLED)
                .build();

        assertThrows(BusinessException.class, order::checkPendingStatus);
    }

    @Test
    void checkCanCancelShouldValidateStates() {
        AftersaleOrder order = AftersaleOrder.builder()
                .status(AftersaleStatus.TO_BE_RECEIVED)
                .build();
        assertDoesNotThrow(order::checkCanCancel);

        order.setStatus(AftersaleStatus.PENDING);
        assertThrows(BusinessException.class, order::checkCanCancel);
    }

    @Test
    void approveAndRejectPathsShouldUpdateStatus() {
        AftersaleOrder order = AftersaleOrder.builder()
                .status(AftersaleStatus.PENDING)
                .build();

        order.approveToBeReceived("同意退货");
        assertEquals(AftersaleStatus.TO_BE_RECEIVED, order.getStatus());
        assertEquals("同意退货", order.getConclusion());

        order.approveToBeCompleted("同意维修");
        assertEquals(AftersaleStatus.TO_BE_COMPLETED, order.getStatus());

        order.reject("拒绝");
        assertEquals(AftersaleStatus.REJECTED, order.getStatus());
        assertEquals("拒绝", order.getConclusion());
    }

    @Test
    void cancelShouldTransitionToCancelled() {
        AftersaleOrder order = AftersaleOrder.builder()
                .status(AftersaleStatus.TO_BE_COMPLETED)
                .build();

        order.cancel();

        assertEquals(AftersaleStatus.CANCELLED, order.getStatus());
    }

    @Test
    void getAftersaleTypeShouldConvertCode() {
        AftersaleOrder order = AftersaleOrder.builder()
                .type(AftersaleType.REPAIR.getCode())
                .build();

        assertEquals(AftersaleType.REPAIR, order.getAftersaleType());
    }

    private AftersaleOrderPo buildPo() {
        AftersaleOrderPo po = new AftersaleOrderPo();
        po.setId(1L);
        po.setShopId(2L);
        po.setOrderId(3L);
        po.setType(AftersaleType.RETURN.getCode());
        po.setReason("reason");
        po.setStatus(0);
        po.setConclusion("结论");
        po.setGmtCreate(LocalDateTime.now());
        po.setGmtModified(LocalDateTime.now());
        return po;
    }
}
