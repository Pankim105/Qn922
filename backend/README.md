# QN Contest Backend

基于Spring Boot和Hibernate的后端项目，实现了用户注册登录模块，使用Spring Security和JWT认证，采用双令牌过期策略。

## 功能特性

- ✅ 用户注册和登录
- ✅ JWT双令牌策略（Access Token + Refresh Token）
- ✅ Spring Security安全配置
- ✅ Hibernate数据持久化
- ✅ 角色权限控制
- ✅ 全局异常处理
- ✅ 数据验证
- ✅ CORS跨域支持

## 技术栈

- **Spring Boot 3.2.0**
- **Spring Security 6**
- **Spring Data JPA**
- **Hibernate**
- **JWT (jjwt 0.12.3)**
- **H2 Database** (开发环境)
- **MySQL** (生产环境)
- **Maven**

## 项目结构

```
src/main/java/com/qncontest/
├── QnContestApplication.java          # 主启动类
├── config/                           # 配置类
│   ├── DataInitializer.java          # 数据初始化
│   ├── PasswordEncoderConfig.java    # 密码编码器配置
│   └── SecurityConfig.java           # Spring Security配置
├── controller/                       # 控制器
│   ├── AuthController.java           # 认证控制器
│   └── TestController.java           # 测试控制器
├── dto/                             # 数据传输对象
│   ├── AuthResponse.java            # 认证响应
│   ├── LoginRequest.java            # 登录请求
│   ├── RegisterRequest.java         # 注册请求
│   └── RefreshTokenRequest.java     # 刷新令牌请求
├── entity/                          # 实体类
│   ├── Role.java                    # 角色枚举
│   ├── RefreshToken.java            # 刷新令牌实体
│   └── User.java                    # 用户实体
├── exception/                       # 异常处理
│   └── GlobalExceptionHandler.java  # 全局异常处理器
├── repository/                      # 数据访问层
│   ├── RefreshTokenRepository.java  # 刷新令牌仓库
│   └── UserRepository.java          # 用户仓库
├── security/                        # 安全相关
│   ├── AccessDeniedHandlerJwt.java  # 访问拒绝处理器
│   ├── AuthEntryPointJwt.java       # 认证入口点
│   ├── JwtAuthenticationFilter.java # JWT认证过滤器
│   └── JwtUtils.java                # JWT工具类
└── service/                         # 服务层
    ├── AuthService.java             # 认证服务
    ├── RefreshTokenService.java     # 刷新令牌服务
    └── UserDetailsServiceImpl.java  # 用户详情服务实现
```

## 快速开始

### 1. 环境要求

- Java 17+
- Maven 3.6+

### 2. 运行项目

```bash
# 进入backend目录
cd backend

# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run
```

### 3. 访问应用

- 应用地址: http://localhost:8080/api
- 数据库: MySQL (数据库名: `qn`)
  - 主机: localhost:3306
  - 用户名: root
  - 密码: 123456

## API接口

### 认证接口

#### 用户注册
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}
```

#### 用户登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

#### 刷新令牌
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

#### 用户登出
```http
POST /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "your-refresh-token"
}
```

### 测试接口

#### 公开端点
```http
GET /api/test/public
```

#### 用户端点（需要认证）
```http
GET /api/test/user
Authorization: Bearer your-access-token
```

#### 管理员端点（需要管理员权限）
```http
GET /api/test/admin
Authorization: Bearer your-access-token
```

## JWT双令牌策略

### Access Token
- 有效期: 15分钟
- 用途: 访问受保护的资源
- 存储: 内存中

### Refresh Token
- 有效期: 7天
- 用途: 刷新Access Token
- 存储: 数据库中

### 令牌刷新流程
1. 客户端使用Refresh Token请求新的Access Token
2. 服务器验证Refresh Token的有效性
3. 如果有效，生成新的Access Token
4. 返回新的Access Token和原有的Refresh Token

## 默认用户

项目启动时会自动创建以下用户：

- **管理员用户**
  - 用户名: `admin`
  - 密码: `admin123`
  - 角色: `ADMIN`

- **测试用户**
  - 用户名: `testuser`
  - 密码: `test123`
  - 角色: `USER`

## 配置说明

### 环境变量配置

项目支持通过环境变量配置敏感信息，提高安全性：

```bash
# 数据库配置
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=qn
export DB_USERNAME=root
export DB_PASSWORD=123456

# JWT配置
export JWT_SECRET=mySecretKey123456789012345678901234567890
export JWT_ACCESS_EXPIRATION=900000
export JWT_REFRESH_EXPIRATION=604800000

# 管理员注册密钥
export ADMIN_REGISTRATION_KEY=123456
```

### application.yml配置项

```yaml
# JWT配置（支持环境变量）
jwt:
  secret: ${JWT_SECRET:mySecretKey123456789012345678901234567890}
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:900000}
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}

# 管理员注册配置（支持环境变量）
admin:
  registration-key: ${ADMIN_REGISTRATION_KEY:123456}

# 数据库配置（支持环境变量）
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:qn}?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}
```

详细的环境变量配置说明请参考 [ENVIRONMENT_CONFIG.md](ENVIRONMENT_CONFIG.md)

## 安全特性

1. **密码加密**: 使用BCrypt加密存储密码
2. **JWT认证**: 无状态认证机制
3. **双令牌策略**: Access Token + Refresh Token
4. **角色权限**: 基于角色的访问控制
5. **CORS支持**: 跨域资源共享配置
6. **异常处理**: 统一的异常处理机制

## 开发说明

### 添加新的API端点

1. 在对应的Controller中添加方法
2. 使用`@PreAuthorize`注解控制权限
3. 在SecurityConfig中配置路径权限

### 添加新的角色

1. 在`Role`枚举中添加新角色
2. 更新相关的权限检查逻辑

### 数据库迁移

项目使用H2内存数据库进行开发，生产环境可以切换到MySQL：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qncontest
    username: your-username
    password: your-password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
```

## 许可证

MIT License
