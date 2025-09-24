# Configuration Management System

## Overview

The ChitChat backend now uses a database-driven configuration system that stores all sensitive secrets and configuration values in PostgreSQL instead of hardcoding them in application files. This allows for:

- ✅ **Secure Secret Management**: No secrets in version control
- ✅ **Dynamic Configuration**: Update configs without code changes
- ✅ **Environment Flexibility**: Different configs for dev/staging/prod
- ✅ **Audit Trail**: Track configuration changes
- ✅ **Centralized Management**: Single source of truth for all configs

## Database Schema

### ApplicationConfig Table

```sql
CREATE TABLE application_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT NOT NULL,
    description TEXT,
    is_encrypted BOOLEAN DEFAULT FALSE,
    service VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Configuration Categories

| Service | Description | Examples |
|---------|-------------|----------|
| `SHARED` | Used by multiple services | JWT secret, Firebase config, Database password |
| `USER_SERVICE` | User service specific | Twilio credentials |
| `MEDIA_SERVICE` | Media service specific | AWS S3 credentials |
| `NOTIFICATION_SERVICE` | Notification service specific | Firebase messaging |
| `API_GATEWAY` | API Gateway specific | Rate limiting, routing config |

## Configuration Values

### JWT Configuration
- `jwt.secret`: JWT signing secret key (encrypted)
- `jwt.expiration`: Token expiration time in seconds

### Twilio Configuration
- `twilio.account.sid`: Twilio Account SID (encrypted)
- `twilio.auth.token`: Twilio Auth Token (encrypted)
- `twilio.phone.number`: Twilio phone number for SMS

### Firebase Configuration
- `firebase.project.id`: Firebase project ID
- `firebase.web.api.key`: Firebase Web API Key (encrypted)
- `firebase.auth.domain`: Firebase Auth Domain
- `firebase.storage.bucket`: Firebase Storage Bucket

### Database Configuration
- `database.password`: PostgreSQL database password (encrypted)

### AWS Configuration (for Media Service)
- `aws.access.key`: AWS Access Key (encrypted)
- `aws.secret.key`: AWS Secret Key (encrypted)
- `aws.s3.bucket`: S3 bucket name
- `aws.s3.region`: AWS region

## Usage in Code

### ConfigurationService Interface

```java
@Service
public class MyService {
    
    private final ConfigurationService configurationService;
    
    public MyService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
    
    public void doSomething() {
        // Get configuration values
        String jwtSecret = configurationService.getJwtSecret();
        String twilioPhone = configurationService.getTwilioPhoneNumber();
        
        // Get with default values
        Integer timeout = configurationService.getConfigValueAsInt("timeout", 5000);
        
        // Get all configs for a service
        Map<String, String> twilioConfigs = configurationService.getConfigurationsByService("USER_SERVICE");
    }
}
```

### Convenience Methods

The `ConfigurationService` provides convenience methods for common configurations:

```java
// JWT
String jwtSecret = configurationService.getJwtSecret();
Long jwtExpiration = configurationService.getJwtExpiration();

// Twilio
String accountSid = configurationService.getTwilioAccountSid();
String authToken = configurationService.getTwilioAuthToken();
String phoneNumber = configurationService.getTwilioPhoneNumber();

// Firebase
String projectId = configurationService.getFirebaseProjectId();
String webApiKey = configurationService.getFirebaseWebApiKey();
String authDomain = configurationService.getFirebaseAuthDomain();
String storageBucket = configurationService.getFirebaseStorageBucket();

// Database
String dbPassword = configurationService.getDatabasePassword();
```

## Management APIs

### Get Configuration Value
```bash
GET /api/admin/config/{configKey}
Authorization: Bearer {admin_token}
```

### Get Service Configurations
```bash
GET /api/admin/config/service/{service}
Authorization: Bearer {admin_token}
```

### Get Configurations by Pattern
```bash
GET /api/admin/config/pattern/{pattern}
Authorization: Bearer {admin_token}
```

### Set Configuration Value
```bash
POST /api/admin/config/{configKey}
Content-Type: application/x-www-form-urlencoded

