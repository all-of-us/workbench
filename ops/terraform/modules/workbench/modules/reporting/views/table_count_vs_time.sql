-- simple count of each table over time. Demonstrates time series
-- aggregation across snapshots. Note that if the user table is missing a timestamp,
-- we consider it a bad snapshot, but any other table will return zero rows.
SELECT
    TIMESTAMP_MILLIS(u.snapshot_timestamp) AS snapshot,
    (
        SELECT
            COUNT(u_inner.user_id)
        FROM
            `${project}`.${dataset}.user u_inner
        WHERE
                u_inner.snapshot_timestamp = u.snapshot_timestamp) AS user_count,
    (
        SELECT
            COUNT(w.workspace_id)
        FROM
            `${project}`.${dataset}.workspace w
        WHERE
                w.snapshot_timestamp = u.snapshot_timestamp) AS workspace_count,
    (
        SELECT
            COUNT(c.cohort_id)
        FROM
            `${project}`.${dataset}.cohort c
        WHERE
                c.snapshot_timestamp = u.snapshot_timestamp) AS cohort_count,
    (
        SELECT
            COUNT(institution_id)
        FROM
            `${project}`.${dataset}.institution i
        WHERE
                i.snapshot_timestamp = u.snapshot_timestamp) AS institution_count
FROM
    `${project}`.${dataset}.user u
GROUP BY
    u.snapshot_timestamp
ORDER BY
    u.snapshot_timestamp;
