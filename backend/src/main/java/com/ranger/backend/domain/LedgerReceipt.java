package com.ranger.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name="ranger_receipts", uniqueConstraints=@UniqueConstraint(name="uk_receipt_owner_source", columnNames={"owner_uid", "source_key"}), indexes=@Index(name="idx_receipt_owner_time", columnList="owner_uid,occurred_at"))
public class LedgerReceipt {
    @Id private UUID id;
    @Column(name="owner_uid", nullable=false, length=128) private String ownerUid;
    @Column(name="source_key", nullable=false, length=180) private String sourceKey;
    @Column(nullable=false, length=120) private String merchant;
    @Column(nullable=false, length=32) private String category;
    @Column(name="occurred_at", nullable=false) private Instant occurredAt;
    @Column(name="approximate_time", nullable=false) private boolean approximateTime;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal total;
    @Column(name="fee_total", nullable=false, precision=12, scale=2) private BigDecimal fees;
    @Column(nullable=false, length=32) private String source;
    @Column(name="review_required", nullable=false) private boolean reviewRequired;
    @OneToMany(mappedBy="receipt", cascade=CascadeType.ALL, orphanRemoval=true)
    @BatchSize(size=50) private Set<LedgerItem> items = new LinkedHashSet<>();
    @OneToMany(mappedBy="receipt", cascade=CascadeType.ALL, orphanRemoval=true)
    @BatchSize(size=50) private Set<LedgerFee> feeDetails = new LinkedHashSet<>();
    protected LedgerReceipt() {}
    public LedgerReceipt(UUID id, String ownerUid, String sourceKey, String merchant, String category,
                         Instant occurredAt, boolean approximateTime, BigDecimal total, BigDecimal fees,
                         String source, boolean reviewRequired) {
        this.id=id; this.ownerUid=ownerUid; this.sourceKey=sourceKey; this.merchant=merchant;
        this.category=category; this.occurredAt=occurredAt; this.approximateTime=approximateTime;
        this.total=total; this.fees=fees; this.source=source; this.reviewRequired=reviewRequired;
    }
    public void addItem(LedgerItem item) { items.add(item); item.setReceipt(this); }
    public void addFee(LedgerFee fee) { feeDetails.add(fee); fee.setReceipt(this); }
    public UUID getId(){return id;} public String getOwnerUid(){return ownerUid;} public String getSourceKey(){return sourceKey;}
    public String getMerchant(){return merchant;} public String getCategory(){return category;} public Instant getOccurredAt(){return occurredAt;}
    public boolean isApproximateTime(){return approximateTime;} public BigDecimal getTotal(){return total;} public BigDecimal getFees(){return fees;}
    public String getSource(){return source;} public boolean isReviewRequired(){return reviewRequired;}
    public Set<LedgerItem> getItems(){return items;} public Set<LedgerFee> getFeeDetails(){return feeDetails;}
}
