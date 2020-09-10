-- demonstrate using latest snapshot only in query. This could be faster with time partitioning.
SELECT
    TIMESTAMP_MILLIS(r.snapshot_timestamp) AS snapshot,
    r.first_name,
    r.username,
    ARRAY_AGG(STRUCT(workspace_id,
                     w.name)) AS workspaces
FROM
    reporting_test.workspace AS w
        INNER JOIN
    reporting_test.researcher AS r
    ON
                r.researcher_id = w.creator_id
            AND w.snapshot_timestamp = r.snapshot_timestamp
WHERE
        w.snapshot_timestamp = (
        SELECT
            MAX(snapshot_timestamp) AS latest
        FROM
            reporting_test.researcher)
GROUP BY
    r.snapshot_timestamp,
    r.username,
    r.first_name;
