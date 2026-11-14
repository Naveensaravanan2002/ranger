package com.ranger.backend.api;

import com.ranger.backend.service.LedgerService;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/public")
public class PublicController {
    private final LedgerService ledger;
    public PublicController(LedgerService ledger){this.ledger=ledger;}
    @GetMapping("/health") public ResponseEntity<Map<String,String>> health(){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("status","ready"));}
    @GetMapping("/demo") public ResponseEntity<Models.Snapshot> demo(@RequestParam(defaultValue="Asia/Kolkata") String timezone,
            @RequestParam(defaultValue="23:00") String start, @RequestParam(defaultValue="04:00") String end){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ledger.demo(timezone,start,end));
    }
}
