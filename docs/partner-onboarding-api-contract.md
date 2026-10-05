# Partner App Onboarding API Contract

This contract is the backend source of truth for the partner app onboarding screen.

## Endpoints

- `GET /api/partner/onboarding`
- `GET /api/partner/profile`
- `POST /api/partner/onboarding`
- `PUT /api/partner/onboarding`
- `PATCH /api/admin/partners/{partnerId}/onboarding/review`

`/api/v1/...` aliases are supported for the same routes.

## Required Onboarding Fields

Onboarding submission accepts `multipart/form-data` only. Text fields and required
document image files are submitted in the same request:

- `fullName`
- `dateOfBirth`, formatted as `YYYY-MM-DD` and in the past
- `gender`
- `addressLine1`
- `city`
- `state`
- `pincode`, exactly 6 digits
- `serviceTypes` or `skills`, 1 to 2 selected active categories from `GET /api/partner/categories`
- `accountNumber`, 9 to 18 digits
- `ifscCode`, valid Indian IFSC format
- `aadhaarFront`, uploaded Aadhaar front document
- `aadhaarBack`, uploaded Aadhaar back document
- `panCard`, uploaded PAN document
- `selfie`, uploaded selfie image

## Optional Fields

- `certificate`

The partner phone number is not submitted in onboarding. The backend reads the
authenticated login mobile number from the bearer token and returns it as
`mobileNumber` in onboarding/profile responses.

`serviceCategoryId` remains accepted for legacy clients, but the partner app should
submit only `skills`/`serviceTypes`.

## Multipart Compatibility Aliases

Multipart submissions accept the same common field names and file names used by the
partner app. Uploaded files are stored first, then converted to the internal
`PartnerOnboardingRequest`. The backend does not accept pre-uploaded document URL
fields for onboarding submission.

The partner app should not send hardcoded `serviceCategoryId` in onboarding. It should
fetch active categories from `GET /api/partner/categories`, show them as the service type
dropdown/multi-select options, and submit the selected category names or IDs as
`serviceTypes`. The backend accepts at most 2 selected service types.

## Status Rules

Submitting onboarding resets:

- `applicationStatus` to `PENDING`
- `documentStatus` to `PENDING`
- `bankVerificationStatus` to `PENDING`
- previous rejection reasons to `null`

Admin review updates `applicationStatus`, `documentStatus`, reviewer metadata, and
requires a rejection reason when the application or document review is rejected.
