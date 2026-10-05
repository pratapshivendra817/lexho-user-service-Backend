# Support & Compliance API

All responses use the existing `ApiResponse<T>` envelope unless an existing admin API uses its legacy admin response wrapper.

## Public Website APIs

| Method | Endpoint | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/api/v1/legal/terms` | Public | Current published Terms & Conditions |
| GET | `/api/v1/legal/privacy` | Public | Current published Privacy Policy |
| GET | `/api/v1/legal/cancellation` | Public | Current published Cancellation & Refund Policy |
| GET | `/api/v1/legal/partner-terms` | Public | Current published Partner Terms |
| GET | `/api/v1/legal/cookie` | Public | Current published Cookie Policy |
| GET | `/api/v1/legal/disclaimer` | Public | Current published Disclaimer |
| GET | `/api/v1/legal/grievance` | Public | Current published Grievance Policy |
| GET | `/api/v1/legal/home` | Public | Current published Home content |
| GET | `/api/v1/legal/about` | Public | Current published About content |
| GET | `/api/v1/legal/current?documentType=PRIVACY_POLICY` | Public | Current published document by type |
| GET | `/api/v1/faqs?userType=CUSTOMER` | Public | Active FAQs for `GENERAL` plus requested user type |
| GET | `/api/v1/support/contact` | Public | Company support email and mobile only |

## Legal Acceptance APIs

| Method | Endpoint | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/api/v1/legal/accept` | Customer/Partner | Body: `documentType`, optional `documentVersion` |
| GET | `/api/v1/legal/acceptance-history` | Customer/Partner | Current user's legal acceptance history |

## Admin Legal APIs

| Method | Endpoint | Auth | Notes |
| --- | --- | --- | --- |
| GET | `/api/v1/admin/legal` | Admin | List legal documents by `platform`, default `PARTNER` |
| GET | `/api/v1/admin/legal/{id}` | Admin | Get one legal document version |
| POST | `/api/v1/admin/legal` | Admin | Create draft version |
| PUT | `/api/v1/admin/legal/{id}` | Admin | Edit draft only |
| POST | `/api/v1/admin/legal/{id}/publish` | Admin | Publish version and archive previous current version |
| POST | `/api/v1/admin/legal/{id}/archive` | Admin | Archive version |
| GET | `/api/v1/admin/legal/{id}/versions` | Admin | Version history by document type |

Legacy `/api/v1/admin/legal-content` and `/api/v1/content/documents/{slug}` routes remain available.

## Account Deletion APIs

| Method | Endpoint | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/api/v1/account/delete-request` | Customer/Partner | Creates `REQUESTED` deletion request |
| GET | `/api/v1/account/delete-request/status` | Customer/Partner | Latest request, or `NONE` |
| POST | `/api/v1/account/delete-request/cancel` | Customer/Partner | Cancels open request |
| GET | `/api/v1/admin/account-deletion` | Admin | List requests, optional `status` |
| GET | `/api/v1/admin/account-deletion/{id}` | Admin | Request detail with business condition counts |
| POST | `/api/v1/admin/account-deletion/{id}/approve` | Admin | Moves to `SCHEDULED` if checks pass |
| POST | `/api/v1/admin/account-deletion/{id}/reject` | Admin | Body: `reason` |
| POST | `/api/v1/admin/account-deletion/{id}/process` | Admin | Marks `DELETED` and anonymizes profile fields |

Open deletion requests are blocked by active booking counts. Payment/refund/payout rows are retained; this module does not hard-delete transactional records or create a duplicate refund/payment processor.

## Configuration

Support contact defaults can be overridden with:

```properties
lexho.support.email=support@lexho.in
lexho.support.mobile=+910000000000
```
