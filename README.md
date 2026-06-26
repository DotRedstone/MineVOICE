# MineVOICE

MineVOICE 是面向 Minecraft 1.21.1 / NeoForge 的双模式 3D 语音基础设施。它的主路径不是让玩家手动填写语音服务器，而是由 Minecraft 服务端下发 endpoint 和短期 token，玩家进服后自动完成语音连接。

当前公开版本为 [v0.1.0-alpha.3](https://github.com/DotRedstone/MineVOICE/releases/tag/v0.1.0-alpha.3)。当前开发分支继续推进 Local 内置语音服务端、基础 jitter buffer 和 Local/LAN 测试脚本。Alpha 版本仍不建议用于高人数生产服。

## 部署包

| 文件 | 用途 |
| --- | --- |
| `MineVOICE-NeoForge-1.21.1-<版本>.jar` | 同一个 NeoForge 模组文件，客户端和 Minecraft 服务端都安装。Local 模式只需要这个文件。 |
| `MineVOICE-Voice-Server-<版本>.zip` | Remote 模式使用的独立 UDP 语音服务器核心。 |

## 模式选择

| 模式 | 适合场景 | 状态 |
| --- | --- | --- |
| Local | 小服、朋友服、本机双客户端、Open to LAN | 初步可用，仍需真实多人验收 |
| Remote | 多人服、独立节点、Docker、低带宽游戏服 | 可用，仍是推荐稳定测试路径 |

Local 模式由 Minecraft 服务端 JVM 内启动 embedded UDP voice server；Remote 模式由独立 `standalone-server` 进程负责 UDP 鉴权和转发。两种模式都继续使用服务端下发 token，`sharedSecret` 不会下发给客户端。

## 当前能力

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| 自动下发 endpoint/token | 可用 | Minecraft 服务端给每个玩家下发短期 HMAC token。 |
| Remote 独立语音服务器 | 可用 | 独立 UDP 服务端继续可用。 |
| Local 内置语音服务端 | 可用 | MC 服务端 `mode=local` 时自动启动 UDP 服务，停服释放端口。 |
| Open to LAN 语音 | 可用 | LAN host 可复用 Local 配置；多网卡场景建议手动指定 `localVoiceAdvertiseHost`。 |
| 范围语音 / 队伍语音 | 可用 | 范围语音按维度和距离路由，队伍语音不受距离限制。 |
| 游戏内设备设置和 HUD | 可用 | 支持设备选择、音频测试、环境音效开关，以及动态 3D-to-2D 屏幕投影玩家头像与屏幕边缘方位指示器。 |
| 智能语音唤醒 (VAD) | 可用 | 允许玩家调节独立的公共频道/队伍频道触发阈值，过滤静音底噪，避免发送无用包。 |
| 基础立体声方向感 | 可用 | Java Sound fallback 基于相对位置做左右声道 pan。 |
| jitter buffer | 可用 | 接收端按说话者和 sequence 做基础乱序重排与迟到包丢弃。 |
| Opus | 可用 | 默认使用纯 Java Concentus Opus；服务端只转发 encoded frame，初始化失败可回退 `mock-pcm`。 |
| OpenAL 位置音源 | 可用 | 支持硬核 3D 空间音效（环境距离衰减、方向衰减）。 |
| 基础遮挡 / 低通 | planned | 待接入完整的声学阻隔折算。 |
| Sound Physics Remastered 兼容 | planned | 只作为 optional compat 设计方向，不是硬依赖。 |

## 快速配置

Local 小服：

```properties
mode=local
localVoiceBindHost=0.0.0.0
localVoiceBindPort=24454
localVoiceAdvertiseHost=auto
localVoiceAdvertisePort=24454
enableLanVoiceServer=true
sharedSecret=replace-with-a-long-random-secret
proximityDistance=48
enableDebugLog=false
```

Remote 多人服：

```properties
mode=remote
remoteVoiceHost=voice.example.com
remoteVoicePort=24454
sharedSecret=replace-with-a-long-random-secret
enableDebugLog=false
```

Remote 需要放行 UDP `24454`。`remoteVoiceHost` 必须是玩家客户端可访问的地址，不要填 Docker 容器名、`127.0.0.1` 或仅服务端内部可见的地址。

## 游戏内操作

| 操作 | 默认按键 |
| --- | --- |
| 打开 MineVOICE 面板 | `O` |
| 范围语音 | 按住 `V` |
| 队伍语音 | 按住 `G` |

按键可在 Minecraft 原生按键设置里的 MineVOICE 分类修改。

## 本地测试

```powershell
.\scripts\start-local-demo.ps1
.\scripts\start-remote-demo.ps1
.\scripts\start-lan-demo.ps1
```

Local demo 会启动 Minecraft 服务端和两个客户端，不需要独立语音服务器窗口。Remote demo 会额外启动 standalone voice server。

## 文档

- [配置说明](docs/configuration.md)
- [部署说明](docs/deployment.md)
- [Demo 验证](docs/demo.md)
- [协议说明](docs/protocol.md)
- [开发说明](docs/development.md)
- [算法接口](docs/algorithm-api.md)
- [路线图](docs/roadmap.md)
- [TODO](TODO.md)

## Alpha 限制

请不要把 Alpha 版本直接用于高人数生产服。当前重点是验证自动连接、UDP 转发、范围/队伍路由、设备设置、Local/Remote 双模式、Opus 编码链路和基础网络稳定性。OpenAL、降噪、真实遮挡和 Sound Physics optional compat 还没有完成生产级实现。
