package com.ranger.backend;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.ranger.backend.parser.ReceiptParser;

class ReceiptParserTests {
    private final ReceiptParser parser=new ReceiptParser();
    @Test void parsesOnlyExplicitValuesAndMarksApproximateTime() {
        var parsed=parser.parse("""
            <div data-merchant="Test shop" data-category="FOOD"></div>
            <div data-total="125.00"></div><div data-item><b class="item-name">Burger</b></div>
            <div data-fee><span class="fee-name">Delivery</span><span class="fee-amount">₹ 25.00</span></div>
            <script>doNotExecute()</script>
            """,Instant.parse("2026-10-22T20:00:00Z"));
        assertEquals(new BigDecimal("125.00"),parsed.total());
        assertEquals(new BigDecimal("25.00"),parsed.fees());
        assertTrue(parsed.approximateTime());assertTrue(parsed.items().getFirst().junkFood());
        assertNull(parsed.items().getFirst().amount());assertTrue(parsed.reviewRequired());
    }
    @Test void rejectsMissingOrNegativeTotal(){
        assertThrows(IllegalArgumentException.class,()->parser.parse("<p>hello</p>",Instant.now()));
        assertThrows(IllegalArgumentException.class,()->parser.parse("<div data-total='-50'></div>",Instant.now()));
    }
    @Test void parsesTimezoneQualifiedOrderTime(){
        var r=parser.parse("<div data-total='10'></div><time datetime='2026-10-22T23:00:00+05:30'></time>",Instant.EPOCH);
        assertEquals(Instant.parse("2026-10-22T17:30:00Z"),r.occurredAt()); assertFalse(r.approximateTime());
    }
}
