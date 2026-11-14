package com.ranger.backend;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;import java.time.Instant;import java.util.List;import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.ranger.backend.api.Models.*;
import com.ranger.backend.service.AnalyticsService;
class AnalyticsTests {
    @Test void midnightBoundaryAndUnknownItemPricesAreHonest(){
        Receipt start=new Receipt(UUID.randomUUID(),"A","FOOD",Instant.parse("2026-10-01T17:30:00Z"),false,new BigDecimal("100.00"),new BigDecimal("5.00"),List.of(new Item(UUID.randomUUID(),"Burger",null,true)),List.of(),"SAMPLE",false);
        Receipt end=new Receipt(UUID.randomUUID(),"B","FOOD",Instant.parse("2026-10-01T22:30:00Z"),false,new BigDecimal("200.00"),BigDecimal.ZERO,List.of(),List.of(),"SAMPLE",false);
        Summary s=new AnalyticsService().summarize(List.of(start,end),"Asia/Kolkata","23:00","04:00");
        assertEquals(new BigDecimal("100.00"),s.lateNight());assertEquals(new BigDecimal("300.00"),s.totalSpend());assertEquals(1,s.junkItemsWithoutPrice());assertEquals(new BigDecimal("0.00"),s.junkFood());
    }
}
