package com.ranger.backend.api;

import com.ranger.backend.service.LedgerService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/private")
public class LedgerController {
    private final LedgerService ledger;
    public LedgerController(LedgerService ledger){this.ledger=ledger;}
    @GetMapping("/ledger") public ResponseEntity<Models.Snapshot> ledger(@AuthenticationPrincipal Jwt user,
            @RequestParam(defaultValue="Asia/Kolkata") String timezone, @RequestParam(defaultValue="23:00") String start,
            @RequestParam(defaultValue="04:00") String end){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ledger.userLedger(user.getSubject(),timezone,start,end));
    }
    @PostMapping("/import") public Map<String,Object> importReceipt(@AuthenticationPrincipal Jwt user,@Valid @RequestBody Models.ImportRequest request){
        boolean added=ledger.importHtml(user.getSubject(),request.html(),request.receivedAt(),LedgerService.manualKey(request.html()),"MANUAL");
        return Map.of("imported",added,"message",added?"Receipt imported. Review its extracted details.":"This receipt is already in your ledger.");
    }
    @PatchMapping("/receipts/{receiptId}/items/{itemId}") public ResponseEntity<Void> item(@AuthenticationPrincipal Jwt user,
            @PathVariable UUID receiptId,@PathVariable UUID itemId,@RequestBody Models.ItemUpdate update){
        ledger.updateItem(user.getSubject(),receiptId,itemId,update.junkFood()); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/receipts/{receiptId}") public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt user,@PathVariable UUID receiptId){
        ledger.deleteOne(user.getSubject(),receiptId); return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/data") public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal Jwt user){
        ledger.deleteAll(user.getSubject()); return ResponseEntity.noContent().build();
    }
}
