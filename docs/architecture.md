# 架构说明

## 架构概览

| 组件 | 运行位置 | 核心职责 |
| --- | --- | --- |
| Client Mod | 玩家客户端 | 自动连接语音服务器、音频采集播放、空间音效 |
| Minecraft Server Mod | 游戏服务器 | 玩家身份、配置、位置同步、token 下发 |
| Standalone Voice Server | 独立节点或 Remote 部署 | UDP 转发、会话管理、鉴权、带宽统计 |
| common | 编译期共享模块 | 协议、配置、接口、通用工具 |

## 职责表

| 职责 | Client Mod | MC Server Mod | Standalone Voice Server |
| --- | --- | --- | --- |
| 玩家身份来源 | 否 | 是 | 验证 token |
| 语音服务器地址下发 | 接收 | 是 | 否 |
| UDP 音频收发 | 是 | Local 模式预留 | 是 |
| 位置同步 | 接收/上报 | 是 | 使用同步结果筛选 |
| 空间音效 | 是 | 否 | 否 |
| 带宽统计 | 可显示 | 可查询 | 是 |

## Local / Remote 对比

| 项目 | Local | Remote |
| --- | --- | --- |
| 语音服务器位置 | MC 服务端模组启动的内置实例 | 独立机器、独立 jar、二进制或 Docker |
| 适用场景 | 小服、简单部署 | 多人服、低带宽游戏服、独立语音节点 |
| 玩家配置 | 自动下发 | 自动下发 |
| sharedSecret | 服务端内部使用 | MC 服务端和独立语音服务器共享 |

## 为什么语音流量要与游戏服务器解耦

Minecraft 服务端的主要职责是世界状态、玩家状态和游戏逻辑。语音流量具有高频、实时、UDP 优先的特点，把它与游戏逻辑解耦可以降低主服带宽压力，也能让 Remote 模式迁移到更合适的网络节点。

## 为什么服务器只负责转发

服务端做复杂混音会增加 CPU 压力，也会让空间音效与玩家本地设置耦合。MineVOICE 的设计是服务端负责鉴权、筛选和转发，客户端根据位置、音量设置和输出设备计算最终播放效果。

## bindHost 与 advertiseHost

| 配置 | 含义 |
| --- | --- |
| `bindHost` / `bindPort` | 服务端实际监听地址和端口 |
| `advertiseHost` / `advertisePort` | 下发给客户端连接的地址和端口 |

面板服、容器和 NAT 环境中，监听地址通常是 `0.0.0.0`，但客户端需要连接公网域名或公网 IP，因此两组配置必须分开。

## 模块边界规则

- `common` 不依赖 Minecraft、NeoForge 或真实 UDP 实现。
- `minecraft-neoforge` 不包含独立部署逻辑。
- `standalone-server` 不依赖 Minecraft API。
- `client-sim` 不接入 Minecraft，只用于协议和负载模拟。
- Local 与 Remote 的差异应在配置和启动路径表达，不要互相污染。

## 真实 Payload 注册与连接信息下发流程

为了让客户端能够连接到正确的语音服务器并完成鉴权，我们需要通过 Minecraft 原生的 Custom Payload 机制下发连接信息。

### 1. Payload 注册
在 `minecraft-neoforge` 模块的 `MineVoiceMod` 初始化阶段，我们注册了一个双向的自定义 Payload `VoiceServerInfoPayload`。
- 该 Payload 包含了 `voiceHost`、`voicePort`、`protocolVersion` 以及临时的 `token`。
- 它被注册为 `Configuration` 或 `Play` 阶段的合法数据包。

### 2. 玩家加入时的下发流程
1. **玩家登录**：当玩家成功连接到 Minecraft 服务端并触发 `PlayerEvent.PlayerLoggedInEvent` 时，服务端截获该事件。
2. **生成 Token**：服务端利用配置中的 `sharedSecret`、玩家的 UUID、服务端生成的唯一 Server ID 以及当前时间，通过 HMAC-SHA256 算法签名生成一个带有过期时间的临时 `AuthToken`。
3. **构造 Payload**：服务端根据当前是 Local 还是 Remote 模式，读取对应的 `advertiseHost` 和 `advertisePort`，连同生成的 Token 一并封装进 `VoiceServerInfoPayload`。
4. **网络发送**：通过 NeoForge 的 `PacketDistributor` 将该 Payload 仅发送给触发登录事件的这位玩家。
5. **客户端接收**：玩家客户端收到 Payload 后，解析出 IP、端口和 Token，随后启动后台的 UDP `ClientVoiceThread`，向目标语音服务器发送 `HELLO` 和 `AUTH` 握手包。
