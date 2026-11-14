# Experimental Swiggy-shaped receipt parser

## Status

This is an **experimental, test-first implementation**, not verified support for actual Swiggy emails. The bundled sample is entirely invented and has no connection to a real order, merchant email or purchase. It exercises a narrow table-based HTML shape; real formats vary and must be validated separately. Merchant names are inferred from message contents, **not authenticated senders**. All matching results are marked for review.

No AI or paid API is used. The existing tester-gated Gmail connection, 25-message/90-day search, same-account requirement and provider policy limitations still apply. Gmail import currently reads HTML message bodies, not text-only messages or PDF attachments.

## Fictional example

`backend/src/test/resources/merchant-samples/swiggy-like.html` uses:

- Veg burger ₹180 + fries ₹80, one of each.
- Delivery fee ₹20 + platform fee ₹5.
- Total ₹285, which already includes those fees; the fees are not added again.
- Fictional order time 31 October 2026, 11:25 PM IST. It is late-night under the default 23:00 inclusive to 04:00 exclusive interval.

A safe end-to-end test can send the **rendered fictional HTML body**, not an attachment, from the tester account to itself with an order/receipt subject. It must be a newly composed message. The email sender is the tester, not Swiggy; this tests message retrieval and parsing but not merchant authentication or real-world compatibility. Do not impersonate the merchant. Do not forward or paste original personal receipts, credentials, tokens or private data. Safely redacted or freshly fictionalized examples can be considered later.

## Conservative behavior and remaining gaps

- The experimental path recognizes a limited Swiggy-shaped table format with explicit INR totals and order context. Ambiguous, conflicting, missing or negative totals in this path are rejected; partial details may remain unextracted. It does **not** make this guarantee for the older generic parser or all possible emails.
- Obvious cancellation/refund text in the body is rejected for matching Swiggy-shaped input, but subject-only or differently phrased events can be missed. Cancellation, refunds, replacements and reconciliation are not fully implemented.
- Recognized fee lines are evidence of amounts extracted, **not proof of a complete fee breakdown**. No extracted fee lines are displayed as unknown. The existing database/API `fees` number remains a sum of extracted lines, initialized to zero even when no lines were extracted; consumers must inspect `feeDetails` and must not treat that number as complete fees. This is not a database-level completeness model.
- Unknown item prices stay null. For a multi-quantity item with an ambiguous `Price` column, the parser does not guess the line price. Junk-food tags are editable heuristics, not nutrition or health conclusions.
- A timezone-qualified order time is preferred. If no supported, unambiguous time is found, the email received time is used and labeled approximate. That can affect late-night classification.
- Duplicate detection remains scoped to the **same Gmail message ID**. Different messages about the same order can still be counted twice. Sequential re-sync was tested; concurrency and order-level deduplication have not been verified. Review all imported records.
- Previously imported messages are not automatically re-parsed after an update. Use a new fictional message for the test; avoid sending multiple copies, as they have different message IDs.
- This implementation does not establish coverage for Zomato, Amazon, Flipkart or arbitrary merchants.

Run backend tests with Java 21 and the Maven wrapper. The parser tests and H2 integration test are synthetic. They do not demonstrate actual Gmail, Neon, production security, provider approval or real receipt compatibility.
