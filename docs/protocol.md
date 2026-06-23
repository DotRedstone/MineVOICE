# MineVOICE 协议

## 版本与边界

当前 VoiceProtocolVersion.CURRENT 为 2。v2 增加语音通道和经过 HMAC 签名的 Minecraft 服务端状态快照。

UDP 协议负责低延迟握手、认证、音频帧和服务端状态同步。Minecraft 自定义 payload 负责客户端 UI 所需的组队、闭麦与名册状态。客户端坐标不作为范围语音的可信来源。

## 固定包头

每个 VoicePacket 使用大端二进制编码：

| 顺序 | 类型 | 字段 |
| --- | --- | --- |
| 1 | int | magic，MVOC |
| 2 | int | protocol version |
| 3 | int | packet type ordinal |
| 4 | long | player UUID most bits，服务端状态包为 0 |
| 5 | long | player UUID least bits，服务端状态包为 0 |
| 6 | long | sequence |
| 7 | long | timestampMillis |
| 8 | int | payload length |
| 9 | byte[] | payload |

单个 payload 最大 64 KiB。未知版本或非法长度必须拒绝，不得猜测字段布局。

## 包类型

~~~
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
~~~

PLAYER_POSITION 保留给兼容与诊断；当前生产路由使用签名的 SERVER_STATE。

## 认证流程

~~~
Minecraft Server -> 临时 HMAC token -> Client
Client -> HELLO -> Voice Server
Client -> AUTH(token) -> Voice Server
Voice Server -> AUTH_OK / AUTH_FAILED -> Client
~~~

临时 token 包含 playerUuid、issuedAt、expiresAt、serverId 和 signature。签名算法为 HMAC-SHA256。共享密钥只存在于 Minecraft 服务端和独立语音服务端，绝不下发给客户端。

客户端音频帧必须同时满足：

1. UDP 源地址和端口与已认证会话一致。
2. 包头 playerId 与 token UUID 一致。
3. VoiceFrame.senderPlayerId 与包头 UUID 一致。

## 音频帧

VOICE_FRAME payload 的字段顺序：

| 类型 | 字段 |
| --- | --- |
| long + long | sender UUID |
| long | sequence |
| long | timestampMillis |
| int | sampleRate |
| int | channels |
| int | VoiceChannel ordinal |
| int | encoded audio length |
| byte[] | encoded audio |

VoiceChannel：

- PROXIMITY：同一维度，发送者与接收者的平方距离不超过 proximityDistance²。
- GROUP：发送者与接收者拥有相同的 MineVOICE group UUID，距离不限制。

范围和组队资格由 VoiceRelayService 最终判定。客户端选择通道只表达意图，不能扩大接收者集合。

## 服务端状态快照

Minecraft 服务端每 20 tick，并在玩家登录、退出、组队或闭麦变更后发送 SERVER_STATE。该 payload 包含：

~~~
generatedAtMillis
players[]
  playerId
  playerName
  dimensionId
  x, y, z
  groupId, groupName
  muted
HMAC-SHA256 signature
~~~

独立语音服务端只接受签名正确的快照，并使用最新快照决定 PROXIMITY / GROUP 接收者。没有可用状态的玩家不会收到或转发音频，避免退回到广播给所有人的不安全行为。

## Minecraft 自定义 Payload

| 方向 | Payload | 用途 |
| --- | --- | --- |
| Server -> Client | voice_server_info | 下发端点、token、协议版本 |
| Server -> Client | voice_roster | 玩家名称、队伍、远端闭麦状态，供 HUD/名牌使用 |
| Client -> Server | voice_group_action | 创建、加入、离开临时语音队伍 |
| Client -> Server | voice_player_status | 同步本人的麦克风静音状态 |

语音队伍当前是在线会话组：最后一名成员离开后自动删除；不持久化、不含密码。密码组和持久化组应在新的 payload 版本中增加，不能复用未定义字段。

## 算法扩展

方块样本、遮挡、降噪和 3D 处理的稳定接口见 [算法接口](algorithm-api.md)。音频帧与世界上下文使用不同同步节奏，避免把方块数据塞进每个 20 ms 的 UDP 音频包。
