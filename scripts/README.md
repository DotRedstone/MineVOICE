# 本地 Demo 脚本

## 启动

在仓库根目录运行：

```powershell
.\scripts\start-local-demo.ps1
```

脚本会打开四个窗口：独立语音服务器、Minecraft 开发服务端、客户端 A、客户端 B。

两个客户端进入多人游戏，直接连接：

```text
127.0.0.1:25565
```

再在每个客户端通过 `Mods -> MineVOICE -> Config` 测试输出设备、麦克风、按键说话。

## 停止

先关闭两个 Minecraft 客户端窗口，然后执行：

```powershell
.\scripts\stop-local-demo.ps1
```

脚本只会尝试停止占用 UDP `24454` 或 TCP `25565` 的 MineVOICE 相关进程。
