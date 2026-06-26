# MineVOICE 手测回归清单

本清单用于 Alpha release 前的真实客户端验收。自动构建、client-sim 和脚本压测只能证明协议和链路可跑，以下项目必须进游戏验证。

## Local 双客户端

1. 运行 `.\scripts\start-local-demo.ps1`。
2. 两个客户端加入 `127.0.0.1:25565`。
3. 验证自动连接、范围语音、队伍语音、静音、屏蔽听音、断线重连。
4. 停服后确认 UDP `24454` 释放；重启服务端确认 Local voice server 可再次启动。

## Remote 双客户端

1. 运行 `.\scripts\start-remote-demo.ps1`。
2. 两个客户端加入开发服务端。
3. 验证 standalone voice server 日志里没有 auth failed、identity mismatch 或 token parse failed。
4. 在游戏内执行 `/minevoice status` 和 `/minevoice test-endpoint`，记录输出。

## Open to LAN

1. 运行 `.\scripts\start-lan-demo.ps1`。
2. 客户端 A 开单人世界并 Open to LAN。
3. 客户端 B 从 LAN 世界加入。
4. 如果多网卡或虚拟组网失败，把 A 的 `localVoiceAdvertiseHost` 改成目标网卡 IP 后重试。

## Opus 听感和 CPU

1. 默认 `voiceCodec=opus` 测试范围语音和队伍语音。
2. 用 `.\scripts\compare-codec-bandwidth.ps1 -OutputPath "$env:TEMP\minevoice-bandwidth.txt"` 记录 Opus/mock-pcm 字节数。
3. 观察两个客户端 CPU 占用，记录是否有爆音、延迟明显升高或掉包。

## 空间方向和遮挡

1. B 分别站在 A 的左、右、前、后，确认左右声像不反。
2. B 绕 A 移动，确认 pan 变化平滑。
3. B 站到墙后，确认声音有降低和变闷效果。
4. 在设置页 Debug tab 记录 spatial 摘要中的 `distance`、`pan`、`gain`、`occlusion`。

## 设备切换

1. 客户端保持 `microphoneDevice=default`、`outputDevice=default`。
2. 进游戏后插拔耳机，切换 Windows 默认麦克风和默认扬声器。
3. 确认 MineVOICE 会自动重开采集/播放线；若失败，打开设置页重新测试输入/输出设备并记录日志。

## OpenAL 可选后端

1. 设置页语音页把播放后端切到 `openal`。
2. 重连语音后验证 Debug tab 的 `playback=openal`。
3. 测试多人同屏说话、玩家移动、离开游戏、切回 Java Sound。
4. 如果 OpenAL 初始化失败，确认 Debug tab 回落到 `java-sound` 且客户端没有崩溃。
