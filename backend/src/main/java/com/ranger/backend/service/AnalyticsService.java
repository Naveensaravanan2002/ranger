package com.ranger.backend.service;

import com.ranger.backend.api.Models.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    public Summary summarize(List<Receipt> receipts, String timezone, String startValue, String endValue) {
        ZoneId zone; LocalTime start, end;
        try { zone=ZoneId.of(timezone); start=LocalTime.parse(startValue); end=LocalTime.parse(endValue); }
        catch (DateTimeException ex) { throw new IllegalArgumentException("Invalid timezone or late-night interval."); }
        if (start.equals(end)) throw new IllegalArgumentException("Late-night start and end must be different.");
        BigDecimal total=zero(), fees=zero(), late=zero(), junk=zero(), investments=zero(); int unknown=0;
        for (Receipt receipt : receipts) {
            if ("INVESTMENT".equals(receipt.category())) { investments=investments.add(receipt.total()); continue; }
            total=total.add(receipt.total()); fees=fees.add(receipt.fees());
            LocalTime time=receipt.occurredAt().atZone(zone).toLocalTime();
            boolean isLate = start.isBefore(end) ? !time.isBefore(start) && time.isBefore(end)
                    : !time.isBefore(start) || time.isBefore(end);
            if (isLate) late=late.add(receipt.total());
            for (Item item : receipt.items()) if (item.junkFood()) {
                if (item.amount() == null) unknown++; else junk=junk.add(item.amount());
            }
        }
        return new Summary(total,fees,late,junk,unknown,investments,receipts.size(),timezone,startValue,endValue);
    }
    private static BigDecimal zero(){return new BigDecimal("0.00");}
}
