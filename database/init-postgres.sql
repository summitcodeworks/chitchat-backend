-- ChitChat Backend PostgreSQL Database Initialization Script
-- Database: chitchat
-- Username: summitcodeworks
-- Password: 8ivhaah8

-- Note: Database is already created by Docker environment variables
-- We just need to create tables and data

-- Use the chitchat database
\c chitchat;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    about TEXT,
    last_seen TIMESTAMP,
    is_online BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    firebase_uid VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create blocks table
CREATE TABLE IF NOT EXISTS blocks (
    id BIGSERIAL PRIMARY KEY,
    blocker_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(blocker_id, blocked_id)
);

-- Create media_files table
CREATE TABLE IF NOT EXISTS media_files (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    s3_key VARCHAR(500) NOT NULL,
    s3_bucket VARCHAR(255) NOT NULL,
    s3_url TEXT NOT NULL,
    thumbnail_url TEXT,
    thumbnail_s3_key VARCHAR(500),
    media_type VARCHAR(20) NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT', 'VOICE_NOTE')),
    uploaded_by BIGINT NOT NULL,
    description TEXT,
    width INTEGER,
    height INTEGER,
    duration BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING' CHECK (status IN ('UPLOADING', 'PROCESSING', 'COMPLETED', 'FAILED', 'DELETED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create calls table
CREATE TABLE IF NOT EXISTS calls (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    caller_id BIGINT NOT NULL,
    callee_id BIGINT NOT NULL,
    call_type VARCHAR(10) NOT NULL CHECK (call_type IN ('VOICE', 'VIDEO')),
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED' CHECK (status IN ('INITIATED', 'RINGING', 'ANSWERED', 'REJECTED', 'ENDED', 'FAILED', 'MISSED')),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    duration BIGINT,
    caller_sdp TEXT,
    callee_sdp TEXT,
    ice_candidates TEXT,
    rejection_reason TEXT,
    end_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('MESSAGE', 'CALL', 'STATUS_UPDATE', 'FRIEND_REQUEST', 'GROUP_INVITE', 'SYSTEM')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED')),
    data TEXT,
    image_url TEXT,
    action_url TEXT,
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create device_tokens table
CREATE TABLE IF NOT EXISTS device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token TEXT NOT NULL,
    device_type VARCHAR(10) NOT NULL CHECK (device_type IN ('ANDROID', 'IOS', 'WEB')),
    device_id VARCHAR(255) NOT NULL,
    app_version VARCHAR(20),
    os_version VARCHAR(20),
    device_model VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, device_id)
);

-- Create admin_users table
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'ANALYST')),
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create user_actions table
CREATE TABLE IF NOT EXISTS user_actions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    details TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'PENDING')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create application_config table for storing secrets and configuration
CREATE TABLE IF NOT EXISTS application_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    description TEXT,
    is_encrypted BOOLEAN DEFAULT FALSE,
    service VARCHAR(50) NOT NULL, -- Which service this config belongs to
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create otp_requests table for comprehensive OTP tracking
CREATE TABLE IF NOT EXISTS otp_requests (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    request_ip VARCHAR(45),
    user_agent VARCHAR(500),
    request_headers TEXT,
    request_payload TEXT,
    response_status VARCHAR(20),
    response_message VARCHAR(500),
    response_payload TEXT,
    sms_sent BOOLEAN DEFAULT FALSE,
    sms_error_message VARCHAR(1000),
    twilio_message_sid VARCHAR(100),
    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    verification_attempts INTEGER DEFAULT 0,
    last_verification_attempt TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_phone_number ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(firebase_uid);
CREATE INDEX IF NOT EXISTS idx_users_is_online ON users(is_online);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);

CREATE INDEX IF NOT EXISTS idx_blocks_blocker_id ON blocks(blocker_id);
CREATE INDEX IF NOT EXISTS idx_blocks_blocked_id ON blocks(blocked_id);

