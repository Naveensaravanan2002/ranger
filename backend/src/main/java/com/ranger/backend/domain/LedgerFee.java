package com.ranger.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="ranger_fees")
public class LedgerFee {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="receipt_id",nullable=false) private LedgerReceipt receipt;
    @Column(nullable=false,length=120) private String label;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
    protected LedgerFee() {}
    public LedgerFee(UUID id,String label,BigDecimal amount){this.id=id;this.label=label;this.amount=amount;}
    void setReceipt(LedgerReceipt receipt){this.receipt=receipt;}
    public String getLabel(){return label;} public BigDecimal getAmount(){return amount;}
}
