CREATE TABLE security_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    user_id UUID REFERENCES users(id),
    email VARCHAR(254),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX security_events_created_at_idx ON security_events (created_at);
CREATE INDEX security_events_user_id_idx ON security_events (user_id);
