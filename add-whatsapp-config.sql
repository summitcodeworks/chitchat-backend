-- Add WhatsApp configuration to app_config table
-- This script adds the Twilio WhatsApp number configuration

-- Insert WhatsApp sender number configuration
INSERT INTO app_config (config_key, config_value, description, is_encrypted, created_at, updated_at)
VALUES 
('twilio.whatsapp.number', '+918929607491', 'Twilio WhatsApp Business API sender number', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (config_key) 
DO UPDATE SET 
    config_value = EXCLUDED.config_value,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- Verify the configuration
SELECT * FROM app_config WHERE config_key = 'twilio.whatsapp.number';
