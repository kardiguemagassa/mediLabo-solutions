-- Author: Kardigué MAGASSA
-- MediLabo medilabo_notification - Database Schema
-- Date : January 5th 2026
-- Version: 1.0

-- General Rules ---
-- Use underscore_names instead of CamelCase --
-- Table names should be plural --
-- Spell out id fields (item_id instead of id) --
-- Don't use ambiguous column names --
-- Name foreign key columns the same as the columns they refer to --
-- Use caps for all SQL keywords --

BEGIN;

CREATE TABLE IF NOT EXISTS messages (
    message_id BIGSERIAL PRIMARY KEY,
    message_uuid VARCHAR(40) NOT NULL,
    conversation_id VARCHAR(40) NOT NULL,
    sender_uuid VARCHAR(40) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    sender_email VARCHAR(100) NOT NULL,
    sender_image_url VARCHAR(255),
    sender_role VARCHAR(20),
    receiver_uuid VARCHAR(40) NOT NULL,
    receiver_name VARCHAR(100) NOT NULL,
    receiver_email VARCHAR(100) NOT NULL,
    receiver_image_url VARCHAR(255),
    receiver_role VARCHAR(20),
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_messages_message_uuid UNIQUE (message_uuid)
);

CREATE TABLE IF NOT EXISTS message_statuses (
    message_status_id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_uuid VARCHAR(40) NOT NULL, 
    message_status VARCHAR(10) DEFAULT 'UNREAD',
    read_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT ck_message_statuses_status CHECK (message_status IN ('UNREAD', 'READ')),
    CONSTRAINT fk_message_statuses_message_id FOREIGN KEY (message_id) 
        REFERENCES messages (message_id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uq_message_statuses_message_user UNIQUE (message_id, user_uuid)
);

CREATE TABLE IF NOT EXISTS conversations (
    conversation_id BIGSERIAL PRIMARY KEY,
    conversation_uuid VARCHAR(40) NOT NULL,
    participant_1_uuid VARCHAR(40) NOT NULL,
    participant_1_name VARCHAR(100),
    participant_1_role VARCHAR(20),
    participant_2_uuid VARCHAR(40) NOT NULL,
    participant_2_name VARCHAR(100),
    participant_2_role VARCHAR(20),
    subject VARCHAR(255),
    last_message_at TIMESTAMP(6) WITH TIME ZONE,
    message_count INTEGER DEFAULT 0,
    created_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_conversations_uuid UNIQUE (conversation_uuid)
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender_uuid ON messages(sender_uuid);
CREATE INDEX IF NOT EXISTS idx_messages_receiver_uuid ON messages(receiver_uuid);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_message_statuses_user_uuid ON message_statuses(user_uuid);


-- FUNCTION: create_message
-- Crée un message avec toutes les infos dénormalisées et gère la création de la conversation et des statuts de message
CREATE OR REPLACE FUNCTION create_message(
    IN p_message_uuid VARCHAR(40),
    IN p_conversation_id VARCHAR(40),
    IN p_sender_uuid VARCHAR(40),
    IN p_sender_name VARCHAR(100),
    IN p_sender_email VARCHAR(100),
    IN p_sender_image_url VARCHAR(255),
    IN p_sender_role VARCHAR(20),
    IN p_receiver_uuid VARCHAR(40),
    IN p_receiver_name VARCHAR(100),
    IN p_receiver_email VARCHAR(100),
    IN p_receiver_image_url VARCHAR(255),
    IN p_receiver_role VARCHAR(20),
    IN p_subject VARCHAR(255),
    IN p_message TEXT
)
RETURNS TABLE (
    message_id BIGINT,
    message_uuid VARCHAR,
    conversation_id VARCHAR,
    sender_uuid VARCHAR,
    sender_name VARCHAR,
    sender_email VARCHAR,
    sender_image_url VARCHAR,
    sender_role VARCHAR,
    receiver_uuid VARCHAR,
    receiver_name VARCHAR,
    receiver_email VARCHAR,
    receiver_image_url VARCHAR,
    receiver_role VARCHAR,
    subject VARCHAR,
    message TEXT,
    status VARCHAR,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
)
LANGUAGE PLPGSQL
AS $$
DECLARE 
    v_message_id BIGINT;
BEGIN
    INSERT INTO messages (
        message_uuid, conversation_id,
        sender_uuid, sender_name, sender_email, sender_image_url, sender_role,
        receiver_uuid, receiver_name, receiver_email, receiver_image_url, receiver_role,
        subject, message
    )
    VALUES (
        p_message_uuid, p_conversation_id,
        p_sender_uuid, p_sender_name, p_sender_email, p_sender_image_url, p_sender_role,
        p_receiver_uuid, p_receiver_name, p_receiver_email, p_receiver_image_url, p_receiver_role,
        p_subject, p_message
    )
    RETURNING messages.message_id INTO v_message_id;
    
    -- Utilisation de ON CONFLICT pour éviter l'erreur si sender == receiver
    INSERT INTO message_statuses (message_id, user_uuid, message_status)
    VALUES (v_message_id, p_sender_uuid, 'READ') ON CONFLICT DO NOTHING;
    
    INSERT INTO message_statuses (message_id, user_uuid, message_status)
    VALUES (v_message_id, p_receiver_uuid, 'UNREAD') ON CONFLICT DO NOTHING;
    
    INSERT INTO conversations (
        conversation_uuid, participant_1_uuid, participant_1_name, participant_1_role,
        participant_2_uuid, participant_2_name, participant_2_role,
        subject, last_message_at, message_count
    )
    VALUES (p_conversation_id, p_sender_uuid, p_sender_name, p_sender_role,
            p_receiver_uuid, p_receiver_name, p_receiver_role, p_subject, NOW(), 1)
    ON CONFLICT (conversation_uuid) DO UPDATE SET
        last_message_at = NOW(),
        message_count = conversations.message_count + 1,
        updated_at = NOW();
    
    RETURN QUERY
    SELECT 
        messages.message_id, messages.message_uuid, messages.conversation_id,
        messages.sender_uuid, messages.sender_name, messages.sender_email, messages.sender_image_url, messages.sender_role,
        messages.receiver_uuid, messages.receiver_name, messages.receiver_email, messages.receiver_image_url, messages.receiver_role,
        messages.subject, messages.message, 'READ'::VARCHAR, messages.created_at, messages.updated_at
    FROM messages
    WHERE messages.message_id = v_message_id;
END;
$$;

-- 3. FUNCTION 2: mark_message_read
CREATE OR REPLACE FUNCTION mark_message_read(p_user_uuid VARCHAR(40), p_message_id BIGINT)
RETURNS VARCHAR LANGUAGE PLPGSQL AS $$
BEGIN
    UPDATE message_statuses SET message_status = 'READ', read_at = NOW()
    WHERE message_id = p_message_id AND user_uuid = p_user_uuid;
    RETURN 'READ';
END; $$;

-- 4. FUNCTION 3: get_unread_count
CREATE OR REPLACE FUNCTION get_unread_count(p_user_uuid VARCHAR(40))
RETURNS INTEGER LANGUAGE PLPGSQL AS $$
DECLARE v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM message_statuses
    WHERE user_uuid = p_user_uuid AND message_status = 'UNREAD';
    RETURN v_count;
END; $$;

COMMIT;