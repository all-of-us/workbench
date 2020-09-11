-- Fetch all workspaces from this environment that aren't  deleted and that have active user creators

select w.workspace_id, w.name, FLOOR(1000 * RAND()) AS size, w.creator_id, w.creation_time
FROM workbench.workspace AS w
INNER JOIN user AS u ON w.creator_id = u.user_id
WHERE w.active_status = 0 AND
      u.disabled  = false;
