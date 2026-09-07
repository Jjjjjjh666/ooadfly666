package cn.edu.xmu.aftersale.controller;

import cn.edu.xmu.aftersale.AftersaleApplication;
import cn.edu.xmu.aftersale.client.LogisticsClient;
import cn.edu.xmu.aftersale.client.ServiceClient;
import cn.edu.xmu.aftersale.client.dto.CreatePackageResponse;
import cn.edu.xmu.aftersale.dao.AftersaleOrderRepository;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.aftersale.model.AftersaleStatus;
import cn.edu.xmu.aftersale.model.AftersaleType;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 售后控制器Web集成测试
 */
@SpringBootTest(classes = AftersaleApplication.class)
@AutoConfigureMockMvc
@DisplayName("售后控制器Web集成测试")
public class AftersaleControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogisticsClient logisticsClient;

    @MockBean
    private ServiceClient serviceClient;

    @Autowired
    private AftersaleOrderRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        try { jdbcTemplate.execute("ALTER TABLE aftersales ADD COLUMN express_id BIGINT NULL"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("ALTER TABLE aftersales ADD COLUMN return_express_id BIGINT NULL"); } catch (Exception ignored) {}

        InternalReturnObject<CreatePackageResponse> createPackageReturn = new InternalReturnObject<>();
        createPackageReturn.setErrno(ReturnNo.OK.getErrNo());
        createPackageReturn.setErrmsg(ReturnNo.OK.getMessage());
        createPackageReturn.setData(new CreatePackageResponse(888L, "BILL-888", 2, 0));
        when(logisticsClient.createPackage(anyLong(), anyString(), any())).thenReturn(createPackageReturn);

        InternalReturnObject<Void> logisticsSuccess = new InternalReturnObject<>();
        logisticsSuccess.setErrno(ReturnNo.OK.getErrNo());
        logisticsSuccess.setErrmsg(ReturnNo.OK.getMessage());
        when(logisticsClient.cancelPackage(anyLong(), anyLong(), anyString())).thenReturn(logisticsSuccess);

        cn.edu.xmu.javaee.core.model.ReturnObject serviceSuccess = new cn.edu.xmu.javaee.core.model.ReturnObject(ReturnNo.OK);
        when(serviceClient.createServiceOrder(anyLong(), anyLong(), any())).thenReturn(serviceSuccess);
        when(serviceClient.cancelServiceOrder(anyLong(), anyLong(), anyString())).thenReturn(serviceSuccess);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM aftersales");
    }

    // 验收 API: /accept
    @Nested
    @DisplayName("商户验收售后API - PUT /shops/{shopid}/aftersaleorders/{id}/accept")
    class AcceptAftersaleTests {
        @Test
        @DisplayName("【正常】退货验收通过 -> 已验收")
        void acceptReturn_Pass_ShouldBeReceived() throws Exception {
            AftersaleOrder order = createOrder(1L, AftersaleType.RETURN, AftersaleStatus.TO_BE_RECEIVED);

            String body = """
                    {"accept": true, "conclusion": "验收通过"}
                    """;

            mockMvc.perform(put("/shops/1/aftersaleorders/{id}/accept", order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errno").value(ReturnNo.OK.getErrNo()))
                    .andExpect(jsonPath("$.data.status").value(AftersaleStatus.RECEIVED.getCode()));

            AftersaleOrder updated = repository.findById(1L, order.getId());
            assertThat(updated.getStatus()).isEqualTo(AftersaleStatus.RECEIVED);
        }

        @Test
        @DisplayName("【正常】换货验收不通过 -> 已拒绝并生成返件单")
        void acceptExchange_Reject_ShouldBeRejectedAndReturnExpress() throws Exception {
            AftersaleOrder order = createOrder(2L, AftersaleType.EXCHANGE, AftersaleStatus.TO_BE_RECEIVED);

            String body = """
                    {"accept": false, "conclusion": "破损"}
                    """;

            mockMvc.perform(put("/shops/2/aftersaleorders/{id}/accept", order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(AftersaleStatus.REJECTED.getCode()));

            AftersaleOrder updated = repository.findById(2L, order.getId());
            assertThat(updated.getStatus()).isEqualTo(AftersaleStatus.REJECTED);
            assertThat(updated.getReturnExpressId()).isNotNull();
            verify(logisticsClient, times(1)).createPackage(eq(2L), anyString(), any());
        }
    }

    // 已验收处理 API: /receive （已验收 -> 完成/发货）
    @Nested
    @DisplayName("商户处理已验收商品API - PUT /shops/{shopid}/aftersaleorders/{id}/receive")
    class ProcessReceivedTests {
        @Test
        @DisplayName("【正常】退货已验收后处理 -> 完成")
        void processReturn_ShouldComplete() throws Exception {
            AftersaleOrder order = createOrder(1L, AftersaleType.RETURN, AftersaleStatus.RECEIVED);

            String body = """
                    {"conclusion": "退款完成"}
                    """;

            mockMvc.perform(put("/shops/1/aftersaleorders/{id}/receive", order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(AftersaleStatus.COMPLETED.getCode()));
        }

        @Test
        @DisplayName("【正常】换货已验收后处理 -> 发货完成")
        void processExchange_ShouldCreateOutboundAndComplete() throws Exception {
            AftersaleOrder order = createOrder(2L, AftersaleType.EXCHANGE, AftersaleStatus.RECEIVED);

            String body = """
                    {"conclusion": "已补发"}
                    """;

            mockMvc.perform(put("/shops/2/aftersaleorders/{id}/receive", order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(AftersaleStatus.COMPLETED.getCode()));

            AftersaleOrder updated = repository.findById(2L, order.getId());
            assertThat(updated.getReturnExpressId()).isNotNull();
            verify(logisticsClient, times(1)).createPackage(eq(2L), anyString(), any());
        }
    }

    // 审核 API
    @Nested
    @DisplayName("商户审核售后API - PUT /shops/{shopid}/aftersaleorders/{id}/confirm")
    class ConfirmAftersaleTests {
        @Test
        @DisplayName("【正常】审核通过退货 -> 待验收并创建运单")
        void confirmReturn_Approve() throws Exception {
            AftersaleOrder order = createOrder(1L, AftersaleType.RETURN, AftersaleStatus.PENDING);
            String body = """
                    {"confirm": true, "conclusion": "同意退货"}
                    """;

            mockMvc.perform(put("/shops/1/aftersaleorders/{id}/confirm", order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(AftersaleStatus.TO_BE_RECEIVED.getCode()));

            verify(logisticsClient).createPackage(eq(1L), anyString(), any());
        }

        @Test
        @DisplayName("【正常】审核通过维修 -> 待完成并创建服务单")
        void confirmRepair_Approve() throws Exception {
            AftersaleOrder order = createOrder(1L, AftersaleType.REPAIR, AftersaleStatus.PENDING);
            String body = """
                    {"confirm": true, "conclusion": "同意维修"}
                    """;

            mockMvc.perform(put("/shops/1/aftersaleorders/{id}/confirm", order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(AftersaleStatus.TO_BE_COMPLETED.getCode()));

            verify(serviceClient).createServiceOrder(eq(1L), eq(order.getId()), any());
        }

        @Test
        @DisplayName("【正常】审核拒绝 -> 已拒绝")
        void confirm_Reject() throws Exception {
            AftersaleOrder order = createOrder(1L, AftersaleType.RETURN, AftersaleStatus.PENDING);
            String body = """
                    {"confirm": false, "conclusion": "不符合条件"}
                    """;

            mockMvc.perform(put("/shops/1/aftersaleorders/{id}/confirm", order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(AftersaleStatus.REJECTED.getCode()));
        }
    }

    // 取消 API
    @Nested
    @DisplayName("商户取消售后API - DELETE /shops/{shopid}/aftersaleorders/{id}/cancel")
    class CancelAftersaleTests {
        @Test
        @DisplayName("【正常】取消退货售后单")
        void cancelReturn() throws Exception {
            AftersaleOrder order = createOrder(1L, AftersaleType.RETURN, AftersaleStatus.TO_BE_RECEIVED);
            order.setExpressId(888L);
            repository.save(order);

            String body = """
                    {"confirm": true, "reason": "客户放弃"}
                    """;

            mockMvc.perform(delete("/shops/1/aftersaleorders/{id}/cancel", order.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value(AftersaleStatus.CANCELLED.getCode()));
        }

        @Test
        @DisplayName("【异常】confirm非true -> 400")
        void cancelConfirmNotTrue() throws Exception {
            String body = """
                    {"confirm": false, "reason": "test"}
                    """;

            mockMvc.perform(delete("/shops/1/aftersaleorders/1/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    // 协议格式
    @Nested
    @DisplayName("HTTP协议和格式测试")
    class HttpProtocolTests {
        @Test
        void wrongContentTypeShouldReturn415() throws Exception {
            mockMvc.perform(put("/shops/1/aftersaleorders/1/confirm")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("plain"))
                    .andDo(print())
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    private AftersaleOrder createOrder(Long shopId, AftersaleType type, AftersaleStatus status) {
        AftersaleOrder order = AftersaleOrder.builder()
                .shopId(shopId)
                .orderId(1000L + type.getCode())
                .customerId(1L)
                .productId(1L)
                .type(type.getCode())
                .status(status)
                .reason("测试")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return repository.create(order);
    }
}

