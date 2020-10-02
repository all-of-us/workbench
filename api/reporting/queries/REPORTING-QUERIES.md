# Reporting Dataset Queries

## Live queries
Several analysis activities are interewted only in the most recent
snapshot of the data. Views defining `latest_users.sql. These queries
(in the latest subdirectory) simply plug in the max `snapshot_timestamp`
value. For example, the latest user rows are available by this query
(saved as a veiw in the `reporting_test` dataset).

### Basic Live Queries
The simple versions of these queries return all columns of the asscociated
tables from the most recent snapshot. If a reporting upload job is in progress
or only recently completed, the results may be incomplete.
```sql
-- All users from the most recent snapshot
SELECT
    u.*
FROM
    reporting_test.user u
WHERE
        u.snapshot_timestamp = (
        SELECT
            MAX(u2.snapshot_timestamp)
        FROM
            reporting_test.user u2)
ORDER BY
    u.username;
```

### Structured Results
Several of our tables are very wide, but groups of columns within them
are frequently used together (or don't make sense whhen  distriuted)
throughout the result set. For this purpose, there are structured versions
of the queries taking advantage of the `STRUCT` function in BigQuery  (creating
a `RECORD` column in the result set). The structured query for users looks like
```sql
SELECT
  TIMESTAMP_MILLIS(u.snapshot_timestamp) AS snapshot,
  STRUCT( u.user_id,
    u.username,
    u.disabled,
    u.creation_time,
    u.first_sign_in_time,
    u.first_registration_completion_time,
    u.last_modified_time) AS account,
  STRUCT( u.given_name,
    u.family_name,
    u.about_you,
    u.area_of_research,
    u.current_position,
    u.demographic_survey_completion_time) AS profile,
  STRUCT(u.contact_email,
    u.street_address_1,
    u.street_address_2,
    u.city,
    u.state,
    u.zip_code,
    u.country,
    u.professional_url) AS contact_info,
  STRUCT( STRUCT(u.compliance_training_completion_time AS completion_time,
      u.compliance_training_bypass_time AS bypass_time,
      u.compliance_training_expiration_time AS expiration_time) AS compliance_training,
    STRUCT(data_use_agreement_signed_version AS signed_version,
      data_use_agreement_completion_time AS completion_timei,
      data_use_agreement_bypass_time AS bypass_time) AS data_use_agreement,
    STRUCT(u.era_commons_completion_time AS completion_time,
      u.era_commons_bypass_time AS bypass_time) AS era_commons,
    STRUCT(u.two_factor_auth_completion_time AS completion_time,
      u.two_factor_auth_bypass_time AS bypass_time) AS two_factor_auth,
    u.data_access_level,
    STRUCT(u.free_tier_credits_limit_days_override AS days,
      u.free_tier_credits_limit_dollars_override AS dollars) AS free_tier_credits_limit_override) AS compliance
FROM
  reporting_test.latest_users u
ORDER BY
  u.user_id;
```

When viewing the result set for the above query, columns have headings such as
`profile.about_you` and `compliance.data_use_agreement.bypass_time`. An easier way to visualize
the groupings is int he JSON tab:
```json
[
  {
    "snapshot": "2020-10-02 02:00:07.622 UTC",
    "account": {
      "user_id": "137",
      "username": "calbach@fake-research-aou.org",
      "disabled": false,
      "creation_time": "1970-01-01 00:00:00 UTC",
      "first_sign_in_time": "2018-07-02 19:49:11 UTC",
      "first_registration_completion_time": "2020-01-03 21:23:45 UTC",
      "last_modified_time": "2020-10-02 01:33:15 UTC"
    },
    "profile": {
      "given_name": "CH",
      "family_name": "Albach",
      "about_you": null,
      "area_of_research": null,
      "current_position": null,
      "demographic_survey_completion_time": null
    },
    "contact_info": {
      "contact_email": "calbach@fake-research-aou.org",
      "street_address_1": null,
      "street_address_2": null,
      "city": null,
      "state": null,
      "zip_code": null,
      "country": null,
      "professional_url": null
    },
    "compliance": {
      "compliance_training": {
        "completion_time": null,
        "bypass_time": "2019-10-24 16:36:46 UTC",
        "expiration_time": null
      },
      "data_use_agreement": {
        "signed_version": "3",
        "completion_timei": null,
        "bypass_time": null
      },
      "era_commons": {
        "completion_time": "2019-05-15 17:07:05 UTC",
        "bypass_time": "2019-10-24 16:36:47 UTC"
      },
      "two_factor_auth": {
        "completion_time": "2019-04-26 21:32:04 UTC",
        "bypass_time": "2019-10-24 16:36:49 UTC"
      },
      "data_access_level": "registered",
      "free_tier_credits_limit_override": {
        "days": null,
        "dollars": null
      }
    }
  }
]
```
This representation is much easier to work with than the flat view.

The current structured workspace result  view looks like the following:
```json

```
