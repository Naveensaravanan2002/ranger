package com.ranger.backend.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** API contracts deliberately contain no JPA entities or raw email bodies. */
public final class Models {
    private Models() {}
    public record Item(UUID id, String name, BigDecimal amount, boolean junkFood) {}
    public record Fee(String label, BigDecimal amount) {}
    public record Receipt(UUID id, String merchant, String category, Instant occurredAt,
                          boolean approximateTime, BigDecimal total, BigDecimal fees,
                          List<Item> items, List<Fee> feeDetails, String source, boolean reviewRequired) {}
    public record Summary(BigDecimal totalSpend, BigDecimal extraFees, BigDecimal lateNight,
                          BigDecimal junkFood, int junkItemsWithoutPrice, BigDecimal investments,
                          int receiptCount, String timezone, String lateStart, String lateEnd) {}
    public record Snapshot(boolean demo, Summary summary, List<Receipt> receipts) {}
    public record ImportRequest(@NotBlank @Size(max=100000) String html, @NotNull Instant receivedAt) {}
    public record ItemUpdate(boolean junkFood) {}
    public record GmailRequest(@NotBlank @Size(max=4096) String accessToken) {}
    public record GmailResult(int scanned, int imported, int duplicates, int skipped, String message) {}
}
