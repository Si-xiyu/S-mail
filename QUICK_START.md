# SmartMail 快速启动参考卡

## 🚀 最快启动方式（3 个终端）

### 终端 1：启动后端
```bash
cd backend
mvn spring-boot:run
```
✅ 看到：`Tomcat started on port(s): 8080`

---

### 终端 2：启动前端
```bash
cd frontend
npm install  # 首次运行需要
npm run dev
```
✅ 看到：`Local: http://127.0.0.1:5173`

---

### 终端 3：验证连接
```bash
# 测试后端 API
这里建议通过postman进行测试 因为后端的必须要携带

# 测试前端
curl http://127.0.0.1:5173
```

✅ 都返回内容说明连接成功

---

## 🔗 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端 API | http://localhost:8080 | Spring Boot |
| 前端应用 | http://127.0.0.1:5173 | Vite Dev Server |
| 浏览器打开 | http://127.0.0.1:5173 | 👈 在这里访问应用 |

---

## 📱 浏览器验证步骤

### 1️⃣ 打开页面
```
http://127.0.0.1:5173
```

### 2️⃣ 按 F12 打开开发者工具

### 3️⃣ Network 标签中应该看到
```
✅ /api/v1/...  Status: 200
✅ index.js     Status: 200
✅ index.css    Status: 200
```

### 4️⃣ Console 没有红色错误

### 5️⃣ 测试登录
- 输入邮箱：`demo@smartmail.local`
- 输入密码：`123456`
- 点击登录

### 6️⃣ Console 中验证 token
```javascript
localStorage.getItem('smartmail_token')
// 应该看到一个很长的字符串
```

---

## ❌ 快速故障排查

| 问题 | 解决方案 |
|------|--------|
| 端口 8080 被占用 | `lsof -i :8080 && kill -9 <PID>` |
| npm 依赖问题 | `rm -rf node_modules && npm install` |
| API 连接失败 | 确认 vite.config.ts 代理配置：`'/api': 'http://localhost:8080'` |
| 空白页面 | F12 → Console 查看错误，清除缓存 |
| 401 错误 | 重新登录，检查 token 是否保存 |

---

## 📋 完整检查清单

```
☐ Java 已安装：java -version
☐ Maven 已安装：mvn -v
☐ Node.js 已安装：node -v
☐ npm 已安装：npm -v

☐ 后端启动成功：看到 "Tomcat started on port 8080"
☐ 前端启动成功：看到 "ready in xxx ms"
☐ 浏览器打开：http://127.0.0.1:5173

☐ Network 有 /api/ 请求
☐ Console 无红色错误
☐ 可以输入邮箱密码
☐ 登录后有 token
```

---

## 🔧 常用命令

```bash
# 清理后端
cd backend && mvn clean

# 构建后端
cd backend && mvn package -DskipTests

# 运行后端
cd backend && mvn spring-boot:run

# 或使用 JAR 运行
cd backend && java -jar target/smartmail-backend-0.0.1-SNAPSHOT.jar

# 安装前端依赖
cd frontend && npm install

# 启动前端开发服务器
cd frontend && npm run dev

# 构建前端生产版本
cd frontend && npm run build

# 预览生产版本
cd frontend && npm run preview
```

---

## 🌐 API 端点速查

```bash
# 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@smartmail.local","password":"123456"}'

# 注册
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"new@smartmail.local","username":"New User","password":"123456"}'

# 获取邮箱列表
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/mailbox?folder=INBOX

# 获取邮件详情
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/mails/1
```

---

## 💡 提示

- **后端优先**：总是先启动后端，再启动前端
- **保持运行**：启动后要保持两个终端窗口打开
- **热重载**：前端文件修改会自动重新加载，无需重启
- **清缓存**：如果出现奇怪问题，按 Ctrl+Shift+Delete 清除浏览器缓存
- **查看日志**：错误信息通常在终端中，仔细看日志

---

## 📞 求助信息

遇到问题时收集这些信息：

1. 完整的错误信息（从终端复制）
2. 浏览器 Console 中的错误（F12）
3. Network 标签中失败的请求
4. 你执行的命令
5. 预期结果 vs 实际结果

---

**🎉 祝你运行顺利！** 🚀

