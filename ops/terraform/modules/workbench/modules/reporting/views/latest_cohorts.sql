-- All cohorts from the most recent snapshot
SELECT
    c.*
FROM
    `${project}`.${dataset}.cohort c
WHERE
        c.snapshot_timestamp = (
        SELECT
            MAX(u.snapshot_timestamp)
        FROM
            `${project}`.${dataset}.user u)
ORDER BY
    c.cohort_id;
