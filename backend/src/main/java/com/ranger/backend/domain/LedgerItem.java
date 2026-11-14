package com.ranger.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="ranger_items")
public class LedgerItem {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="receipt_id",nullable=false) private LedgerReceipt receipt;
    @Column(nullable=false,length=160) private String name;
    @Column(precision=12,scale=2) private BigDecimal amount;
    @Column(name="junk_food",nullable=false) private boolean junkFood;
    protected LedgerItem() {}
    public LedgerItem(UUID id,String name,BigDecimal amount,boolean junkFood){this.id=id;this.name=name;this.amount=amount;this.junkFood=junkFood;}
    void setReceipt(LedgerReceipt receipt){this.receipt=receipt;}
    public UUID getId(){return id;} public String getName(){return name;} public BigDecimal getAmount(){return amount;}
    public boolean isJunkFood(){return junkFood;} public void setJunkFood(boolean value){junkFood=value;}
}