configValue=value&description=description&isEncrypted=true&service=USER_SERVICE
Authorization: Bearer {admin_token}
```

### Delete Configuration
```bash
DELETE /api/admin/config/{configKey}
Authorization: Bearer {admin_token}
```

### Configuration Health Check
```bash
GET /api/admin/config/health
Authorization: Bearer {admin_token}
```

## Database Management

### Initialize Configuration
```bash
# Run the database initialization script
psql -h your-host -p 5432 -U your-user -d chitchat -f database/init-postgres.sql
```

### Update Configuration
```bash
# Run the configuration update script
psql -h your-host -p 5432 -U your-user -d chitchat -f update-config.sql
```

### Manual Configuration Updates
```sql
-- Update JWT secret
UPDATE application_config 
SET config_value = 'new-secret-key', updated_at = CURRENT_TIMESTAMP 
WHERE config_key = 'jwt.secret';

-- Add new configuration
INSERT INTO application_config (config_key, config_value, description, is_encrypted, service) 
VALUES ('new.config.key', 'value', 'Description', false, 'SHARED')
ON CONFLICT (config_key) DO UPDATE SET 
    config_value = EXCLUDED.config_value,
    updated_at = CURRENT_TIMESTAMP;

-- View all configurations
SELECT config_key, service, is_encrypted, updated_at 
FROM application_config 
ORDER BY service, config_key;
```

## Security Considerations

### Encryption
- Sensitive values are marked with `is_encrypted = true`
- In production, implement proper encryption/decryption
- Use environment variables for encryption keys

### Access Control
- Configuration APIs require admin authentication
- Use role-based access control (RBAC)
- Audit configuration changes

### Backup
- Regularly backup the `application_config` table
- Include configuration in disaster recovery plans
- Test configuration restore procedures

## Migration from Hardcoded Values

### Before (Hardcoded)
```yaml
# application.yml
jwt:
  secret: hardcoded-secret-key
twilio:
  account:
    sid: hardcoded-sid
  auth:
    token: hardcoded-token
```

### After (Database-driven)
```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET:fallback-secret-key}  # Fallback only
twilio:
  account:
    sid: ${TWILIO_ACCOUNT_SID:}  # Fallback only
  auth:
    token: ${TWILIO_AUTH_TOKEN:}  # Fallback only
```

### Code Changes
```java
// Before
@Value("${jwt.secret}")
private String jwtSecret;

// After
private final ConfigurationService configurationService;

public String getJwtSecret() {
    return configurationService.getJwtSecret();
}
```

## Testing

### Test Configuration Service
```bash
# Run the test script
./test-config.sh
```

### Unit Tests
```java
@Test
void testConfigurationService() {
    // Mock configuration service
    when(configurationService.getJwtSecret()).thenReturn("test-secret");
    
    // Test your service
    String secret = myService.getJwtSecret();
    assertEquals("test-secret", secret);
}
```

## Troubleshooting

### Common Issues

1. **Configuration not found**
   - Check if config exists in database
   - Verify service name matches
   - Check cache refresh

2. **Database connection issues**
   - Verify database credentials
   - Check network connectivity
   - Ensure table exists

3. **Permission errors**
   - Verify admin authentication
   - Check role permissions
   - Ensure proper authorization

### Debug Commands
```sql
-- Check if configuration exists
SELECT * FROM application_config WHERE config_key = 'jwt.secret';

-- Check service configurations
SELECT * FROM application_config WHERE service = 'USER_SERVICE';

-- Check recent updates
SELECT * FROM application_config ORDER BY updated_at DESC LIMIT 10;
```

## Best Practices

1. **Naming Convention**
   - Use dot notation: `service.category.key`
   - Be descriptive: `jwt.secret` not `secret`
   - Group by service: `twilio.account.sid`

2. **Security**
   - Mark sensitive values as encrypted
   - Use strong secrets
   - Rotate secrets regularly

3. **Documentation**
   - Document all configuration keys
   - Include descriptions
   - Specify service ownership

4. **Monitoring**
   - Monitor configuration changes
   - Alert on missing critical configs
   - Track configuration usage

## Future Enhancements

- [ ] Configuration versioning
- [ ] Environment-specific configs
- [ ] Configuration templates
- [ ] Automated secret rotation
- [ ] Configuration drift detection
- [ ] Integration with external secret managers (AWS Secrets Manager, HashiCorp Vault)

---

*Last updated: September 24, 2025*
