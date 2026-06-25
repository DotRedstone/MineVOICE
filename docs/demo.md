# Demo 验证

## Remote 本地双客户端

```powershell
.\scripts\start-remote-demo.ps1
```

脚本会启动独立语音服务器、Minecraft 开发服务端、客户端 A、客户端 B。两个客户端连接：

```text
127.0.0.1:25565
```

验证：

1. 两个客户端按 `O` 打开 MineVOICE 面板，状态应为已连接。
2. A/B 相距 48 方块内，按住 `V` 可听到范围语音。
3. 超过 48 方块后，范围语音不应继续转发。
4. 创建同一语音队伍后，按住 `G` 的队伍语音不受距离限制。

## Local 内置服务端

```powershell
.\scripts\start-local-demo.ps1
```

脚本只启动 Minecraft 服务端和两个客户端，不启动 standalone voice server。服务端 `mode=local` 后会在同一 JVM 内绑定 UDP `24454`。

服务端日志应包含：

```text
MineVOICE local voice server starting on 0.0.0.0:24454
MineVOICE local voice endpoint advertised as <host>:24454
```

验证流程同 Remote demo。额外检查：关闭 Minecraft 服务端后 UDP `24454` 应释放。

## Open to LAN

```powershell
.\scripts\start-lan-demo.ps1
```

1. 客户端 A 创建单人世界并点击 Open to LAN。
2. 客户端 B 从多人游戏中的 LAN 世界加入。
3. 两端 MineVOICE 面板应显示已连接。
4. 测试 `V` 范围语音和 `G` 队伍语音。

如果使用 ZeroTier、Radmin VPN 或 Tailscale，请把 host 的 `localVoiceAdvertiseHost` 改成对应虚拟网卡 IP。

## 空间方向测试

1. B 站在 A 左侧，A 应听到偏左。
2. B 站在 A 右侧，A 应听到偏右。
3. B 在 A 正前和正后，左右应接近均衡。
4. B 从左到右绕 A，pan 应平滑变化。

当前空间化是 Java Sound 左右声道 pan，不是真正 OpenAL 位置音源。

## Debug 查看

- 客户端：`showDebugConnectionInfo=true` 会显示连接状态提示。
- 服务端：`enableDebugLog=true` 会输出 token、状态同步、UDP 转发等调试日志。
- codec：默认 `opus`；服务端通过 `voice_server_info` 下发 codec，客户端 debug verbose 会显示当前 codec。
- jitter：播放端已有基础 buffer，后续会把 stats 暴露到 HUD 或命令。
