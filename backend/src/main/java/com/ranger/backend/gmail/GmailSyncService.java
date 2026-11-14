package com.ranger.backend.gmail;

import com.ranger.backend.api.Models.GmailResult;
import com.ranger.backend.service.LedgerService;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

/** TEST-ONLY, user-initiated Gmail import. Short-lived token lives in memory only. */
@Service
public class GmailSyncService {
    private static final String GMAIL_SCOPE="https://www.googleapis.com/auth/gmail.readonly";
    private final boolean enabled; private final String clientId; private final Set<String> testers;
    private final LedgerService ledger; private final HttpClient client;
    private final JsonMapper mapper=JsonMapper.builder().build();
    private static final int MAX_GOOGLE_RESPONSE_BYTES=524_288;
    public GmailSyncService(@Value("${ranger.gmail.enabled}") boolean enabled,
                            @Value("${ranger.gmail.client-id}") String clientId,
                            @Value("${ranger.gmail.testers}") String testers, LedgerService ledger) {
        this.enabled=enabled; this.clientId=clientId.trim(); this.ledger=ledger;
        this.testers=new HashSet<>(Arrays.stream(testers.split(",")).map(String::trim).filter(s->!s.isBlank()).map(s->s.toLowerCase(Locale.ROOT)).toList());
        this.client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }
    public GmailResult sync(Jwt user, String accessToken) {
        if (!enabled || clientId.isBlank() || testers.isEmpty()) throw new SecurityException("Gmail testing is not enabled here.");
        String email=user.getClaimAsString("email");
        if (email==null || !Boolean.TRUE.equals(user.getClaim("email_verified")) || !testers.contains(email.toLowerCase(Locale.ROOT)))
            throw new SecurityException("This verified account is not on the Gmail tester list.");
        try {
            // Whole user-initiated sync has a 35-second budget; no token is placed in a URL or log.
            long deadline=System.nanoTime()+Duration.ofSeconds(35).toNanos();
            String form="access_token="+URLEncoder.encode(accessToken,StandardCharsets.UTF_8);
            HttpRequest.Builder tokenRequest=HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/tokeninfo"))
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form));
            // Google tokeninfo is suitable for this experiment, not for a production verification system.
            Map<?,?> token=readLimited(tokenRequest,deadline);
            if (token==null) throw new SecurityException("Google authorization could not be verified.");
            String party=text(token.get("azp")); if(party.isBlank()) party=text(token.get("aud"));
            String scopes=text(token.get("scope"));
            if (!clientId.equals(party) || Arrays.stream(scopes.split(" ")).noneMatch(GMAIL_SCOPE::equals))
                throw new SecurityException("The Gmail grant is not for this testing client or lacks permission.");
            Map<?,?> info=get("https://openidconnect.googleapis.com/v1/userinfo",accessToken,deadline);
            String googleSub=text(info.get("sub")), googleEmail=text(info.get("email"));
            if (googleSub.isBlank() || !Boolean.TRUE.equals(info.get("email_verified")) || !email.equalsIgnoreCase(googleEmail))
                throw new SecurityException("The authorized Google mailbox must match your verified Ranger account.");
            checkGoogleIdentity(user, googleSub);
            Map<?,?> profile=get("https://gmail.googleapis.com/gmail/v1/users/me/profile",accessToken,deadline);
            if (!googleEmail.equalsIgnoreCase(text(profile.get("emailAddress"))))
                throw new SecurityException("The Gmail profile did not match the authorized Google account.");
            String url=UriComponentsBuilder.fromUriString("https://gmail.googleapis.com/gmail/v1/users/me/messages")
                    .queryParam("maxResults",25).queryParam("q","newer_than:90d {subject:order subject:receipt subject:invoice}")
                    .build().encode().toUriString();
            Map<?,?> listing=get(url,accessToken,deadline);
            Object rawMessages=listing.get("messages"); List<?> messages=rawMessages instanceof List<?> list ? list : List.of();
            int scanned=0, imported=0, duplicate=0, skipped=0;
            for(Object entry:messages) {
                if(scanned>=25) break;
                if (!(entry instanceof Map<?,?> map)) continue;
                String id=text(map.get("id"));
                if (!id.matches("[A-Za-z0-9_-]{1,100}")) continue;
                scanned++;
                String key="gmail:"+googleSub+":"+id;
                if(ledger.exists(user.getSubject(),key)){duplicate++; continue;}
                Map<?,?> message=get("https://gmail.googleapis.com/gmail/v1/users/me/messages/"+id+"?format=full&fields=id,internalDate,payload",accessToken,deadline);
                String html=htmlPart(message.get("payload"),0);
                Instant received;
                try { received=Instant.ofEpochMilli(Long.parseLong(text(message.get("internalDate")))); }
                catch(RuntimeException ex) { skipped++; continue; }
                if(html==null || html.length()>100_000){skipped++;continue;}
                try { if (ledger.importHtml(user.getSubject(),html,received,key,"GMAIL")) imported++; else duplicate++; }
                catch (IllegalArgumentException ex) {skipped++;}
            }
            return new GmailResult(scanned,imported,duplicate,skipped,"Testing only. Review imported receipts; unsupported layouts are skipped.");
        } catch (SecurityException ex) { throw ex; }
        catch (Exception ex) { throw new IllegalArgumentException("Gmail authorization or sync failed. Retry with the same verified test account."); }
    }
    private Map<?,?> get(String uri,String token,long deadline) throws Exception {
        return readLimited(HttpRequest.newBuilder(URI.create(uri)).header("Authorization","Bearer "+token).GET(),deadline);
    }
    private Map<?,?> readLimited(HttpRequest.Builder request,long deadline) throws Exception {
        long remaining=deadline-System.nanoTime();
        if(remaining<=0) throw new IllegalArgumentException("Google sync timed out.");
        long timeout=Math.min(remaining,Duration.ofSeconds(10).toNanos());
        request.timeout(Duration.ofNanos(timeout));
        var future=client.sendAsync(request.build(), ignored -> new LimitedBodySubscriber(MAX_GOOGLE_RESPONSE_BYTES));
        HttpResponse<byte[]> response;
        try { response=future.get(timeout,TimeUnit.NANOSECONDS); }
        catch(Exception ex) { future.cancel(true); throw ex; }
        if(response.statusCode()<200 || response.statusCode()>=300) throw new IllegalArgumentException("Google API request failed.");
        Map<?,?> body=mapper.readValue(response.body(),Map.class);
        return body==null ? Map.of() : body;
    }
    private static String htmlPart(Object payload,int depth) {
        if(depth>5 || !(payload instanceof Map<?,?> part)) return null;
        String type=text(part.get("mimeType")), name=text(part.get("filename"));
        if("text/html".equalsIgnoreCase(type) && name.isBlank() && part.get("body") instanceof Map<?,?> body){
            String encoded=text(body.get("data"));
            if(encoded.length()>140_000) return null;
            try { return new String(Base64.getUrlDecoder().decode(encoded),StandardCharsets.UTF_8); }
            catch(IllegalArgumentException ignored){return null;}
        }
        if(part.get("parts") instanceof List<?> parts) for(Object child:parts){String html=htmlPart(child,depth+1); if(html!=null)return html;}
        return null;
    }
    static void checkGoogleIdentity(Jwt user,String googleSub) {
        Object firebase=user.getClaim("firebase");
        if(firebase instanceof Map<?,?> f && f.get("identities") instanceof Map<?,?> ids && ids.get("google.com") instanceof List<?> googleIds && !googleIds.isEmpty()) {
            if(googleIds.stream().noneMatch(googleSub::equals)) throw new SecurityException("Google account does not match the Ranger login.");
        }
    }
    private static String text(Object object){return object==null?"":String.valueOf(object);}
}
