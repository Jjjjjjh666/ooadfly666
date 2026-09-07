package cn.edu.xmu.service;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.service.controller.dto.ServiceProviderDraftHistoryResponse;
import cn.edu.xmu.service.controller.dto.ServiceProviderDraftListResult;
import cn.edu.xmu.service.dao.ServiceProviderDraftRepository;
import cn.edu.xmu.service.dao.ServiceProviderRepository;
import cn.edu.xmu.service.model.DraftStatus;
import cn.edu.xmu.service.model.ServiceProvider;
import cn.edu.xmu.service.model.ServiceProviderDraft;
import cn.edu.xmu.service.service.ServiceProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 服务商服务测试
 * 测试服务商变更审核功能
 */
class ServiceProviderServiceTest {

    @Mock
    private ServiceProviderDraftRepository repository;
    @Mock
    private ServiceProviderRepository serviceProviderRepository;

    @InjectMocks
    private ServiceProviderService serviceProviderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testReviewDraft_Approve() {
        // 准备测试数据 - 审核通过
        Long draftId = 1L;
        Integer conclusion = 1;
        String opinion = "审核通过";
        
        ServiceProviderDraft draft = ServiceProviderDraft.builder()
                .id(draftId)
                .serviceProviderId(1L)
                .providerName("张三维修服务")
                .status(DraftStatus.PENDING)
                .contactPerson("张三")
                .contactPhone("13800138000")
                .address("厦门")
                .build();
        ServiceProvider provider = ServiceProvider.builder()
                .id(1L)
                .name("旧名称")
                .consignee("旧联系人")
                .address("旧地址")
                .mobile("旧电话")
                .build();

        // Mock行为
        when(repository.findById(draftId)).thenReturn(draft);
        when(serviceProviderRepository.findById(1L)).thenReturn(provider);

        // 执行测试
        serviceProviderService.reviewDraft(draftId, conclusion, opinion);

        // 验证结果
        verify(repository).findById(draftId);
        verify(repository).save(draft);
        verify(serviceProviderRepository).findById(1L);
        verify(serviceProviderRepository).save(provider);
        assertEquals(DraftStatus.APPROVED, draft.getStatus());
        assertEquals(opinion, draft.getOpinion());
        // 确认由草稿应用的新值
        assertEquals("张三维修服务", provider.getName());
        assertEquals("张三", provider.getConsignee());
        assertEquals("厦门", provider.getAddress());
        assertEquals("13800138000", provider.getMobile());
    }

    @Test
    void testReviewDraft_Reject() {
        // 准备测试数据 - 审核拒绝
        Long draftId = 2L;
        Integer conclusion = 0;
        String opinion = "资质不符合要求";
        
        ServiceProviderDraft draft = ServiceProviderDraft.builder()
                .id(draftId)
                .serviceProviderId(2L)
                .providerName("李四售后服务")
                .status(DraftStatus.PENDING)
                .build();

        // Mock行为
        when(repository.findById(draftId)).thenReturn(draft);

        // 执行测试
        serviceProviderService.reviewDraft(draftId, conclusion, opinion);

        // 验证结果
        verify(repository).findById(draftId);
        verify(repository).save(draft);
        verify(serviceProviderRepository, never()).findById(any());
        verify(serviceProviderRepository, never()).save(any());
        assertEquals(DraftStatus.REJECTED, draft.getStatus());
        assertEquals(opinion, draft.getOpinion());
    }

