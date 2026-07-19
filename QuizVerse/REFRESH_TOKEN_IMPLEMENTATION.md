# Refresh Token Implementation Guide

## Overview
Refresh tokens have been fully implemented to allow users to obtain new access tokens without re-logging in. The system uses database-persisted refresh tokens with expiration and revocation support.

## What Was Implemented

### 1. **RefreshToken Entity** (`model/RefreshToken.java`)
- Stores refresh tokens in the database with the following fields:
  - `id`: Primary key
  - `user`: Many-to-one relationship with User
  - `token`: Unique token string (UUID-based)
  - `expiryDate`: Expiration timestamp (Instant)
  - `revoked`: Boolean flag for soft deletion (logout)

### 2. **RefreshTokenRepository** (`repository/RefreshTokenRepository.java`)
- Custom JPA repository with methods:
  - `findByToken(String token)`: Find token by value
  - `findByUserAndRevokedFalse(User user)`: Get active token for user
  - `deleteByUser(User user)`: Cleanup for user deletion
  - `deleteByUserAndRevokedTrue(User user)`: Cleanup revoked tokens

### 3. **RefreshTokenService** (`service/RefreshTokenService.java`)
Business logic for token management:
- `createRefreshToken(username)`: Creates new token, revokes old ones (keeps only one active per user)
- `verifyRefreshToken(token)`: Validates token (checks signature, expiration, revocation status)
- `revokeRefreshToken(token)`: Marks token as revoked (soft delete for logout)
- `revokeAllRefreshTokens(username)`: Logout from all devices
- `deleteExpiredTokens()`: Cleanup scheduled task (optional)

### 4. **RefreshTokenRequest DTO** (`dto/RefreshTokenRequest.java`)
Request payload for refresh operations:
```json
{
  "refreshToken": "uuid-string"
}
```

### 5. **Updated AuthResponse DTO** (`dto/AuthResponse.java`)
Response now includes:
```json
{
  "accessToken": "jwt-token",
  "refreshToken": "uuid-string",
  "tokenType": "Bearer"
}
```

### 6. **New AuthController Endpoints** (`controller/AuthController.java`)

#### POST `/api/auth/register`
- Now returns both `accessToken` and `refreshToken`
- Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer"
}
```

#### POST `/api/auth/login`
- Now returns both `accessToken` and `refreshToken`
- Same response format as register

#### POST `/api/auth/refresh`
- **Request**: `RefreshTokenRequest` with refresh token
- **Response**: New `accessToken` with same `refreshToken`
- Returns 401 if token is invalid/expired/revoked
```json
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### POST `/api/auth/logout`
- Revokes the refresh token
- Clears security context
- Request:
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

## Database Schema
The `refresh_tokens` table is automatically created:
```sql
CREATE TABLE refresh_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token VARCHAR(500) NOT NULL UNIQUE,
  expiry_date DATETIME(6) NOT NULL,
  revoked BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Configuration
In `application.properties`:
```properties
# Refresh token expiration (86400000 ms = 24 hours)
jwt.refresh-expiration-ms=86400000
```

## Security Features
1. **One Active Token Per User**: Old tokens are automatically revoked when new ones are created
2. **Expiration Validation**: Tokens older than `jwt.refresh-expiration-ms` are rejected
3. **Revocation Support**: Logout invalidates tokens without database deletion (audit trail)
4. **UUID-based Tokens**: Cryptographically random refresh tokens (not JWTs)
5. **Validation**: Tokens are validated for existence, expiration, and revocation status

## Usage Workflow

### Initial Login/Register
```
1. User → POST /api/auth/login
2. Server returns { accessToken, refreshToken }
3. Client stores both tokens (access token in memory, refresh token in secure storage)
```

### API Requests with Access Token
```
1. Client → GET /api/quiz with Authorization: Bearer <accessToken>
2. JwtAuthenticationFilter validates JWT
3. If valid, request proceeds
```

### Refresh Access Token
```
1. Client detects access token expiration (401 from API)
2. Client → POST /api/auth/refresh with { refreshToken }
3. Server validates refresh token and returns new { accessToken, refreshToken }
4. Client updates access token and retries API request
```

### Logout
```
1. Client → POST /api/auth/logout with { refreshToken }
2. Server revokes refresh token
3. Client deletes both tokens locally
```

## Testing Endpoints

### Register and get tokens
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "email": "alice@example.com",
    "password": "SecurePass123"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "SecurePass123"
  }'
```

### Refresh token
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

### Logout
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

## Integration Points
- `JwtAuthenticationFilter` remains unchanged (still validates access tokens)
- `SecurityConfig` remains unchanged (refresh endpoints are public)
- Existing `/api/auth/register` and `/api/auth/login` now return refresh tokens
- All protected endpoints work with access tokens (unchanged)

## Future Enhancements
1. **Token Rotation**: Implement automatic refresh token rotation on each use
2. **Device Tracking**: Store device info with refresh tokens for "logout from all devices"
3. **Token Blacklist**: Redis-based blacklist for instant token revocation
4. **Audit Log**: Track refresh token usage for security monitoring
5. **Sliding Window**: Extend expiration on each refresh (optional)
6. **Scheduled Cleanup**: Periodic deletion of revoked/expired tokens

## Files Added/Modified
- ✅ Created: `model/RefreshToken.java`
- ✅ Created: `repository/RefreshTokenRepository.java`
- ✅ Created: `service/RefreshTokenService.java`
- ✅ Created: `dto/RefreshTokenRequest.java`
- ✅ Modified: `dto/AuthResponse.java` (added refreshToken field)
- ✅ Modified: `controller/AuthController.java` (added refresh/logout endpoints, updated register/login)

## Build & Run
From project root with JDK installed:
```powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```

## Notes
- The environment used for this implementation runs JRE only (no JDK), so compilation was blocked. Run locally with JDK 21+ to verify compilation.
- All code follows Spring Boot best practices and the project's layered architecture.
- Refresh tokens are stateless (but database-persisted unlike JWTs) and can be invalidated on demand.

