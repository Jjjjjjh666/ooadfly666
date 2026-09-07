package cn.edu.xmu.aftersale.model.strategy;

import cn.edu.xmu.aftersale.client.LogisticsClient;
import cn.edu.xmu.aftersale.client.dto.CreatePackageRequest;
import cn.edu.xmu.aftersale.client.dto.CreatePackageResponse;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleStatus;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.aftersale.model.strategy.impl.ExchangeConfirmStrategy;
import cn.edu.xmu.aftersale.model.strategy.impl.RepairConfirmStrategy;
import cn.edu.xmu.aftersale.model.strategy.impl.ReturnConfirmStrategy;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 售后审核策略Mock测试
 * 重点测试：退货/换货审核通过时创建运单的逻辑
 */
@ExtendWith(MockitoExtension.class)
class AftersaleConfirmStrategyMockTest {

    @Mock
    private LogisticsClient logisticsClient;
    
    private ReturnConfirmStrategy returnConfirmStrategy;
    private ExchangeConfirmStrategy exchangeConfirmStrategy;

    @BeforeEach
    void setUp() {
        returnConfirmStrategy = new ReturnConfirmStrategy(logisticsClient);
        exchangeConfirmStrategy = new ExchangeConfirmStrategy(logisticsClient);
    }

    /**
     * 测试：退货审核通过应该创建运单 ⭐ 核心
     */
    @Test
    void returnConfirmApprovedShouldCreatePackage() {
        // Arrange: 准备待审核的退货售后单
        AftersaleOrder order = AftersaleOrder.builder()
                .id(100L)
                .shopId(1L)
                .orderId(200L)
                .type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.PENDING)
                .reason("商品质量问题")
                .build();
        
        // Mock: 模拟物流API返回运单ID=888
        CreatePackageResponse mockResponse = new CreatePackageResponse();
        mockResponse.setId(888L);
        mockResponse.setBillCode("RETURN123456");
        mockResponse.setPayMethod(2);
        mockResponse.setStatus(0);
        
        InternalReturnObject<CreatePackageResponse> returnObject = 
            new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        returnObject.setData(mockResponse);
        
        when(logisticsClient.createPackage(anyLong(), anyString(), any(CreatePackageRequest.class)))
            .thenReturn(returnObject);
        
        // Act: 执行审核（通过）
        returnConfirmStrategy.confirm(order, true, "同意退货");
        
        // Assert: 验证结果
        // 1. 状态已更新为待验收
        assertEquals(AftersaleStatus.TO_BE_RECEIVED, order.getStatus());
        
        // 2. 结论已记录
        assertEquals("同意退货", order.getConclusion());
        
        // 3. 运单ID已保存 ⭐ 核心验证
        assertEquals(888L, order.getExpressId());
        
        // 4. 验证物流API被调用
        ArgumentCaptor<CreatePackageRequest> captor = 
            ArgumentCaptor.forClass(CreatePackageRequest.class);
        verify(logisticsClient, times(1))
            .createPackage(eq(1L), anyString(), captor.capture());
        
