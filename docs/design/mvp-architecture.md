# SmartMail 第一轮 MVP 架构说明

第一轮目标是搭建前端、后端、Agent 三部分的 mock 骨架，并实现后端与 Agent 的最小闭环。

## 服务边界

```text
Vue 前端
  -> Spring Boot REST API
      -> MySQL / H2
      -> Agent API
          -> Spring Boot Internal Tool API
```

## 后端 MVP

后端负责基础邮件系统主链路：

- 注册、登录、轻量 token 鉴权。
- 邮件发送。
- 收件箱、已发送、垃圾箱列表。
- 邮件详情。
- 已读、星标、删除。
- AI 门面接口。
- Internal Tool API。

数据库沿用“邮件原文”和“用户邮箱条目”分离设计：

- `mail_message`：邮件主内容。
- `mail_recipient`：收件人关系。
- `mailbox_item`：用户视角下的文件夹、已读、星标、优先级、删除状态。
- `mail_ai_result`：AI 结果。

## Agent MVP

Agent 不直接访问数据库，只通过 Internal Tool API 获取和写回数据。

当前实现是规则版 Agent Loop：

```text
接收任务
  -> 规划任务
  -> tool:get_mail
  -> 规则引擎生成摘要/回复/分析
  -> tool:set_priority，可选
  -> tool:save_ai_result
  -> 返回结构化结果
```

后续可以把规则引擎替换为 LLM Provider，但工具边界不变。

## 前端 MVP

前端提供一个类 Notion Mail 的工作台骨架：

- 侧边栏账号区和文件夹。
- 邮件列表。
- 邮件阅读区。
- 写邮件表单。
- AI 摘要、回复草稿、优先级分析入口。

后端未启动时，前端使用 mock 数据兜底，便于 UI 和交互继续开发。

## 本地启动

### 后端

开发期可先使用 H2：

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

连接 MySQL 时使用默认配置或环境变量：

```text
SMARTMAIL_DB_URL
SMARTMAIL_DB_USERNAME
SMARTMAIL_DB_PASSWORD
```

### Agent

```powershell
cd agent
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

### 前端

```powershell
cd frontend
npm install
npm run dev
```

访问：

```text
http://127.0.0.1:5173
```
