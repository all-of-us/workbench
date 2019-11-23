# Action Audit Queries

## Schema
The Action Audit schema is a flattened schema where each row of the database 

## Target Properties
The target properties are key-value string pairs and are not in a strictly
controlled schema, but are rather enforced by application constraints. Each target type 
supports one or more properties, and these are outlined below:

| Target Type | Property  | Actions |
|-------------|----------|---------|
| WORKSPACE   | NAME     | CREATE, EDIT, DUPLICATE |
| WORKSPACE | INTENDED_STUDY | CREATE, EDIT, DUPLICATE |
| WORKSPACE | CREATOR |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | ADDITIONAL_NOTES |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | ANTICIPATED_FINDINGS |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | DISEASE_OF_FOCUS |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | REASON_FOR_ALL_OF_US |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | NAMESPACE |  CREATE, EDIT, DUPLICATE |
| WORKSPACE | CDR_VERSION_ID |  CREATE, EDIT, DUPLICATE |


## Example Queries
These queries are the same for every environment wiht the exception of the BigQuery
Dataset and Table names. The fully qualified table name is of the form
`$GCP_PROJECT.$BIGQUERY_DATASET.$TABLE`. You'll need appropriate permissions on the
project, dataset, and BigQuery service for these to work.

| Environment | Fully Qualified Table Name |
|---|----|
| Test | `all-of-us-workbench-test.workbench_action_audit_test.workbench_action_audit_test` |
| Prod | `all-of-us-rw-prod.workbench_action_audit_prod.workbench_action_audit_prod` |

### All Events by Time`
All events by anyone, ordered by time ascending.
```genericsql
SELECT
  TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) as event_time,
  jsonPayload.agent_type AS agent_type,
  CAST(jsonPayload.agent_id AS INT64) AS agent_id,
  jsonPayload.agent_email AS agent_email,
  jsonPayload.action_id AS action_id,
  jsonPayload.action_type AS action_type,
  jsonPayload.target_type AS target_type,
  CAST(jsonPayload.target_id AS INT64) AS target_id,
  jsonPayload.target_property AS target_property,
  jsonPayload.prev_value AS prev_value,
  jsonPayload.new_value AS new_value
FROM `all-of-us-rw-prod.workbench_action_audit_prod.workbench_action_audit_prod`
ORDER BY event_time, agent_id
LIMIT 1000
```

### All events for User with given AoU Email

```genericsql
SELECT
  TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) as event_time,
  jsonPayload.agent_type AS agent_type,
  CAST(jsonPayload.agent_id AS INT64) AS agent_id,
  jsonPayload.agent_email AS agent_email,
  jsonPayload.action_id AS action_id,
  jsonPayload.action_type AS action_type,
  jsonPayload.target_type AS target_type,
  CAST(jsonPayload.target_id AS INT64) AS target_id,
  jsonPayload.target_property AS target_property,
  jsonPayload.prev_value AS prev_value,
  jsonPayload.new_value AS new_value
FROM `all-of-us-rw-prod.workbench_action_audit_prod.workbench_action_audit_prod`
WHERE jsonPayload.agent_type = 'USER' AND
  jsonPayload.agent_email = 'username@researchallofus.org'
ORDER BY event_time, agent_id
LIMIT 1000
```
