package cn.edu.xmu.service.service;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.service.client.LogisticsClient;
import cn.edu.xmu.service.client.dto.CreatePackageRequest;
import cn.edu.xmu.service.client.dto.CreatePackageResponse;
import cn.edu.xmu.service.dao.ServiceOrderRepository;
import cn.edu.xmu.service.model.ServiceOrder;
import cn.edu.xmu.service.model.ServiceOrderStatus;
import cn.edu.xmu.service.model.ServiceOrderType;
import cn.edu.xmu.service.model.strategy.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 服务单服务集成测试
 * 使用真实的策略实例，只Mock外部API（LogisticsClient）
 * 验证完整的业务流程执行
 */
@ExtendWith(MockitoExtension.class)
class ServiceOrderServiceIntegrationTest {

    // ✅ 只Mock外部依赖
    @Mock
    private LogisticsClient logisticsClient;
    @Mock
    private ServiceOrderRepository repository;

    // ✅ 使用真实的策略实例
    private ServiceOrderService serviceOrderService;

    @BeforeEach
    void setUp() {
        // 创建真实的策略实例（注入Mock的LogisticsClient）
        MailInAssignStrategy mailInAssignStrategy = new MailInAssignStrategy(logisticsClient);
        OnsiteAssignStrategy onsiteAssignStrategy = new OnsiteAssignStrategy();

        MailInCancelStrategy mailInCancelStrategy = new MailInCancelStrategy(logisticsClient);
        OnsiteCancelStrategy onsiteCancelStrategy = new OnsiteCancelStrategy();

        MailInCompleteStrategy mailInCompleteStrategy = new MailInCompleteStrategy();
        OnsiteCompleteStrategy onsiteCompleteStrategy = new OnsiteCompleteStrategy();

        // 创建Service（注入真实策略）
        serviceOrderService = new ServiceOrderService(
                repository,
                List.of(mailInAssignStrategy, onsiteAssignStrategy),
                List.of(mailInCancelStrategy, onsiteCancelStrategy),
                List.of(mailInCompleteStrategy, onsiteCompleteStrategy)
        );
    }

    // ==================== 服务单创建测试 ====================

