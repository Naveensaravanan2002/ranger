package com.ranger.backend;

import com.ranger.backend.parser.ReceiptParser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Invented examples only: these do not certify any real merchant email format. */
class SwiggyLikeParserTests {
    private final ReceiptParser parser = new ReceiptParser();
    private final Instant received = Instant.parse("2026-11-01T04:00:00Z");
    private static String example(String rows) {
        return "<h1>Swiggy</h1><p>Restaurant: Sample Kitchen</p><p>Order placed: 31 October 2026, 11:25 PM IST</p><table>" + rows + "</table>";
    }
    private static String total(String amount) { return "<tr><td>Grand total</td><td>" + amount + "</td></tr>"; }

    @Test void parsesInventedReceiptAndNeverPretendsItIsVerified() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/merchant-samples/swiggy-like.html")) {
            assertNotNull(in); html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        var r = parser.parse(html, received);
        assertEquals("Swiggy", r.merchant()); assertEquals("FOOD", r.category());
        assertEquals(new BigDecimal("285.00"), r.total()); assertEquals(new BigDecimal("25.00"), r.fees());
        assertEquals(2, r.items().size()); assertTrue(r.items().stream().allMatch(ReceiptParser.ParsedItem::junkFood));
        assertEquals(new BigDecimal("180.00"), r.items().getFirst().amount());
        assertEquals(2, r.feeDetails().size());
        assertEquals(Instant.parse("2026-10-31T17:55:00Z"), r.occurredAt());
        assertFalse(r.approximateTime()); assertTrue(r.reviewRequired());
    }
    @Test void rejectsConflictingOrAmbiguousTotalsRatherThanPickingOne() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(example(total("₹285") + "<tr><td>Total paid</td><td>₹265</td></tr>"), received));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(example(total("₹300 ₹285")), received));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(example(total("₹-285")), received));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(example(total("285")), received));
    }
    @Test void acceptsRepeatedIdenticalExplicitTotalButDoesNotAddThem() {
        var r = parser.parse(example(total("₹285.00") + "<tr><td>Order total</td><td>INR 285.00</td></tr>"), received);
        assertEquals(new BigDecimal("285.00"), r.total());
    }
    @Test void rejectsPromotionsAndCancellationNoticesEvenIfTheyContainATotal() {
        String promo = "<h1>Swiggy</h1><p>Special savings on your next order!</p><table>" + total("₹100") + "</table>";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(promo, received));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(example(total("₹285")) + "<p>Your order was cancelled.</p>", received));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(example(total("₹285")) + "<p>Order #FICTIONAL-9 was cancelled.</p>", received));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(example(total("₹285")) + "<p>Your order has been refunded.</p>", received));
        String structuredPromo = "<h1>Swiggy</h1><p>Special savings on your next order!</p><div data-total='100'></div>";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(structuredPromo, received));
        String structuredCancel = "<h1>Swiggy</h1><p>Order placed</p><div data-total='100'></div><p>Order cancelled</p>";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(structuredCancel, received));
    }
    @Test void skipsUncertainFeeRatherThanPickingTheOriginalOrDiscountedPrice() {
        var r = parser.parse(example("<tr><td>Delivery fee</td><td>₹50 ₹35</td></tr>" + total("₹285")), received);
        assertTrue(r.feeDetails().isEmpty()); assertEquals(new BigDecimal("0.00"), r.fees()); assertTrue(r.reviewRequired());
    }
    @Test void repeatedDeliveryBreakdownsAreNotDoubleCounted() {
        var r = parser.parse(example("<tr><td>Delivery charges</td><td>₹20</td></tr><tr><td>Delivery fee</td><td>₹20</td></tr>" +
            "<tr><td>Platform fee</td><td>₹5</td></tr>" + total("₹285")), received);
        assertEquals(1, r.feeDetails().size()); assertEquals(new BigDecimal("5.00"), r.fees());
    }
    @Test void explicitFreeFeeIsDifferentFromMissingFee() {
        var r = parser.parse(example("<tr><td>Delivery charge</td><td>FREE</td></tr>" + total("₹285")), received);
        assertEquals(1, r.feeDetails().size()); assertEquals(new BigDecimal("0.00"), r.feeDetails().getFirst().amount());
    }
    @Test void doesNotInventMultiQuantityLinePriceFromAnAmbiguousPriceColumn() {
        String table = "<table><tr><th>Item</th><th>Qty</th><th>Price</th></tr><tr><td>Burger</td><td>2</td><td>₹100</td></tr></table>";
        var r = parser.parse(example(total("₹285")) + table, received);
        assertEquals(1, r.items().size()); assertTrue(r.items().getFirst().junkFood()); assertNull(r.items().getFirst().amount());
    }
    @Test void explicitLineTotalIsUsableForMultipleItems() {
        String table = "<table><tr><th>Item</th><th>Qty</th><th>Line total</th></tr><tr><td>Burger</td><td>2</td><td>₹200</td></tr></table>";
        var r = parser.parse(example(total("₹285")) + table, received);
        assertEquals(new BigDecimal("200.00"), r.items().getFirst().amount());
    }
    @Test void multipleItemTablesAreLeftForReviewInsteadOfDuplicatingItems() {
        String table = "<table><tr><th>Item</th><th>Qty</th><th>Price</th></tr><tr><td>Burger</td><td>1</td><td>₹100</td></tr></table>";
        var r = parser.parse(example(total("₹285")) + table + table, received);
        assertTrue(r.items().isEmpty()); assertTrue(r.reviewRequired());
    }
    @Test void unqualifiedOrderTimeFallsBackToTheEmailTimestamp() {
        var r = parser.parse("<h1>Swiggy</h1><p>Order placed: 31 October 2026, 11:25 PM</p><table>" + total("₹285") + "</table>", received);
        assertTrue(r.approximateTime()); assertEquals(received, r.occurredAt());
    }
    @Test void noRestaurantEvidenceDoesNotAutomaticallyBecomeFood() {
        var r = parser.parse("<h1>Swiggy</h1><p>Order summary</p><p>Instamart</p><table>" + total("₹285") + "</table>", received);
        assertEquals("OTHER", r.category());
    }
    @Test void ordinaryExistingReceiptStillWorks() {
        var r = parser.parse("<div data-merchant='Example Shop'></div><div data-total='125.00'></div>", received);
        assertEquals("Example Shop", r.merchant()); assertEquals(new BigDecimal("125.00"), r.total());
    }
    @Test void previousFictional150ReceiptStillParses() {
        String html = "<p><strong>FICTIONAL TEST RECEIPT — NOT A REAL PURCHASE</strong></p><p>Ranger Test Cafe</p>" +
                "<table><tr><td>Order total</td><td>₹150.00</td></tr></table>";
        var r = parser.parse(html, received);
        assertEquals(new BigDecimal("150.00"), r.total()); assertEquals("Unknown merchant", r.merchant());
        assertEquals("OTHER", r.category()); assertTrue(r.approximateTime()); assertTrue(r.reviewRequired());
    }
}
