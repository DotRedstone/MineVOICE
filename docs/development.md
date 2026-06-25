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

## client-sim 压测

`client-sim` 会生成 20ms / 48 kHz / mono PCM，再按指定 codec 编成 `VOICE_FRAME`。默认 codec 是 `opus`，也可以传 `mock` / `mock-pcm` 做裸 PCM 对比。

```powershell
.\gradlew.bat :client-sim:run --args="5 127.0.0.1 24454 change-me 10 3 PROXIMITY opus"
```

参数顺序：

```text
clients host port sharedSecret framesPerClient playerSpacing channel codec packetLossRate reorderWindow
```

输出里的 `udpSentBytes` / `udpReceivedBytes` 是完整 UDP packet 编码后的字节数，`voicePayloadSentBytes` / `voicePayloadReceivedBytes` 是 encoded audio payload 字节数。`pcmBytesPerFrame=1920` 表示 Opus 编码前每帧 PCM 大小，可用来估算 Opus 压缩效果。

`packetLossRate` 可传 `0.1` 或 `10` 表示 10% 模拟丢帧；`reorderWindow=3` 表示每 3 帧内做一次乱序发送，用于压 jitter buffer。

## 音频 DSP 接口

麦克风采集后、codec 编码前会调用 `AudioCaptureProcessor`。当前默认是 `NoopAudioCaptureProcessor`，不改变 PCM；后续降噪、AEC 或自动增益可以在独立算法模块中实现该接口。实现必须足够快，不能阻塞音频采集线程。

## OpenAL 和空间策略

Minecraft 本身使用 OpenAL。后续接入时必须避免破坏 MC 声音上下文。当前生产路径保留 Java Sound fallback，代码已拆出 `VoicePlaybackBackend`、`JavaSoundVoicePlaybackBackend`、`OpenAlVoicePlaybackBackend` 和 listener/source provider。OpenAL class 探测使用反射，不额外引入 native 依赖；真正的 per-speaker source 生命周期仍是下一步。

当前 Java Sound 空间路径已包含基础 pan、距离衰减、遮挡降音量和轻量低通。遮挡采样发生在客户端 tick 的玩家位置刷新中，播放线程只读取快照，避免在音频线程扫方块。Sound Physics Remastered 只做安装探测和 fallback 标记，不调用第三方内部 API。

## 测试说明

`common:test` 在 Windows Unicode 工作目录下会把测试 classpath 同步到临时 ASCII 路径后执行，CI Linux 也会运行。新增协议、codec、jitter、空间纯 Java 逻辑时应优先放在 `common/src/test`。
