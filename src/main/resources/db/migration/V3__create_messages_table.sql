CREATE TABLE messages (
    id              UUID PRIMARY KEY,
    room_id         UUID NOT NULL REFERENCES rooms (id),
    sender_id       UUID NOT NULL REFERENCES users (id),
    sender_username VARCHAR(32) NOT NULL,
    content         VARCHAR(2000) NOT NULL,
    sent_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_room_sent_at ON messages (room_id, sent_at DESC);
