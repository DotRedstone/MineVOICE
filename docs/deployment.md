# 部署说明

## 当前支持的部署方式

v0.1.0-alpha.2 仅支持 **Remote 模式**：Minecraft 服务端和独立语音服务器分别运行。客户端和 Minecraft 服务端安装同一个 NeoForge 模组；独立语音服务器负责 UDP 鉴权与音频转发。

Local 内置语音服务器尚未实现。不要把 `mode` 设置为 `local` 后用于联机测试。

## 准备文件

从 [Release](https://github.com/DotRedstone/MineVOICE/releases) 下载：

1. `MineVOICE-NeoForge-1.21.1-<版本>.jar`
2. `MineVOICE-Voice-Server-<版本>.zip`

将 jar 放进 Minecraft 服务端的 `mods` 目录，并要求每位玩家在客户端 `mods` 目录安装相同版本。解压 zip 到一台玩家可访问的机器，Minecraft 服务端与语音服务器可以是同一台主机，也可以是两台主机。

## 配置独立语音服务器

编辑解压目录根部的 `minevoice-standalone.properties`：

```properties
bindHost=0.0.0.0
bindPort=24454
sharedSecret=replace-with-a-long-random-secret
maxPlayers=100
proximityDistance=48
enableBandwidthStats=true
enableDebugLog=false
```

`sharedSecret` 必须是长随机字符串，不应提交到 Git、显示在截图中或发送给玩家。

Windows 运行：

```powershell
.\bin\standalone-server.bat
```

Linux 运行：

```bash
./bin/standalone-server
```

启动日志应显示 UDP 监听地址和端口。

## 配置 Minecraft 服务端

首次启动 Minecraft 服务端后，编辑 `config/minevoice-server.properties`：

```properties
mode=remote
remoteVoiceHost=voice.example.com
remoteVoicePort=24454
sharedSecret=replace-with-a-long-random-secret
enableDebugLog=false
```

| 配置 | 要求 |
| --- | --- |
| `mode` | 当前必须为 `remote`。 |
| `remoteVoiceHost` | 填写玩家客户端能够访问的公网 IP、域名或虚拟局域网 IP，不是 Docker 容器名或 `127.0.0.1`。 |
| `remoteVoicePort` | 与独立语音服务器的 `bindPort` 一致。 |
| `sharedSecret` | 与独立语音服务器完全一致。 |

修改配置后重启 Minecraft 服务端。玩家进服时会自动接收语音服务器地址和短期 token。

## 网络和防火墙

独立语音服务器默认使用 UDP `24454`，必须允许玩家客户端访问。

| 场景 | `remoteVoiceHost` | 额外要求 |
| --- | --- | --- |
| 局域网联机 | 语音服务器的局域网 IP，例如 `192.168.1.100` | 放行主机防火墙的 UDP `24454`。 |
| 云服务器 / VPS | 公网 IP 或解析到它的域名 | 云安全组和系统防火墙都放行 UDP `24454`。 |
| Docker | 宿主机公网 IP 或域名 | 映射 UDP 端口，例如 `24454:24454/udp`。 |
| 虚拟局域网 | ZeroTier、Radmin VPN 等分配的 IP | 所有语音玩家必须加入同一虚拟网络。 |
| 内网穿透 | 穿透服务提供的公网地址 | 隧道必须支持 UDP，不支持 TCP-only 隧道。 |

## Docker 部署

Docker 只用于独立语音服务器，不替代 Minecraft 模组。

```bash
./gradlew :standalone-server:installDist
docker compose -f docker/docker-compose.yml up -d --build
```

将 `MINEVOICE_SHARED_SECRET` 通过安全的环境变量或面板 secret 注入，并确保它与 Minecraft 服务端配置一致。不要将密钥写入镜像、compose 示例或仓库。

## 联机验证

1. 启动独立语音服务器，确认日志没有绑定错误。
2. 启动 Minecraft 服务端，再让两名已安装模组的玩家进服。
3. 两人按 `O` 打开 MineVOICE 面板，确认连接状态已连接。
4. 在 48 方块内测试 `V` 范围语音；超过 48 方块后应听不到。
5. 建立同一语音队伍后测试 `G` 队伍语音；超距时仍应可听到。

## 常见问题

| 现象 | 可能原因 | 检查方式 |
| --- | --- | --- |
| 客户端鉴权失败 | 两端 `sharedSecret` 不一致、服务器时间异常或 token 过期 | 对比两端配置，查看 Minecraft 与语音服务器日志。 |
| 客户端未连接 | UDP 端口未开放或 `remoteVoiceHost` 不可访问 | 从另一台玩家网络检查 UDP 端口与地址。 |
| 只有部分玩家无声 | 输入/输出设备不对、个人静音、系统权限或网络限制 | 在设置页切回系统默认并测试输入/输出。 |
| 局域网正常，公网无声 | 下发了内网地址，或云防火墙未放行 UDP | 改为公网地址或域名，检查 UDP 安全组。 |
| 超距仍有范围声音 | 玩家仍在同一队伍且使用队伍频道，或位置状态未同步 | 退出语音队伍并观察服务端调试日志。 |
