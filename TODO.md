# MineVOICE TODO

本清单按可测试产品优先级排序。`[x]` 表示已有代码路径和本地编译验证；仍需真实多人手测的项会单独标注。

## P0: 双模式联机稳定性

- [x] **Remote 独立语音服务端保持可用**：standalone UDP 鉴权、session、relay、SERVER_STATE 路由继续保留。
- [x] **Local embedded voice server 初步实现**：`mode=local` 时 Minecraft 服务端 JVM 内启动 UDP voice server，停服关闭 socket。仍需双真实客户端手动验收。
- [x] **Local 状态同步**：Local 模式不再向自己发 UDP `SERVER_STATE`，而是同 JVM 更新 session registry。
- [x] **advertiseHost 自动处理**：`auto` 避免下发 `0.0.0.0`，本机连接使用 loopback，LAN 优先选择 site-local 地址。多网卡仍建议手动配置。
- [x] **Local/Remote/LAN demo 脚本**：新增 `start-local-demo.ps1`、`start-remote-demo.ps1`、`start-lan-demo.ps1`。
- [ ] **真实双客户端回归**：覆盖连接、范围语音、队伍语音、静音、屏蔽听音、断开重连和停服释放端口。
- [ ] **Windows 测试 runner 修复**：解决 Unicode 工作目录导致 JUnit worker 找不到测试类的问题。

## P1: 音频链路

- [x] **基础 jitter buffer**：按说话者和 sequence 维护缓冲，支持乱序重排、迟到包丢弃和丢包跳过。
- [x] **codec 抽象**：保留 `VoiceCodec`、`VoiceCodecFactory`、`VoiceAudioFormat`，当前有效实现为 `mock-pcm`。
- [ ] **Opus 编解码**：选择 Java 21 / Minecraft 分发友好的方案，补 fallback、带宽对比和双客户端验收。
- [ ] **语音激活优化**：补 hysteresis、噪声门和 UI 可解释反馈。
- [ ] **降噪 / 回声消除接口**：先做可替换 DSP 接口，避免阻塞游戏线程。

## P2: 空间语音

- [x] **基础 pan 修正**：左右声道 pan 使用水平距离归一化，音量仍按 3D 距离衰减。
- [ ] **空间 debug**：暴露 speaker、distance、pan、gain、dimension、channel。
- [ ] **OpenAL backend**：实现 per-speaker source、listener pose、资源释放和 Java Sound fallback。
- [ ] **基础遮挡和低通**：基于 listener/speaker 之间的方块采样计算 muffled gain / low-pass。
- [ ] **Sound Physics optional compat**：只检测和预留 backend，不引入硬依赖。

## P3: UI / HUD

- [x] **基础 MineVOICE 面板和设置界面**：按 `O` 打开，支持设备设置、测试输入/输出、静音、屏蔽听音。
- [x] **说话者头像 HUD**：右下显示最近说话玩家头像；队伍成员在左下显示。
- [ ] **名牌旁图标渲染**：当前仍是 name tag 文字符号，后续改为小图标布局，避免遮挡原版名牌。
- [ ] **队伍 HUD 细化**：显示成员连接、静音、说话、按住 `G` 的更清晰反馈。
- [ ] **UI 视觉统一**：继续基于原版容器边缘风格打磨，不复制第三方模组资源。

## P4: 运维和发布

- [x] **自动发布**：推送 `v*` 标签会构建 NeoForge jar、standalone zip、Docker 镜像和 GitHub Pre-release。
- [x] **Docker standalone 示例**：只用于语音服务器，UDP 端口和 `MINEVOICE_SHARED_SECRET` 环境变量明确。
- [ ] **管理命令**：`/minevoice status`、`debug`、`reload`、`test-endpoint`。
- [ ] **诊断指标**：ping、packet loss、jitter stats、codec、playback backend、UDP 速率。
- [ ] **发布验收记录**：每个 tag 记录 Minecraft/NeoForge 版本、双客户端结果、已知问题和回滚版本。
