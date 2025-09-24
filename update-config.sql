-- Script to update application configuration in database
-- Run this script to populate the application_config table with secrets

-- Connect to the chitchat database
\c chitchat;

-- Insert or update JWT configuration
INSERT INTO application_config (config_key, config_value, description, is_encrypted, service) VALUES
('jwt.secret', 'chitchat-secret-key-for-jwt-token-generation-2024', 'JWT signing secret key', true, 'SHARED'),
('jwt.expiration', '3600', 'JWT token expiration time in seconds', false, 'SHARED')

ON CONFLICT (config_key) DO UPDATE SET
    config_value = EXCLUDED.config_value,
    description = EXCLUDED.description,
    is_encrypted = EXCLUDED.is_encrypted,
    service = EXCLUDED.service,
    updated_at = CURRENT_TIMESTAMP;

-- Insert or update Twilio configuration
INSERT INTO application_config (config_key, config_value, description, is_encrypted, service) VALUES
('twilio.account.sid', 'AC2ab597ad50aff6005c95c4024370b5c2', 'Twilio Account SID', true, 'USER_SERVICE'),
('twilio.auth.token', '8869105b7563b5978ae1dd5ac843a473', 'Twilio Auth Token', true, 'USER_SERVICE'),
('twilio.phone.number', '+18587805063', 'Twilio phone number for sending SMS', false, 'USER_SERVICE')

ON CONFLICT (config_key) DO UPDATE SET
    config_value = EXCLUDED.config_value,
    description = EXCLUDED.description,
    is_encrypted = EXCLUDED.is_encrypted,
    service = EXCLUDED.service,
    updated_at = CURRENT_TIMESTAMP;

-- Insert or update Firebase configuration
INSERT INTO application_config (config_key, config_value, description, is_encrypted, service) VALUES
('firebase.project.id', 'chitchat-9c074', 'Firebase project ID', false, 'SHARED'),
('firebase.web.api.key', 'AIzaSyBefqzOkJgvV0qnmc4Qds43Gi5XvdmAl7g', 'Firebase Web API Key', true, 'SHARED'),
('firebase.auth.domain', 'chitchat-9c074.firebaseapp.com', 'Firebase Auth Domain', false, 'SHARED'),
('firebase.storage.bucket', 'chitchat-9c074.firebasestorage.app', 'Firebase Storage Bucket', false, 'SHARED')

ON CONFLICT (config_key) DO UPDATE SET
    config_value = EXCLUDED.config_value,
    description = EXCLUDED.description,
    is_encrypted = EXCLUDED.is_encrypted,
    service = EXCLUDED.service,
    updated_at = CURRENT_TIMESTAMP;

-- Insert or update Database configuration
INSERT INTO application_config (config_key, config_value, description, is_encrypted, service) VALUES
('database.password', '8ivhaah8', 'PostgreSQL database password', true, 'SHARED')

ON CONFLICT (config_key) DO UPDATE SET
    config_value = EXCLUDED.config_value,
    description = EXCLUDED.description,
    is_encrypted = EXCLUDED.is_encrypted,
    service = EXCLUDED.service,
    updated_at = CURRENT_TIMESTAMP;

-- Verify the configuration was inserted
SELECT config_key, config_value, description, is_encrypted, service, updated_at 
FROM application_config 
ORDER BY service, config_key;

COMMIT;
