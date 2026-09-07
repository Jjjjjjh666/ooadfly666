package cn.edu.xmu.service.model.strategy;

import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.service.client.LogisticsClient;
import cn.edu.xmu.service.client.dto.CreatePackageRequest;
import cn.edu.xmu.service.client.dto.CreatePackageResponse;
import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderStatus;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.impl.MailInAssignStrategy;
import cn.edu.xmu.service.model.strategy.impl.OnsiteAssignStrategy;
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
 * 服务单派工策略测试
 * 重点测试：寄修派工时生成运单的逻辑
 */
@ExtendWith(MockitoExtension.class)
class ServiceOrderAssignStrategyTest {

    @Mock
    private LogisticsClient logisticsClient;
    
    private MailInAssignStrategy mailInAssignStrategy;
    private OnsiteAssignStrategy onsiteAssignStrategy;

    @BeforeEach
    void setUp() {
        mailInAssignStrategy = new MailInAssignStrategy(logisticsClient);
        onsiteAssignStrategy = new OnsiteAssignStrategy();
    }

    /**
     * 测试：寄修派工应该创建运单
     * 这是核心测试用例
     */
    @Test
    void mailInAssignShouldCreatePackage() {
        // Arrange: 准备测试数据
        ServiceOrder order = ServiceOrder.builder()
                .id(100L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())  // 寄修
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .consignee("张三")
                .mobile("13800138000")
                .address("福建省厦门市思明区厦门大学")
                .regionId(123456L)
                .productId(888L)
                .build();
        
        // Mock: 模拟物流模块返回成功响应
        CreatePackageResponse mockResponse = new CreatePackageResponse();
        mockResponse.setId(999L);  // 运单ID
        mockResponse.setBillCode("SF1234567890");  // 运单号
        mockResponse.setPayMethod(2);
        mockResponse.setStatus(0);
        
        InternalReturnObject<CreatePackageResponse> returnObject = 
            new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        returnObject.setData(mockResponse);
        
        when(logisticsClient.createPackage(anyLong(), anyString(), any(CreatePackageRequest.class)))
            .thenReturn(returnObject);
        
        // Act: 执行派工策略
        mailInAssignStrategy.assign(order, 66L);  // 派工给师傅66
        
        // Assert: 验证结果
        // 1. 验证状态已更新为已派工
        assertEquals(ServiceOrderStatus.ASSIGNED, order.getStatus());
        
        // 2. 验证师傅ID已设置
        assertEquals(66L, order.getServiceStaffId());
        
        // 3. 验证运单ID已保存
        assertEquals(999L, order.getExpressId());
        
        // 4. 验证是否调用了物流客户端
        ArgumentCaptor<CreatePackageRequest> captor = 
            ArgumentCaptor.forClass(CreatePackageRequest.class);
        verify(logisticsClient, times(1))
            .createPackage(eq(1L), anyString(), captor.capture());
        
        // 5. 验证请求参数是否正确
        CreatePackageRequest request = captor.getValue();
        assertEquals(0L, request.getContractId());  // 自动选择合同
        assertEquals(2, request.getPayMethod());     // 收方付
        assertNotNull(request.getAddress());
        assertEquals("张三", request.getAddress().getName());  // ✅ 客户姓名（发件人）
        assertEquals("13800138000", request.getAddress().getMobile());  // ✅ 客户电话
        assertEquals(123456L, request.getAddress().getRegionId());  // ✅ 客户地区
        assertNotNull(request.getCargoDetails());
        assertEquals(1, request.getCargoDetails().size());
        assertEquals("待维修产品", request.getCargoDetails().get(0).getName());
    }

    /**
     * 测试：寄修派工时物流API调用失败应该抛出异常
     */
    @Test
    void mailInAssignShouldThrowExceptionWhenLogisticsFails() {
        // Arrange
        ServiceOrder order = ServiceOrder.builder()
                .id(100L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .consignee("李四")
                .mobile("13900139000")
                .address("测试地址")
                .productId(777L)
                .build();
        
        // Mock: 模拟物流模块抛出异常
        when(logisticsClient.createPackage(anyLong(), anyString(), any()))
            .thenThrow(new RuntimeException("物流服务不可用"));
        
        // Act & Assert: 验证异常
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> mailInAssignStrategy.assign(order, 77L)
        );
        
        assertTrue(exception.getMessage().contains("创建寄修运单失败"));
        
        // 验证物流客户端被调用了
        verify(logisticsClient, times(1))
            .createPackage(anyLong(), anyString(), any());
    }

