-- ToDo应用数据库更新脚本
-- 更新schedules表以支持更多功能

-- 如果表不存在则创建新表
CREATE TABLE IF NOT EXISTS schedules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE,
    due_date DATE,
    completed BOOLEAN DEFAULT FALSE,
    priority VARCHAR(10) DEFAULT '中',
    category VARCHAR(50) DEFAULT '默认',
    tags VARCHAR(255),
    reminder_time DATETIME,
    color VARCHAR(20) DEFAULT '#2196F3',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 如果表已存在，添加新列
ALTER TABLE schedules 
ADD COLUMN IF NOT EXISTS start_date DATE AFTER description,
ADD COLUMN IF NOT EXISTS priority VARCHAR(10) DEFAULT '中' AFTER completed,
ADD COLUMN IF NOT EXISTS category VARCHAR(50) DEFAULT '默认' AFTER priority,
ADD COLUMN IF NOT EXISTS tags VARCHAR(255) AFTER category,
ADD COLUMN IF NOT EXISTS reminder_time DATETIME AFTER tags,
ADD COLUMN IF NOT EXISTS color VARCHAR(20) DEFAULT '#2196F3' AFTER reminder_time;

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_due_date ON schedules(due_date);
CREATE INDEX IF NOT EXISTS idx_completed ON schedules(completed);
CREATE INDEX IF NOT EXISTS idx_priority ON schedules(priority);
CREATE INDEX IF NOT EXISTS idx_category ON schedules(category);

-- 创建每日完成统计视图
CREATE OR REPLACE VIEW daily_completion_stats AS
SELECT 
    DATE(updated_at) as completion_date,
    COUNT(*) as completed_count
FROM schedules 
WHERE completed = TRUE
GROUP BY DATE(updated_at)
ORDER BY completion_date;
