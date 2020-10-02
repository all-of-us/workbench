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

### Re-structured Result Sets
Several of our tables are very wide, but groups of columns within them
are frequently used together (or don't make sense when  distributed)
throughout the result set. For this purpose, there are structured versions
of the queries taking advantage of the `STRUCT` function in BigQuery  (creating
a `RECORD` column in the result set). The structured query for users looks like the
following. Note that this representation *does not constitute an official schema or
contract*, but is provided as a reasonable veiw of the logical  structure of our tables
and a useful starting point for future ad-hoc queries. 
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
                   data_use_agreement_completion_time AS completion_time,
                   data_use_agreement_bypass_time AS bypass_time) AS data_use_agreement,
            STRUCT(u.era_commons_completion_time AS completion_time,
                   u.era_commons_bypass_time AS bypass_time) AS era_commons,
            STRUCT(u.two_factor_auth_completion_time AS completion_time,
                   u.two_factor_auth_bypass_time AS bypass_time) AS two_factor_auth,
            u.data_access_level) AS compliance,
    STRUCT(u.free_tier_credits_limit_days_override AS days,
           u.free_tier_credits_limit_dollars_override AS dollars) AS free_tier_credits_limit_override
FROM
    reporting_test.latest_users u
ORDER BY
    u.user_id;
```

When viewing the result set for the above query, columns have headings such as
`profile.about_you` and `compliance.data_use_agreement.bypass_time`. An easier way to visualize
the groupings is int he JSON tab:
```json
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
        "completion_time": null,
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
      "data_access_level": "registered"
    },
    "free_tier_credits_limit_override": {
      "days": null,
      "dollars": null
    }
  }
```
This representation is much easier to work with than the flat view.

The current structured workspace result  view looks like the following:
```json
{
    "metadata": {
      "workspace_id": "130",
      "name": "test-workspace",
      "creator_id": "140",
      "cdr_version_id": "1",
      "creation_time": "2018-06-28 13:42:53 UTC"
    },
    "history": {
      "creation_time": "2018-06-28 13:42:53 UTC",
      "last_accessed_time": null,
      "last_modified_time": "2018-06-28 13:42:53 UTC"
    },
    "research_purpose": {
      "additional_notes": null,
      "ancestry": false,
      "anticipated_findings": null,
      "commercial_purpose": false,
      "control_set": true,
      "disease_focused_research": false,
      "disease_of_focus": null,
      "disseminate_research_other": null,
      "drug_development": false,
      "educational": false,
      "ethics": false,
      "intended_study": "",
      "methods_development": false,
      "other_population_details": null,
      "other_purpose": false,
      "other_purpose_details": null,
      "population_health": false,
      "reason_for_all_of_us": null,
      "scientific_approach": null,
      "social_behavioral": false,
      "review_rerquest": {
        "rerquested": false,
        "time_requested": null,
        "approved": null,
        "needs_user_prompt": "0"
      }
    },
    "billing": {
      "status": "ACTIVE",
      "account_type": "FREE_TIER"
    }
  }
```

The cohort table is interesting in that it contains a JSON column, `criteria`.  BigQuery
has evolving support for JSON operations in queries, including a new `JSON_ARRAY_EXTRACT`  method.
It's  not clear yet how  far we can (or should) go in remapping the entire criteria column,
but the current "structuerd" query for cohorts re-aggregates the outerrmost array in the
criteria strurcture (so-called `includes`) into a RECORD type:
```sql
  -- Demonstrate a bit of highh-level structure applied to the cohort table. Splits criteria JSON object
  -- into top-level includes array.
SELECT
  TIMESTAMP_MILLIS(c.snapshot_timestamp) AS snapshot,
  c.creator_id,
  STRUCT( c.workspace_id,
    c.cohort_id,
    c.name,
    c.description) AS metadata,
  STRUCT(c.creation_time,
    c.last_modified_time) AS history,
  STRUCT(JSON_EXTRACT_ARRAY(c.criteria,
      '$.includes') AS includes) AS criteria
FROM
  reporting_test.latest_cohorts c
ORDER BY
  c.snapshot_timestamp,
  c.creator_id,
  c.workspace_id,
  c.cohort_id,
  c.name,
  c.description,
  c.criteria,
  c.creation_time,
  c.last_modified_time
  -- ORDER BY c.snapshot_timestamp, c.creator_id, c.workspace_id, c.cohort_id;
