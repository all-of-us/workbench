-- all workspaces  in most recent snapshot. Useful for ad-hoc queries against curent state of the
-- workbench.
SELECT
    w.*
FROM
    reporting_test.workspace w
WHERE
        w.snapshot_timestamp = (
        SELECT
            MAX(vs2.snapshot_timestamp)
        FROM
            reporting_test.verified_snapshot vs2)
ORDER BY
    w.workspace_id;