    /**
     * 测试：寄修策略应该支持寄修类型
     */
    @Test
    void mailInStrategyShouldSupportMailInType() {
        assertTrue(mailInAssignStrategy.support(ServiceOrderType.MAIL_IN_REPAIR.getCode()));
        assertFalse(mailInAssignStrategy.support(ServiceOrderType.ONSITE_REPAIR.getCode()));
    }

    /**
     * 测试：上门维修派工不应该创建运单
     */
    @Test
    void onsiteAssignShouldNotCreatePackage() {
        // Arrange
        ServiceOrder order = ServiceOrder.builder()
                .id(200L)
                .type(ServiceOrderType.ONSITE_REPAIR.getCode())  // 上门维修
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .consignee("王五")
                .mobile("13700137000")
                .address("测试地址")
                .build();
        
        // Act: 执行派工策略
        onsiteAssignStrategy.assign(order, 88L);
        
        // Assert: 验证结果
        // 1. 状态已更新
        assertEquals(ServiceOrderStatus.ASSIGNED, order.getStatus());
        
        // 2. 师傅ID已设置
        assertEquals(88L, order.getServiceStaffId());
        
        // 3. 运单ID应该为空（上门维修不需要运单）
        assertNull(order.getExpressId());
    }

    /**
     * 测试：上门策略应该支持上门维修类型
     */
    @Test
    void onsiteStrategyShouldSupportOnsiteType() {
        assertTrue(onsiteAssignStrategy.support(ServiceOrderType.ONSITE_REPAIR.getCode()));
        assertFalse(onsiteAssignStrategy.support(ServiceOrderType.MAIL_IN_REPAIR.getCode()));
    }

    /**
     * 测试：寄修派工应该正确传递所有参数到物流API
     */
    @Test
    void mailInAssignShouldPassCorrectParametersToLogisticsAPI() {
        // Arrange
        ServiceOrder order = ServiceOrder.builder()
                .id(300L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .consignee("测试客户")
                .mobile("13800000000")
                .address("测试地址")
                .regionId(888888L)
                .productId(666L)
                .build();
        
        // Mock
        CreatePackageResponse mockResponse = new CreatePackageResponse();
        mockResponse.setId(555L);
        mockResponse.setBillCode("TEST123");
        
        InternalReturnObject<CreatePackageResponse> returnObject = 
            new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        returnObject.setData(mockResponse);
        
        when(logisticsClient.createPackage(anyLong(), anyString(), any()))
            .thenReturn(returnObject);
        
        // Act
        mailInAssignStrategy.assign(order, 99L);
        
        // Assert: 详细验证请求参数
        ArgumentCaptor<CreatePackageRequest> captor = 
            ArgumentCaptor.forClass(CreatePackageRequest.class);
        verify(logisticsClient).createPackage(eq(1L), anyString(), captor.capture());
        
        CreatePackageRequest request = captor.getValue();
        
        // 验证合同ID
        assertEquals(0L, request.getContractId(), "合同ID应该为0（自动选择）");
        
        // 验证支付方式
        assertEquals(2, request.getPayMethod(), "支付方式应该为2（收方付）");
        
        // 验证地址信息（客户地址 - 发件人）
        assertNotNull(request.getAddress(), "地址不能为空");
        assertEquals("测试客户", request.getAddress().getName());  // ✅ 客户姓名
        assertEquals("13800000000", request.getAddress().getMobile());  // ✅ 客户电话
        assertEquals(888888L, request.getAddress().getRegionId());  // ✅ 客户地区
        assertEquals("测试地址", request.getAddress().getAddress());  // ✅ 客户详细地址
        
        // 验证货物信息
        assertNotNull(request.getCargoDetails(), "货物信息不能为空");
        assertEquals(1, request.getCargoDetails().size(), "应该有1件货物");
        CreatePackageRequest.CargoDetail cargo = request.getCargoDetails().get(0);
        assertEquals(666L, cargo.getId(), "货物ID应该等于产品ID");
        assertEquals("待维修产品", cargo.getName());
        assertEquals(1, cargo.getCount());
        assertEquals("件", cargo.getUnit());
        assertEquals(1000, cargo.getWeight());
    }
}

