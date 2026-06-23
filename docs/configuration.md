# 配置说明

## Minecraft 服务端

文件：config/minevoice-server.properties

Remote 模式示例：

~~~
mode=remote
remoteVoiceHost=voice.example.com
remoteVoicePort=24454
sharedSecret=replace-with-a-long-random-secret
enableDebugLog=false
~~~

| 配置 | 说明 |
| --- | --- |
| mode | 当前版本必须为 `remote`；`local` 是内置语音服务器的预留模式，尚不可用于联机。 |
| remoteVoiceHost | 必须是玩家客户端可访问的公开地址，不是服务端内部地址 |
| remoteVoicePort | 独立服务端 UDP 端口 |
| sharedSecret | 与独立服务端完全一致的 HMAC 密钥，不得给玩家 |
| enableDebugLog | 输出握手与状态同步诊断日志 |

## 独立语音服务端

文件：minevoice-standalone.properties

~~~
bindHost=0.0.0.0
bindPort=24454
sharedSecret=replace-with-a-long-random-secret
maxPlayers=100
proximityDistance=48
enableBandwidthStats=true
enableDebugLog=false
~~~

| 配置 | 说明 |
| --- | --- |
| bindHost / bindPort | 实际 UDP 监听地址 |
| sharedSecret | 必须与 Minecraft 服务端一致 |
| maxPlayers | 预留容量参数 |
| proximityDistance | 非组队 PROXIMITY 语音最大距离，单位为方块 |
| enableBandwidthStats | 保留流量统计 |
| enableDebugLog | 输出帧转发和服务端状态快照日志 |

可用环境变量覆盖：MINEVOICE_BIND_HOST、MINEVOICE_BIND_PORT、MINEVOICE_SHARED_SECRET、MINEVOICE_MAX_PLAYERS、MINEVOICE_PROXIMITY_DISTANCE、MINEVOICE_ENABLE_BANDWIDTH_STATS、MINEVOICE_ENABLE_DEBUG_LOG。

## 客户端

文件：config/minevoice-client.properties。普通玩家不需要填写语音服务端地址或 token。

~~~
microphoneDevice=default
outputDevice=default
pushToTalkKey=V
activationMode=PUSH_TO_TALK
voiceActivationThreshold=0.35
masterVolume=1.0
voiceChatVolume=1.0
microphoneVolume=1.0
muted=false
deafened=false
~~~

游戏内按 O 打开 MineVOICE 主面板；齿轮进入详细设置。系统默认交由 Java Sound 使用操作系统默认设备，手动设备保存稳定 mixer ID。耳机插拔后可回到系统默认，或显式选择新设备并使用测试输入/测试输出验证。
