package cn.edu.xmu.service.model.strategy;

import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.service.client.LogisticsClient;
import cn.edu.xmu.service.client.dto.CreatePackageResponse;
import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderStatus;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.impl.MailInCancelStrategy;
import cn.edu.xmu.service.model.strategy.impl.OnsiteCancelStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 服务单取消策略测试
 * 重点测试：寄修取消时取消运单的逻辑
 */
@ExtendWith(MockitoExtension.class)
class ServiceOrderCancelStrategyTest {

    @Mock
    private LogisticsClient logisticsClient;
    
    private MailInCancelStrategy mailInCancelStrategy;
    private OnsiteCancelStrategy onsiteCancelStrategy;

    @BeforeEach
    void setUp() {
        mailInCancelStrategy = new MailInCancelStrategy(logisticsClient);
        onsiteCancelStrategy = new OnsiteCancelStrategy();
    }

    /**
     * 测试：寄修取消（已收件状态）应该取消运单 ⭐ 核心
     */
    @Test
    void mailInCancelWithReceivedStatusShouldCancelPackage() {
        // Arrange: 准备已收件的寄修服务单
        ServiceOrder order = ServiceOrder.builder()
                .id(200L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.RECEIVED)  // 已收件状态
                .expressId(888L)  // 有运单ID
                .build();

        InternalReturnObject<CreatePackageResponse> createReturn = new InternalReturnObject<>();
        createReturn.setErrno(ReturnNo.OK.getErrNo());
        createReturn.setData(new CreatePackageResponse(900L, "RT-TEST", 2, 0));
        when(logisticsClient.createPackage(anyLong(), anyString(), any())).thenReturn(createReturn);
        
        // Mock: 模拟物流API返回成功
        InternalReturnObject<Void> returnObject = new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenReturn(returnObject);
        
        // Act: 执行取消策略
        mailInCancelStrategy.cancel(order);
        
        // Assert: 验证结果
        // 1. 验证状态已更新为已取消
        assertEquals(ServiceOrderStatus.CANCELED, order.getStatus());
        
        // 2. 验证是否调用了物流取消API
        verify(logisticsClient, times(1))
            .cancelPackage(eq(1L), eq(888L), anyString());
    }

    /**
     * 测试：寄修取消（已派工状态）应该取消运单
     */
    @Test
    void mailInCancelWithAssignedStatusShouldCancelPackage() {
        // Arrange: 准备已派工的寄修服务单
        ServiceOrder order = ServiceOrder.builder()
                .id(201L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.ASSIGNED)  // 已派工状态
                .expressId(777L)  // 有运单ID（派工时创建的）
                .serviceStaffId(66L)
                .build();
        
        // Mock
        InternalReturnObject<Void> returnObject = new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenReturn(returnObject);
        
        // Act
        mailInCancelStrategy.cancel(order);
        
        // Assert
        assertEquals(ServiceOrderStatus.CANCELED, order.getStatus());
        
        // 验证取消运单API被调用
        verify(logisticsClient, times(1))
            .cancelPackage(eq(1L), eq(777L), anyString());
    }

    /**
     * 测试：寄修取消（待派工状态）不应该取消运单
     */
    @Test
    void mailInCancelWithToBeAssignedStatusShouldNotCancelPackage() {
        // Arrange: 准备待派工的寄修服务单（还没有运单）
        ServiceOrder order = ServiceOrder.builder()
                .id(202L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)  // 待派工状态
                .expressId(null)  // 没有运单ID
                .build();
        
        // Act
        mailInCancelStrategy.cancel(order);
        
        // Assert
        assertEquals(ServiceOrderStatus.CANCELED, order.getStatus());
        
        // 验证没有调用取消运单API
        verify(logisticsClient, never())
            .cancelPackage(anyLong(), anyLong(), anyString());
    }

    /**
     * 测试：取消运单失败时应该抛出异常
     */
    @Test
    void mailInCancelShouldThrowExceptionWhenCancelPackageFails() {
        // Arrange
        ServiceOrder order = ServiceOrder.builder()
                .id(203L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.RECEIVED)
                .expressId(666L)
                .build();

        InternalReturnObject<CreatePackageResponse> createReturn = new InternalReturnObject<>();
        createReturn.setErrno(ReturnNo.OK.getErrNo());
        createReturn.setData(new CreatePackageResponse(901L, "RT-FAIL", 2, 0));
        when(logisticsClient.createPackage(anyLong(), anyString(), any())).thenReturn(createReturn);
        
        // Mock: 模拟物流API抛出异常
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenThrow(new RuntimeException("物流服务不可用"));
        
        // Act & Assert: 验证异常
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> mailInCancelStrategy.cancel(order)
        );
        
