-- Create database (run this separately if needed)
-- CREATE DATABASE chatapp WITH ENCODING 'UTF8';

-- Connect to the database
-- \c chatapp

-- Create users table
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     username VARCHAR(100) NOT NULL UNIQUE,
    connection_mode VARCHAR(20) NOT NULL,
    online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMP WITH TIME ZONE
                            );

-- Create messages table
CREATE TABLE IF NOT EXISTS messages (
                                        id SERIAL PRIMARY KEY,
                                        sender_id INTEGER NOT NULL REFERENCES users(id),
    receiver_id INTEGER REFERENCES users(id),
    content TEXT NOT NULL,
    type VARCHAR(10) NOT NULL,
    broadcast BOOLEAN NOT NULL DEFAULT FALSE,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                            );

-- Create file_attachments table
CREATE TABLE IF NOT EXISTS file_attachments (
                                                id SERIAL PRIMARY KEY,
                                                message_id INTEGER NOT NULL REFERENCES messages(id),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    upload_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expiry_time TIMESTAMP WITH TIME ZONE NOT NULL,
                              downloaded BOOLEAN DEFAULT FALSE
                              );

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_broadcast ON messages(broadcast);
CREATE INDEX IF NOT EXISTS idx_file_expiry ON file_attachments(expiry_time);

-- Create a view for active public users
CREATE OR REPLACE VIEW active_public_users AS
SELECT * FROM users
WHERE online = TRUE AND connection_mode = 'PUBLIC';

-- Create a view for pending files
CREATE OR REPLACE VIEW pending_files AS
SELECT
    f.id,
    f.original_filename,
    f.file_size,
    f.upload_time,
    f.expiry_time,
    s.username AS sender,
    r.username AS receiver,
    f.downloaded
FROM
    file_attachments f
        JOIN messages m ON f.message_id = m.id
        JOIN users s ON m.sender_id = s.id
        JOIN users r ON m.receiver_id = r.id
WHERE
    f.downloaded = FALSE
  AND f.expiry_time > CURRENT_TIMESTAMP;