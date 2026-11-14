package com.ranger.backend.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Experimental Swiggy-shaped HTML parser, tested on invented examples only.
 * It does not authenticate the sender or establish compatibility with actual email layouts.
 * Ambiguous totals are rejected and ambiguous details are left out for review.
 */
final class SwiggyLikeParser {
    private static final BigDecimal MAX = new BigDecimal("9999999999.99");
    private static final Pattern BRAND = Pattern.compile("(?i)^swiggy\\b");
    private static final Pattern ORDER = Pattern.compile("(?i)\\border\\s+(?:placed|confirmed|summary|details)\\b");
    private static final Pattern NEGATIVE_STATUS = Pattern.compile(
            "(?i)\\b(?:your\\s+)?order\\b.{0,80}\\b(?:cancelled|canceled|refunded|reversed)\\b" +
            "|\\b(?:cancelled|canceled|refunded)\\s+order\\b" +
            "|\\b(?:order\\s+cancellation|cancellation\\s+(?:confirmed|processed))\\b" +
            "|\\brefund\\s+(?:(?:was|is)\\s+|has\\s+been\\s+)?(?:issued|processed|credited|successful|completed)\\b");
    private static final Pattern RESTAURANT = Pattern.compile("(?i)\\brestaurant\\s*:\\s*\\S+");
    private static final Pattern HIDDEN = Pattern.compile("(?i)(?:^|;)\\s*(?:display\\s*:\\s*none|visibility\\s*:\\s*hidden)(?:\\s*!important)?\\s*(?:;|$)");
    private static final Pattern INR = Pattern.compile("(?i)^(?:₹|rs\\.?|inr)\\s*((?:\\d{1,3}(?:,\\d{3})+|\\d{1,2}(?:,\\d{2})+,\\d{3}|\\d{1,10})(?:\\.\\d{1,2})?)$");
    private static final Pattern TEXT_TIME = Pattern.compile("(?i)^order\\s+(?:placed(?:\\s+at)?|time)\\s*:?\\s*(\\d{1,2}\\s+[a-z]+\\s+\\d{4},\\s+\\d{1,2}:\\d{2}\\s+[ap]m)\\s+(?:IST|\\+05:30)$");
    private static final DateTimeFormatter TEXT_DATE = new DateTimeFormatterBuilder().parseCaseInsensitive()
            .appendPattern("d MMMM uuuu, h:mm a").toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern JUNK = Pattern.compile("(?i)\\b(burger|fries|pizza|chips|soda|ice[ -]?cream|cake|pastry|doughnut|donut)\\b");
    private static final Set<String> TOTAL_LABELS = Set.of("grand total", "order total", "total paid", "amount paid");
    private static final Set<String> NAME_HEADERS = Set.of("item", "item name", "product", "product name");
    private static final Set<String> QUANTITY_HEADERS = Set.of("qty", "quantity");
    private static final Set<String> AMOUNT_HEADERS = Set.of("price", "amount", "line total", "item total");
    private static final Set<String> LINE_TOTAL_HEADERS = Set.of("line total", "item total");
    private static final Set<String> SUMMARY_LABELS = Set.of("item total", "subtotal", "grand total", "order total", "total paid", "amount paid", "discount", "gst", "tax", "taxes");

    private SwiggyLikeParser() {}

    /** Returns null when not a candidate; throws on an unsafe/unsupported Swiggy-shaped candidate. */
    static ReceiptParser.ParsedReceipt tryParse(Document original, Instant receivedAt) {
        // Work on a copy; do not alter the behavior of the established generic parser.
        Document doc = original.clone();
        for (Element e : doc.getAllElements()) {
            if (e.hasAttr("hidden") || "true".equalsIgnoreCase(e.attr("aria-hidden")) || HIDDEN.matcher(e.attr("style")).find()) e.remove();
        }
        String text = normalize(doc.body().text());
        if (!BRAND.matcher(text).find()) return null;
        if (!ORDER.matcher(text).find() || NEGATIVE_STATUS.matcher(text).find())
            throw unsupported();
        // Existing explicitly marked fixtures continue through the original parser after the status check.
        if (doc.selectFirst("[data-total], .grand-total, .order-total, .total-paid") != null) return null;

        BigDecimal total = readTotal(doc);
        List<ReceiptParser.ParsedItem> items = readItems(doc);
        List<ReceiptParser.ParsedFee> fees = readFees(doc);
        BigDecimal sum = new BigDecimal("0.00");
        for (var fee : fees) sum = sum.add(fee.amount());
        if (sum.compareTo(MAX) > 0) throw unsupported();
        TimeValue time = readTime(doc, receivedAt);
        String category = RESTAURANT.matcher(text).find() ? "FOOD" : "OTHER";
        // The name is inferred from content, not authenticated. Every such result needs review.
        return new ReceiptParser.ParsedReceipt("Swiggy", category, time.instant(), time.approximate(),
                total, sum, List.copyOf(items), List.copyOf(fees), true);
    }

