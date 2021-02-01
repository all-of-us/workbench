-- All cohorts from the most recent snapshot
SELECT
    c.*
FROM
    reporting_test.cohort c
WHERE
        c.snapshot_timestamp = (
        SELECT
            MAX(vs2.snapshot_timestamp)
        FROM
            reporting_test.verified_snapshot vs2)
ORDER BY
    c.cohort_id;
