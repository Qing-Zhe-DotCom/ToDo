ALTER TABLE schedules ADD COLUMN start_at TEXT;
ALTER TABLE schedules ADD COLUMN due_at TEXT;

UPDATE schedules
SET start_at = CASE
    WHEN start_at IS NOT NULL AND trim(start_at) <> '' THEN start_at
    WHEN start_date IS NOT NULL AND trim(start_date) <> '' THEN start_date || 'T00:00:00'
    ELSE NULL
END;

UPDATE schedules
SET due_at = CASE
    WHEN due_at IS NOT NULL AND trim(due_at) <> '' THEN due_at
    WHEN due_date IS NOT NULL AND trim(due_date) <> '' THEN due_date || 'T23:59:00'
    ELSE NULL
END;
