# MineVOICE

MineVOICE 是 Minecraft 1.21.1 / NeoForge 的多人语音模组。它提供范围语音、队伍语音、游戏内设备设置与说话状态显示；玩家进入 Minecraft 服务器后自动获得语音连接信息，无需填写语音 IP、端口或 token。

当前版本为 [v0.1.0-alpha.1](https://github.com/DotRedstone/MineVOICE/releases/tag/v0.1.0-alpha.1)。这是可供联机测试的 Alpha 版本，部署前请阅读下方限制说明。

## 下载与安装

Release 只有两个需要部署的文件：

| 文件 | 安装位置 | 用途 |
| --- | --- | --- |
| `MineVOICE-NeoForge-1.21.1-<版本>.jar` | 每位玩家客户端的 `mods` 目录，以及 Minecraft 服务端的 `mods` 目录 | 同一个 NeoForge 模组包，两端都必须安装，版本必须一致。 |
| `MineVOICE-Voice-Server-<版本>.zip` | 一台可被玩家访问的语音服务器 | 独立语音服务器核心。解压、配置并启动后，为 Remote 模式提供 UDP 语音转发。 |

`Source code (zip)` 与 `Source code (tar.gz)` 是 GitHub 自动生成的源码归档，不是安装包。

## 快速部署

1. 将 NeoForge 模组 jar 放入 Minecraft 服务端和所有客户端的 `mods` 目录。
2. 解压独立语音服务器 zip，编辑根目录的 `minevoice-standalone.properties`：

   ```properties
   bindHost=0.0.0.0
   bindPort=24454
   sharedSecret=请替换为长随机密钥
   proximityDistance=48
   ```

3. 启动独立语音服务器：Windows 运行 `bin/standalone-server.bat`；Linux 运行 `bin/standalone-server`。
4. 在 Minecraft 服务端首次启动后，编辑 `config/minevoice-server.properties`：

   ```properties
   mode=remote
   remoteVoiceHost=玩家可以访问的公网IP或域名
   remoteVoicePort=24454
   sharedSecret=与独立语音服务器完全一致的长随机密钥
   ```

5. 放行独立语音服务器的 UDP `24454` 端口，重启 Minecraft 服务端后让玩家进服。

`remoteVoiceHost` 必须填写玩家能够访问的地址，不能填写 Docker 容器名、`127.0.0.1` 或仅服务端可见的内网地址。

## 游戏内操作

| 操作 | 默认按键 | 说明 |
| --- | --- | --- |
| 打开 MineVOICE 面板 | `O` | 查看连接状态，打开设置或队伍面板。 |
| 范围语音 | `V` | 按住后向附近玩家说话，默认距离由服务端 `proximityDistance` 控制。 |
| 队伍语音 | `G` | 按住后只向同一语音队伍成员说话，不受距离限制。 |
| 静音麦克风 / 屏蔽听音 | 主面板图标 | 立即停止发送麦克风，或停止播放其他玩家声音。 |

按键可在 Minecraft 原生“按键设置”里的 MineVOICE 分类中修改。进入设置后可选择输入和输出设备、调整音量，并使用测试输入/输出确认设备是否正常。

## 当前可用能力

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| 自动连接与鉴权 | 可用 | Minecraft 服务端向客户端下发短期 HMAC token，客户端自动连接 UDP 语音服务器。 |
| 输入设备采集和输出播放 | 可用 | 基于 Java Sound，支持系统默认设备与手动选择。 |
| 范围和队伍语音路由 | 可用 | 非队伍语音按维度与方块距离筛选；队伍语音独立路由。 |
| 游戏内状态界面 | 可用 | 包含连接状态、设备设置、静音控制、说话人和名牌状态。 |
| 初步立体声方向感 | 可用 | 根据说话者相对位置调整左右声道，尚不是完整声学模拟。 |
| Opus 编码、抖动缓冲 | 未实现 | 当前链路用于功能验证，带宽和网络抗抖动能力不适合大规模正式服。 |
| 降噪、回声消除、语音激活 | 未实现 | 当前建议使用按键说话。 |
| 方块遮挡和真实 3D 声学 | 未实现 | 后续算法会使用服务端下发的方块上下文，不会改变队伍语音的距离规则。 |
| 内置 Local 语音服务端 | 未实现 | 当前仅验证并支持 Remote 独立语音服务器部署。 |

## 验证联机

推荐使用两名真实玩家、一个 Minecraft 服务端和一个独立语音服务器验证：

1. 两名玩家进入同一世界，确认 `O` 面板显示已连接。
2. 相距 48 方块内，玩家 A 按住 `V` 说话，玩家 B 应听到声音并看到说话状态。
3. 相距超过 48 方块，玩家 B 不应再听到 A 的范围语音。
4. 创建并加入同一语音队伍后，相距超过 48 方块时按住 `G`，两端仍应互相听到。
5. 切换耳机或麦克风后，在设置页改回“系统默认”或重新选择设备，再使用测试输入/输出验证。

完整的本地双客户端开发验证见 [本地 Demo](docs/demo.md)，网络、端口和 Docker 部署见 [部署说明](docs/deployment.md)。

## 文档

- [配置说明](docs/configuration.md)
- [部署说明](docs/deployment.md)
- [本地 Demo](docs/demo.md)
- [协议说明](docs/protocol.md)
- [算法接口](docs/algorithm-api.md)
- [开发说明](docs/development.md)
- [产品 TODO](TODO.md)
- [路线图](docs/roadmap.md)

## Alpha 限制

请勿将 Alpha 版本直接用于高人数生产服务器。当前重点是验证设备、鉴权、UDP 转发、范围/队伍路由和游戏内交互；音频压缩、网络抗抖动、降噪与完整 3D 声学仍在后续计划中。不要把 `sharedSecret` 提交到 Git、截图或发送给玩家。
