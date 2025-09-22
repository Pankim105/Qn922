# API 测试文档

## 环境准备

1. 启动后端服务：`mvn spring-boot:run`
2. 服务地址：`http://localhost:8080/api`
3. 使用Postman、curl或其他API测试工具

## 测试用例

### 1. 用户注册

**请求：**
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "password123"
}
```

**预期响应：**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": 1,
    "username": "newuser",
    "email": "newuser@example.com",
    "role": "USER"
  }
}
```

### 2. 用户登录

**请求：**
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**预期响应：**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440001",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@qncontest.com",
    "role": "ADMIN"
  }
}
```

### 3. 刷新令牌

**请求：**
```http
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440001"
}
```

**预期响应：**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440001",
  "tokenType": "Bearer",
  "expiresIn": 900000,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@qncontest.com",
    "role": "ADMIN"
  }
}
```

### 4. 用户登出

**请求：**
```http
POST http://localhost:8080/api/auth/logout
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440001"
}
```

**预期响应：**
```json
{
  "message": "登出成功"
}
```

### 5. 测试公开端点

**请求：**
```http
GET http://localhost:8080/api/test/public
```

**预期响应：**
```json
{
  "message": "这是一个公开的端点"
}
```

### 6. 测试用户端点（需要认证）

**请求：**
```http
GET http://localhost:8080/api/test/user
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**预期响应：**
```json
{
  "message": "这是用户端点"
}
```

### 7. 测试管理员端点（需要管理员权限）

**请求：**
```http
GET http://localhost:8080/api/test/admin
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**预期响应：**
```json
{
  "message": "这是管理员端点"
}
```

## 错误测试用例

### 1. 重复用户名注册

**请求：**
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "admin",
  "email": "test@example.com",
  "password": "password123"
}
```

**预期响应：**
```json
{
  "error": "注册失败",
  "message": "用户名已存在"
}
```

### 2. 无效登录

**请求：**
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "wrongpassword"
}
```

**预期响应：**
```json
{
  "error": "登录失败",
  "message": "用户名或密码错误"
}
```

### 3. 无效令牌访问

**请求：**
```http
GET http://localhost:8080/api/test/user
Authorization: Bearer invalid_token
```

**预期响应：**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token validation failed",
  "path": "/api/test/user"
}
```

### 4. 权限不足

**请求：**
```http
GET http://localhost:8080/api/test/admin
Authorization: Bearer user_access_token
```

**预期响应：**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied",
  "path": "/api/test/admin"
}
```

## 默认用户账号

- **管理员用户**
  - 用户名：`admin`
  - 密码：`admin123`
  - 角色：`ADMIN`

- **测试用户**
  - 用户名：`testuser`
  - 密码：`test123`
  - 角色：`USER`

## 数据库访问

- **数据库类型**：MySQL
- **数据库名**：`qn`
- **主机**：localhost:3306
- **用户名**：root
- **密码**：123456
- **JDBC URL**：`jdbc:mysql://localhost:3306/qn?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true`

## 注意事项

1. Access Token有效期为15分钟
2. Refresh Token有效期为7天
3. 所有需要认证的请求都需要在Header中携带`Authorization: Bearer <token>`
4. 项目使用MySQL数据库，数据库名为`qn`
5. 数据库会在首次启动时自动创建
6. 生产环境建议使用独立的MySQL实例
