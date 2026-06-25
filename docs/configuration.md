# 配置说明

## Minecraft 服务端配置

文件：`config/minevoice-server.properties`

### Local 内置语音服务端

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
enableSpatialDebug=false
```

| 配置 | 说明 |
| --- | --- |
| `mode` | `local` 时由 Minecraft 服务端模组在同一 JVM 内启动 UDP voice server。 |
| `localVoiceBindHost` / `localVoiceBindPort` | 实际监听地址和端口。服务端常用 `0.0.0.0:24454`。 |
| `localVoiceAdvertiseHost` / `localVoiceAdvertisePort` | 下发给客户端连接的地址和端口。`auto` 会优先选择可访问的非 loopback 地址。 |
| `enableLanVoiceServer` | 为 Open to LAN 场景保留的开关；当前 Local 路径已可在 integrated server 中使用。 |
| `sharedSecret` | 服务端本地签发 token 使用的密钥，不下发给客户端。 |
| `proximityDistance` | 范围语音最大距离，单位为方块。 |
| `enableSpatialDebug` | 预留空间音效调试开关，默认关闭，避免刷屏。 |

兼容旧字段：`bindHost`、`bindPort`、`advertiseHost`、`advertisePort` 仍会被读取，但新配置请使用 `localVoice*`。

### Remote 独立语音服务端

```properties
mode=remote
remoteVoiceHost=voice.example.com
remoteVoicePort=24454
sharedSecret=replace-with-a-long-random-secret
enableDebugLog=false
```

| 配置 | 说明 |
| --- | --- |
| `remoteVoiceHost` | 玩家客户端能访问的公网、内网或组网地址。不要填 Docker 容器名或仅服务端可见的地址。 |
| `remoteVoicePort` | 独立语音服务器 UDP 端口。 |
| `sharedSecret` | 必须与 standalone voice server 完全一致。 |

## 音频和空间配置

```properties
voiceCodec=mock
audioPlaybackBackend=auto
spatialBackend=auto
enableOcclusion=true
occlusionStrength=0.6
occlusionLowPass=true
enableSoundPhysicsCompat=true
jitterBufferMs=60
```

| 配置 | 当前状态 |
| --- | --- |
| `voiceCodec` | 当前可用值是 `mock`。`opus` 已作为配置目标保留，但还没有真实 Opus 实现。 |
| `audioPlaybackBackend` | 当前实际使用 Java Sound；`openal` 是后续 backend 目标。 |
| `spatialBackend` | 当前为基础 pan 空间化；OpenAL/Sound Physics 仍是 planned。 |
| `enableOcclusion` / `occlusionStrength` / `occlusionLowPass` | 配置位已保留，真实方块遮挡仍待实现。 |
| `jitterBufferMs` | 文档口径为 60ms；当前 Java Sound 播放端使用 3 帧左右的基础 jitter buffer。 |

## 独立语音服务器配置

文件：`minevoice-standalone.properties`

```properties
bindHost=0.0.0.0
bindPort=24454
sharedSecret=replace-with-a-long-random-secret
maxPlayers=100
proximityDistance=48
enableBandwidthStats=true
enableDebugLog=false
```

环境变量可覆盖：`MINEVOICE_BIND_HOST`、`MINEVOICE_BIND_PORT`、`MINEVOICE_SHARED_SECRET`、`MINEVOICE_MAX_PLAYERS`、`MINEVOICE_PROXIMITY_DISTANCE`、`MINEVOICE_ENABLE_BANDWIDTH_STATS`、`MINEVOICE_ENABLE_DEBUG_LOG`。

## 客户端配置

文件：`config/minevoice-client.properties`

普通玩家不需要填写语音服务器地址或 token。客户端只保存设备、音量、按键和调试偏好。

```properties
microphoneDevice=default
outputDevice=default
pushToTalkKey=V
activationMode=PUSH_TO_TALK
voiceActivationThreshold=0.35
masterVolume=1.0
voiceChatVolume=1.0
microphoneVolume=1.0
spatialAudioEnabled=true
voiceCodec=mock
muted=false
deafened=false
showDebugConnectionInfo=false
```