CREATE INDEX IF NOT EXISTS idx_media_files_uploaded_by ON media_files(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_media_files_media_type ON media_files(media_type);
CREATE INDEX IF NOT EXISTS idx_media_files_status ON media_files(status);

CREATE INDEX IF NOT EXISTS idx_calls_caller_id ON calls(caller_id);
CREATE INDEX IF NOT EXISTS idx_calls_callee_id ON calls(callee_id);
CREATE INDEX IF NOT EXISTS idx_calls_session_id ON calls(session_id);
CREATE INDEX IF NOT EXISTS idx_calls_status ON calls(status);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);

CREATE INDEX IF NOT EXISTS idx_device_tokens_user_id ON device_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_device_tokens_is_active ON device_tokens(is_active);

CREATE INDEX IF NOT EXISTS idx_admin_users_username ON admin_users(username);
CREATE INDEX IF NOT EXISTS idx_admin_users_email ON admin_users(email);

CREATE INDEX IF NOT EXISTS idx_user_actions_user_id ON user_actions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_actions_action ON user_actions(action);
CREATE INDEX IF NOT EXISTS idx_user_actions_created_at ON user_actions(created_at);

CREATE INDEX IF NOT EXISTS idx_application_config_key ON application_config(config_key);
CREATE INDEX IF NOT EXISTS idx_application_config_service ON application_config(service);

CREATE INDEX IF NOT EXISTS idx_otp_requests_phone_number ON otp_requests(phone_number);
CREATE INDEX IF NOT EXISTS idx_otp_requests_otp_code ON otp_requests(otp_code);
CREATE INDEX IF NOT EXISTS idx_otp_requests_expires_at ON otp_requests(expires_at);
CREATE INDEX IF NOT EXISTS idx_otp_requests_is_verified ON otp_requests(is_verified);
CREATE INDEX IF NOT EXISTS idx_otp_requests_created_at ON otp_requests(created_at);
CREATE INDEX IF NOT EXISTS idx_otp_requests_phone_otp ON otp_requests(phone_number, otp_code);
CREATE INDEX IF NOT EXISTS idx_otp_requests_phone_created ON otp_requests(phone_number, created_at);

-- Insert default admin user (password: admin123 - should be changed in production)
INSERT INTO admin_users (username, email, password, role, is_active) 
VALUES ('admin', 'admin@chitchat.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'SUPER_ADMIN', true)
ON CONFLICT (username) DO NOTHING;

-- Insert JWT configuration
INSERT INTO application_config (config_key, config_value, description, is_encrypted, service) 
VALUES ('jwt.secret', '3703ea2161827e52832c162790d1a511e2a7aa22120c34f6d620e703d876697b', 'JWT secret key for token signing', false, 'SHARED')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO application_config (config_key, config_value, description, is_encrypted, service) 
VALUES ('jwt.expiration', '3600', 'JWT token expiration time in seconds', false, 'SHARED')
ON CONFLICT (config_key) DO NOTHING;

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_media_files_updated_at BEFORE UPDATE ON media_files FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_calls_updated_at BEFORE UPDATE ON calls FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_notifications_updated_at BEFORE UPDATE ON notifications FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_device_tokens_updated_at BEFORE UPDATE ON device_tokens FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
-- JWT Tokens table
CREATE TABLE jwt_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1000) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_jwt_tokens_token ON jwt_tokens(token);
CREATE INDEX idx_jwt_tokens_user_id ON jwt_tokens(user_id);
CREATE INDEX idx_jwt_tokens_phone_number ON jwt_tokens(phone_number);
CREATE INDEX idx_jwt_tokens_expires_at ON jwt_tokens(expires_at);
CREATE INDEX idx_jwt_tokens_is_revoked ON jwt_tokens(is_revoked);

-- Add foreign key constraint
ALTER TABLE jwt_tokens ADD CONSTRAINT fk_jwt_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE TRIGGER update_admin_users_updated_at BEFORE UPDATE ON admin_users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_application_config_updated_at BEFORE UPDATE ON application_config FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_otp_requests_updated_at BEFORE UPDATE ON otp_requests FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_jwt_tokens_updated_at BEFORE UPDATE ON jwt_tokens FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Grant permissions to summitcodeworks user
GRANT ALL PRIVILEGES ON DATABASE chitchat TO summitcodeworks;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO summitcodeworks;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO summitcodeworks;

COMMIT;
