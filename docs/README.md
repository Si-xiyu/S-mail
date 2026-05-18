 docs/ 是项目正式文档目录，和 doc/ 区分开：

  - doc/：放原始资料，比如老师给的需求 PDF、已有 prompt。
  - docs/：放你们自己产出的项目文档，后面用于提交、答辩、协作。

  当前 docs 下面有 4 个模块：

  docs/
  ├─ api/
  ├─ design/
  ├─ report/
  └─ test/

  docs/api/

  放前后端接口文档。比如：

  auth-api.md        登录、注册、当前用户接口
  mail-api.md        发邮件、邮件详情、邮件列表接口
  attachment-api.md  附件上传下载接口
  ai-api.md          AI 摘要、回复草稿、优先级接口

  这里要写清楚：接口路径、请求方法、请求参数、响应格式、错误码。前后端联调主要看这个目录。

  docs/design/

  放设计类文档。比如：

  database-design.md    数据库表设计、E-R 图说明
  system-design.md      系统概要设计
  backend-design.md     后端模块设计
  frontend-design.md    前端页面和组件设计
  ai-design.md          AI 插件设计

  这里主要服务于课程要求里的“概要设计、详细设计”。数据库设计、模块划分、业务流程都应该放这里。

  docs/report/

  放报告类文档。比如：

  weekly-report/        个人或小组周报
  implementation.md     实现说明
  summary.md            项目总结
  presentation-outline.md 答辩 PPT 大纲

  这个目录偏提交材料和过程记录。每两周周报、最终实现说明、答辩材料草稿都可以放这里。

  docs/test/

  放测试相关文档。比如：

  test-plan.md          测试计划
  test-cases.md         测试用例
  test-report.md        测试报告
  api-test-record.md    接口测试记录

  这里要记录哪些功能测过、怎么测、输入是什么、预期结果是什么、实际结果是什么。最后提交“测试文档”时主要从这个目录整理。

  简单说：

  api     给前后端联调用
  design  给设计文档和架构说明用
  report  给周报、实现说明、答辩材料用
  test    给测试计划、用例和测试报告用