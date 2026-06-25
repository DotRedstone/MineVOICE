# 开发说明

## 模块边界

| 模块 | 职责 |
| --- | --- |
| `common` | 协议、配置、codec/jitter/spatial 接口和纯 Java 工具 |
| `standalone-server` | 独立 UDP voice server、session、relay、auth、发行入口 |
| `minecraft-neoforge` | NeoForge 客户端/服务端集成、自动下发连接信息、UI、Java Sound 客户端 |
| `client-sim` | 假客户端和压测入口 |

Local 模式目前通过 NeoForge 模块复用 standalone 的 UDP/session/relay/auth 代码路径，避免维护两套转发实现。

## 常用命令

```powershell
.\gradlew.bat projects
.\gradlew.bat :common:build
.\gradlew.bat :standalone-server:build
.\gradlew.bat :client-sim:build
.\gradlew.bat :minecraft-neoforge:build
```

开发客户端：

```powershell
.\gradlew.bat :minecraft-neoforge:runClient
```

Local/Remote/LAN demo：

```powershell
.\scripts\start-local-demo.ps1
.\scripts\start-remote-demo.ps1
.\scripts\start-lan-demo.ps1
```

## Opus 依赖策略

当前没有把第三方 Opus 库打进客户端。原因：

1. Java 21 + Minecraft 客户端分发需要确认跨平台 native 或纯 Java 方案。
2. 许可证、体积、加载失败 fallback 都需要单独验收。
3. 语音服务器只转发 encoded frame，codec 可以在客户端侧无痛替换。

因此当前实现保留 `VoiceCodec` / `VoiceCodecFactory` / `VoiceAudioFormat`，有效 codec 是 `mock-pcm`。接入 Opus 时要先补依赖说明、fallback、带宽对比和双客户端验收。

## OpenAL 策略

Minecraft 本身使用 OpenAL。后续接入时必须避免破坏 MC 声音上下文。当前生产路径保留 Java Sound fallback，OpenAL 先按 backend 抽象和 per-speaker source 生命周期实现，不应一次性替换全部播放链路。

## 测试说明

`common:test` 在 Windows Unicode 工作目录下按现有配置跳过，CI Linux 会运行。新增协议、codec、jitter、空间纯 Java 逻辑时应优先放在 `common/src/test`。
