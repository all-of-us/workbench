# Action Audit System

## Language
Much of this system is written in Kotlin, as a prototype/demo of that language in this project.

## Stackdriver logging (aka Cloud Logging)
We send events with a custom JsonPayload. This payload contains all the columns 
of the (only) table in our schema. It's important to keep this consistent over time so that
the dataset remains consistently queryable. Migrations are possible, but this may complicate
queries.

## Schema
See schema [outline](action-audit-queries.md). Due to Stackdriver JSON limitations, we use double fields for all numeric and timestamp
types, and strings for everything else.

### Target Types
A target is an object being created or acted upon in some way in the system, or the
system itself. This column is required, and should be one of the values in the [TargetType](../src/main/java/org/pmiops/workbench/actionaudit/TargetType.kt)
enum.

### Action Types
An action is anything a user (or other agent) can do in the system. It should correspond
to one of the [ActionType](../src/main/java/org/pmiops/workbench/actionaudit/ActionType.kt) values.

### Action IDs
Many actions have several key-value pairs we want to record. For example, when a user creates
a workspace, there are 20 or so fields and boolean options selectable. Since we don't want
a massive number of columns in the table, and since we can't use neseted fields in BigQuery
when sending a JsonPayload, we cheat by using many events for this action. These are then
tied together by an Action ID, which is a random GUID string. Auditors can inject a
`Provider<String>` with the qualifier `@Qualifier("ACTION_ID")` to generate new action IDs.

### Agent Types
There are a handful of agent types in the stytem. By far the majority of actions use a USER
agent type. These are listed in the [AgentType](../src/main/java/org/pmiops/workbench/actionaudit/AgentType.kt) enum.

### Target Properties
In the schema we have a place for optional key-value string pairs describing
the target being operated on. In order to pull these from application objects, there's a Model-backed
Target Propoerty [interface](../src/main/java/org/pmiops/workbench/actionaudit/targetproperties/ModelBackedTargetProperty.kt).
This interface describes a contract that an enum type can implement in order to provide all available values,
as well as a map from old to new values for actions that wish to capture those events.

For cases where the target does not correspond to a single application model class, there's a simpler
parent interface called `SimpleTargetProperty`, which just dictates that a property name be specified.

## ActionAuditEvent
The [ActionAuditEvent](../src/main/java/org/pmiops/workbench/actionaudit/ActionAuditEvent.kt) class
is an immutable Kotlin data class which stores values for every column in our schema
(i.e. the JsonPayload values). When creating these in Kotlin, named parameter syntax is
available. For Java code, there's a builder for this purpose.

## Auditors
In order to minimize the intrusiveness of any instrumentation, we have Auditor services that
handle creating ActionAuditEvents to be passed on to the ActionAuditService. A canonical
example is the [WorkspaceAuditorImpl](../src/main/java/org/pmiops/workbench/actionaudit/auditors/WorkspaceAuditorImpl.kt), which demonstrates creating the events for workspace
create, duplicate, share, and delete. Each public method on this service generates event(s)
for a single action.

## ActionAuditService
The [ActionAuditServiceImpl](../src/main/java/org/pmiops/workbench/actionaudit/ActionAuditServiceImpl.kt) handles translating `ActionAuditEvent`s into `LogEntry` object for
passing on to Cloud Logging. It writes to a log named `workbench-action-audit-$env`, where
`$env` is the running environment according to the JSON configuration.
