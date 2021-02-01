-- All users from the most recent snapshot
SELECT
    u.*
FROM
    reporting_test.user u
WHERE
        u.snapshot_timestamp = (
        SELECT
            MAX(vs2.snapshot_timestamp)
        FROM
            reporting_test.verified_snapshot vs2)
ORDER BY
    u.username;
