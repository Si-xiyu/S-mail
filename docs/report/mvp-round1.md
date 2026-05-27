# 第一轮 MVP 完成记录

## 已完成

- 建立 `backend/dev`、`agent/dev`、`frontend/dev`、`docs/dev` 分支，分别承载不同模块改动。
- 后端完成 Spring Boot Maven 工程、数据库 schema、实体、Mapper、通用响应、异常处理和轻量 token。
- 后端完成注册登录、发送邮件、邮箱列表、邮件详情、已读、星标、删除。
- 后端完成 AI 门面 API 和 Agent Internal Tool API。
- Agent 完成 FastAPI 服务、任务接口、规则 Agent Loop、后端工具调用和 mock fallback。
- 前端完成 Vue 3 + Vite 工作台骨架，可调用后端 API，也可在后端不可用时使用 mock 数据。

## 已验证

- `backend`: `mvn test` 通过。
- `agent`: `python -m compileall app` 通过。
- `frontend`: `npm run build` 通过。

## 后续建议

1. 合并后启动三服务，做一次真实联调。
2. 后端补附件上传下载和搜索接口。
3. Agent 接入真实 LLM Provider，同时保留规则 fallback。
4. 前端拆分组件和 Pinia Store，避免 `App.vue` 继续变大。
5. 补充接口测试和演示数据。
