package com.ranger.backend;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc
class LedgerApiTests {
    @Autowired MockMvc mvc;
    private static final String RECEIPT="""
       {"html":"<div data-merchant='Shop' data-category='FOOD'></div><div data-total='125.00'></div><div data-item><span class='item-name'>Burger</span><span class='item-amount'>100.00</span></div><div data-fee><span class='fee-name'>Delivery</span><span class='fee-amount'>25.00</span></div><time datetime='2026-10-22T23:00:00+05:30'></time>","receivedAt":"2026-10-22T18:00:00Z"}
       """;
    @Test void publicDemoUsesFixturesAndProtectedApiRejectsMissingTokens() throws Exception {
        mvc.perform(get("/api/public/demo")).andExpect(status().isOk()).andExpect(jsonPath("$.demo").value(true))
          .andExpect(jsonPath("$.summary.totalSpend").value(2036.0)).andExpect(jsonPath("$.summary.extraFees").value(159.0))
          .andExpect(jsonPath("$.summary.lateNight").value(1087.0)).andExpect(jsonPath("$.summary.junkFood").value(979.0));
        mvc.perform(get("/api/private/ledger")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/private/ledger").header("Authorization","Bearer invalid.token.value")).andExpect(status().isUnauthorized());
    }
    @Test void oversizedPrivateRequestIsRejectedBeforeParsing() throws Exception {
        String large="x".repeat(1_048_577);
        mvc.perform(post("/api/private/import").with(jwt().jwt(j->j.subject("bounded").claim("email_verified",true)))
            .contentType(MediaType.APPLICATION_JSON).content(large)).andExpect(status().isPayloadTooLarge());
    }
    @Test void verifiedIdentityAndOwnerIsolationAreRequiredAndImportsAreIdempotent() throws Exception {
        String ownerA="test-a-"+UUID.randomUUID(), ownerB="test-b-"+UUID.randomUUID();
        mvc.perform(get("/api/private/ledger").with(jwt().jwt(j->j.subject("unverified").claim("email_verified",false))))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/private/import").with(jwt().jwt(j->j.subject(ownerA).claim("email_verified",true)))
            .contentType(MediaType.APPLICATION_JSON).content(RECEIPT)).andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(true));
        mvc.perform(post("/api/private/import").with(jwt().jwt(j->j.subject(ownerA).claim("email_verified",true)))
            .contentType(MediaType.APPLICATION_JSON).content(RECEIPT)).andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(false));
        String ownerABody=mvc.perform(get("/api/private/ledger").with(jwt().jwt(j->j.subject(ownerA).claim("email_verified",true))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.receipts.length()").value(1)).andExpect(jsonPath("$.summary.totalSpend").value(125.0))
            .andExpect(jsonPath("$.summary.extraFees").value(25.0)).andExpect(jsonPath("$.summary.lateNight").value(125.0))
            .andReturn().getResponse().getContentAsString();
        String receiptId=JsonPath.read(ownerABody,"$.receipts[0].id"), itemId=JsonPath.read(ownerABody,"$.receipts[0].items[0].id");
        mvc.perform(get("/api/private/ledger").with(jwt().jwt(j->j.subject(ownerB).claim("email_verified",true))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.receipts.length()").value(0));
        // A different owner may import identical content, but cannot change the first owner's record.
        mvc.perform(post("/api/private/import").with(jwt().jwt(j->j.subject(ownerB).claim("email_verified",true)))
            .contentType(MediaType.APPLICATION_JSON).content(RECEIPT)).andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(true));
        mvc.perform(patch("/api/private/receipts/{receiptId}/items/{itemId}",receiptId,itemId)
            .with(jwt().jwt(j->j.subject(ownerB).claim("email_verified",true))).contentType(MediaType.APPLICATION_JSON)
            .content("{\"junkFood\":false}")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/private/receipts/{receiptId}",receiptId).with(jwt().jwt(j->j.subject(ownerB).claim("email_verified",true))))
            .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/private/data").with(jwt().jwt(j->j.subject(ownerB).claim("email_verified",true))))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/private/ledger").with(jwt().jwt(j->j.subject(ownerA).claim("email_verified",true))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.receipts.length()").value(1));
        mvc.perform(post("/api/private/test-gmail/sync").with(jwt().jwt(j->j.subject(ownerA).claim("email_verified",true).claim("email","owner@example.com")))
            .contentType(MediaType.APPLICATION_JSON).content("{\"accessToken\":\"not-a-real-token\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/private/data").with(jwt().jwt(j->j.subject(ownerA).claim("email_verified",true))))
            .andExpect(status().isNoContent());
    }
}