        assertTrue(exception.getMessage().contains("取消运单失败"));
        
        // 验证物流API被调用了
        verify(logisticsClient, times(1))
            .cancelPackage(anyLong(), anyLong(), anyString());
    }

    /**
     * 测试：上门维修取消不应该调用物流API
     */
    @Test
    void onsiteCancelShouldNotCallLogisticsAPI() {
        // Arrange
        ServiceOrder order = ServiceOrder.builder()
                .id(300L)
                .type(ServiceOrderType.ONSITE_REPAIR.getCode())
                .status(ServiceOrderStatus.ASSIGNED)
                .serviceStaffId(88L)
                .build();
        
        // Act
        onsiteCancelStrategy.cancel(order);
        
        // Assert
        assertEquals(ServiceOrderStatus.CANCELED, order.getStatus());
        
        // 验证没有调用物流API（上门维修不需要运单）
        verify(logisticsClient, never())
            .cancelPackage(anyLong(), anyLong(), anyString());
    }

    /**
     * 测试：策略类型支持验证
     */
    @Test
    void mailInStrategyShouldSupportMailInType() {
        assertTrue(mailInCancelStrategy.support(ServiceOrderType.MAIL_IN_REPAIR.getCode()));
        assertFalse(mailInCancelStrategy.support(ServiceOrderType.ONSITE_REPAIR.getCode()));
    }

    @Test
    void onsiteStrategyShouldSupportOnsiteType() {
        assertTrue(onsiteCancelStrategy.support(ServiceOrderType.ONSITE_REPAIR.getCode()));
        assertFalse(onsiteCancelStrategy.support(ServiceOrderType.MAIL_IN_REPAIR.getCode()));
    }

    /**
     * 测试：验证传递给物流API的参数
     */
    @Test
    void mailInCancelShouldPassCorrectParametersToLogisticsAPI() {
        // Arrange
        ServiceOrder order = ServiceOrder.builder()
                .id(204L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.RECEIVED)
                .expressId(555L)  // 运单ID
                .build();

        InternalReturnObject<CreatePackageResponse> createReturn = new InternalReturnObject<>();
        createReturn.setErrno(ReturnNo.OK.getErrNo());
        createReturn.setData(new CreatePackageResponse(902L, "RT-PARAM", 2, 0));
        when(logisticsClient.createPackage(anyLong(), anyString(), any())).thenReturn(createReturn);
        
        // Mock
        InternalReturnObject<Void> returnObject = new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenReturn(returnObject);
        
        // Act
        mailInCancelStrategy.cancel(order);
        
        // Assert: 详细验证参数
        verify(logisticsClient).cancelPackage(
            eq(1L),      // shopId
            eq(555L),    // expressId（运单ID）
            anyString()  // authorization
        );
    }

    /**
     * 测试：多种状态的取消场景
     */
    @Test
    void mailInCancelShouldHandleDifferentStatuses() {
        // 场景1：已收件 + 有运单 → 应该取消运单
        ServiceOrder order1 = ServiceOrder.builder()
                .id(301L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.RECEIVED)
                .expressId(111L)
                .build();

        InternalReturnObject<CreatePackageResponse> createReturn = new InternalReturnObject<>();
        createReturn.setErrno(ReturnNo.OK.getErrNo());
        createReturn.setData(new CreatePackageResponse(903L, "RT-BATCH", 2, 0));
        when(logisticsClient.createPackage(anyLong(), anyString(), any())).thenReturn(createReturn);
        
        InternalReturnObject<Void> returnObject = new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenReturn(returnObject);
        
        mailInCancelStrategy.cancel(order1);
        verify(logisticsClient, times(1)).cancelPackage(anyLong(), eq(111L), anyString());
        
        // 场景2：已派工 + 有运单 → 应该取消运单
        reset(logisticsClient);
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenReturn(returnObject);
        
        ServiceOrder order2 = ServiceOrder.builder()
                .id(302L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.ASSIGNED)
                .expressId(222L)
                .build();
        
        mailInCancelStrategy.cancel(order2);
        verify(logisticsClient, times(1)).cancelPackage(anyLong(), eq(222L), anyString());
        
        // 场景3：待派工 + 无运单 → 不应该取消运单
        reset(logisticsClient);
        
        ServiceOrder order3 = ServiceOrder.builder()
                .id(303L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .expressId(null)
                .build();
        
        mailInCancelStrategy.cancel(order3);
        verify(logisticsClient, never()).cancelPackage(anyLong(), anyLong(), anyString());
    }
}
