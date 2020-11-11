SELECT
    w.*
FROM
    `${project}`.${dataset}.workspace w
WHERE
    w.snapshot_timestamp = (
        SELECT
            MAX(u.snapshot_timestamp)
        FROM
            `${project}`.${dataset}.user u)

ORDER BY
    w.workspace_id;
