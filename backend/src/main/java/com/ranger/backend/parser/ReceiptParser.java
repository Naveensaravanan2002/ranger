package com.ranger.backend.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** Conservative, deterministic parser for supported HTML receipt shapes. Never executes HTML. */
@Component
public class ReceiptParser {
    private static final Pattern MONEY = Pattern.compile("(?<![\\d.])-?\\d[\\d,]*(?:\\.\\d{1,2})?");
    private static final Pattern JUNK = Pattern.compile("(?i)\\b(burger|fries|pizza|chips|soda|ice[ -]?cream|cake|pastry|doughnut|donut)\\b");
    private static final Set<String> CATEGORIES = Set.of("FOOD", "GROCERIES", "SHOPPING", "INVESTMENT", "OTHER");
    public record ParsedItem(String name, BigDecimal amount, boolean junkFood) {}
    public record ParsedFee(String label, BigDecimal amount) {}
    public record ParsedReceipt(String merchant, String category, Instant occurredAt, boolean approximateTime,
                                BigDecimal total, BigDecimal fees, List<ParsedItem> items,
                                List<ParsedFee> feeDetails, boolean reviewRequired) {}

    public ParsedReceipt parse(String html, Instant receivedAt) {
        if (html == null || html.isBlank() || html.length() > 100_000 || receivedAt == null)
            throw new IllegalArgumentException("Receipt must contain at most 100,000 characters and a received time.");
        Document doc = Jsoup.parse(html);
        doc.select("script,style,iframe,object,embed").remove();
        ParsedReceipt swiggyLike = SwiggyLikeParser.tryParse(doc, receivedAt);
        if (swiggyLike != null) return swiggyLike;
        Element merchantEl = doc.selectFirst("[data-merchant], .merchant-name, meta[name=merchant]");
        String merchant = merchantEl == null ? "Unknown merchant" : clean(
                merchantEl.hasAttr("data-merchant") ? merchantEl.attr("data-merchant") :
                merchantEl.hasAttr("content") ? merchantEl.attr("content") : merchantEl.text(), 120);
        if (merchant.isBlank()) merchant = "Unknown merchant";
        Element categoryEl = doc.selectFirst("[data-category]");
        String category = categoryEl == null ? "OTHER" : categoryEl.attr("data-category").toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(category)) category = "OTHER";
        Element totalEl = doc.selectFirst("[data-total], .grand-total, .order-total, .total-paid");
        if (totalEl == null) {
            for (Element row : doc.select("tr")) {
                String rowText = row.text().toLowerCase(Locale.ROOT);
                if (rowText.startsWith("grand total") || rowText.startsWith("total paid") || rowText.startsWith("order total")) {
                    totalEl = row.select("td").last(); break;
                }
            }
        }
        BigDecimal total = totalEl == null ? null : money(totalEl.hasAttr("data-total") ? totalEl.attr("data-total") : totalEl.text());
        if (total == null) throw new IllegalArgumentException("Could not identify an explicit receipt total. Nothing was imported.");

        Instant happened = receivedAt; boolean approximate = true;
        Element time = doc.selectFirst("time[datetime], [data-order-time]");
        if (time != null) {
            String value = time.hasAttr("datetime") ? time.attr("datetime") : time.attr("data-order-time");
            try { happened = OffsetDateTime.parse(value).toInstant(); approximate = false; }
            catch (DateTimeParseException ignored) { try { happened = Instant.parse(value); approximate = false; } catch (DateTimeParseException ignoredAgain) {} }
        }
        List<ParsedItem> items = new ArrayList<>();
        for (Element row : doc.select("[data-item], .item-line")) {
            if (items.size() >= 100) break;
            Element nameEl = row.selectFirst(".item-name, [data-item-name]");
            String name = clean(nameEl == null ? row.attr("data-item") :
                    nameEl.hasAttr("data-item-name") ? nameEl.attr("data-item-name") : nameEl.text(), 160);
            if (name.isBlank()) continue;
            Element amountEl = row.selectFirst(".item-amount, [data-item-amount]");
            BigDecimal amount = amountEl == null ? null : money(amountEl.hasAttr("data-item-amount") ? amountEl.attr("data-item-amount") : amountEl.text());
            boolean junk = row.hasAttr("data-junk") ? Boolean.parseBoolean(row.attr("data-junk")) : JUNK.matcher(name).find();
            items.add(new ParsedItem(name, amount, junk));
        }
        List<ParsedFee> fees = new ArrayList<>();
        BigDecimal feeTotal = BigDecimal.ZERO.setScale(2);
        for (Element row : doc.select("[data-fee], .fee-line")) {
            if (fees.size() >= 40) break;
            Element labelEl = row.selectFirst(".fee-name, [data-fee-name]");
            String label = clean(labelEl == null ? row.attr("data-fee") :
                    labelEl.hasAttr("data-fee-name") ? labelEl.attr("data-fee-name") : labelEl.text(), 120);
            Element amountEl = row.selectFirst(".fee-amount, [data-fee-amount]");
            BigDecimal amount = amountEl == null ? null : money(amountEl.hasAttr("data-fee-amount") ? amountEl.attr("data-fee-amount") : amountEl.text());
            if (!label.isBlank() && amount != null) { fees.add(new ParsedFee(label, amount)); feeTotal = feeTotal.add(amount); }
        }
        if (feeTotal.compareTo(new BigDecimal("9999999999.99")) > 0) throw new IllegalArgumentException("Fee total is too large.");
        boolean review = approximate || merchant.equals("Unknown merchant") || items.isEmpty() || feeTotal.compareTo(total) > 0;
        return new ParsedReceipt(merchant, category, happened, approximate, total, feeTotal, List.copyOf(items), List.copyOf(fees), review);
    }

    private static String clean(String value, int max) {
        String result = value == null ? "" : value.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        return result.length() <= max ? result : result.substring(0, max);
    }
    private static BigDecimal money(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher match = MONEY.matcher(text.replace("\u00a0", " "));
        if (!match.find()) return null;
        try {
            BigDecimal value = new BigDecimal(match.group().replace(",", "")).setScale(2, RoundingMode.UNNECESSARY);
            return value.signum() < 0 || value.compareTo(new BigDecimal("9999999999.99")) > 0 ? null : value;
        } catch (ArithmeticException | NumberFormatException e) { return null; }
    }
}
