# Backend Build ACL Failure

## 复查状态

已解决（2026-07-06）。在获得沙箱外 Maven 执行权限后，`mvn clean -DskipTests package` 成功重建 `backend/target`；阶段 2A 独立测试随后以 15 个测试、0 失败通过。后续阶段的 `mvn -DskipTests package` 也持续成功。本记录保留用于说明环境故障及处置过程，不再构成阻塞。

## 问题描述

阶段 2A 的后端编译与测试无法启动。Maven 在复制资源到 `backend/target/classes` 时因 Windows ACL 拒绝访问而失败，尚未进入 Java 编译。

## 错误日志

```text
Failed to execute goal maven-resources-plugin:resources
filtering backend/src/main/resources/application-dev.yml to
backend/target/classes/application-dev.yml failed with FileNotFoundException:
backend/target/classes/application-dev.yml (拒绝访问)
```

## 已尝试方案

1. 主 Agent 执行 `mvn -DskipTests package`：资源复制阶段失败。
2. 后端测试 Agent 执行 `mvn -DskipTests compile`：相同 ACL 错误。
3. 请求沙箱外执行 `mvn clean -DskipTests package`：审批因当前工具额度限制被系统拒绝。

## 推测原因

基线测试 Agent 在隔离身份下生成了 `backend/target`，目录或文件 ACL 与当前执行身份不兼容，导致后续 Maven 无法覆盖资源文件。

## 后续建议

1. 在权限允许时删除生成目录 `backend/target`，或恢复当前用户对该目录的完全控制。
2. 重新执行 `mvn clean test`。
3. 补充阶段 2A 的认证、Thread 权限、DTO、BCC 与重复收件人测试。
4. 在测试成功前不要提交阶段 2A 的后端生产改动。
