CREATE TABLE notifications (
    id         VARCHAR(36)   PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message    VARCHAR(1000) NOT NULL,
    seen       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_id_seen ON notifications (user_id, seen);
