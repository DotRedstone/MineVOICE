# 部署说明

MineVOICE 支持 Local 和 Remote 两种部署模式。玩家主路径始终是进服后自动接收语音 endpoint 和 token，不需要手动填写语音服务器地址。

## Local 独立 Minecraft 服务端

适合小服和朋友服。只需要安装 NeoForge 模组 jar。

1. 将 `MineVOICE-NeoForge-1.21.1-<版本>.jar` 放入 Minecraft 服务端和所有客户端的 `mods` 目录。
2. 编辑 Minecraft 服务端的 `config/minevoice-server.properties`：

   ```properties
   mode=local
   localVoiceBindHost=0.0.0.0
   localVoiceBindPort=24454
   localVoiceAdvertiseHost=auto
   localVoiceAdvertisePort=24454
   sharedSecret=replace-with-a-long-random-secret
   proximityDistance=48
   ```

3. 放行服务端 UDP `24454`。
4. 启动 Minecraft 服务端。日志应出现：

   ```text
   MineVOICE local voice server starting on 0.0.0.0:24454
   MineVOICE local voice endpoint advertised as <host>:24454
   ```

`localVoiceAdvertiseHost=auto` 不会下发 `0.0.0.0`。如果多网卡、ZeroTier、Radmin VPN 或 Tailscale 环境选择不对，请手动填对应网卡 IP。

## Open to LAN

Open to LAN 使用 integrated server，仍走 Local 内置语音服务端路径。

1. LAN host 客户端安装 MineVOICE。
2. 在 host 客户端运行目录的 `config/minevoice-server.properties` 使用 Local 配置。
3. Host 进入单人世界并点击 Open to LAN。
4. Guest 从 Minecraft 多人游戏里的 LAN 世界加入。

LAN 场景下不要把 `localVoiceAdvertiseHost` 配成 `127.0.0.1`，否则其他机器无法连接。多网卡环境建议手动指定局域网或组网 IP。

## Remote 独立语音服务器

适合多人服、独立节点和 Docker 部署。

1. Minecraft 服务端和所有客户端安装 NeoForge 模组 jar。
2. 独立机器或同机解压 `MineVOICE-Voice-Server-<版本>.zip`。
3. 配置 `minevoice-standalone.properties`：

   ```properties
   bindHost=0.0.0.0
   bindPort=24454
   sharedSecret=replace-with-a-long-random-secret
   proximityDistance=48
   ```

4. Minecraft 服务端配置：

   ```properties
   mode=remote
   remoteVoiceHost=voice.example.com
   remoteVoicePort=24454
   sharedSecret=replace-with-a-long-random-secret
   ```

5. 启动 standalone：

   ```powershell
   .\bin\standalone-server.bat
   ```

   ```bash
   ./bin/standalone-server
   ```

`remoteVoiceHost` 必须是玩家客户端可访问的地址。不要填 Docker 服务名、容器内 IP、`127.0.0.1` 或只在 Minecraft 服务端本机可见的地址。

## 运维命令

Minecraft 服务端内可使用：

| 命令 | 用途 |
| --- | --- |
| `/minevoice status` | 查看 mode、endpoint、codec、协议版本、Local UDP 状态、在线语音玩家和队伍数量。 |
| `/minevoice debug` | 查看更详细的配置状态，包含 debug、遮挡预留项、Sound Physics compat 和 jitterBufferMs。 |
| `/minevoice reload` | 重载 `minevoice-server.properties`，重启 Local UDP 服务，并给在线玩家重新下发 endpoint/token。 |
| `/minevoice test-endpoint` | 解析当前会下发的语音 endpoint，并提示 Remote loopback 或 Local auto 场景。 |

`debug` 和 `reload` 需要 2 级权限。Remote 模式下如果 `remoteVoiceHost` 写成 `127.0.0.1` 或 `localhost`，`test-endpoint` 会提示远端客户端通常无法访问。

## Docker

Docker 只用于 standalone voice server，不替代 Minecraft 模组。

```yaml
services:
  minevoice-server:
    image: ghcr.io/dotredstone/minevoice-server:latest
    ports:
      - "24454:24454/udp"
    environment:
      MINEVOICE_BIND_HOST: "0.0.0.0"
      MINEVOICE_BIND_PORT: "24454"
      MINEVOICE_SHARED_SECRET: "change-me"
      MINEVOICE_PROXIMITY_DISTANCE: "48"
```

生产环境请用部署系统注入真实 `MINEVOICE_SHARED_SECRET`，不要把真实密钥提交到 Git。

## Sound Physics Remastered

Sound Physics Remastered 是推荐搭配和未来 optional compat 方向，不是 MineVOICE 的硬依赖。没安装它时 MineVOICE 必须正常工作；安装后如兼容层不可用，也应回退 MineVOICE 自带空间逻辑。
