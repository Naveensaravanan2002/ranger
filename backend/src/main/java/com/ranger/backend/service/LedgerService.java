package com.ranger.backend.service;

import com.ranger.backend.api.Models.*;
import com.ranger.backend.domain.*;
import com.ranger.backend.parser.ReceiptParser;
import com.ranger.backend.parser.ReceiptParser.ParsedReceipt;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {
    private final ReceiptRepository repo; private final ReceiptParser parser; private final AnalyticsService analytics;
    public LedgerService(ReceiptRepository repo, ReceiptParser parser, AnalyticsService analytics){this.repo=repo;this.parser=parser;this.analytics=analytics;}
    private static final List<String> DEMO = List.of("01-burger.html","02-pizza.html","03-groceries.html","04-books.html","05-snacks.html","06-investment.html","07-coffee.html");

    public Snapshot demo(String zone,String start,String end) {
        List<Receipt> rows=DEMO.stream().map(name -> {
            try {
                String raw=new String(new ClassPathResource("demo/"+name).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                ParsedReceipt parsed=parser.parse(raw, Instant.parse("2026-10-25T12:00:00Z"));
                return toDto(entity(parsed,"synthetic-demo","demo:"+name,"SAMPLE"));
            } catch (java.io.IOException e) { throw new IllegalStateException("Demo fixture is unavailable."); }
        }).toList();
        return new Snapshot(true, analytics.summarize(rows,zone,start,end),rows);
    }
    @Transactional(readOnly=true)
    public Snapshot userLedger(String uid,String zone,String start,String end) {
        List<Receipt> rows=repo.findByOwnerUidOrderByOccurredAtDesc(uid).stream().map(this::toDto).toList();
        return new Snapshot(false,analytics.summarize(rows,zone,start,end),rows);
    }
    @Transactional
    public boolean importHtml(String uid,String html,Instant received,String sourceKey,String source) {
        if (repo.existsByOwnerUidAndSourceKey(uid,sourceKey)) return false;
        ParsedReceipt parsed=parser.parse(html,received);
        repo.saveAndFlush(entity(parsed,uid,sourceKey,source));
        return true;
    }
    public static String manualKey(String html) {
        try { return "manual:"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(html.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
    @Transactional
    public void updateItem(String uid,UUID receiptId,UUID itemId,boolean junk) {
        LedgerReceipt receipt=repo.findByIdAndOwnerUid(receiptId,uid).orElseThrow(() -> new IllegalArgumentException("Receipt not found."));
        LedgerItem item=receipt.getItems().stream().filter(it -> it.getId().equals(itemId)).findFirst().orElseThrow(() -> new IllegalArgumentException("Item not found."));
        item.setJunkFood(junk);
    }
    @Transactional
    public void deleteOne(String uid,UUID id) { repo.delete(repo.findByIdAndOwnerUid(id,uid).orElseThrow(() -> new IllegalArgumentException("Receipt not found."))); }
    @Transactional
    public void deleteAll(String uid) { repo.deleteByOwnerUid(uid); }
    public boolean exists(String uid,String key){return repo.existsByOwnerUidAndSourceKey(uid,key);}
    private LedgerReceipt entity(ParsedReceipt p,String uid,String key,String source) {
        UUID id=UUID.nameUUIDFromBytes((uid+":"+key).getBytes(StandardCharsets.UTF_8));
        LedgerReceipt r=new LedgerReceipt(id,uid,key,p.merchant(),p.category(),p.occurredAt(),p.approximateTime(),p.total(),p.fees(),source,p.reviewRequired() || "GMAIL".equals(source));
        for (int i=0;i<p.items().size();i++){var item=p.items().get(i); r.addItem(new LedgerItem(UUID.nameUUIDFromBytes((id+":item:"+i).getBytes(StandardCharsets.UTF_8)),item.name(),item.amount(),item.junkFood()));}
        for (int i=0;i<p.feeDetails().size();i++){var fee=p.feeDetails().get(i); r.addFee(new LedgerFee(UUID.nameUUIDFromBytes((id+":fee:"+i).getBytes(StandardCharsets.UTF_8)),fee.label(),fee.amount()));}
        return r;
    }
    private Receipt toDto(LedgerReceipt r) {
        return new Receipt(r.getId(),r.getMerchant(),r.getCategory(),r.getOccurredAt(),r.isApproximateTime(),r.getTotal(),r.getFees(),
                r.getItems().stream().map(i -> new Item(i.getId(),i.getName(),i.getAmount(),i.isJunkFood())).toList(),
                r.getFeeDetails().stream().map(f -> new Fee(f.getLabel(),f.getAmount())).toList(),r.getSource(),r.isReviewRequired());
    }
}
