-- Update device_tokens table to allow null user_id for anonymous device registration
-- This allows devices to register before user authentication

ALTER TABLE device_tokens ALTER COLUMN user_id DROP NOT NULL;

-- Add a comment to document the change
COMMENT ON COLUMN device_tokens.user_id IS 'User ID - can be null for anonymous device registration before user login';
