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
