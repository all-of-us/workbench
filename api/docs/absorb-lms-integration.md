# Absorb LMS Integration

The Workbench integrates with Absorb LMS (the Training and Education Platform, or TEP) to verify
that researchers have completed required compliance training before being granted access to
Registered Tier and Controlled Tier data.

## Overview

When a user navigates to their profile or access renewal page, the Workbench calls the Absorb REST
API to check whether the user has completed and passed the required compliance training courses. The
results determine whether the user is granted access to the corresponding data tier.

All Absorb API calls are made against the base URL: `https://rest.myabsorb.com`

## API Endpoints

### POST /authenticate

Authenticates the integration with Absorb to obtain a time-limited access token (valid for 4 hours).

**Request:**
```json
{
  "username": "<admin_username>",
  "password": "<admin_password>",
  "privateKey": "<api_key_guid>"
}
```

**Headers:** `x-api-key`

**Response (200):**
```json
"<access_token_string>"
```

### GET /users

Looks up a user in Absorb by email address to retrieve their Absorb user ID. This also tells us
whether the user has ever logged into Absorb (accounts are created on first login).

**Query parameter:** `_filter=username eq '{email}'` (OData filter syntax)

**Headers:** `x-api-key`, `Authorization` (access token)

**Response (200):**
```json
{
  "totalItems": 1,
  "returnedItems": 1,
  "limit": 10,
  "offset": 0,
  "users": [
    {
      "id": "<user_guid>",
      "username": "user@example.com"
    }
  ]
}
```

If the user has never logged into Absorb, `users` will be an empty array (`totalItems: 0`).

### GET /users/{userId}/enrollments

Retrieves active course enrollments for a user to check compliance training completion status and
scores.

**Path parameter:** `userId` (Absorb user GUID)

**Query parameter:** `_filter=isActive` (only active enrollments)

**Headers:** `x-api-key`, `Authorization` (access token)

**Response (200):**
```json
{
  "totalItems": 2,
  "returnedItems": 2,
  "limit": 10,
  "offset": 0,
  "enrollments": [
    {
      "id": "<enrollment_guid>",
      "courseId": "9ad49c70-3b72-4789-8282-5794efcd4ce1",
      "courseName": "RT Compliance Training",
      "dateCompleted": "2025-06-15T14:30:00",
      "isActive": true,
      "score": 95.0
    },
    {
      "id": "<enrollment_guid>",
      "courseId": "3765dc64-cc64-4efa-bfc0-9a4dc2e9d09d",
      "courseName": "CT Compliance Training",
      "dateCompleted": null,
      "isActive": true,
      "score": null
    }
  ]
}
```

## Tracked Courses

| Course | Course ID | Access Module |
|--------|-----------|---------------|
| Registered Tier (RT) Compliance Training | `9ad49c70-3b72-4789-8282-5794efcd4ce1` | RT_COMPLIANCE_TRAINING |
| Controlled Tier (CT) Compliance Training | `3765dc64-cc64-4efa-bfc0-9a4dc2e9d09d` | CT_COMPLIANCE_TRAINING |

A course is considered passed when `dateCompleted` is non-null AND `score >= 80`.

## Trigger and Call Sequence

The Absorb sync is **not scheduled or automated**. It is triggered exclusively by an explicit call
to the Workbench API endpoint:

```
POST /v1/account/sync-compliance-training-status
```

This is called when a user navigates to their profile or access renewal page in the Workbench UI.

Each sync triggers 3 Absorb API calls in sequence:

1. `POST /authenticate` — obtain access token
2. `GET /users` — look up user by email
3. `GET /users/{userId}/enrollments` — fetch active enrollments

### Short-circuit conditions

- If the user is a **service account**, no Absorb calls are made.
- If the user has **never logged into Absorb** (no user found in step 2), the flow stops after
  step 2 (2 calls total).

There are no cron jobs, webhooks, batch syncs, or event-driven triggers for the Absorb integration.

## Authentication and Credentials

Admin credentials (API key, username, password) are stored in each environment's credentials bucket: `absorb-credentials.json`.
They are retrieved at runtime via `CloudStorageClient.getAbsorbCredentials()`. A new access token is
obtained per sync call (tokens are valid for 4 hours).

## Key Source Files

| File | Purpose |
|------|---------|
| `src/main/resources/absorb.yaml` | OpenAPI spec defining Absorb API endpoints and schemas |
| `src/main/java/org/pmiops/workbench/absorb/AbsorbService.java` | Service interface |
| `src/main/java/org/pmiops/workbench/absorb/AbsorbServiceImpl.java` | Service implementation — makes the 3 API calls |
| `src/main/java/org/pmiops/workbench/absorb/Credentials.java` | Holds apiKey, accessToken, userId |
| `src/main/java/org/pmiops/workbench/absorb/Enrollment.java` | Holds courseId, completionTime, score |
| `src/main/java/org/pmiops/workbench/compliancetraining/ComplianceTrainingServiceImpl.java` | Orchestrates sync, maps courses to access modules, enforces passing score |
| `src/main/java/org/pmiops/workbench/api/ProfileController.java` | Exposes the sync endpoint |
