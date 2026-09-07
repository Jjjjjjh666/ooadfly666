package cn.edu.xmu.aftersale.model.strategy;

import cn.edu.xmu.aftersale.client.LogisticsClient;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleStatus;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.impl.ExchangeCancelStrategy;
import cn.edu.xmu.aftersale.model.strategy.impl.RepairCancelStrategy;
import cn.edu.xmu.aftersale.model.strategy.impl.ReturnCancelStrategy;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 售后取消策略Mock测试
 * 重点测试：退货/换货取消时取消运单的逻辑
 */
@ExtendWith(MockitoExtension.class)
class AftersaleCancelStrategyMockTest {

    @Mock
    private LogisticsClient logisticsClient;
    
    @Mock
    private cn.edu.xmu.aftersale.client.ServiceClient serviceClient;
    
    private ReturnCancelStrategy returnCancelStrategy;
    private ExchangeCancelStrategy exchangeCancelStrategy;
    private RepairCancelStrategy repairCancelStrategy;

    @BeforeEach
    void setUp() {
        returnCancelStrategy = new ReturnCancelStrategy(logisticsClient);
        exchangeCancelStrategy = new ExchangeCancelStrategy(logisticsClient);
        repairCancelStrategy = new RepairCancelStrategy(serviceClient);
    }

    /**
     * 测试：退货取消时应该取消运单 ⭐ 核心
     * 场景：售后单状态为待验收，有expressId
     */
    @Test
    void returnCancelShouldCancelPackageWhenExpressIdExists() {
        // Arrange: 待验收状态的退货单，已有运单ID
        AftersaleOrder order = AftersaleOrder.builder()
                .id(100L)
                .shopId(1L)
                .orderId(200L)
                .type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED)  // 待验收状态
                .expressId(888L)  // ⭐ 已有运单ID
                .build();
        
        // Mock: 物流取消API返回成功
        InternalReturnObject<Void> returnObject = new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        returnObject.setErrmsg("成功");
        
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenReturn(returnObject);
        
        // Act: 执行取消
        returnCancelStrategy.cancel(order, "客户不想退了");
        
        // Assert
        // 1. 售后单状态已变为已取消
        assertEquals(AftersaleStatus.CANCELLED, order.getStatus());
        
