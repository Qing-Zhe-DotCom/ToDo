CREATE TABLE IF NOT EXISTS schedules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    start_date TEXT,
    due_date TEXT,
    completed INTEGER NOT NULL DEFAULT 0,
    priority TEXT DEFAULT '中',
    category TEXT DEFAULT '默认',
    tags TEXT,
    reminder_time TEXT,
    color TEXT DEFAULT '#2196F3',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
