# 安装指南

本指南将帮助您完成QN Contest系统的安装和配置。

## 环境准备

### 1. Java环境
```bash
# 检查Java版本
java -version

# 如果版本低于17，请安装Java 17+
# Windows: 下载Oracle JDK或OpenJDK
# macOS: brew install openjdk@17
# Ubuntu: sudo apt install openjdk-17-jdk
```

### 2. Maven环境
```bash
# 检查Maven版本
mvn -version

# 如果未安装，请安装Maven 3.8+
# Windows: 下载Maven并配置环境变量
# macOS: brew install maven
# Ubuntu: sudo apt install maven
```

### 3. MySQL数据库
```bash
# 安装MySQL 8.0+
# Windows: 下载MySQL Installer
# macOS: brew install mysql
# Ubuntu: sudo apt install mysql-server

# 启动MySQL服务
# Windows: 通过服务管理器启动
# macOS: brew services start mysql
# Ubuntu: sudo systemctl start mysql

# 创建数据库用户
mysql -u root -p
CREATE USER 'qncontest'@'localhost' IDENTIFIED BY 'your_password';
CREATE DATABASE qncontest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON qncontest.* TO 'qncontest'@'localhost';
FLUSH PRIVILEGES;
```

### 4. Node.js环境
```bash
# 检查Node.js版本
node -version
npm -version

# 如果版本低于16，请安装Node.js 16+
# 下载地址: https://nodejs.org/
```

## 项目安装

### 1. 克隆项目
```bash
git clone https://github.com/your-repo/qncontest.git
cd qncontest
```

### 2. 后端安装
```bash
cd backend

# 配置数据库连接
cp src/main/resources/application.yml src/main/resources/application-local.yml

# 编辑配置文件
# 修改数据库连接信息
# 添加DashScope API Key
```

**配置文件示例** (`application-local.yml`):
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qncontest?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: qncontest
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

# DashScope配置
dashscope:
  api-key: your_dashscope_api_key
  model: qwen-plus
  temperature: 0.7
  max-tokens: 2000

# JWT配置
jwt:
  secret: your_jwt_secret_key_here
  expiration: 900000
  refresh-expiration: 86400000
```

### 3. 数据库初始化
```bash
# 执行数据库初始化脚本
mysql -u qncontest -p qncontest < database-init.sql

# 或者使用批处理脚本（Windows）
# 双击运行 init-db.bat
```

### 4. 启动后端服务
```bash
# 使用Maven启动
mvn spring-boot:run

# 或者先编译再启动
mvn clean package
java -jar target/qncontest-1.0.0.jar

# 服务将在 http://localhost:8080 启动
```

### 5. 前端安装

#### 5.1 安装UI组件库
```bash
# 进入UI组件库目录
cd ui

# 安装依赖
npm install

# 构建组件库（必须先构建，web项目依赖此库）
npm run build

# 构建完成后会生成 dist/ 目录，包含编译后的组件库
```

#### 5.2 安装Web前端
```bash
# 进入web前端目录
cd ../web

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 服务将在 http://localhost:5173 启动
```

**重要提示**: 必须先构建UI组件库，因为web项目依赖编译后的组件库文件。

## 配置说明

### 1. 环境变量配置
创建 `.env` 文件（可选）:
```bash
# 后端配置
DASHSCOPE_API_KEY=your_dashscope_api_key
JWT_SECRET=your_jwt_secret_key
DB_PASSWORD=your_database_password

# 前端配置
VITE_API_BASE_URL=http://localhost:8080/api
```

### 2. 数据库配置
确保数据库配置正确：
- 数据库名称: `qncontest`
- 字符集: `utf8mb4`
- 排序规则: `utf8mb4_unicode_ci`
- 时区: `Asia/Shanghai`

### 3. API配置
获取DashScope API Key：
1. 访问[阿里云DashScope控制台](https://dashscope.console.aliyun.com/)
2. 创建API Key
3. 在配置文件中填入API Key

## 验证安装

### 1. 后端验证
```bash
# 检查服务状态
curl http://localhost:8080/api/health

# 测试认证接口
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"panzijian1234","password":"123456"}'
```

### 2. 前端验证
1. 打开浏览器访问 `http://localhost:5173`
2. 使用测试账户登录
3. 测试角色扮演功能

### 3. 数据库验证
```sql
-- 检查表是否创建成功
USE qncontest;
SHOW TABLES;

-- 检查测试数据
SELECT * FROM users;
SELECT * FROM world_templates;
```

## 常见问题

### 1. 数据库连接失败
- 检查MySQL服务是否启动
- 验证数据库用户名和密码
- 确认数据库名称正确

### 2. API Key无效
- 检查DashScope API Key是否正确
- 确认API Key有足够的调用额度
- 验证网络连接正常

### 3. 端口冲突
- 后端默认端口8080，前端默认端口5173
- 可在配置文件中修改端口
- 使用 `netstat -an | findstr :8080` 检查端口占用

### 4. 依赖安装失败
- 检查网络连接
- 尝试使用国内镜像源
- 清除缓存后重新安装

### 5. UI组件库构建失败
- 确保Node.js版本 >= 16
- 检查ui目录下的package.json配置
- 确保所有依赖都已正确安装
- 查看构建日志中的具体错误信息

### 6. Web项目启动失败
- 确保UI组件库已成功构建
- 检查web项目是否正确引用了ui/dist目录
- 验证所有依赖都已安装
- 检查端口5173是否被占用

## 下一步

安装完成后，您可以：

1. [快速体验](quick-start.md) - 体验系统功能
2. [配置说明](configuration.md) - 了解详细配置
3. [开发指南](../development/README.md) - 开始开发

## 获取帮助

如果遇到安装问题：

1. 查看[故障排除指南](../maintenance/troubleshooting.md)
2. 检查[常见问题](../maintenance/troubleshooting.md#常见问题)
3. 提交Issue到项目仓库

---

**安装完成！开始您的角色扮演之旅吧！** 🎭
