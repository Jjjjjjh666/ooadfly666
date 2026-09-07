package cn.edu.xmu.service.model;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.service.dao.po.ServiceOrderPo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ServiceOrderTest {

    @Test
    void fromPoShouldConvertFields() {
        ServiceOrderPo po = buildPo();
        po.setStatus(3);

        ServiceOrder order = ServiceOrder.fromPo(po);

        assertEquals(ServiceOrderStatus.RECEIVED, order.getStatus());
        assertEquals(po.getConsignee(), order.getConsignee());
    }

    @Test
    void fromPoShouldFallbackToPendingWhenStatusNull() {
        ServiceOrderPo po = buildPo();
        po.setStatus(null);

        ServiceOrder order = ServiceOrder.fromPo(po);

        assertEquals(ServiceOrderStatus.PENDING, order.getStatus());
    }

    @Test
    void toPoShouldMirrorDomainValues() {
        LocalDateTime now = LocalDateTime.now();
        ServiceOrder order = ServiceOrder.builder()
                .id(2L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .consignee("张三")
                .address("厦门")
                .mobile("13800138000")
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .description("desc")
                .serviceStaffId(10L)
                .serviceProviderId(20L)
                .serviceContractId(30L)
                .serviceId(40L)
                .customerId(50L)
                .regionId(60L)
                .productId(70L)
                .aftersaleId(80L)
                .expressId(90L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ServiceOrderPo po = order.toPo();

        assertEquals(order.getId(), po.getId());
        assertEquals(Integer.valueOf(ServiceOrderStatus.TO_BE_ASSIGNED.ordinal()), po.getStatus());
        assertEquals(order.getExpressId(), po.getExpressId());
    }

    @Test
    void checkPendingStatusShouldThrowWhenInvalid() {
        ServiceOrder order = ServiceOrder.builder()
                .status(ServiceOrderStatus.CANCELED)
                .build();

        assertThrows(BusinessException.class, order::checkPendingStatus);
    }

    @Test
    void checkToBeAssignedStatusShouldThrowWhenInvalid() {
        ServiceOrder order = ServiceOrder.builder()
                .status(ServiceOrderStatus.ASSIGNED)
                .build();

        assertThrows(BusinessException.class, order::checkToBeAssignedStatus);
    }

    @Test
    void checkAssignedStatusShouldThrowWhenInvalid() {
        ServiceOrder order = ServiceOrder.builder()
                .status(ServiceOrderStatus.PENDING)
                .build();

        assertThrows(BusinessException.class, order::checkAssignedStatus);
    }

    @Test
    void checkCanCancelShouldValidate() {
        ServiceOrder order = ServiceOrder.builder()
                .status(ServiceOrderStatus.ASSIGNED)
                .build();
        assertDoesNotThrow(order::checkCanCancel);

        order.setStatus(ServiceOrderStatus.COMPLETED);
        assertThrows(BusinessException.class, order::checkCanCancel);
    }

    @Test
    void stateTransitionsShouldUpdateFields() {
        ServiceOrder order = ServiceOrder.builder()
                .status(ServiceOrderStatus.PENDING)
                .build();

        order.approve(5L);
        assertEquals(ServiceOrderStatus.TO_BE_ASSIGNED, order.getStatus());
        assertEquals(5L, order.getServiceProviderId());

        order.reject();
        assertEquals(ServiceOrderStatus.REJECTED, order.getStatus());

        order.setStatus(ServiceOrderStatus.TO_BE_ASSIGNED);
        order.assign(7L);
        assertEquals(ServiceOrderStatus.ASSIGNED, order.getStatus());
        assertEquals(7L, order.getServiceStaffId());

        order.receive();
        assertEquals(ServiceOrderStatus.RECEIVED, order.getStatus());

        order.complete();
        assertEquals(ServiceOrderStatus.COMPLETED, order.getStatus());

        order.cancel();
        assertEquals(ServiceOrderStatus.CANCELED, order.getStatus());
    }

    @Test
    void getServiceOrderTypeShouldConvertValue() {
        ServiceOrder order = ServiceOrder.builder()
                .type(ServiceOrderType.ONSITE_REPAIR.getCode())
                .build();

        assertEquals(ServiceOrderType.ONSITE_REPAIR, order.getServiceOrderType());
    }

    private ServiceOrderPo buildPo() {
        ServiceOrderPo po = new ServiceOrderPo();
        po.setId(1L);
        po.setType(ServiceOrderType.ONSITE_REPAIR.getCode());
        po.setConsignee("张三");
        po.setAddress("厦门");
        po.setMobile("138");
        po.setStatus(0);
        po.setDescription("desc");
        po.setServiceStaffId(10L);
        po.setServiceProviderId(20L);
        po.setServiceContractId(30L);
        po.setServiceId(40L);
        po.setCustomerId(50L);
        po.setRegionId(60L);
        po.setProductId(70L);
        po.setAftersaleId(80L);
        po.setExpressId(90L);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        return po;
    }
}
