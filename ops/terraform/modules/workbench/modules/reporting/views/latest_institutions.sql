-- All institutions from the most recent snapshot. Some may not  have
-- users associated with them.
SELECT
    i.*
FROM
    `${project}`.${dataset}.institution i
WHERE
        i.snapshot_timestamp = (
        SELECT
            MAX(u.snapshot_timestamp)
        FROM
            `${project}`.${dataset}.user u)
ORDER BY
    i.institution_id;
