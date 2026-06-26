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
voiceCodec=opus
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
| `voiceCodec` | 默认 `opus`，使用纯 Java Concentus Opus。`mock` / `mock-pcm` 仍可用于调试和 fallback。 |
| `audioPlaybackBackend` | `auto` / `java-sound` 使用兼容性播放路径；`openal` 使用每位说话者独立 source。支持 EFX 时会额外启用直达声低通和环境混响，不支持时保留普通 OpenAL source。 |
| `spatialBackend` | 历史兼容字段。实际空间参数由客户端 `minevoice-acoustics.properties` 管理。 |
| `enableOcclusion` / `occlusionStrength` / `occlusionLowPass` | 历史兼容字段。当前客户端使用材质化直达声遮挡：不同材料分别影响传输音量和高频保留。 |
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
voiceCodec=opus
audioPlaybackBackend=auto
muted=false
deafened=false
showDebugConnectionInfo=false
debugInfoLevel=OFF
groupActivationMode=PUSH_TO_TALK
groupVoiceActivationThreshold=0.35
hudEnabled=true
nameplateIconsEnabled=true
```

`audioPlaybackBackend` 支持 `auto`、`java-sound`、`openal`。默认 `auto` 会继续使用 Java Sound 稳定路径；选择 `openal` 时客户端会尝试独立 OpenAL context、每位说话者独立 source，以及可选 EFX 滤镜/混响。初始化、EFX 或 source 写入失败会回落到 Java Sound。

## 声学材质表

文件：`config/minevoice-acoustics.properties`。客户端首次进入世界时自动生成；编辑后最多约一秒生效，不需要重启游戏。

```properties
enabled=true
sourceRefreshIntervalMs=100
environmentRefreshIntervalMs=400
probeDistance=18.0
probeCount=12
maxOcclusionSamples=96
reflectionStrength=0.85
debugRenderRays=false

# transmissionGain,highFrequencyGain,reflectivity
material.stone=0.72,0.82,0.82
material.wood=0.62,0.55,0.58
material.metal=0.82,0.94,0.93
material.glass=0.76,0.88,0.72
material.wool=0.18,0.12,0.08
material.soil=0.38,0.28,0.22
material.snow=0.24,0.10,0.12

# 指定方块可覆盖其默认 SoundType 材质分类
block.minecraft.obsidian=stone
block.example.acoustic_foam=wool
```

`transmissionGain` 控制穿过一层方块后的直达声音量，`highFrequencyGain` 控制高频保留，`reflectivity` 控制该材质参与一阶反射和环境混响的强度。`probeCount`、`maxOcclusionSamples` 越高，空间细节越多但客户端 CPU 开销也越高。`debugRenderRays=true` 会在世界内绘制青色环境探针、红/绿直达路径和橙色反射路径，适合调试，正常游玩请保持关闭。