```

An example otuput row that has more than one include item is this:
```json
{
   "snapshot":"2020-10-02 02:00:07.622 UTC",
   "creator_id":"137",
   "metadata":{
      "workspace_id":"8464",
      "cohort_id":"7837",
      "name":"asdfasdfasdf",
      "description":null
   },
   "history":{
      "creation_time":"2020-06-18 23:51:06 UTC",
      "last_modified_time":"2020-06-18 23:51:06 UTC"
   },
   "criteria":{
      "includes":[
         "{\"id\":\"includes_4x4rkpum4\",\"items\":[{\"id\":\"items_wbq8bl8cn\",\"type\":\"PERSON\",\"searchParameters\":[{\"parameterId\":\"\",\"name\":\"Deceased\",\"domain\":\"PERSON\",\"type\":\"DECEASED\",\"group\":false,\"attributes\":[]}],\"modifiers\":[]}],\"temporal\":false}",
         "{\"id\":\"includes_b521sfl5j\",\"items\":[{\"id\":\"items_nvb45uj8o\",\"type\":\"SURVEY\",\"searchParameters\":[{\"parameterId\":\"param325080\",\"name\":\"Overall Health\",\"domain\":\"SURVEY\",\"type\":\"PPI\",\"group\":true,\"attributes\":[],\"ancestorData\":false,\"standard\":false,\"conceptId\":1585710,\"subtype\":\"SURVEY\"}],\"modifiers\":[]}],\"temporal\":false}"
      ]
   }
}
```

Further extraction and transformation is hopefully possible, but it's likely to involve
inefficient nested JSON function calls.

## Time Series
Several analyses of interest involve watching functions  of each  snapshot  over time, to 
gain insights into trends in usage, registration, and collaboration. Currently a single
query  demonstrates this approach, though others are envisioned.

The followign query simply counts the rows in each table at each snapshot:

```sql
-- simple count of each table over time. Demonstrates time series aggregation across snapshots
SELECT
    TIMESTAMP_MILLIS(u.snapshot_timestamp) AS snapshot,
    (
        SELECT
            COUNT(u_inner.user_id)
        FROM
            reporting_test.user u_inner
        WHERE
                u_inner.snapshot_timestamp = u.snapshot_timestamp) AS user_count,
    (
        SELECT
            COUNT(w.workspace_id)
        FROM
            reporting_test.workspace w
        WHERE
                w.snapshot_timestamp = u.snapshot_timestamp) AS workspace_count,
    (
        SELECT
            COUNT(c.cohort_id)
        FROM
            reporting_test.cohort c
        WHERE
                c.snapshot_timestamp = u.snapshot_timestamp) AS cohort_count
FROM
    reporting_test.user u
GROUP BY
    u.snapshot_timestamp
ORDER BY
    u.snapshot_timestamp;
```

The only  tricky bit is remembering to use a subquery for each table,
as naively joining on the snapshot_timestamp and counting gives
the incorrect result. Performance isn't blazing fast, but so far isn't  prohibitive either.
It's  possible we'll want to run a scheduled  query to evaluate these results on a regular
basis.

Rersults as of  today look like  the following. Note that currently there's  no provision
for selecting only the  most recent result set for any calendar day. It's hoped that
by providing either a view or conveniently named result table  will allow connecting
a tool  like Looker  or Tableau directly for ad hoc analysis. 

```json
[
  {
    "snapshot": "2020-09-28 15:51:50.885 UTC",
    "user_count": "2273",
    "workspace_count": "2135",
    "cohort_count": "0"
  },
  {
    "snapshot": "2020-10-01 17:00:45.677 UTC",
    "user_count": "2277",
    "workspace_count": "2161",
    "cohort_count": "2803"
  },
  {
    "snapshot": "2020-10-02 01:09:48.685 UTC",
    "user_count": "2277",
    "workspace_count": "2257",
    "cohort_count": "2820"
  }
]
```

## Future development
Live and structured views for additional tables  will be proided as these are  added. We
also plan to use  Terraform to  update views and scheduled queries in each environment
along with dataset definitions and table schemas. Additional y-values for  time series
are planned as well.
