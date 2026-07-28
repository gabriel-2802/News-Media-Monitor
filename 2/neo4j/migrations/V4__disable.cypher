
MATCH (n:NewsSource)
SET n.failure_count = coalesce(n.failure_count, 0),
    n.is_disabled = coalesce(n.is_disabled, false)
