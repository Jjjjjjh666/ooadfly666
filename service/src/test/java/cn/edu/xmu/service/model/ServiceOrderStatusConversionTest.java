package cn.edu.xmu.service.model;

import cn.edu.xmu.service.dao.po.ServiceOrderPo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 覆盖状态码 <-> 枚举 的转换分支，避免Jacoco遗漏
 */
class ServiceOrderStatusConversionTest {

    @Test
    void shouldConvertIntToEnumForAllStatuses() {
        Object[][] cases = new Object[][]{
                {1, ServiceOrderStatus.TO_BE_ASSIGNED},
                {2, ServiceOrderStatus.ASSIGNED},
                {4, ServiceOrderStatus.REJECTED},
                {5, ServiceOrderStatus.COMPLETED},
                {6, ServiceOrderStatus.CANCELED},
                {7, ServiceOrderStatus.RETURNED},
        };

        for (Object[] c : cases) {
            Integer code = (Integer) c[0];
            ServiceOrderStatus expected = (ServiceOrderStatus) c[1];

            ServiceOrderPo po = new ServiceOrderPo();
            po.setStatus(code);

            ServiceOrder order = ServiceOrder.fromPo(po);
            assertEquals(expected, order.getStatus(), "int->enum code=" + code);
        }
    }

    @Test
    void shouldConvertEnumToIntForAllStatuses() {
        Object[][] cases = new Object[][]{
                {ServiceOrderStatus.TO_BE_ASSIGNED, 1},
                {ServiceOrderStatus.ASSIGNED, 2},
                {ServiceOrderStatus.REJECTED, 4},
                {ServiceOrderStatus.COMPLETED, 5},
                {ServiceOrderStatus.CANCELED, 6},
                {ServiceOrderStatus.RETURNED, 7},
        };

        for (Object[] c : cases) {
            ServiceOrderStatus status = (ServiceOrderStatus) c[0];
            Integer expected = (Integer) c[1];

            ServiceOrder order = ServiceOrder.builder()
                    .status(status)
                    .build();

            ServiceOrderPo po = order.toPo();
            assertEquals(expected, po.getStatus(), "enum->int status=" + status);
        }
    }
}

