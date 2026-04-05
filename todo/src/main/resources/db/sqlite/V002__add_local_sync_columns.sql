ALTER TABLE schedules ADD COLUMN deleted_at TEXT;
ALTER TABLE schedules ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE schedules ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'local_only';
ALTER TABLE schedules ADD COLUMN last_synced_at TEXT;
ALTER TABLE schedules ADD COLUMN device_id TEXT;
ALTER TABLE schedules ADD COLUMN metadata_json TEXT;