    @Test
    void createServiceOrder_ShouldFillDefaultValues() {
        // Arrange
        ArgumentCaptor<ServiceOrder> captor = ArgumentCaptor.forClass(ServiceOrder.class);
        doAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        }).when(repository).create(any(ServiceOrder.class));

        // Act - 传入空字符串
        Long id = serviceOrderService.createServiceOrder(1L, 2L, null, "", "", "");

        // Assert
        assertEquals(100L, id);
        verify(repository).create(captor.capture());

        ServiceOrder created = captor.getValue();
        assertEquals("客户", created.getConsignee()); // 默认值
        assertEquals("待填写地址", created.getAddress()); // 默认值
        assertEquals("待填写电话", created.getMobile()); // 默认值
        assertEquals(0, created.getType()); // 默认类型
        assertEquals(ServiceOrderStatus.PENDING, created.getStatus());
        assertEquals(2L, created.getAftersaleId());
    }

    @Test
    void createServiceOrder_ShouldUseProvidedValues() {
        // Arrange
        ArgumentCaptor<ServiceOrder> captor = ArgumentCaptor.forClass(ServiceOrder.class);
        doAnswer(invocation -> {
            ServiceOrder order = invocation.getArgument(0);
            order.setId(101L);
            return order;
        }).when(repository).create(any());

        // Act - 传入具体值
        Long id = serviceOrderService.createServiceOrder(
                1L, 3L, 1, "张三", "厦门大学", "13800138000");

        // Assert
        verify(repository).create(captor.capture());
        ServiceOrder created = captor.getValue();
        assertEquals("张三", created.getConsignee());
        assertEquals("厦门大学", created.getAddress());
        assertEquals("13800138000", created.getMobile());
        assertEquals(1, created.getType());
    }

    // ==================== 服务单审核测试 ====================

    @Test
    void confirmServiceOrder_Approve_ShouldUpdateToBeAssigned() {
        // Arrange
        ServiceOrder order = buildOrder(ServiceOrderStatus.PENDING, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(5L)).thenReturn(order);

        // Act
        serviceOrderService.confirmServiceOrder(10L, 5L, true);

        // Assert
        assertEquals(ServiceOrderStatus.TO_BE_ASSIGNED, order.getStatus());
        assertEquals(10L, order.getServiceProviderId());
        verify(repository).save(order);
    }

    @Test
    void confirmServiceOrder_Reject_ShouldUpdateToRejected() {
        // Arrange
        ServiceOrder order = buildOrder(ServiceOrderStatus.PENDING, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(6L)).thenReturn(order);

        // Act
        serviceOrderService.confirmServiceOrder(10L, 6L, false);

        // Assert
        assertEquals(ServiceOrderStatus.REJECTED, order.getStatus());
        assertNull(order.getServiceProviderId()); // 拒绝时不设置服务商
        verify(repository).save(order);
    }

    @Test
    void confirmServiceOrder_InvalidStatus_ShouldThrowException() {
        // Arrange - 已派工状态不能审核
        ServiceOrder order = buildOrder(ServiceOrderStatus.ASSIGNED, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(7L)).thenReturn(order);

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                serviceOrderService.confirmServiceOrder(10L, 7L, true));

        verify(repository, never()).save(any());
    }

    // ==================== 服务单派工测试 ====================

    @Test
    void assignMailInServiceOrder_ShouldCreatePackage() {
        // Arrange
        ServiceOrder order = ServiceOrder.builder()
                .id(10L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .consignee("张三")
                .mobile("13800138000")
                .address("厦门市思明区厦门大学")
                .regionId(350203L)
                .productId(10L)
                .build();

        when(repository.findById(10L)).thenReturn(order);

        // Mock物流API返回
        CreatePackageResponse mockResponse = new CreatePackageResponse(999L, "SF1234567890", 2, 0);
        InternalReturnObject<CreatePackageResponse> returnObject = new InternalReturnObject<>();
        returnObject.setErrno(ReturnNo.OK.getErrNo());
        returnObject.setData(mockResponse);
        when(logisticsClient.createPackage(anyLong(), anyString(), any(CreatePackageRequest.class)))
                .thenReturn(returnObject);

        // Act
        serviceOrderService.assignServiceOrder(1L, 10L, 66L);

        // Assert - 验证业务逻辑
        assertEquals(ServiceOrderStatus.ASSIGNED, order.getStatus());
        assertEquals(66L, order.getServiceStaffId());
        assertEquals(999L, order.getExpressId()); // ⭐ 核心验证：运单ID已保存

        // Assert - 验证物流API被正确调用
        ArgumentCaptor<CreatePackageRequest> captor = ArgumentCaptor.forClass(CreatePackageRequest.class);
        verify(logisticsClient, times(1)).createPackage(eq(1L), anyString(), captor.capture());

        CreatePackageRequest request = captor.getValue();
        assertEquals(0L, request.getContractId());
        assertEquals(2, request.getPayMethod()); // 收方付
        assertEquals("张三", request.getAddress().getName());
        assertEquals("13800138000", request.getAddress().getMobile());
        assertEquals(350203L, request.getAddress().getRegionId());

        verify(repository).save(order);
    }

    @Test
    void assignOnsiteServiceOrder_ShouldNotCreatePackage() {
        // Arrange
        ServiceOrder order = buildOrder(ServiceOrderStatus.TO_BE_ASSIGNED, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(11L)).thenReturn(order);

        // Act
        serviceOrderService.assignServiceOrder(1L, 11L, 77L);

        // Assert
        assertEquals(ServiceOrderStatus.ASSIGNED, order.getStatus());
        assertEquals(77L, order.getServiceStaffId());
        assertNull(order.getExpressId()); // ⭐ 核心验证：上门维修没有运单

        // ⭐ 核心验证：物流API没有被调用
        verify(logisticsClient, never()).createPackage(anyLong(), anyString(), any());
        verify(repository).save(order);
    }

    @Test
    void assignServiceOrder_InvalidStatus_ShouldThrowException() {
        // Arrange - 待接收状态不能派工
        ServiceOrder order = buildOrder(ServiceOrderStatus.PENDING, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(12L)).thenReturn(order);

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                serviceOrderService.assignServiceOrder(1L, 12L, 88L));

        verify(logisticsClient, never()).createPackage(anyLong(), anyString(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void assignServiceOrder_UnsupportedType_ShouldThrowException() {
        // Arrange - 不支持的类型
        ServiceOrder order = ServiceOrder.builder()
                .id(13L)
                .type(999) // ⚠️ 不支持的类型
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .build();

        when(repository.findById(13L)).thenReturn(order);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                serviceOrderService.assignServiceOrder(1L, 13L, 99L));

        verify(repository, never()).save(any());
    }

    // ==================== 服务单验收测试 ====================

    @Test
    void receiveServiceOrder_MailInType_ShouldUpdateToReceived() {
        // Arrange
        ServiceOrder order = buildOrder(ServiceOrderStatus.ASSIGNED, ServiceOrderType.MAIL_IN_REPAIR);
        when(repository.findById(20L)).thenReturn(order);

        // Act
        serviceOrderService.receiveServiceOrder(2L, 20L);

        // Assert
        assertEquals(ServiceOrderStatus.RECEIVED, order.getStatus());
        verify(repository).save(order);
    }

    @Test
    void receiveServiceOrder_OnsiteType_ShouldThrowException() {
        // Arrange - 上门维修不能验收收件
        ServiceOrder order = buildOrder(ServiceOrderStatus.ASSIGNED, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(21L)).thenReturn(order);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                serviceOrderService.receiveServiceOrder(2L, 21L));

        assertTrue(exception.getMessage().contains("只有寄修类型的服务单才能验收收件"));
        verify(repository, never()).save(any());
    }

    @Test
    void receiveServiceOrder_InvalidStatus_ShouldThrowException() {
        // Arrange - 待派工状态不能验收
        ServiceOrder order = buildOrder(ServiceOrderStatus.TO_BE_ASSIGNED, ServiceOrderType.MAIL_IN_REPAIR);
        when(repository.findById(22L)).thenReturn(order);

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                serviceOrderService.receiveServiceOrder(2L, 22L));

        verify(repository, never()).save(any());
    }

    // ==================== 服务单完成测试 ====================

    @Test
    void completeOnsiteServiceOrder_ShouldUpdateToCompleted() {
        // Arrange - 上门维修：已派工 → 已完成
        ServiceOrder order = buildOrder(ServiceOrderStatus.ASSIGNED, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(30L)).thenReturn(order);

        // Act
        serviceOrderService.completeServiceOrder(1L, 30L);

        // Assert
        assertEquals(ServiceOrderStatus.COMPLETED, order.getStatus());
        verify(repository).save(order);
    }

    @Test
    void completeMailInServiceOrder_ShouldUpdateToCompleted() {
        // Arrange - 寄修：已收件 → 已完成
        ServiceOrder order = buildOrder(ServiceOrderStatus.RECEIVED, ServiceOrderType.MAIL_IN_REPAIR);
        when(repository.findById(31L)).thenReturn(order);

        // Act
        serviceOrderService.completeServiceOrder(1L, 31L);

        // Assert
        assertEquals(ServiceOrderStatus.COMPLETED, order.getStatus());
        verify(repository).save(order);
    }

    @Test
    void completeServiceOrder_InvalidStatusOrType_ShouldThrowException() {
        // Arrange - 寄修但状态是已派工（应该是已收件）
        ServiceOrder order = buildOrder(ServiceOrderStatus.ASSIGNED, ServiceOrderType.MAIL_IN_REPAIR);
        when(repository.findById(32L)).thenReturn(order);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                serviceOrderService.completeServiceOrder(1L, 32L));

        assertTrue(exception.getMessage().contains("当前状态和类型不支持完成操作"));
        verify(repository, never()).save(any());
    }

    // ==================== 服务单取消测试 ====================

    @Test
    void cancelMailInServiceOrder_WithExpressId_ShouldCancelPackage() {
        // Arrange - 寄修已收件，有运单
        ServiceOrder order = ServiceOrder.builder()
                .id(40L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.RECEIVED)
                .expressId(999L) // ⭐ 已有运单ID
                .build();

        when(repository.findById(40L)).thenReturn(order);

        // Mock返件运单创建
        CreatePackageResponse createResponse = new CreatePackageResponse(777L, "RT-001", 2, 0);
        InternalReturnObject<CreatePackageResponse> createReturn = new InternalReturnObject<>();
        createReturn.setErrno(ReturnNo.OK.getErrNo());
        createReturn.setData(createResponse);
        when(logisticsClient.createPackage(anyLong(), anyString(), any(CreatePackageRequest.class)))
                .thenReturn(createReturn);

        // Mock物流取消API
        InternalReturnObject<Void> cancelReturn = new InternalReturnObject<>();
        cancelReturn.setErrno(ReturnNo.OK.getErrNo());
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
                .thenReturn(cancelReturn);

        // Act
        serviceOrderService.cancelServiceOrder(1L, 40L);

        // Assert
        assertEquals(ServiceOrderStatus.CANCELED, order.getStatus());
        assertEquals(777L, order.getReturnExpressId());

        // ⭐ 核心验证：返件创建 + 取消原运单都被调用
        verify(logisticsClient, times(1))
                .createPackage(anyLong(), anyString(), any(CreatePackageRequest.class));
        verify(logisticsClient, times(1))
                .cancelPackage(eq(1L), eq(999L), anyString());
        verify(repository).save(order);
    }

    @Test
    void cancelMailInServiceOrder_WithoutExpressId_ShouldNotCallLogistics() {
        // Arrange - 待派工状态，还没有运单
        ServiceOrder order = ServiceOrder.builder()
                .id(41L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .expressId(null) // ⭐ 没有运单ID
                .build();

        when(repository.findById(41L)).thenReturn(order);

        // Act
        serviceOrderService.cancelServiceOrder(1L, 41L);

        // Assert
        assertEquals(ServiceOrderStatus.CANCELED, order.getStatus());

        // ⭐ 核心验证：物流API没有被调用
        verify(logisticsClient, never())
                .createPackage(anyLong(), anyString(), any(CreatePackageRequest.class));
        verify(logisticsClient, never())
                .cancelPackage(anyLong(), anyLong(), anyString());
        verify(repository).save(order);
    }

    @Test
    void cancelOnsiteServiceOrder_ShouldNotCallLogistics() {
        // Arrange - 上门维修
        ServiceOrder order = buildOrder(ServiceOrderStatus.ASSIGNED, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findById(42L)).thenReturn(order);

        // Act
        serviceOrderService.cancelServiceOrder(1L, 42L);

        // Assert
        assertEquals(ServiceOrderStatus.CANCELED, order.getStatus());

        // ⭐ 核心验证：物流API没有被调用（上门维修没有运单）
        verify(logisticsClient, never())
                .cancelPackage(anyLong(), anyLong(), anyString());
        verify(repository).save(order);
    }

    @Test
    void cancelServiceOrder_InvalidStatus_ShouldThrowException() {
        // Arrange - 已完成状态不能取消
        ServiceOrder order = buildOrder(ServiceOrderStatus.COMPLETED, ServiceOrderType.MAIL_IN_REPAIR);
        when(repository.findById(43L)).thenReturn(order);

        // Act & Assert
        assertThrows(BusinessException.class, () ->
                serviceOrderService.cancelServiceOrder(1L, 43L));

        verify(logisticsClient, never()).cancelPackage(anyLong(), anyLong(), anyString());
        verify(repository, never()).save(any());
    }

    // ==================== 内部API调用测试 ====================

    @Test
    void cancelServiceOrderByAftersale_Found_ShouldCancel() {
        // Arrange
        ServiceOrder order = buildOrder(ServiceOrderStatus.RECEIVED, ServiceOrderType.MAIL_IN_REPAIR);
        order.setExpressId(888L);
        when(repository.findByAftersaleId(100L)).thenReturn(order);

        // Mock返件运单创建
        CreatePackageResponse createResponse = new CreatePackageResponse(779L, "RT-AFT", 2, 0);
        InternalReturnObject<CreatePackageResponse> createReturn = new InternalReturnObject<>();
        createReturn.setErrno(ReturnNo.OK.getErrNo());
        createReturn.setData(createResponse);
        when(logisticsClient.createPackage(anyLong(), anyString(), any(CreatePackageRequest.class)))
                .thenReturn(createReturn);

        InternalReturnObject<Void> cancelReturn = new InternalReturnObject<>();
        cancelReturn.setErrno(ReturnNo.OK.getErrNo());
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
                .thenReturn(cancelReturn);

        // Act
        serviceOrderService.cancelServiceOrderByAftersale(100L, "售后取消");

        // Assert
        assertEquals(ServiceOrderStatus.CANCELED, order.getStatus());
        verify(logisticsClient).cancelPackage(anyLong(), eq(888L), anyString());
        verify(repository).save(order);
    }

    @Test
    void cancelServiceOrderByAftersale_NotFound_ShouldNotThrow() {
        // Arrange - 未找到服务单
        when(repository.findByAftersaleId(200L)).thenReturn(null);

        // Act & Assert - 不应该抛出异常
        assertDoesNotThrow(() ->
                serviceOrderService.cancelServiceOrderByAftersale(200L, "原因"));

        verify(logisticsClient, never()).cancelPackage(anyLong(), anyLong(), anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void cancelServiceOrderByAftersale_InvalidStatus_ShouldThrowException() {
        // Arrange - 已完成状态不能取消
        ServiceOrder order = buildOrder(ServiceOrderStatus.COMPLETED, ServiceOrderType.ONSITE_REPAIR);
        when(repository.findByAftersaleId(300L)).thenReturn(order);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                serviceOrderService.cancelServiceOrderByAftersale(300L, "原因"));

        assertTrue(exception.getMessage().contains("服务单状态不允许取消"));
        verify(repository, never()).save(any());
    }

    // ==================== 异常场景测试 ====================

    @Test
    void assignMailInServiceOrder_LogisticsApiFails_ShouldThrowException() {
        // Arrange
        ServiceOrder order = ServiceOrder.builder()
                .id(50L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.TO_BE_ASSIGNED)
                .consignee("测试")
                .mobile("13800138000")
                .address("地址")
                .regionId(123L)
                .productId(10L)
                .build();

        when(repository.findById(50L)).thenReturn(order);

        // Mock物流API抛出异常
        when(logisticsClient.createPackage(anyLong(), anyString(), any()))
                .thenThrow(new RuntimeException("物流服务不可用"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                serviceOrderService.assignServiceOrder(1L, 50L, 100L));

        assertTrue(exception.getMessage().contains("物流服务不可用"));
        verify(logisticsClient, times(1)).createPackage(anyLong(), anyString(), any());
    }

    @Test
    void cancelMailInServiceOrder_LogisticsApiFails_ShouldThrowException() {
        // Arrange
        ServiceOrder order = ServiceOrder.builder()
                .id(51L)
                .type(ServiceOrderType.MAIL_IN_REPAIR.getCode())
                .status(ServiceOrderStatus.RECEIVED)
                .expressId(999L)
                .build();

        when(repository.findById(51L)).thenReturn(order);

        // 返件运单创建成功
        CreatePackageResponse createResponse = new CreatePackageResponse(778L, "RT-FAIL", 2, 0);
        InternalReturnObject<CreatePackageResponse> createReturn = new InternalReturnObject<>();
        createReturn.setErrno(ReturnNo.OK.getErrNo());
        createReturn.setData(createResponse);
        when(logisticsClient.createPackage(anyLong(), anyString(), any(CreatePackageRequest.class)))
                .thenReturn(createReturn);

        // Mock物流取消API抛出异常
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("物流服务不可用"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                serviceOrderService.cancelServiceOrder(1L, 51L));

        assertTrue(exception.getMessage().contains("物流服务不可用"));
        verify(logisticsClient, times(1)).createPackage(anyLong(), anyString(), any(CreatePackageRequest.class));
    }

    // ==================== 辅助方法 ====================

    private ServiceOrder buildOrder(ServiceOrderStatus status, ServiceOrderType type) {
        return ServiceOrder.builder()
                .id(999L)
                .type(type.getCode())
                .status(status)
                .aftersaleId(300L)
                .consignee("测试客户")
                .mobile("13800138000")
                .address("测试地址")
                .regionId(123456L)
                .productId(10L)
                .build();
    }
}

