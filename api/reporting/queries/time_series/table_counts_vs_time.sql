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
