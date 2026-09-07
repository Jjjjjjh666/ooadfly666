package cn.edu.xmu.service.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumsTest {

    @Test
    void draftStatusOfShouldReturnEnum() {
        assertEquals(DraftStatus.PENDING, DraftStatus.of("PENDING"));
        assertThrows(IllegalArgumentException.class, () -> DraftStatus.of("UNKNOWN"));
    }

    @Test
    void serviceOrderStatusOfShouldReturnEnum() {
        assertEquals(ServiceOrderStatus.PENDING, ServiceOrderStatus.of("PENDING"));
        assertThrows(IllegalArgumentException.class, () -> ServiceOrderStatus.of("invalid"));
    }

    @Test
    void serviceOrderStatusHelpersShouldReflectTransitions() {
        assertTrue(ServiceOrderStatus.CANCELED.isTerminal());
        assertTrue(ServiceOrderStatus.TO_BE_ASSIGNED.canCancel());
        assertFalse(ServiceOrderStatus.COMPLETED.canCancel());
    }

    @Test
    void serviceOrderTypeValueOfShouldReturnEnum() {
        assertEquals(ServiceOrderType.ONSITE_REPAIR, ServiceOrderType.valueOf(0));
        assertThrows(IllegalArgumentException.class, () -> ServiceOrderType.valueOf(99));
    }
}
