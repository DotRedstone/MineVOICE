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

当前 Opus 路径使用 `io.github.jaredmdobson:concentus`，这是纯 Java Opus 实现，优先避免 native libopus 在 Minecraft 客户端分发时带来的平台加载问题。NeoForge 发行 jar 通过 jarJar 携带该依赖；standalone 和 client-sim 通过 Gradle application 分发传递依赖。

实现边界：

1. `VoiceCodecFactory` 默认选择 `opus`，初始化失败时回退 `mock-pcm-fallback`。
2. 语音服务器只转发 encoded frame，不解码、不混音。
3. Minecraft 服务端通过 `voice_server_info` 下发 `voiceCodec`，客户端以服务端值为准，避免本地配置不一致。
4. Opus decoder 是有状态对象，客户端播放端按远端说话者单独维护 decoder。

后续仍需要补真实双客户端带宽对比、Opus 初始化失败提示和更细的 codec debug 统计。

## OpenAL 策略

Minecraft 本身使用 OpenAL。后续接入时必须避免破坏 MC 声音上下文。当前生产路径保留 Java Sound fallback，OpenAL 先按 backend 抽象和 per-speaker source 生命周期实现，不应一次性替换全部播放链路。

## 测试说明

`common:test` 在 Windows Unicode 工作目录下按现有配置跳过，CI Linux 会运行。新增协议、codec、jitter、空间纯 Java 逻辑时应优先放在 `common/src/test`。
