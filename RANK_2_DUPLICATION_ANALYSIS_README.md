# Rank #2 Duplication Analysis

## Scope

Target files:

- `application-module/src/main/java/com/lexho/user_service/lexhopartner/service/PartnerOperationsService.java`
- `application-module/src/main/java/com/lexho/user_service/lexhopartner/service/PartnerSupportService.java`

Candidate duplicated logic:

- `serializeAttachmentUrls`
- `deserializeAttachmentUrls`
- Most of `sanitizeAttachmentUrls`

This document is analysis only. No refactoring has been applied.

## Exact Duplicated Methods and Blocks

### PartnerOperationsService

Duplicated declarations and helpers:

- `OBJECT_MAPPER`
- `STRING_LIST_TYPE`
- `MAX_ATTACHMENTS`
- `MAX_ATTACHMENT_URL_LENGTH`
- `serializeAttachmentUrls(List<String> attachmentUrls)`
- `deserializeAttachmentUrls(String rawValue)`
- `sanitizeAttachmentUrls(List<String> attachmentUrls)`

Relevant location:

- `PartnerOperationsService.java`, around lines `46-49`
- `PartnerOperationsService.java`, around lines `525-570`

### PartnerSupportService

Duplicated declarations and helpers:

- `OBJECT_MAPPER`
- `STRING_LIST_TYPE`
- `MAX_ATTACHMENTS`
- `MAX_ATTACHMENT_URL_LENGTH`
- `serializeAttachmentUrls(List<String> attachmentUrls)`
- `deserializeAttachmentUrls(String rawValue)`
- `sanitizeAttachmentUrls(List<String> attachmentUrls)`

Relevant location:

- `PartnerSupportService.java`, around lines `35-39`
- `PartnerSupportService.java`, around lines `268-313`

Approximate duplicated code: `29-33` effective lines.

## Behavioral Comparison

The two implementations are behaviorally identical for attachment URL handling.

| Concern | PartnerOperationsService | PartnerSupportService | Result |
| --- | --- | --- | --- |
| ObjectMapper usage | `new ObjectMapper()` | `new ObjectMapper()` | Identical |
| ObjectMapper config | Default Jackson config | Default Jackson config | Identical |
| JSON type | `TypeReference<List<String>>` | `TypeReference<List<String>>` | Identical |
| Null attachment input | Returns `List.of()`, then serializes as `null` | Returns `List.of()`, then serializes as `null` | Identical |
| Empty attachment input | Returns `List.of()`, then serializes as `null` | Returns `List.of()`, then serializes as `null` | Identical |
| Blank URL values | Skipped | Skipped through `blankToNull` | Identical |
| Trimming | `value.trim()` | `blankToNull(value)` returns `value.trim()` | Identical |
| Deduplication | `sanitized.contains(normalized)` | `sanitized.contains(normalized)` | Identical |
| Ordering | First occurrence order preserved in `ArrayList` | First occurrence order preserved in `ArrayList` | Identical |
| Max attachment count | `10` | `10` | Identical |
| Max URL length | `500` | `500` | Identical |
| Overlong URL exception | `BadRequestException("attachmentUrl is too long")` | Same | Identical |
| Too many attachments exception | `BadRequestException("No more than 10 attachments are allowed")` | Same | Identical |
| Serialization failure | Throws `BadRequestException("Invalid attachmentUrls")` | Same | Identical |
| Invalid stored JSON | Returns `List.of()` | Returns `List.of()` | Identical |
| Warning/error logging | None | None | Identical |

## Textual Difference

`sanitizeAttachmentUrls` is not completely textually identical because:

- `PartnerOperationsService` inlines blank normalization:

```java
String normalized = value == null || value.isBlank() ? null : value.trim();
```

- `PartnerSupportService` delegates to its local helper:

```java
String normalized = blankToNull(value);
```

`PartnerSupportService.blankToNull` is:

```java
private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
}
```

Therefore, this difference is textual only. Behavior remains identical.

## Intentional vs Accidental Duplication

The duplication appears accidental/genuine.

The surrounding domain behavior is intentionally different:

- `PartnerOperationsService` stores attachment URLs on `PartnerWarningComment`.
- `PartnerSupportService` stores attachment URLs on `SupportTicket` and `SupportTicketReply`.

However, the attachment URL JSON format and validation rules are the same. There is no evidence that either service intentionally needs different attachment handling.

## Proposed Minimal Refactoring

Create a package-private static helper in:

```text
application-module/src/main/java/com/lexho/user_service/lexhopartner/service/PartnerAttachmentUrlSupport.java
```

Recommended shape:

```java
final class PartnerAttachmentUrlSupport {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final int MAX_ATTACHMENTS = 10;
    private static final int MAX_ATTACHMENT_URL_LENGTH = 500;

    private PartnerAttachmentUrlSupport() {
    }

    static String serializeAttachmentUrls(List<String> attachmentUrls) {
        // move existing logic exactly
    }

    static List<String> deserializeAttachmentUrls(String rawValue) {
        // move existing logic exactly
    }

    static List<String> sanitizeAttachmentUrls(List<String> attachmentUrls) {
        // move existing behavior exactly
    }
}
```

Do not introduce a Spring bean. The helper should remain package-private.

Do not move `PartnerSupportService.blankToNull`, because it is also used for unrelated service-specific behavior such as category and booking ID normalization.

## Refactoring Constraints

Any future implementation should preserve:

- JSON array-of-strings format
- Default Jackson `ObjectMapper` behavior
- Null and empty input behavior
- Blank string filtering
- URL trimming
- First-occurrence ordering
- Deduplication behavior
- Maximum attachment count of `10`
- Maximum URL length of `500`
- Existing exception messages
- Invalid JSON fallback to `List.of()`
- No logging behavior
- Public APIs and method signatures

## Risk Level

Risk level: low.

The logic is duplicated and behaviorally identical. The main risks are accidental drift during extraction, especially:

- Changing exception messages
- Changing the timing of the `MAX_ATTACHMENTS` validation
- Changing the JSON format
- Accidentally moving unrelated `blankToNull` behavior

## Expected SonarQube Duplication Reduction

Expected reduction: approximately `29-33` duplicated lines.

The refactor should remove the duplicated helper block and related constants from both services, likely resolving the Rank #2 duplication candidate.

## Existing Test Coverage

Existing tests that touch these paths:

- `application-module/src/test/java/com/lexho/user_service/lexhopartner/service/PartnerSupportServiceTest.java`
  - `createsPartnerTicketWithPartnerCategoryPrefix`
  - `detailReturnsPartnerAndSupportMessages`
  - `replyAndStatusUpdateShouldSupportTwoWayFlow`

- `application-module/src/test/java/com/lexho/user_service/lexhopartner/service/PartnerOperationsServiceTest.java`
  - `warningDetailAcknowledgeAndDisputeShouldPersistMetadata`

## Test Coverage Gaps

No targeted tests were found for:

- Blank URL filtering
- Duplicate URL deduplication
- Overlong URL rejection
- More than 10 attachments rejection
- Malformed stored JSON fallback
- Empty or null attachment serialization returning `null`

These gaps do not block a minimal extraction if the logic is moved exactly, but they are useful regression tests if this helper is refactored later.
