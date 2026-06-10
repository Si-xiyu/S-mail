# SmartMail Docs

`docs/` 是 SmartMail 的正式项目文档目录。原始课程资料、计划书模板和历史 prompt 放在 `doc/`；项目当前决策、接口、架构、报告和测试材料放在 `docs/`。

进入项目时建议先读根目录的 [项目架构与技术选型.md](../项目架构与技术选型.md)，再按需要阅读本目录下的展开文档。

## 文档入口

- [../项目架构与技术选型.md](../项目架构与技术选型.md)：项目主入口，包含产品定位、边界、技术栈、模块和文档导航。
- [../CONTEXT.md](../CONTEXT.md)：领域术语和已确认边界。
- [../SmartMail项目全貌.md](../SmartMail项目全貌.md)：面向人类快速理解的项目总览。

## 目录说明

```text
docs/
├─ api/       # 前后端和 Agent 接口契约
├─ design/    # 架构、数据库、前端、后端、AI 设计
├─ report/    # 周报、实现记录、答辩材料
└─ test/      # 测试计划、用例、测试报告，当前可按需创建
```

## 当前正式文档

| 文档 | 作用 |
| --- | --- |
| [design/mvp-architecture.md](./design/mvp-architecture.md) | 目标 MVP 架构、服务边界、核心流程 |
| [api/mvp-api.md](./api/mvp-api.md) | Workspace API、附件、Agent、Internal API 草案 |
| [report/mvp-round1.md](./report/mvp-round1.md) | 第一轮原型完成记录 |

## 编写约定

- `docs/design/` 写目标方案，不写临时会议流水。
- `docs/api/` 写接口路径、请求、响应、状态和错误语义。
- `docs/report/` 保留阶段记录，不随新方案改写历史事实。
- 新增或修改接口时，同步更新 `docs/api/`。
- 修改产品边界或核心术语时，先更新根目录 [CONTEXT.md](../CONTEXT.md)。
