# SmartMail 前后端运行完整指南

## 📋 目录

1. [环境检查](#环境检查)
2. [第一步：启动后端](#第一步启动后端)
3. [第二步：启动前端](#第二步启动前端)
4. [第三步：验证连接](#第三步验证连接)
5. [第四步：功能测试](#第四步功能测试)
6. [问题排查](#问题排查)
7. [快速启动脚本](#快速启动脚本)

---

## 环境检查

### 检查 Java 环境（后端）

```bash
java -version
```

**预期输出**（示例）：
```
openjdk version "17.0.x" 2021-09-14
OpenJDK Runtime Environment (build 17.0.x+...)
```

**要求**：Java 17 或更高版本

---

### 检查 Node.js 环境（前端）

```bash
node -v
npm -v
```

**预期输出**（示例）：
```
v20.x.x
10.x.x
```

**要求**：Node.js 16+，npm 8+

---

### 检查 Maven（后端构建）

```bash
mvn -v
```

**预期输出**（示例）：
```
Apache Maven 3.8.x
```

**要求**：Maven 3.6+（或使用 IDE 内置 Maven）

---

## 第一步：启动后端

### 方式 A：使用 Maven 命令行（推荐）

#### 1️⃣ 进入后端目录

```bash
cd backend
```

#### 2️⃣ 清理并编译

```bash
mvn clean package -DskipTests
```

**说明**：
- `clean` - 清理之前的构建
- `package` - 编译并打包
- `-DskipTests` - 跳过测试（加快速度）

**预期输出**：
```
[INFO] Building jar: .../backend/target/smartmail-backend-0.0.1-SNAPSHOT.jar
[INFO] BUILD SUCCESS
```

**预计时间**：2-5 分钟（首次构建可能较慢）

#### 3️⃣ 启动后端服务

```bash
mvn spring-boot:run
```

**预期输出**：
```
. ____ _ __ _ _
 /\\ / ___'_ __ _ _(_)_ __ __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

[INFO] Starting SmartMailApplication...
[INFO] Tomcat started on port(s): 8080 (http) with context path '/api'
[INFO] SmartMail started successfully!
```

**验证后端启动成功**：
```bash
# 在新的终端窗口运行
curl http://localhost:8080/api/v1/auth/login
```

**预期返回**（可能是错误，但证明服务在运行）：
```json
{
  "code": 400,
  "message": "Request method 'GET' not supported",
  "data": null
}
```

---

### 方式 B：使用 IDE 运行（IntelliJ IDEA / VS Code）

#### IntelliJ IDEA

1. **打开项目**
   - File → Open → 选择 `backend` 目录

2. **配置 Spring Boot Run Configuration**
   - Run → Edit Configurations
   - 点击 `+` → Spring Boot
   - Main class: `com.smartmail.SmartMailApplication`
   - Working directory: `$MODULE_DIR$`

3. **启动**
   - 点击 Run 按钮（或 Shift + F10）

#### VS Code + Spring Boot Extension

1. **安装扩展**
   - Extension Pack for Java
   - Spring Boot Extension Pack

2. **启动**
   - 按 Ctrl+Shift+D 打开 Run
   - 选择 "Spring Boot App"
   - 点击运行

---

### 方式 C：运行已构建的 JAR（生产）

#### 1️⃣ 构建 JAR

```bash
cd backend
mvn clean package -DskipTests
```

#### 2️⃣ 运行 JAR

```bash
java -jar target/smartmail-backend-0.0.1-SNAPSHOT.jar
```

**说明**：
- JAR 文件名可能会因版本而异
- 使用 Tab 键自动补全文件名

---

### ✅ 后端启动检查清单

启动后，确认以下几点：

- [ ] 看到 "Tomcat started on port(s): 8080"
- [ ] 看到 "SmartMail started successfully!"
- [ ] 没有红色错误日志
- [ ] 可以访问 http://localhost:8080/api/v1

---

## 第二步：启动前端

### ⚠️ 重要：保持后端运行

后端必须持续运行。建议在**新的终端窗口**中启动前端。

---

### 方式 A：开发模式（推荐）

#### 1️⃣ 进入前端目录

```bash
cd frontend
```

#### 2️⃣ 安装依赖（首次运行）

```bash
npm install
```

**说明**：
- 这会安装 package.json 中的所有依赖
- 如果已经安装过，可以跳过此步骤
- 首次安装可能需要 2-5 分钟

**预期输出**：
```
added 1234 packages, and audited 1235 packages in 2m34s
```

#### 3️⃣ 启动开发服务器

```bash
npm run dev
```

**预期输出**：
```
VITE v6.4.2  ready in 234 ms

➜  Local:   http://127.0.0.1:5173/
➜  press h + enter to show help
```

**说明**：
- 前端服务运行在 `http://127.0.0.1:5173`
- Vite 会监视文件变化，自动重新加载
- 不要关闭此终端，直到测试完成

---

### 方式 B：生产构建

#### 1️⃣ 构建前端

```bash
cd frontend
npm install
npm run build
```

**预期输出**：
```
✓ 1673 modules transformed.
✓ built in 7.12s
```

#### 2️⃣ 预览生产构建

```bash
npm run preview
```

**预期输出**：
```
➜  Local:   http://127.0.0.1:4173/
```

---

### ✅ 前端启动检查清单

启动后，确认以下几点：

- [ ] 看到 "ready in xxx ms"
- [ ] 看到本地 URL（通常是 http://127.0.0.1:5173）
- [ ] 没有红色错误日志
- [ ] 可以打开浏览器访问前端地址

---

## 第三步：验证连接

### 1️⃣ 打开浏览器

在地址栏输入：

```
http://127.0.0.1:5173
```

**预期结果**：
- SmartMail 应用界面加载
- 看到登录页面（如果已实现）或邮件列表

---

### 2️⃣ 打开浏览器开发者工具

按 `F12` 或 `右键 → 检查`

#### 切换到 Network 标签

1. 打开 "Network" 标签
2. 刷新页面（Ctrl+R 或 Cmd+R）
3. 查看网络请求列表

---

### 3️⃣ 验证 API 代理工作

在 Network 标签中查看：

#### ✅ 成功的标志

```
Request URL: http://127.0.0.1:5173/api/v1/...
Status: 200 (或其他 2xx-4xx 状态码)
```

#### ❌ 失败的标志

```
Status: 0 或 ERR_*
CORS 错误
```

---

### 4️⃣ 测试登录接口（推荐）

#### 使用 curl 命令

打开新的终端窗口，运行：

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@smartmail.local","password":"123456"}'
```

**预期输出**（成功登录）：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGc...",
    "userId": 1,
    "email": "demo@smartmail.local",
    "username": "Demo User"
  }
}
```

**预期输出**（登录失败）：
```json
{
  "code": 401,
  "message": "Invalid credentials",
  "data": null
}
```

**说明**：
- 无论成功还是失败，只要返回 JSON 就说明连接成功
- 确保后端和前端都在运行

---

### 5️⃣ 使用浏览器 Console 测试（高级）

在浏览器 F12 → Console 标签中运行：

```javascript
// 测试前端是否能调用 API
fetch('/api/v1/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'demo@smartmail.local',
    password: '123456'
  })
})
.then(res => res.json())
.then(data => console.log('API Response:', data))
.catch(err => console.error('Error:', err))
```

**预期输出**：
```
API Response: {code: 0, message: "success", data: {...}}
```

---

## 第四步：功能测试

### 🧪 基础流程测试

#### 1️⃣ 用户注册（如果前端有实现）

- 点击"注册"按钮
- 填写邮箱、用户名、密码
- 点击"注册"
- **验证**：应该看到成功提示或跳转到邮箱列表

#### 2️⃣ 用户登录

- 输入邮箱（例如：`demo@smartmail.local`）
- 输入密码（例如：`123456`）
- 点击"登录"

**验证方式**：
```javascript
// 在 Console 中运行
localStorage.getItem('smartmail_token')
// 应该看到一个很长的字符串（JWT token）
```

#### 3️⃣ 加载邮箱列表

- 登录后应该自动加载邮件列表
- 检查 Network 标签中是否有：
  ```
  GET /api/v1/mailbox?folder=INBOX
  Status: 200
  ```

#### 4️⃣ 查看邮件详情

- 点击列表中的邮件
- 应该加载邮件详情
- 检查 Network 中是否有：
  ```
  GET /api/v1/mails/{mailId}
  Status: 200
  ```

#### 5️⃣ 发送邮件

- 点击"撰写"按钮
- 填写收件人、主题、内容
- 点击"发送"
- **验证**：看到成功提示，检查 Network 中是否有：
  ```
  POST /api/v1/mails
  Status: 200
  ```

---

## 问题排查

### 问题 1：后端无法启动

#### 错误信息示例
```
Caused by: java.net.BindException: Address already in use
```

#### 解决方案

**原因**：端口 8080 已被占用

**方案 A：杀死占用端口的进程**

Linux/Mac：
```bash
lsof -i :8080
kill -9 <PID>
```

Windows：
```bash
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**方案 B：更改后端端口**

编辑 `backend/src/main/resources/application.properties`：
```properties
server.port=8081
```

然后修改前端代理配置，编辑 `frontend/vite.config.ts`：
```typescript
server: {
  proxy: {
    '/api': 'http://localhost:8081'  // 改为新端口
  }
}
```

---

### 问题 2：前端无法启动

#### 错误信息示例
```
error when starting dev server:
Error: ENOENT: no such file or directory
```

#### 解决方案

**方案 A：重新安装依赖**

```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

**方案 B：检查 Node.js 版本**

```bash
node -v
# 应该是 16+ 或更高
```

如果版本太低，更新 Node.js：
```bash
# 使用 nvm（推荐）
nvm install node
nvm use node

# 或使用包管理器
brew install node@20  # macOS
choco install nodejs  # Windows
apt-get install nodejs  # Linux
```

---

### 问题 3：前后端连接失败（CORS 错误）

#### 错误信息示例
```
Access to XMLHttpRequest at 'http://localhost:8080/api/v1/...' 
from origin 'http://127.0.0.1:5173' has been blocked by CORS policy
```

#### 解决方案

**原因**：后端未配置 CORS

**方案 A：确认 Vite 代理配置**

检查 `frontend/vite.config.ts`：

```typescript
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080'  // ✅ 确保这里配置正确
    }
  }
})
```

**验证代理工作**：

在浏览器 Network 标签中，请求 URL 应该显示：
```
http://127.0.0.1:5173/api/v1/...  (前端 URL)
```

不应该显示：
```
http://localhost:8080/api/v1/...  (后端直接 URL)
```

如果显示后端 URL，说明代理未工作。

**方案 B：清缓存并重启**

```bash
# 前端目录
rm -rf .vite dist
npm run dev
```

**方案 C：后端添加 CORS 配置**

如果代理无法解决，编辑 `backend/src/main/java/com/smartmail/config/CorsConfig.java`：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://127.0.0.1:5173", "http://localhost:5173")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

重启后端：
```bash
mvn spring-boot:run
```

---

### 问题 4：登录后没有 Token 保存

#### 错误信息示例
```javascript
localStorage.getItem('smartmail_token')  // 返回 null
```

#### 解决方案

**方案 A：检查浏览器 Console**

按 F12 → Console，查看是否有错误：
```
TypeError: Cannot read property 'setItem' of undefined
```

**方案 B：检查登录请求**

Network 标签 → POST /api/v1/auth/login

- Status 应该是 200
- Response 应该包含 `token` 字段：
  ```json
  {
    "code": 0,
    "data": {
      "token": "eyJ...",
      "userId": 1,
      "email": "...",
      "username": "..."
    }
  }
  ```

**方案 C：手动测试 localStorage**

在 Console 中运行：
```javascript
localStorage.setItem('test', 'hello')
localStorage.getItem('test')  // 应该返回 'hello'
```

如果返回 null，说明浏览器禁用了 localStorage（检查隐私设置）。

---

### 问题 5：API 返回 401 错误

#### 错误信息示例
```json
{
  "code": 401,
  "message": "Unauthorized",
  "data": null
}
```

#### 解决方案

**原因 1：Token 过期或无效**

```bash
# 重新登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@smartmail.local","password":"123456"}'
```

获取新 Token，然后在前端重新登录。

**原因 2：Token 格式不正确**

检查 Authorization header 是否正确：
```javascript
// 在 Console 中
localStorage.getItem('smartmail_token')  // 应该是完整的 JWT
```

**原因 3：后端用户不存在**

确保用户已在后端注册或存在。检查后端日志：
```
[ERROR] User not found: demo@smartmail.local
```

---

### 问题 6：API 返回 400 错误

#### 错误信息示例
```json
{
  "code": 400,
  "message": "Bad Request",
  "data": null
}
```

#### 解决方案

**原因**：请求参数不正确

检查：
1. JSON 格式是否正确
2. 必需字段是否都有
3. 字段名称是否拼写正确

**示例：错误的请求体**
```json
{
  "email": "demo@smartmail.local"
  // ❌ 缺少 password 字段
}
```

**正确的请求体**
```json
{
  "email": "demo@smartmail.local",
  "password": "123456"  // ✅ 必需字段
}
```

---

### 问题 7：后端数据库连接失败

#### 错误信息示例
```
Caused by: java.sql.SQLException: Cannot get a connection
Caused by: org.h2.jdbc.JdbcSQLException: Database "mem:smartmail" not found
```

#### 解决方案

**原因**：数据库未初始化

**方案 A：检查 application.properties**

编辑 `backend/src/main/resources/application.properties`：

```properties
# ✅ 确保有这些配置
spring.datasource.url=jdbc:h2:mem:smartmail
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
```

**方案 B：查看 schema.sql**

检查是否有 `schema.sql` 在 `src/main/resources/` 目录：

```sql
-- schema.sql 应该自动创建必要的表
CREATE TABLE user (...);
CREATE TABLE mail (...);
...
```

**方案 C：重启后端**

```bash
mvn clean spring-boot:run
```

---

### 问题 8：前端显示空白页

#### 错误信息示例
页面什么都没显示，或只显示背景色

#### 解决方案

**方案 A：检查浏览器 Console**

按 F12 → Console，查看错误：
```
Uncaught TypeError: mailStore is not defined
```

**方案 B：清除缓存**

```bash
# 前端目录
rm -rf .vite dist node_modules/.vite
npm run dev
```

**方案 C：检查文件是否正确加载**

Network 标签 → 查看 JS/CSS 文件：
- `index.js` Status 应该是 200
- `index.css` Status 应该是 200

如果状态是 404，文件可能未编译。

**方案 D：查看浏览器控制台输出**

```bash
# 前端终端窗口应该显示：
[hmr] connected  # 说明热重载已连接
```

---

## 快速启动脚本

### 🚀 一键启动（推荐）

#### Linux / Mac

创建文件 `start.sh`：

```bash
#!/bin/bash

echo "========================================="
echo "SmartMail 前后端启动脚本"
echo "========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查后端进程
echo -e "${YELLOW}[1/4] 检查后端进程...${NC}"
if lsof -i :8080 > /dev/null 2>&1; then
    echo -e "${RED}❌ 错误：端口 8080 已被占用${NC}"
    echo "请运行: lsof -i :8080 && kill -9 <PID>"
    exit 1
fi

# 启动后端
echo -e "${YELLOW}[2/4] 启动后端...${NC}"
cd backend
mvn clean package -DskipTests > /dev/null 2>&1
mvn spring-boot:run &
BACKEND_PID=$!
echo -e "${GREEN}✓ 后端已启动 (PID: $BACKEND_PID)${NC}"

# 等待后端启动
echo -e "${YELLOW}[3/4] 等待后端就绪...${NC}"
sleep 5

# 启动前端
echo -e "${YELLOW}[4/4] 启动前端...${NC}"
cd ../frontend
npm install > /dev/null 2>&1
npm run dev &
FRONTEND_PID=$!
echo -e "${GREEN}✓ 前端已启动 (PID: $FRONTEND_PID)${NC}"

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}✓ 启动完成！${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo -e "后端地址：${YELLOW}http://localhost:8080${NC}"
echo -e "前端地址：${YELLOW}http://127.0.0.1:5173${NC}"
echo ""
echo "按 Ctrl+C 停止所有服务"
echo ""

# 等待信号
trap "kill $BACKEND_PID $FRONTEND_PID" EXIT
wait
```

使用方法：

```bash
chmod +x start.sh
./start.sh
```

---

#### Windows

创建文件 `start.bat`：

```batch
@echo off
setlocal enabledelayedexpansion

echo =========================================
echo SmartMail 前后端启动脚本
echo =========================================
echo.

REM 检查后端端口
echo [1/4] 检查后端进程...
netstat -ano | findstr :8080 > nul
if !errorlevel! equ 0 (
    echo [错误] 端口 8080 已被占用
    echo 请运行: netstat -ano ^| findstr :8080
    pause
    exit /b 1
)

REM 启动后端
echo [2/4] 启动后端...
cd backend
start cmd /k "mvn clean package -DskipTests && mvn spring-boot:run"
echo [√] 后端启动中，请等待...
timeout /t 5

REM 启动前端
echo [3/4] 启动前端...
cd ..\frontend
call npm install > nul 2>&1
start cmd /k "npm run dev"
echo [√] 前端启动中...

echo.
echo =========================================
echo [√] 启动完成！
echo =========================================
echo.
echo 后端地址: http://localhost:8080
echo 前端地址: http://127.0.0.1:5173
echo.
echo 按任意键关闭此窗口...
pause > nul
```

使用方法：

双击 `start.bat` 文件

---

### 📋 手动启动步骤（分窗口）

#### 终端 1 - 后端

```bash
cd backend
mvn spring-boot:run
```

#### 终端 2 - 前端

```bash
cd frontend
npm install
npm run dev
```

#### 终端 3 - 测试（可选）

```bash
# 测试登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@smartmail.local","password":"123456"}'
```

---

## ✅ 完整启动检查清单

启动完成后，逐一检查：

### 后端检查

- [ ] `mvn spring-boot:run` 或 IDE 开始运行
- [ ] 看到 "Tomcat started on port(s): 8080"
- [ ] 看到 "SmartMail started successfully"
- [ ] 日志没有 ERROR 或 FATAL
- [ ] 可以 curl 访问：`curl http://localhost:8080/api/v1/auth/login`

### 前端检查

- [ ] `npm run dev` 或 IDE 开始运行
- [ ] 看到 "ready in xxx ms"
- [ ] 看到本地 URL（http://127.0.0.1:5173）
- [ ] 终端没有红色错误信息
- [ ] 可以访问本地 URL

### 浏览器检查

- [ ] 打开 http://127.0.0.1:5173
- [ ] 页面加载成功（不是空白或 404）
- [ ] 按 F12 打开开发者工具
- [ ] Network 标签中有 /api/ 请求
- [ ] API 请求 Status 是 200 或 4xx（不是 0 或 ERR_）
- [ ] Console 没有红色错误信息

### 功能检查

- [ ] 可以输入邮箱和密码
- [ ] 可以点击登录/注册按钮
- [ ] 登录成功后 localStorage 有 token
- [ ] 可以看到邮件列表（如果有数据）
- [ ] 点击邮件可以加载详情

---

## 🆘 快速问题诊断

### 现象：后端启动失败

```bash
# 1. 检查 Java 版本
java -version

# 2. 检查端口占用
lsof -i :8080  # Mac/Linux
netstat -ano | findstr :8080  # Windows

# 3. 尝试重新清理构建
cd backend
mvn clean
mvn spring-boot:run
```

### 现象：前端启动失败

```bash
# 1. 检查 Node 版本
node -v

# 2. 重新安装依赖
cd frontend
rm -rf node_modules package-lock.json
npm install

# 3. 启动开发服务器
npm run dev
```

### 现象：API 调用失败

```bash
# 1. 确认后端在运行
curl http://localhost:8080/api/v1/auth/login

# 2. 检查前端代理配置
cat frontend/vite.config.ts  # 查看 proxy 配置

# 3. 检查浏览器 Network 标签
# 请求 URL 应该是 http://127.0.0.1:5173/api/...
# 不应该是 http://localhost:8080/api/...
```

### 现象：登录失败

```bash
# 1. 测试后端登录接口
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"123456"}'

# 2. 查看后端日志，寻找错误信息

# 3. 检查数据库是否有用户数据
```

---

## 📞 获取帮助

### 检查日志

**后端日志**：
```
# 在后端运行的终端中查看，关键词：
ERROR
WARN
Exception
```

**前端日志**：
```
# 在前端运行的终端中查看，关键词：
error
warn
Cannot find module
```

### 收集诊断信息

```bash
# Java 版本
java -version

# Maven 版本
mvn -v

# Node 版本
node -v && npm -v

# 后端状态
curl -v http://localhost:8080/api/v1/auth/login

# 前端状态
curl -v http://127.0.0.1:5173
```

将这些信息保存，便于问题诊断。

---

## 总结

**启动顺序**：后端 → 前端 → 浏览器

**验证步骤**：
1. 后端：curl 测试 API
2. 前端：浏览器打开页面
3. Network：检查 API 请求
4. Console：检查错误信息
5. 功能：测试登录和邮件操作

**问题排查**：
1. 查看终端日志
2. 打开浏览器开发者工具
3. 使用 curl 测试 API
4. 检查配置文件
5. 重启服务

祝你运行顺利！🚀

