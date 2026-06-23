# 贡献规范

MineVOICE 当前处于项目骨架阶段。贡献应优先保持模块边界清晰、验证路径可复现、变更范围可回滚。

## 基本原则

- 先诊断，再修改。
- 最小改动优先。
- 功能稳定优先。
- 不做无关重构。
- 不做未经确认的大范围格式化。
- 不混合重构和行为变化。

## 提交信息

使用：

```text
<type>(<scope>): <中文描述>
```

示例：

```text
build(repo): 初始化 MineVOICE 多模块项目骨架
feat(common): 添加语音协议数据结构
docs(protocol): 补充 Remote 模式鉴权流程
```

## 验证流程

提交前尽量运行：

```bash
./gradlew projects
./gradlew :common:build
./gradlew :standalone-server:build
./gradlew :client-sim:build
```

如果某个模块因为版本或外部依赖未确定无法验证，需要在提交说明或 PR 描述里写清楚。

## TODO 规则

代码中的 TODO 使用：

```java
// TODO(minevoice): describe the next concrete action.
```

TODO 要说明后续动作，不要写“完善功能”这类空话。复杂设计放到 `docs/`，不要塞进代码注释。
