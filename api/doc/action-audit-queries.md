# Action Audit Queries

## Schema
The Action Audit schema is a flattened schema where each row of the database represents
an action taken by a user or a user-visisble action taken on their behalf by either an
administrator, another standard user, or the system itself.

### Actions and Events
A user action may be recorded as multiple rows in the database. This happens because we have
a flat, denormalized schema and can't represent composite structures in a single row. In order to
keep related events in an action together, a randomly generated Action ID is used. This essentially
acts as a paperclip to keep events from the same action together.

### Agent Types
There are three agent types currently:
- USER: standard AoU users. 
- ADMINISTRATOR: Users with Admin privileges. Used for BYPASS events and the like
- SYSTEM: Some part of the All of Us software stack. Typically used for automated events,
or events that happen as a consequence of some other event.

### Target Types
A Target is a thing being operated on during an Action. The following target types are largely
self-explanatory. The design document has additional details.
 - USER
 - WORKBENCH
 - PROFILE
 - ACCOUNT
 - NOTEBOOK
 - NOTEBOOK_SERVER
 - DATASET
 - CONCEPT_SET
 - COHORT
 - CREDIT
 - WORKSPACE

## Target Properties
The target properties are key-value string pairs and are not in a strictly
controlled schema, but are rather enforced by application constraints. Each target type 
supports one or more properties, and these are outlined below:

| Target Type | Property  |
|-------------|----------|
| WORKSPACE   | name     |
| WORKSPACE | intended_study |
| WORKSPACE | creator | 
| WORKSPACE | additional_notes | 
| WORKSPACE | anticipated_findings | 
| WORKSPACE | disease_of_focus | 
| WORKSPACE | reason_for_all_of_us | 
| WORKSPACE | namespace | 
| WORKSPACE | cdr_version_id | 
| PROFILE | user_name |
| PROFILE | contact_email |
| PROFILE | data_access_level |
| PROFILE | given_name |
| PROFILE | phone_number |
| PROFILE | current_position |
| PROFILE | organization |
| PROFILE | disabled |
| PROFILE | about_you |
| PROFILE | area_of_research |
| PROFILE | institutional_affiliations |
| PROFILE | demographic_survey |
| PROFILE | address |
| ACL | ACCESS_LEVEL |

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
ORDER BY event_time, agent_id, action_type
LIMIT 1000
```

### All events for User with given AoU Email (user is agent)
Replace `username@researchallofus.org` with correct email address.
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
ORDER BY event_time, agent_id, action_type
LIMIT 1000
```

### User Profile Edits
Single user Profile Edit actions

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
  jsonPayload.agent_email = 'username@researchallofus.org' AND
  jsonPayload.target_type = 'PROFILE' AND
  jsonPayload.action_type = 'EDIT'
ORDER BY event_time, agent_id
LIMIT 1000
```

### Single-user Profile actions (CREATE, EDIT, DELETE)
Same as above, but we don't restrict the action type. Note that delete profile events only
happen on envvironments where self-service delete is enabled.


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
  jsonPayload.agent_email = 'username@researchallofus.org' AND
  jsonPayload.target_type = 'PROFILE'
ORDER BY event_time, agent_id
LIMIT 1000
```
### All Workspace actions for User (multiple target workspaces)
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
  jsonPayload.agent_email = 'username@researchallofus.org' AND
  jsonPayload.target_type = 'WORKSPACE'
ORDER BY target_id, event_time, action_id
LIMIT 1000
```

### Single-workspace actions (for any users)
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
  jsonPayload.target_id = 523 AND
  jsonPayload.target_type = 'WORKSPACE'
ORDER BY event_time, agent_id, action_id
LIMIT 1000
```
