# AGENTS.md

## Repository Purpose

MineVOICE 是面向 Minecraft 的双模式 3D 语音基础设施。仓库当前目标是维护清晰的多模块骨架、协议边界和验证流程，不在初始化阶段实现完整生产功能。

## Basic Principles

1. 先诊断，再修改。
2. 最小改动优先。
3. 功能稳定优先。
4. 不做无关重构。
5. 不做未经确认的大范围格式化。
6. 每个模块保持单一职责。
7. 不要混合重构和行为变化。
8. 不要为了看起来干净删除尚未验证的 TODO 或 workaround。

## Repository Boundaries

| 模块 | 允许职责 | 禁止混入 |
| --- | --- | --- |
| `common` | 协议、配置、接口、通用工具 | Minecraft 事件、真实 UDP 服务、GUI |
| `minecraft-neoforge` | 客户端/服务端模组骨架、自动下发连接信息 | 独立语音服务器部署细节、完整音频实现 |
| `standalone-server` | 独立语音服务器、UDP 骨架、鉴权入口、指标 | Minecraft API、客户端 GUI |
| `client-sim` | 模拟客户端、假音频帧、后续压测入口 | Minecraft 依赖 |
| `docs` | 架构、协议、配置、部署和开发说明 | 未验证的生产承诺 |

## Module Rules

- `common` 只能放共享协议、配置、接口和工具。
- 不要把音频采集、UDP 转发、Minecraft 事件和空间音效写进一个类。
- Local 和 Remote 模式要保持边界清晰。
- 自动下发连接信息是主路径，客户端手动配置不是主路径。
- 公共接口写 JavaDoc，普通实现类只保留有后续动作的 TODO。

## Commit Style

提交信息使用：

```text
<type>(<scope>): <中文描述>
```

可用 type：`feat`、`fix`、`docs`、`refactor`、`cleanup`、`test`、`build`、`ci`、`chore`。

常用 scope：`repo`、`common`、`protocol`、`auth`、`audio`、`spatial`、`neoforge`、`standalone`、`client`、`config`、`docs`、`docker`、`ci`。

一个提交只做一类事情，不要混合功能修改、重构和文档整理。

## Safety Rules

- 不要提交 secrets、tokens、构建产物、运行时缓存。
- 不要提交个人配置、敏感日志、cookie、session、API key。
- 不要更新 lock 文件，除非依赖更新是任务目标。
- 不要读取或依赖仓库外的私人项目风格；本仓库规则以本文档为准。

## Validation Rules

优先运行：

```bash
./gradlew projects
./gradlew :common:build
./gradlew :standalone-server:build
./gradlew :client-sim:build
```

如果 NeoForge 版本未选定，不要假装完成完整 NeoForge 验证。报告已运行命令、结果、跳过原因和剩余风险。

## Agent Output Requirements

完成后报告：

1. changed files
2. behavior changed or not
3. validation commands run
4. risks and rollback notes
5. next TODO
6. suggested commit message
