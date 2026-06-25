# 协议说明

当前 `VoiceProtocolVersion.CURRENT` 为 `2`。UDP 协议负责握手、鉴权、音频帧、状态同步和断开；Minecraft 自定义 payload 负责下发 endpoint/token、同步队伍和 HUD 所需状态。

## VoicePacket 包头

每个 UDP 包使用大端二进制编码：

| 顺序 | 类型 | 字段 |
| --- | --- | --- |
| 1 | int | magic，`MVOC` |
| 2 | int | protocol version |
| 3 | int | packet type ordinal |
| 4 | long | player UUID most bits，服务端状态包为 0 |
| 5 | long | player UUID least bits，服务端状态包为 0 |
| 6 | long | sequence |
| 7 | long | timestampMillis |
| 8 | int | payload length |
| 9 | byte[] | payload |

单个 payload 最大 64 KiB。未知版本、非法长度或无法解码的包必须拒绝。

## 包类型

```text
HELLO
AUTH
AUTH_OK
AUTH_FAILED
VOICE_FRAME
SERVER_STATE
PLAYER_POSITION
PING
PONG
DISCONNECT
ERROR
```

`PLAYER_POSITION` 当前保留给兼容和诊断。生产路由使用带 HMAC 签名的 `SERVER_STATE` 或 Local 模式下同 JVM 的状态同步。

## 鉴权

```text
Minecraft Server -> short-lived HMAC token -> Client
Client -> HELLO -> Voice Server
Client -> AUTH(token) -> Voice Server
Voice Server -> AUTH_OK / AUTH_FAILED -> Client
```

token 包含 `playerUuid`、`issuedAt`、`expiresAt`、`serverId` 和 HMAC-SHA256 signature。`sharedSecret` 只存在于 Minecraft 服务端和 standalone voice server / embedded voice server 配置中，绝不下发给客户端。

音频包必须满足：

1. UDP 源地址和端口与已鉴权 session 一致。
2. 包头 `playerId` 与 token UUID 一致。
3. `VoiceFrame.senderPlayerId` 与包头 UUID 一致。

## VOICE_FRAME payload

`VOICE_FRAME` 的 payload 是一个 encoded audio frame 容器，不要求语音服务器解码或混音。当前有效 codec 仍是 `mock-pcm`，后续 Opus 会替换 `encoded audio` 内容，服务端转发逻辑不变。

| 类型 | 字段 |
| --- | --- |
| long + long | sender UUID |
| long | frame sequence |
| long | timestampMillis |
| int | sampleRate |
| int | channels |
| int | VoiceChannel ordinal |
| int | encoded audio length |
| byte[] | encoded audio |

`sequence` 由客户端递增，接收端按说话者维护 jitter buffer，用于基础乱序重排、迟到包丢弃和丢包跳过。

## VoiceChannel

- `PROXIMITY`：同一维度，发送者和接收者距离不超过 `proximityDistance`。
- `GROUP`：发送者和接收者在同一 MineVOICE group，距离不限制。

客户端选择 channel 只表达意图，最终接收者集合由服务端权威状态决定。

## SERVER_STATE

Remote 模式下，Minecraft 服务端周期性向 standalone voice server 发送签名快照：

```text
generatedAtMillis
players[]
  playerId
  playerName
  dimensionId
  x, y, z
  groupId, groupName
  muted
HMAC-SHA256 signature
```

Local 模式下，同一 JVM 内直接把相同的玩家状态集合写入 embedded voice server 的 session registry，避免自己给自己发 UDP 状态包。

## Minecraft 自定义 Payload

| 方向 | Payload | 用途 |
| --- | --- | --- |
| Server -> Client | `voice_server_info` | 下发模式、endpoint、token、协议版本 |
| Server -> Client | `voice_roster` | 玩家名称、队伍、远端静音状态 |
| Client -> Server | `voice_group_action` | 创建、加入、离开临时语音队伍 |
| Client -> Server | `voice_player_status` | 同步本地麦克风静音状态 |

语音队伍当前是在线会话组：最后一名成员离开后自动删除，不持久化。
