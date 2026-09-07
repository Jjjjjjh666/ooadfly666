package cn.edu.xmu.service.dao;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.service.dao.po.ServiceOrderPo;
import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderStatus;
import cn.edu.xmu.service.model.ServiceOrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceOrderRepositoryTest {

    @Mock
    private ServiceOrderMapper mapper;

    private ServiceOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ServiceOrderRepository(mapper);
    }

    @Test
    void findByIdShouldReturnDomainObject() {
        when(mapper.findById(1L)).thenReturn(buildPo());

        ServiceOrder order = repository.findById(1L);

        assertEquals(1L, order.getId());
        assertEquals(ServiceOrderStatus.PENDING, order.getStatus());
        verify(mapper).findById(1L);
    }

    @Test
    void findByIdShouldThrowWhenMissing() {
        when(mapper.findById(1L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> repository.findById(1L));
    }

    @Test
    void saveShouldThrowWhenUpdateFails() {
        when(mapper.updateStatus(any(ServiceOrderPo.class))).thenReturn(0);

        assertThrows(BusinessException.class, () -> repository.save(buildOrder()));
    }

    @Test
    void saveShouldUpdateWhenMapperSucceeds() {
        when(mapper.updateStatus(any(ServiceOrderPo.class))).thenReturn(1);

        repository.save(buildOrder());

        verify(mapper).updateStatus(any(ServiceOrderPo.class));
    }

    @Test
    void createShouldSetGeneratedId() {
        doAnswer(invocation -> {
            ServiceOrderPo po = invocation.getArgument(0);
            po.setId(55L);
            return 1;
        }).when(mapper).insert(any(ServiceOrderPo.class));

        ServiceOrder result = repository.create(buildOrder());

        assertEquals(55L, result.getId());
        verify(mapper).insert(any(ServiceOrderPo.class));
    }

    private ServiceOrder buildOrder() {
        return ServiceOrder.builder()
                .id(1L)
                .type(ServiceOrderType.ONSITE_REPAIR.getCode())
                .consignee("张三")
                .address("地址")
                .mobile("138")
                .status(ServiceOrderStatus.PENDING)
                .aftersaleId(10L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ServiceOrderPo buildPo() {
        ServiceOrderPo po = new ServiceOrderPo();
        po.setId(1L);
        po.setType(ServiceOrderType.ONSITE_REPAIR.getCode());
        po.setConsignee("张三");
        po.setAddress("地址");
        po.setMobile("138");
        po.setStatus(0);
        po.setAftersaleId(10L);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        return po;
    }
}
