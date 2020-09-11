CREATE MATERIALIZED VIEW reporting_local.researcher_stats AS
SELECT TIMESTAMP_MILLIS(snapshot_timestamp) AS snapshot_date, COUNT(researcher_id) AS researcher_count
FROM reporting_local.researcher
GROUP BY snapshot_date;
