-- Add scheduling and locking columns to news_sources table for independent worker processing
-- This allows workers to claim and process news sources without a separate job queue

ALTER TABLE news_sources
ADD COLUMN IF NOT EXISTS last_fetched_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS fetched_this_cycle BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS locked_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS consecutive_failures INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS last_error VARCHAR(2000),
ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE;

-- Index for efficient claiming of next available source
CREATE INDEX IF NOT EXISTS idx_news_sources_fetched_cycle
ON news_sources (fetched_this_cycle, last_fetched_at NULLS FIRST, id)
WHERE enabled = true;

-- Index for lock expiry cleanup
CREATE INDEX IF NOT EXISTS idx_news_sources_locked_at
ON news_sources (locked_at)
WHERE locked_by IS NOT NULL;

-- Table to coordinate fetch cycles and clustering trigger across workers
CREATE TABLE IF NOT EXISTS fetch_cycle_state (
    id BIGINT PRIMARY KEY DEFAULT 1,
    cycle_id BIGINT DEFAULT 0,
    clustering_triggered BOOLEAN DEFAULT FALSE,
    clustering_triggered_by VARCHAR(255),
    clustering_triggered_at TIMESTAMP,
    last_cycle_started_at TIMESTAMP,
    CONSTRAINT single_row CHECK (id = 1)
);

-- Insert the single coordination row
INSERT INTO fetch_cycle_state (id, cycle_id, clustering_triggered)
VALUES (1, 0, false)
ON CONFLICT (id) DO NOTHING;
