package com.ranger.backend.api;

import com.ranger.backend.gmail.GmailSyncService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/private/test-gmail")
public class GmailController {
    private final GmailSyncService gmail;
    public GmailController(GmailSyncService gmail){this.gmail=gmail;}
    @PostMapping("/sync") public ResponseEntity<Models.GmailResult> sync(@AuthenticationPrincipal Jwt user,@Valid @RequestBody Models.GmailRequest request){
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(gmail.sync(user,request.accessToken()));
    }
}