    private static BigDecimal readTotal(Document doc) {
        BigDecimal result = null;
        for (Element row : doc.select("tr")) {
            List<Element> cells = directCells(row);
            if (cells.isEmpty() || !TOTAL_LABELS.contains(label(cells.getFirst().text()))) continue;
            if (cells.size() != 2 || !cells.get(1).select("tr").isEmpty()) throw unsupported();
            BigDecimal value = amount(cells.get(1).text(), false);
            if (value == null || (result != null && result.compareTo(value) != 0)) throw unsupported();
            result = value;
        }
        if (result == null) throw unsupported();
        return result;
    }

    private record FeeCandidate(String originalLabel, BigDecimal amount) {}
    private static List<ReceiptParser.ParsedFee> readFees(Document doc) {
        Map<String,List<FeeCandidate>> groups = new LinkedHashMap<>();
        for (Element row : doc.select("tr")) {
            List<Element> cells = directCells(row);
            if (cells.isEmpty()) continue;
            String canonical = feeGroup(label(cells.getFirst().text()));
            if (canonical == null) continue;
            BigDecimal value = cells.size() == 2 && cells.get(1).select("tr").isEmpty() ? amount(cells.get(1).text(), true) : null;
            groups.computeIfAbsent(canonical, ignored -> new ArrayList<>())
                    .add(new FeeCandidate(clean(cells.getFirst().text(), 120), value));
        }
        List<ReceiptParser.ParsedFee> result = new ArrayList<>();
        for (var group : groups.values()) {
            // Repeated or ambiguous breakdowns may be alternative views of the same charge.
            if (group.size() == 1 && group.getFirst().amount() != null && result.size() < 40) {
                var fee = group.getFirst(); result.add(new ReceiptParser.ParsedFee(fee.originalLabel(), fee.amount()));
            }
        }
        return result;
    }
    private static String feeGroup(String name) {
        return switch (name) {
            case "delivery fee", "delivery charge", "delivery charges" -> "delivery";
            case "platform fee", "platform fees" -> "platform";
            case "packaging fee", "packaging charge", "packaging charges", "packing fee", "packing charge", "packing charges",
                 "order packing charges", "restaurant packaging", "restaurant packaging charge", "restaurant packaging charges" -> "packaging";
            case "handling fee", "handling charge", "handling charges" -> "handling";
            case "service fee", "service charge", "service charges" -> "service";
            case "small order fee", "small order charge" -> "small order";
            case "surge fee", "surge charge" -> "surge";
            case "rain fee", "rain charge" -> "rain";
            default -> null;
        };
    }