    @Test
    void testReviewDraft_InvalidConclusion() {
        // 准备测试数据 - 无效的审核结果
        Long draftId = 3L;
        Integer conclusion = 999;  // 无效值
        String opinion = "测试";
        
        ServiceProviderDraft draft = ServiceProviderDraft.builder()
                .id(draftId)
                .serviceProviderId(3L)
                .providerName("王五技术服务")
                .status(DraftStatus.PENDING)
                .build();

        // Mock行为
        when(repository.findById(draftId)).thenReturn(draft);

        // 执行测试并验证异常
        cn.edu.xmu.javaee.core.exception.BusinessException ex = assertThrows(
                cn.edu.xmu.javaee.core.exception.BusinessException.class,
                () -> serviceProviderService.reviewDraft(draftId, conclusion, opinion));
        assertEquals(cn.edu.xmu.javaee.core.model.ReturnNo.BAD_REQUEST, ex.getErrno());

        verify(repository).findById(draftId);
        verify(repository, never()).save(any());
        verify(serviceProviderRepository, never()).findById(any());
    }

    @Test
    void testReviewDraft_WithNullOpinion() {
        // 准备测试数据 - 意见为空
        Long draftId = 4L;
        Integer conclusion = 1;
        String opinion = null;
        
        ServiceProviderDraft draft = ServiceProviderDraft.builder()
                .id(draftId)
                .serviceProviderId(4L)
                .providerName("测试服务商")
                .status(DraftStatus.PENDING)
                .build();

        // Mock行为
        when(repository.findById(draftId)).thenReturn(draft);
        ServiceProvider provider = ServiceProvider.builder()
                .id(4L)
                .build();
        when(serviceProviderRepository.findById(4L)).thenReturn(provider);

        // 执行测试
        serviceProviderService.reviewDraft(draftId, conclusion, opinion);

        // 验证结果 - 应该使用默认意见
        verify(repository).findById(draftId);
        verify(repository).save(draft);
        verify(serviceProviderRepository).save(provider);
        assertEquals(DraftStatus.APPROVED, draft.getStatus());
        assertEquals("审核通过", draft.getOpinion());
    }

    @Test
    void reviewDraftShouldValidateDraftStatus() {
        ServiceProviderDraft draft = ServiceProviderDraft.builder()
                .id(30L)
                .serviceProviderId(5L)
                .status(DraftStatus.APPROVED)
                .build();
        when(repository.findById(30L)).thenReturn(draft);

        assertThrows(BusinessException.class, () -> serviceProviderService.reviewDraft(30L, 1, "opinion"));
        verify(serviceProviderRepository, never()).findById(any());
    }

    @Test
    void listDraftsShouldAssembleResponse() {
        ServiceProviderDraft draft = ServiceProviderDraft.builder()
                .id(1L)
                .serviceProviderId(10L)
                .providerName("张三维修服务")
                .contactPerson("张三")
                .contactPhone("13800138000")
                .address("厦门")
                .status(DraftStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.search(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of(draft));
        when(repository.count(any(), any(), any(), any())).thenReturn(1L);

        ServiceProviderDraftListResult result = serviceProviderService.listDrafts("张三", "张三", "厦门", "138", 1, 5);

        assertEquals(1, result.getList().size());
        assertEquals(1, result.getPage());
        assertEquals(5, result.getPageSize());
        assertEquals(1L, result.getTotal());
        assertTrue(result.getList().get(0).getChange().contains("服务商名称"));
    }

    @Test
    void getDraftHistoryShouldReturnReadableHistory() {
        LocalDateTime createTime = LocalDateTime.now().minusDays(1);
        LocalDateTime updateTime = LocalDateTime.now();
        ServiceProviderDraft draft = ServiceProviderDraft.builder()
                .id(2L)
                .serviceProviderId(20L)
                .providerName("历史服务商")
                .status(DraftStatus.APPROVED)
                .opinion("审核通过")
                .createdAt(createTime)
                .updatedAt(updateTime)
                .build();
        when(repository.findById(2L)).thenReturn(draft);

        ServiceProviderDraftHistoryResponse history = serviceProviderService.getDraftHistory(2L);

        assertEquals(2, history.getHistory().size());
        assertTrue(history.getHistory().get(0).contains("提交变更申请"));
        assertTrue(history.getHistory().get(1).contains("审核通过"));
    }
}