        // 2. ⭐ 核心验证：物流取消API被调用
        verify(logisticsClient, times(1))
            .cancelPackage(eq(1L), eq(888L), anyString());
    }

    /**
     * 测试：换货取消时应该取消运单 ⭐ 核心
     */
    @Test
    void exchangeCancelShouldCancelPackageWhenExpressIdExists() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(101L)
                .shopId(2L)
                .type(AftersaleType.EXCHANGE.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED)
                .expressId(999L)  // ⭐ 已有运单ID
                .build();
        
        // Mock
        InternalReturnObject<Void> returnObject = new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenReturn(returnObject);
        
        // Act
        exchangeCancelStrategy.cancel(order, "商品已收到");
        
        // Assert
        assertEquals(AftersaleStatus.CANCELLED, order.getStatus());
        verify(logisticsClient, times(1))
            .cancelPackage(eq(2L), eq(999L), anyString());
    }

    /**
     * 测试：无运单ID时不应该调用物流API
     * 场景：售后单还在待审核阶段，还没有创建运单
     */
    @Test
    void returnCancelShouldNotCallLogisticsWhenNoExpressId() {
        // Arrange: 待验收状态，但没有运单ID
        AftersaleOrder order = AftersaleOrder.builder()
                .id(102L)
                .shopId(1L)
                .type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED)
                .expressId(null)  // ⭐ 没有运单ID
                .build();
        
        // Act
        returnCancelStrategy.cancel(order, "取消");
        
        // Assert
        assertEquals(AftersaleStatus.CANCELLED, order.getStatus());
        
        // ⭐ 验证物流API没有被调用
        verify(logisticsClient, never())
            .cancelPackage(anyLong(), anyLong(), anyString());
    }

    /**
     * 测试：换货无运单时不调用物流API
     */
    @Test
    void exchangeCancelShouldNotCallLogisticsWhenNoExpressId() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(103L)
                .shopId(1L)
                .type(AftersaleType.EXCHANGE.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED)
                .expressId(null)
                .build();
        
        // Act
        exchangeCancelStrategy.cancel(order, "取消");
        
        // Assert
        assertEquals(AftersaleStatus.CANCELLED, order.getStatus());
        verify(logisticsClient, never())
            .cancelPackage(anyLong(), anyLong(), anyString());
    }

    /**
     * 测试：物流取消API失败时应该抛出异常
     */
    @Test
    void returnCancelShouldThrowExceptionWhenLogisticsFails() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(104L)
                .shopId(1L)
                .type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED)
                .expressId(777L)
                .build();
        
        // Mock: 物流API返回失败
        InternalReturnObject<Void> returnObject = new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.INTERNAL_SERVER_ERR.getErrNo());
        returnObject.setErrmsg("物流服务异常");
        
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenThrow(new RuntimeException("物流服务不可用"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> returnCancelStrategy.cancel(order, "取消")
        );
        
        assertTrue(exception.getMessage().contains("取消退货运单失败"));
        
        verify(logisticsClient, times(1))
            .cancelPackage(eq(1L), eq(777L), anyString());
    }

    /**
     * 测试：换货取消时物流API失败应该抛出异常
     */
    @Test
    void exchangeCancelShouldThrowExceptionWhenLogisticsFails() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(105L)
                .shopId(1L)
                .type(AftersaleType.EXCHANGE.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED)
                .expressId(666L)
                .build();
        
        // Mock
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenThrow(new RuntimeException("网络超时"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> exchangeCancelStrategy.cancel(order, "取消")
        );
        
        assertTrue(exception.getMessage().contains("取消换货运单失败"));
    }

    /**
     * 测试：验证物流取消API的调用参数
     */
    @Test
    void shouldPassCorrectParametersToLogisticsCancelAPI() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(106L)
                .shopId(5L)  // shopId=5
                .type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED)
                .expressId(123L)  // expressId=123
                .build();
        
        // Mock
        InternalReturnObject<Void> returnObject = new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenReturn(returnObject);
        
        // Act
        returnCancelStrategy.cancel(order, "测试");
        
        // Assert: 验证参数
        verify(logisticsClient).cancelPackage(
            eq(5L),    // shopId
            eq(123L),  // expressId
            anyString() // authorization token
        );
    }

    /**
     * 测试：策略类型支持验证
     */
    @Test
    void returnCancelStrategyShouldSupportReturnType() {
        assertTrue(returnCancelStrategy.support(AftersaleType.RETURN.getCode()));
        assertFalse(returnCancelStrategy.support(AftersaleType.EXCHANGE.getCode()));
        assertFalse(returnCancelStrategy.support(AftersaleType.REPAIR.getCode()));
    }

    @Test
    void exchangeCancelStrategyShouldSupportExchangeType() {
        assertTrue(exchangeCancelStrategy.support(AftersaleType.EXCHANGE.getCode()));
        assertFalse(exchangeCancelStrategy.support(AftersaleType.RETURN.getCode()));
        assertFalse(exchangeCancelStrategy.support(AftersaleType.REPAIR.getCode()));
    }

    /**
     * 测试：维修取消不需要调用物流API
     * 因为维修流程不涉及客户寄回商品
     */
    @Test
    void repairCancelShouldNotCallLogistics() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(107L)
                .shopId(1L)
                .type(AftersaleType.REPAIR.getCode())
                .status(AftersaleStatus.TO_BE_COMPLETED)
                .build();
        
        // Mock ServiceClient (维修取消会调用服务模块)
        cn.edu.xmu.javaee.core.model.ReturnObject mockReturnObject = new cn.edu.xmu.javaee.core.model.ReturnObject();
        when(serviceClient.cancelServiceOrder(anyLong(), anyLong(), anyString())).thenReturn(mockReturnObject);
        
        // Act
        repairCancelStrategy.cancel(order, "不修了");
        
        // Assert
        assertEquals(AftersaleStatus.CANCELLED, order.getStatus());
        
        // ⭐ 验证物流API完全没有被调用
        verifyNoInteractions(logisticsClient);
        
        // 验证服务模块API被调用
        verify(serviceClient, times(1)).cancelServiceOrder(eq(1L), eq(107L), eq("不修了"));
    }

    /**
     * 测试：批量取消场景
     * 模拟多个售后单同时取消
     */
    @Test
    void shouldCancelMultiplePackagesCorrectly() {
        // Arrange: 3个退货单
        AftersaleOrder order1 = AftersaleOrder.builder()
                .id(201L).shopId(1L).type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED).expressId(1001L).build();
        
        AftersaleOrder order2 = AftersaleOrder.builder()
                .id(202L).shopId(1L).type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED).expressId(1002L).build();
        
        AftersaleOrder order3 = AftersaleOrder.builder()
                .id(203L).shopId(1L).type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.TO_BE_RECEIVED).expressId(null).build();  // 无运单
        
        // Mock
        InternalReturnObject<Void> returnObject = new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
            .thenReturn(returnObject);
        
        // Act: 批量取消
        returnCancelStrategy.cancel(order1, "批量取消");
        returnCancelStrategy.cancel(order2, "批量取消");
        returnCancelStrategy.cancel(order3, "批量取消");
        
        // Assert
        assertEquals(AftersaleStatus.CANCELLED, order1.getStatus());
        assertEquals(AftersaleStatus.CANCELLED, order2.getStatus());
        assertEquals(AftersaleStatus.CANCELLED, order3.getStatus());
        
        // 只有前两个有运单，所以只调用2次
        verify(logisticsClient, times(2))
            .cancelPackage(anyLong(), anyLong(), anyString());
        
        verify(logisticsClient, times(1))
            .cancelPackage(eq(1L), eq(1001L), anyString());
        
        verify(logisticsClient, times(1))
            .cancelPackage(eq(1L), eq(1002L), anyString());
    }
}