        // 5. 验证请求参数
        CreatePackageRequest request = captor.getValue();
        assertEquals(0L, request.getContractId());  // 自动选择
        assertEquals(2, request.getPayMethod());     // 收方付
        assertNotNull(request.getAddress());
        assertEquals("商家仓库", request.getAddress().getName());
        assertEquals(1, request.getCargoDetails().size());
        assertEquals("退货商品", request.getCargoDetails().get(0).getName());
    }

    /**
     * 测试：换货审核通过应该创建运单 ⭐ 核心
     */
    @Test
    void exchangeConfirmApprovedShouldCreatePackage() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(101L)
                .shopId(2L)
                .orderId(201L)
                .type(AftersaleType.EXCHANGE.getCode())
                .status(AftersaleStatus.PENDING)
                .reason("尺寸不合适")
                .build();
        
        // Mock
        CreatePackageResponse mockResponse = new CreatePackageResponse();
        mockResponse.setId(999L);
        mockResponse.setBillCode("EXCHANGE654321");
        
        InternalReturnObject<CreatePackageResponse> returnObject = 
            new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        returnObject.setData(mockResponse);
        
        when(logisticsClient.createPackage(anyLong(), anyString(), any()))
            .thenReturn(returnObject);
        
        // Act
        exchangeConfirmStrategy.confirm(order, true, "同意换货");
        
        // Assert
        assertEquals(AftersaleStatus.TO_BE_RECEIVED, order.getStatus());
        assertEquals("同意换货", order.getConclusion());
        assertEquals(999L, order.getExpressId());  // ⭐ 运单ID已保存
        
        verify(logisticsClient, times(1))
            .createPackage(eq(2L), anyString(), any());
    }

    /**
     * 测试：退货审核拒绝不应该创建运单
     */
    @Test
    void returnConfirmRejectedShouldNotCreatePackage() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(102L)
                .shopId(1L)
                .type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.PENDING)
                .build();
        
        // Act: 审核拒绝
        returnConfirmStrategy.confirm(order, false, "不符合退货条件");
        
        // Assert
        assertEquals(AftersaleStatus.REJECTED, order.getStatus());
        assertEquals("不符合退货条件", order.getConclusion());
        assertNull(order.getExpressId());  // 没有运单ID
        
        // 验证没有调用物流API
        verify(logisticsClient, never())
            .createPackage(anyLong(), anyString(), any());
    }

    /**
     * 测试：换货审核拒绝不应该创建运单
     */
    @Test
    void exchangeConfirmRejectedShouldNotCreatePackage() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(103L)
                .shopId(1L)
                .type(AftersaleType.EXCHANGE.getCode())
                .status(AftersaleStatus.PENDING)
                .build();
        
        // Act
        exchangeConfirmStrategy.confirm(order, false, "商品已过换货期");
        
        // Assert
        assertEquals(AftersaleStatus.REJECTED, order.getStatus());
        assertNull(order.getExpressId());
        
        verify(logisticsClient, never())
            .createPackage(anyLong(), anyString(), any());
    }

    /**
     * 测试：物流API失败时应该抛出异常
     */
    @Test
    void returnConfirmShouldThrowExceptionWhenLogisticsFails() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(104L)
                .shopId(1L)
                .type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.PENDING)
                .build();
        
        // Mock: 物流API抛出异常
        when(logisticsClient.createPackage(anyLong(), anyString(), any()))
            .thenThrow(new RuntimeException("物流服务不可用"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> returnConfirmStrategy.confirm(order, true, "同意")
        );
        
        assertTrue(exception.getMessage().contains("创建退货运单失败"));
        
        verify(logisticsClient, times(1))
            .createPackage(anyLong(), anyString(), any());
    }

    /**
     * 测试：策略类型支持验证
     */
    @Test
    void returnStrategyShouldSupportReturnType() {
        assertTrue(returnConfirmStrategy.support(AftersaleType.RETURN.getCode()));
        assertFalse(returnConfirmStrategy.support(AftersaleType.EXCHANGE.getCode()));
        assertFalse(returnConfirmStrategy.support(AftersaleType.REPAIR.getCode()));
    }

    @Test
    void exchangeStrategyShouldSupportExchangeType() {
        assertTrue(exchangeConfirmStrategy.support(AftersaleType.EXCHANGE.getCode()));
        assertFalse(exchangeConfirmStrategy.support(AftersaleType.RETURN.getCode()));
        assertFalse(exchangeConfirmStrategy.support(AftersaleType.REPAIR.getCode()));
    }

    /**
     * 测试：验证传递给物流API的详细参数
     */
    @Test
    void shouldPassCorrectParametersToLogisticsAPI() {
        // Arrange
        AftersaleOrder order = AftersaleOrder.builder()
                .id(105L)
                .shopId(3L)
                .type(AftersaleType.RETURN.getCode())
                .status(AftersaleStatus.PENDING)
                .build();
        
        // Mock
        CreatePackageResponse mockResponse = new CreatePackageResponse();
        mockResponse.setId(777L);
        mockResponse.setBillCode("TEST999");
        
        InternalReturnObject<CreatePackageResponse> returnObject = 
            new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        returnObject.setData(mockResponse);
        
        when(logisticsClient.createPackage(anyLong(), anyString(), any()))
            .thenReturn(returnObject);
        
        // Act
        returnConfirmStrategy.confirm(order, true, "测试");
        
        // Assert: 详细验证参数
        ArgumentCaptor<CreatePackageRequest> captor = 
            ArgumentCaptor.forClass(CreatePackageRequest.class);
        verify(logisticsClient).createPackage(eq(3L), anyString(), captor.capture());
        
        CreatePackageRequest request = captor.getValue();
        
        // 验证合同ID
        assertEquals(0L, request.getContractId(), "合同ID应该为0（自动选择）");
        
        // 验证支付方式
        assertEquals(2, request.getPayMethod(), "支付方式应该为2（收方付）");
        
        // 验证地址信息
        assertNotNull(request.getAddress(), "地址不能为空");
        assertEquals("商家仓库", request.getAddress().getName());
        assertEquals("400-888-8888", request.getAddress().getMobile());
        
        // 验证货物信息
        assertNotNull(request.getCargoDetails(), "货物信息不能为空");
        assertEquals(1, request.getCargoDetails().size(), "应该有1件货物");
        assertEquals("退货商品", request.getCargoDetails().get(0).getName());
        assertEquals(1, request.getCargoDetails().get(0).getCount());
        assertEquals("件", request.getCargoDetails().get(0).getUnit());
    }
}

