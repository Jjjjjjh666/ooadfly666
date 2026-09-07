package cn.edu.xmu.service.controller;

import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.service.ServiceApplication;
import cn.edu.xmu.service.dao.ServiceProviderDraftRepository;
import cn.edu.xmu.service.dao.ServiceProviderRepository;
import cn.edu.xmu.service.model.DraftStatus;
import cn.edu.xmu.service.model.ServiceProvider;
import cn.edu.xmu.service.model.ServiceProviderDraft;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 服务商控制器Web集成测试
 * 
 * @author Test Team
 */
@SpringBootTest(classes = ServiceApplication.class)
@AutoConfigureMockMvc
@DisplayName("服务商控制器Web集成测试")
public class ServiceProviderControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServiceProviderDraftRepository draftRepository;

    @Autowired
    private ServiceProviderRepository providerRepository;

    // ==================== 审核服务商变更API测试 ====================

    @Nested
    @DisplayName("平台管理员审核服务商变更API - PUT /draft/{draftid}/review")
    class ReviewDraftTests {

        @Test
        @DisplayName("【正常】审核通过 - 应更新服务商信息并返回成功")
        void reviewDraft_Approve_ShouldUpdateProviderAndReturnOK() throws Exception {
            // Arrange: 准备待审核的草稿和服务商
            ServiceProvider provider = ServiceProvider.builder()
                    .name("旧名称")
                    .consignee("旧联系人")
                    .address("旧地址")
                    .mobile("13800000000")
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            providerRepository.create(provider);

            ServiceProviderDraft draft = ServiceProviderDraft.builder()
                    .serviceProviderId(provider.getId())
                    .providerName("张三维修服务")
                    .contactPerson("张三")
                    .contactPhone("13900139000")
                    .address("厦门市思明区")
                    .status(DraftStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            draftRepository.create(draft);

            String requestBody = """
                    {
                        "conclusion": 1,
                        "opinion": "资质审核通过，服务商信息已更新"
                    }
                    """;

            // Act & Assert
            mockMvc.perform(put("/draft/{draftid}/review", draft.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.errno").value(ReturnNo.OK.getErrNo()))
                    .andExpect(jsonPath("$.errmsg").value(ReturnNo.OK.getMessage()));

            // 验证草稿状态已更新
            ServiceProviderDraft updatedDraft = draftRepository.findById(draft.getId());
            assert updatedDraft.getStatus() == DraftStatus.APPROVED;
            assert "资质审核通过，服务商信息已更新".equals(updatedDraft.getOpinion());

            // 验证服务商信息已更新
            ServiceProvider updatedProvider = providerRepository.findById(provider.getId());
            assert "张三维修服务".equals(updatedProvider.getName());
            assert "张三".equals(updatedProvider.getConsignee());
            assert "厦门市思明区".equals(updatedProvider.getAddress());
            assert "13900139000".equals(updatedProvider.getMobile());
        }

        @Test
        @DisplayName("【正常】审核拒绝 - 草稿状态应更新为已拒绝")
        void reviewDraft_Reject_ShouldUpdateDraftStatusToRejected() throws Exception {
            ServiceProviderDraft draft = ServiceProviderDraft.builder()
                    .serviceProviderId(1L)
                    .providerName("李四售后服务")
                    .contactPerson("李四")
                    .contactPhone("13800138001")
                    .address("福州")
                    .status(DraftStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            draftRepository.create(draft);

            String requestBody = """
                    {
                        "conclusion": 0,
                        "opinion": "资质文件不完整，请补充后重新提交"
                    }
                    """;

            mockMvc.perform(put("/draft/{draftid}/review", draft.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errno").value(0));

            // 验证草稿状态已更新为已拒绝
            ServiceProviderDraft updatedDraft = draftRepository.findById(draft.getId());
            assert updatedDraft.getStatus() == DraftStatus.REJECTED;
            assert "资质文件不完整，请补充后重新提交".equals(updatedDraft.getOpinion());
        }

        @Test
        @DisplayName("【正常】审核通过但opinion为空 - 应使用默认意见")
        void reviewDraft_ApproveWithNullOpinion_ShouldUseDefaultOpinion() throws Exception {
            ServiceProvider provider = ServiceProvider.builder()
                    .name("测试服务商")
                    .consignee("测试")
                    .address("测试地址")
                    .mobile("13800000000")
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            providerRepository.create(provider);

            ServiceProviderDraft draft = ServiceProviderDraft.builder()
                    .serviceProviderId(provider.getId())
                    .providerName("王五技术服务")
                    .contactPerson("王五")
                    .contactPhone("13800138002")
                    .address("泉州")
                    .status(DraftStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            draftRepository.create(draft);

            String requestBody = """
                    {
                        "conclusion": 1
                    }
                    """;

            mockMvc.perform(put("/draft/{draftid}/review", draft.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk());

            // 验证使用了默认意见
            ServiceProviderDraft updatedDraft = draftRepository.findById(draft.getId());
            assert updatedDraft.getStatus() == DraftStatus.APPROVED;
            assert "审核通过".equals(updatedDraft.getOpinion());
        }

        @Test
        @DisplayName("【异常】草稿ID不存在 - 应返回404")
        void reviewDraft_DraftNotFound_ShouldReturn404() throws Exception {
            String requestBody = """
                    {
                        "conclusion": 1,
                        "opinion": "审核通过"
                    }
                    """;

            mockMvc.perform(put("/draft/99999/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errno").value(ReturnNo.SERVICE_DRAFT_NOT_FOUND.getErrNo()));
        }

        @Test
        @DisplayName("【异常】草稿已被审核 - 应返回状态不允许错误")
        void reviewDraft_AlreadyReviewed_ShouldReturnStateNotAllowError() throws Exception {
            ServiceProviderDraft draft = ServiceProviderDraft.builder()
                    .serviceProviderId(1L)
                    .providerName("已审核服务商")
                    .contactPerson("测试")
                    .contactPhone("13800000000")
                    .address("测试")
                    .status(DraftStatus.APPROVED)  // 已审核
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            draftRepository.create(draft);

            String requestBody = """
                    {
                        "conclusion": 1,
                        "opinion": "重复审核"
                    }
                    """;

            mockMvc.perform(put("/draft/{draftid}/review", draft.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errno").value(ReturnNo.SERVICE_DRAFT_STATE_INVALID.getErrNo()))
                    .andExpect(jsonPath("$.errmsg").value(containsString("待审核状态")));
        }

        @Test
        @DisplayName("【异常】参数校验失败 - conclusion为空")
        void reviewDraft_MissingConclusion_ShouldReturn400() throws Exception {
            String requestBody = """
                    {
                        "opinion": "测试"
                    }
                    """;

            mockMvc.perform(put("/draft/1/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errno").value(ReturnNo.FIELD_NOTVALID.getErrNo()));
        }

        @Test
        @DisplayName("【异常】conclusion值非法 - 应返回400")
        void reviewDraft_InvalidConclusion_ShouldReturn400() throws Exception {
            ServiceProviderDraft draft = ServiceProviderDraft.builder()
                    .serviceProviderId(1L)
                    .providerName("测试")
                    .contactPerson("测试")
                    .contactPhone("13800000000")
                    .address("测试")
                    .status(DraftStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            draftRepository.create(draft);

            String requestBody = """
                    {
                        "conclusion": 999,
                        "opinion": "无效的审核结果"
                    }
                    """;

            mockMvc.perform(put("/draft/{draftid}/review", draft.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errno").value(ReturnNo.BAD_REQUEST.getErrNo()));
        }

        @Test
        @DisplayName("【边界】opinion为超长字符串 - 应成功保存")
        void reviewDraft_LongOpinion_ShouldAccept() throws Exception {
            ServiceProvider provider = ServiceProvider.builder()
                    .name("测试")
                    .consignee("测试")
                    .address("测试")
                    .mobile("13800000000")
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            providerRepository.create(provider);

            ServiceProviderDraft draft = ServiceProviderDraft.builder()
                    .serviceProviderId(provider.getId())
                    .providerName("测试")
                    .contactPerson("测试")
                    .contactPhone("13800000000")
                    .address("测试")
                    .status(DraftStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            draftRepository.create(draft);

            String longOpinion = "审核意见".repeat(100);  // 400个字符
            String requestBody = String.format("""
                    {
                        "conclusion": 1,
                        "opinion": "%s"
                    }
                    """, longOpinion);

            mockMvc.perform(put("/draft/{draftid}/review", draft.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk());

            ServiceProviderDraft updatedDraft = draftRepository.findById(draft.getId());
            assert longOpinion.equals(updatedDraft.getOpinion());
        }

        @Test
        @DisplayName("【边界】审核拒绝但opinion为空 - 应使用默认拒绝理由")
        void reviewDraft_RejectWithNullOpinion_ShouldUseDefaultReason() throws Exception {
            ServiceProviderDraft draft = ServiceProviderDraft.builder()
                    .serviceProviderId(1L)
                    .providerName("测试")
                    .contactPerson("测试")
                    .contactPhone("13800000000")
                    .address("测试")
                    .status(DraftStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            draftRepository.create(draft);

            String requestBody = """
                    {
                        "conclusion": 0
                    }
                    """;

            mockMvc.perform(put("/draft/{draftid}/review", draft.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isOk());

            ServiceProviderDraft updatedDraft = draftRepository.findById(draft.getId());
            assert updatedDraft.getStatus() == DraftStatus.REJECTED;
            assert "审核拒绝".equals(updatedDraft.getOpinion());
        }
    }

    // ==================== HTTP协议测试 ====================

    @Nested
    @DisplayName("HTTP协议和格式测试")
    class HttpProtocolTests {

        @Test
        @DisplayName("【格式】Content-Type不是JSON - 应返回415")
        void request_WrongContentType_ShouldReturn415() throws Exception {
            mockMvc.perform(put("/draft/1/review")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("plain text"))
                    .andDo(print())
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("【格式】请求体为空 - 应返回400")
        void request_EmptyBody_ShouldReturn400() throws Exception {
            mockMvc.perform(put("/draft/1/review")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("【格式】JSON格式错误 - 应返回400")
        void request_InvalidJson_ShouldReturn400() throws Exception {
            mockMvc.perform(put("/draft/1/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("【格式】响应Content-Type应为JSON")
        void response_ContentTypeShouldBeJson() throws Exception {
            ServiceProvider provider = ServiceProvider.builder()
                    .name("测试")
                    .consignee("测试")
                    .address("测试")
                    .mobile("13800000000")
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            providerRepository.create(provider);

            ServiceProviderDraft draft = ServiceProviderDraft.builder()
                    .serviceProviderId(provider.getId())
                    .providerName("测试")
                    .contactPerson("测试")
                    .contactPhone("13800000000")
                    .address("测试")
                    .status(DraftStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            draftRepository.create(draft);

            String requestBody = """
                    {
                        "conclusion": 1,
                        "opinion": "通过"
                    }
                    """;

            mockMvc.perform(put("/draft/{draftid}/review", draft.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("【格式】响应体结构验证")
        void response_StructureShouldBeValid() throws Exception {
            ServiceProvider provider = ServiceProvider.builder()
                    .name("测试")
                    .consignee("测试")
                    .address("测试")
                    .mobile("13800000000")
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            providerRepository.create(provider);

            ServiceProviderDraft draft = ServiceProviderDraft.builder()
                    .serviceProviderId(provider.getId())
                    .providerName("测试")
                    .contactPerson("测试")
                    .contactPhone("13800000000")
                    .address("测试")
                    .status(DraftStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            draftRepository.create(draft);

            String requestBody = """
                    {
                        "conclusion": 1,
                        "opinion": "通过"
                    }
                    """;

            mockMvc.perform(put("/draft/{draftid}/review", draft.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(jsonPath("$.errno").exists())
                    .andExpect(jsonPath("$.errmsg").exists())
                    .andExpect(jsonPath("$.errno").value(0))
                    .andExpect(jsonPath("$.errmsg").value(ReturnNo.OK.getMessage()));
        }
    }


    // ==================== 查询草稿列表与历史 ====================

    @Nested
    @DisplayName("平台管理员查询服务商变更申请API - GET /drafts")
    class ListDraftsTests {

        @Test
        @DisplayName("【正常】按服务商名称过滤并分页")
        void listDrafts_ShouldReturnFilteredResult() throws Exception {
            ServiceProviderDraft draft1 = ServiceProviderDraft.builder()
                    .serviceProviderId(10L)
                    .providerName("张三维修服务")
                    .contactPerson("张三")
                    .contactPhone("13800138000")
                    .address("厦门市")
                    .status(DraftStatus.PENDING)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .build();
            draftRepository.create(draft1);

            ServiceProviderDraft draft2 = ServiceProviderDraft.builder()
                    .serviceProviderId(11L)
                    .providerName("李四售后服务")
                    .contactPerson("李四")
                    .contactPhone("13800138001")
                    .address("福州")
                    .status(DraftStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            draftRepository.create(draft2);

            mockMvc.perform(get("/drafts")
                            .param("serviceprovidername", "张三")
                            .param("page", "1")
                            .param("pagesize", "5"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errno").value(ReturnNo.OK.getErrNo()))
                    .andExpect(jsonPath("$.data.list").isArray())
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.pagesize").value(5));
        }
    }

    @Nested
    @DisplayName("平台管理员查看变更历史API - GET /drafts/{draftid}/history")
    class DraftHistoryTests {

        @Test
        @DisplayName("【正常】返回草稿历史记录")
        void getDraftHistory_ShouldReturnHistory() throws Exception {
            ServiceProviderDraft draft = ServiceProviderDraft.builder()
                    .serviceProviderId(20L)
                    .providerName("历史服务商")
                    .contactPerson("联系人A")
                    .contactPhone("13800001111")
                    .address("泉州")
                    .status(DraftStatus.APPROVED)
                    .opinion("审核通过")
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .updatedAt(LocalDateTime.now().minusDays(1))
                    .build();
            draftRepository.create(draft);

            mockMvc.perform(get("/drafts/{draftid}/history", draft.getId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errno").value(ReturnNo.OK.getErrNo()))
                    .andExpect(jsonPath("$.data.history").isArray())
                    .andExpect(jsonPath("$.data.history[0]", containsString("提交变更申请")));
        }
    }
}
