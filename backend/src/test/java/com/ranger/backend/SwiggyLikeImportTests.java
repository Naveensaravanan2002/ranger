package com.ranger.backend;

import com.ranger.backend.service.LedgerService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/** H2 integration with fictional input; not an actual Gmail/Neon test. */
@SpringBootTest
class SwiggyLikeImportTests {
    @Autowired LedgerService ledger;

    @Test void storesFictionalFieldsAndDoesNotRepeatTheSameMessage() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/merchant-samples/swiggy-like.html")) {
            assertNotNull(in); html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String uid = "synthetic-test-" + UUID.randomUUID(), key = "gmail:fake-google-sub:fake-message-1";
        try {
            Instant received = Instant.parse("2026-11-01T04:00:00Z");
            assertTrue(ledger.importHtml(uid, html, received, key, "GMAIL"));
            assertFalse(ledger.importHtml(uid, html, received, key, "GMAIL"));
            var snapshot = ledger.userLedger(uid, "Asia/Kolkata", "23:00", "04:00");
            assertEquals(1, snapshot.receipts().size());
            var r = snapshot.receipts().getFirst();
            assertEquals("Swiggy", r.merchant()); assertEquals("FOOD", r.category());
            assertEquals(new BigDecimal("285.00"), r.total()); assertEquals(new BigDecimal("25.00"), r.fees());
            assertEquals(2, r.items().size()); assertEquals(2, r.feeDetails().size());
            assertEquals("GMAIL", r.source()); assertTrue(r.reviewRequired());
            assertEquals(new BigDecimal("285.00"), snapshot.summary().lateNight());
            assertEquals(new BigDecimal("260.00"), snapshot.summary().junkFood());
            assertEquals(new BigDecimal("25.00"), snapshot.summary().extraFees());
        } finally { ledger.deleteAll(uid); }
    }
}