    private record Header(int name, int quantity, int amount, boolean isLineTotal) {}
    private static List<ReceiptParser.ParsedItem> readItems(Document doc) {
        List<List<ReceiptParser.ParsedItem>> possible = new ArrayList<>();
        for (Element table : doc.select("table")) {
            List<Element> rows = table.select("tr").stream().filter(row -> row.closest("table") == table).toList();
            Header header = null; int start = -1;
            for (int i = 0; i < rows.size(); i++) {
                header = header(directCells(rows.get(i)));
                if (header != null) { start = i + 1; break; }
            }
            if (header == null) continue;
            List<ReceiptParser.ParsedItem> found = new ArrayList<>();
            for (int i = start; i < rows.size() && found.size() < 100; i++) {
                List<Element> cells = directCells(rows.get(i));
                if (cells.size() <= Math.max(header.name(), header.amount())) continue;
                String name = clean(cells.get(header.name()).text(), 160);
                String normalizedName = label(name);
                if (name.isBlank() || SUMMARY_LABELS.contains(normalizedName) || feeGroup(normalizedName) != null) continue;
                if (!cells.get(header.name()).select("tr").isEmpty() || !cells.get(header.amount()).select("tr").isEmpty()) continue;
                boolean single = false;
                if (header.quantity() >= 0 && cells.size() > header.quantity()) {
                    String quantity = normalize(cells.get(header.quantity()).text());
                    if (!quantity.matches("[1-9][0-9]{0,2}")) continue;
                    single = "1".equals(quantity);
                }
                // 'Price' may be a unit or line price. Don't guess when quantity exceeds one.
                BigDecimal value = single || header.isLineTotal() ? amount(cells.get(header.amount()).text(), true) : null;
                found.add(new ReceiptParser.ParsedItem(name, value, JUNK.matcher(name).find()));
            }
            possible.add(found);
        }
        // Multiple item tables can be duplicate mobile/desktop renderings; don't double count them.
        return possible.size() == 1 ? possible.getFirst() : List.of();
    }
    private static Header header(List<Element> cells) {
        int name=-1, quantity=-1, amount=-1; boolean lineTotal=false;
        for (int i = 0; i < cells.size(); i++) {
            String field = label(cells.get(i).text());
            if (NAME_HEADERS.contains(field)) { if (name >= 0) return null; name = i; }
            if (QUANTITY_HEADERS.contains(field)) { if (quantity >= 0) return null; quantity = i; }
            if (AMOUNT_HEADERS.contains(field)) { if (amount >= 0) return null; amount = i; lineTotal = LINE_TOTAL_HEADERS.contains(field); }
        }
        return name >= 0 && amount >= 0 && name != amount ? new Header(name, quantity, amount, lineTotal) : null;
    }

    private record TimeValue(Instant instant, boolean approximate) {}
    private static TimeValue readTime(Document doc, Instant receivedAt) {
        List<Instant> values = new ArrayList<>();
        for (Element e : doc.getAllElements()) {
            Matcher m = TEXT_TIME.matcher(normalize(e.ownText()));
            if (m.matches()) try {
                values.add(LocalDateTime.parse(m.group(1), TEXT_DATE).atZone(ZoneId.of("Asia/Kolkata")).toInstant());
            } catch (DateTimeParseException ignored) { /* fallback */ }
        }
        for (Element time : doc.select("time[datetime]")) {
            Element parent = time.parent();
            String prefix = parent == null ? "" : label(parent.ownText());
            if (!prefix.equals("order placed") && !prefix.equals("order time")) continue;
            try { values.add(OffsetDateTime.parse(time.attr("datetime")).toInstant()); }
            catch (DateTimeParseException ignored) { /* unqualified times are not guessed */ }
        }
        if (values.isEmpty() || values.stream().anyMatch(value -> !value.equals(values.getFirst()))) return new TimeValue(receivedAt, true);
        return new TimeValue(values.getFirst(), false);
    }

    private static List<Element> directCells(Element row) {
        return row.children().stream().filter(e -> e.normalName().equals("td") || e.normalName().equals("th")).toList();
    }
    private static String normalize(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ').replace('\u202f', ' ').replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
    }
    private static String label(String value) { return normalize(value).replaceFirst(":\\s*$", "").toLowerCase(Locale.ROOT); }
    private static String clean(String value, int max) { String s = normalize(value); return s.length() <= max ? s : s.substring(0, max); }
    private static BigDecimal amount(String value, boolean allowFree) {
        String text = normalize(value);
        if (allowFree && "FREE".equalsIgnoreCase(text)) return new BigDecimal("0.00");
        Matcher match = INR.matcher(text);
        if (!match.matches()) return null;
        try {
            BigDecimal result = new BigDecimal(match.group(1).replace(",", "")).setScale(2, RoundingMode.UNNECESSARY);
            return result.signum() < 0 || result.compareTo(MAX) > 0 ? null : result;
        } catch (ArithmeticException | NumberFormatException ignored) { return null; }
    }
    private static IllegalArgumentException unsupported() { return new IllegalArgumentException("Unsupported or ambiguous receipt. Nothing was imported."); }
}
