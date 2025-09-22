# QN Contest Backend 项目总结

## 项目概述

本项目是一个基于Spring Boot和Hibernate的后端应用，实现了完整的用户认证系统，包括用户注册、登录、JWT双令牌认证等功能。

## 核心功能

### 1. 用户认证系统
- ✅ 用户注册（用户名、邮箱、密码验证）
- ✅ 用户登录（用户名/密码认证）
- ✅ JWT双令牌策略（Access Token + Refresh Token）
- ✅ 令牌刷新机制
- ✅ 用户登出（令牌失效）

### 2. 安全特性
- ✅ Spring Security集成
- ✅ BCrypt密码加密
- ✅ JWT令牌验证
- ✅ 角色权限控制（USER、ADMIN、MODERATOR）
- ✅ CORS跨域支持
- ✅ 全局异常处理

### 3. 数据持久化
- ✅ Hibernate ORM
- ✅ JPA Repository
- ✅ H2内存数据库（开发环境）
- ✅ MySQL支持（生产环境）
- ✅ 自动数据初始化

## 技术架构

### 后端技术栈
- **Spring Boot 3.2.0** - 主框架
- **Spring Security 6** - 安全框架
- **Spring Data JPA** - 数据访问层
- **Hibernate** - ORM框架
- **JWT (jjwt 0.12.3)** - 令牌管理
- **H2 Database** - 开发数据库
- **Maven** - 依赖管理

### 项目结构
```
backend/
├── src/main/java/com/qncontest/
│   ├── QnContestApplication.java          # 主启动类
│   ├── config/                           # 配置类
│   │   ├── DataInitializer.java          # 数据初始化
│   │   ├── PasswordEncoderConfig.java    # 密码编码器
│   │   └── SecurityConfig.java           # 安全配置
│   ├── controller/                       # 控制器层
│   │   ├── AuthController.java           # 认证控制器
│   │   └── TestController.java           # 测试控制器
│   ├── dto/                             # 数据传输对象
│   │   ├── AuthResponse.java            # 认证响应
│   │   ├── LoginRequest.java            # 登录请求
│   │   ├── RegisterRequest.java         # 注册请求
│   │   └── RefreshTokenRequest.java     # 刷新令牌请求
│   ├── entity/                          # 实体类
│   │   ├── Role.java                    # 角色枚举
│   │   ├── RefreshToken.java            # 刷新令牌实体
│   │   └── User.java                    # 用户实体
│   ├── exception/                       # 异常处理
│   │   └── GlobalExceptionHandler.java  # 全局异常处理器
│   ├── repository/                      # 数据访问层
│   │   ├── RefreshTokenRepository.java  # 刷新令牌仓库
│   │   └── UserRepository.java          # 用户仓库
│   ├── security/                        # 安全相关
│   │   ├── AccessDeniedHandlerJwt.java  # 访问拒绝处理器
│   │   ├── AuthEntryPointJwt.java       # 认证入口点
│   │   ├── JwtAuthenticationFilter.java # JWT认证过滤器
│   │   └── JwtUtils.java                # JWT工具类
│   └── service/                         # 服务层
│       ├── AuthService.java             # 认证服务
│       ├── RefreshTokenService.java     # 刷新令牌服务
│       └── UserDetailsServiceImpl.java  # 用户详情服务
├── src/main/resources/
│   └── application.yml                  # 应用配置
├── pom.xml                             # Maven配置
├── README.md                           # 项目说明
├── API_TEST.md                         # API测试文档
└── start.bat/start.sh                  # 启动脚本
```

## JWT双令牌策略

### Access Token
- **有效期**: 15分钟
- **用途**: 访问受保护的API端点
- **存储**: 客户端内存中
- **类型标识**: `"type": "access"`

### Refresh Token
- **有效期**: 7天
- **用途**: 刷新Access Token
- **存储**: 数据库表中
- **类型标识**: `"type": "refresh"`

### 令牌刷新流程
1. 客户端使用Refresh Token请求新的Access Token
2. 服务器验证Refresh Token的有效性和类型
3. 如果有效，生成新的Access Token
4. 返回新的Access Token和原有的Refresh Token

## API接口

### 认证接口
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/refresh` - 刷新令牌
- `POST /api/auth/logout` - 用户登出

### 测试接口
- `GET /api/test/public` - 公开端点
- `GET /api/test/user` - 用户端点（需要认证）
- `GET /api/test/admin` - 管理员端点（需要管理员权限）

## 默认用户

项目启动时自动创建：

- **管理员用户**
  - 用户名: `admin`
  - 密码: `admin123`
  - 角色: `ADMIN`

- **测试用户**
  - 用户名: `testuser`
  - 密码: `test123`
  - 角色: `USER`

## 配置说明

### 应用配置 (application.yml)
```yaml
server:
  port: 8080
  servlet:
    context-path: /api

jwt:
  secret: mySecretKey123456789012345678901234567890
  access-token-expiration: 900000    # 15分钟
  refresh-token-expiration: 604800000 # 7天

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: password
```

## 安全特性详解

### 1. 密码安全
- 使用BCrypt算法加密存储密码
- 自动生成盐值，防止彩虹表攻击
- 密码强度验证（最少6位）

### 2. JWT安全
- 使用HMAC-SHA256算法签名
- 令牌包含过期时间
- 区分Access Token和Refresh Token类型
- 支持令牌撤销（登出时删除Refresh Token）

### 3. 权限控制
- 基于角色的访问控制（RBAC）
- 方法级权限注解（@PreAuthorize）
- 细粒度权限控制

### 4. 异常处理
- 统一的异常处理机制
- 详细的错误信息返回
- 安全的错误信息（不泄露敏感信息）

## 部署说明

### 开发环境
1. 确保Java 17+已安装
2. 运行 `mvn spring-boot:run`
3. 访问 http://localhost:8080/api

### 生产环境
1. 修改数据库配置为MySQL
2. 修改JWT密钥为更安全的随机字符串
3. 配置HTTPS
4. 使用Docker或直接部署JAR包

## 扩展建议

### 1. 功能扩展
- 用户资料管理
- 密码重置功能
- 邮箱验证
- 多因素认证（MFA）
- 用户角色管理

### 2. 性能优化
- Redis缓存集成
- 数据库连接池优化
- 令牌黑名单机制
- 限流和熔断

### 3. 监控和日志
- 应用性能监控（APM）
- 结构化日志
- 健康检查端点
- 指标收集

## 总结

本项目成功实现了一个完整的用户认证系统，具有以下特点：

1. **安全性高**: 采用JWT双令牌策略，密码加密存储，细粒度权限控制
2. **架构清晰**: 分层架构，职责分离，易于维护和扩展
3. **功能完整**: 涵盖用户注册、登录、令牌管理等核心功能
4. **易于使用**: 提供详细的API文档和测试用例
5. **可扩展性强**: 模块化设计，便于添加新功能

该项目可以作为其他需要用户认证功能的应用的基础框架，也可以根据具体需求进行定制和扩展。
