# QuizVerse Authentication API - Refresh Token Endpoints

## Summary
Refresh tokens enable users to obtain new access tokens without re-entering credentials. This document provides the complete API contract.

---

## Endpoints

### 1. Register User
**Endpoint**: `POST /api/auth/register`

**Request**:
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePassword123"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTY4NDMyMTIwMCwiZXhwIjoxNjg0MzI0ODAwfQ.signature",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer"
}
```

**Error** (400 Bad Request):
```json
{
  "error": "Username is already taken"
}
```

---

### 2. Login User
**Endpoint**: `POST /api/auth/login`

**Request**:
```json
{
  "username": "john_doe",
  "password": "SecurePassword123"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTY4NDMyMTIwMCwiZXhwIjoxNjg0MzI0ODAwfQ.signature",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer"
}
```

**Error** (401 Unauthorized):
```json
{
  "error": "Invalid credentials"
}
```

---

### 3. Refresh Access Token ⭐ NEW
**Endpoint**: `POST /api/auth/refresh`

**Purpose**: Obtain a new access token using a refresh token (when access token expires).

**Request**:
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTY4NDMyMTIwMCwiZXhwIjoxNjg0MzI0ODAwfQ.signature",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer"
}
```

**Error** (401 Unauthorized):
```json
{
  "error": "Refresh token is invalid or expired"
}
```

**When to use**: Client receives 401 on any protected endpoint, indicating access token expired. Use this endpoint to get a new one without user re-logging in.

---

### 4. Logout User ⭐ NEW
**Endpoint**: `POST /api/auth/logout`

**Purpose**: Invalidate the refresh token and clear session (logout).

**Request**:
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response** (200 OK):
```json
{
  "message": "User logged out successfully"
}
```

---

## Client-Side Implementation Example

### JavaScript/React
```javascript
// Step 1: Register or Login
const authResponse = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'john_doe', password: 'SecurePassword123' })
});

const { accessToken, refreshToken, tokenType } = await authResponse.json();

// Store tokens
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// Step 2: Make API request with access token
const response = await fetch('/api/quiz/all', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

// Step 3: Handle token expiration
if (response.status === 401) {
  // Access token expired, refresh it
  const refreshResponse = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: localStorage.getItem('refreshToken') })
  });

  if (refreshResponse.ok) {
    const { accessToken: newAccessToken } = await refreshResponse.json();
    localStorage.setItem('accessToken', newAccessToken);

    // Retry original request with new token
    const retryResponse = await fetch('/api/quiz/all', {
      headers: {
        'Authorization': `Bearer ${newAccessToken}`
      }
    });
    // ... handle response
  } else {
    // Refresh token invalid, redirect to login
    window.location.href = '/login';
  }
}

// Step 4: Logout
await fetch('/api/auth/logout', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ refreshToken: localStorage.getItem('refreshToken') })
});

localStorage.removeItem('accessToken');
localStorage.removeItem('refreshToken');
```

---

## Security Guidelines

### For Developers
1. **Store refresh tokens securely**: Use HttpOnly cookies or secure storage
2. **Never expose refresh tokens in logs**: Keep them confidential
3. **Use HTTPS**: Always in production
4. **Set appropriate expiration times**:
   - Access token: Short-lived (1 hour recommended)
   - Refresh token: Long-lived (7-30 days recommended)

### For DevOps
1. Keep `jwt.secret` in environment variables or secrets manager
2. Enable CORS properly: Only allow trusted frontend origins
3. Monitor refresh token usage for anomalies
4. Set up periodic token cleanup jobs

---

## Token Characteristics

| Property | Access Token | Refresh Token |
|----------|--------------|---------------|
| Type | JWT | UUID string |
| Lifetime | 1 hour (configurable) | 24 hours (configurable) |
| Usage | API authentication | Token renewal only |
| Storage | Memory/Session | Secure/HttpOnly Cookie |
| Revocation | Instant (checked in DB) | Immediate (soft-delete) |
| Reusable | No (new one on refresh) | Yes (until revoked/expired) |

---

## Configuration

In `src/main/resources/application.properties`:
```properties
# JWT Configuration
jwt.secret=YourVerySecureRandomStringHereShouldBe32BytesOrMore
jwt.expiration-ms=3600000          # 1 hour (in milliseconds)
jwt.refresh-expiration-ms=86400000 # 24 hours (in milliseconds)
```

**Important**: Change `jwt.secret` to a strong, random value before deploying to production!

---

## Flow Diagrams

### Authentication Flow
```
User                    Server              Database
 |                       |                    |
 |--- Register/Login --->|                    |
 |                       |--- Create User --->|
 |                       |<--- User ID -------|
 |                       |--- Create Tokens --|
 |<-- accessToken -------|                    |
 |<-- refreshToken ------|                    |
```

### Refresh Token Flow
```
User                    Server              Database
 |                       |                    |
 |--- API Request ------>|                    |
 |   (with accessToken)  |--- Validate JWT --|
 |                       |<--- Valid --------|
 |<--- 200 OK ----------|                    |
 |   (Response)          |                    |

 [Later: Access token expired]

 |--- API Request ------>|                    |
 |   (with accessToken)  |--- Validate JWT --|
 |                       |<--- Expired ------|
 |<--- 401 Unauthorized-|                    |
 |                       |                    |
 |--- Refresh Token ---->|                    |
 |                       |--- Verify Token --|
 |                       |<--- Valid --------|
 |                       |--- Generate New --|
 |<-- accessToken -------|                    |
 |                       |                    |
 |--- Retry API -------->|                    |
 |   (with new token)    |--- Validate JWT --|
 |                       |<--- Valid --------|
 |<--- 200 OK ----------|                    |
```

---

## Troubleshooting

### "Refresh token is invalid or expired"
- Token has expired (> 24 hours old)
- Token has been revoked (user logged out)
- Token doesn't exist in database
- **Solution**: User must log in again

### Access token doesn't work after refresh
- Make sure to update the access token in client storage
- Verify Bearer token format: `Authorization: Bearer <token>`
- Check token expiration hasn't been reached immediately

### Can't logout
- Ensure refresh token is being sent in request body
- Check that token exists in database (not already revoked)
- Verify database connectivity

---

## Next Steps
- Implement token rotation on each refresh (optional enhancement)
- Add Redis caching for faster token validation
- Set up Scheduled task for cleanup of old revoked tokens
- Add audit logging for token operations

