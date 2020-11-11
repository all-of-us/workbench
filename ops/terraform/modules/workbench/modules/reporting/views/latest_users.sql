SELECT
    u.*
FROM
    `${project}`.${dataset}.user u
WHERE
        u.snapshot_timestamp = (
        SELECT
            MAX(u2.snapshot_timestamp)
        FROM
            `${project}`.${dataset}.user u2)
ORDER BY
    u.username;
