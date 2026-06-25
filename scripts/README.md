# MineVOICE Demo 脚本

## Local 内置语音服务端

```powershell
.\scripts\start-local-demo.ps1
```

这个脚本会打开 3 个窗口：Minecraft 开发服务端、客户端 A、客户端 B。Local 模式不需要额外启动 `standalone-server`。

两个客户端进入多人游戏后连接：

```text
127.0.0.1:25565
```

## Remote 独立语音服务端

```powershell
.\scripts\start-remote-demo.ps1
```

这个脚本会打开 4 个窗口：独立语音服务器、Minecraft 开发服务端、客户端 A、客户端 B。

## Open to LAN

```powershell
.\scripts\start-lan-demo.ps1
```

客户端 A 开单人世界并点击 Open to LAN。客户端 B 从多人游戏里的 LAN 世界加入。多网卡或虚拟组网环境下，把 A 的 `config/minevoice-server.properties` 里的 `localVoiceAdvertiseHost` 改成对应网卡 IP。

## 停止

先关闭 Minecraft 窗口，再运行：

```powershell
.\scripts\stop-local-demo.ps1
```

脚本会尝试停止占用 UDP `24454` 或 TCP `25565` 的 MineVOICE 相关进程。
