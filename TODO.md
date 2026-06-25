# MineVOICE TODO

本清单按可测试产品优先级排序。`[x]` 表示已有代码路径并通过本地编译验证；仍需要真实多人手测的项会单独标注。

当前基线：`v0.1.0-alpha.3`。MineVOICE 继续保持双模式定位：Local 适合小服、朋友服和 Open to LAN；Remote 适合多人服、独立节点和 Docker 部署。玩家主路径仍然是进服后由 Minecraft 服务端自动下发语音 endpoint 和短期 token，不让玩家手动填写语音服务器。

## P0: Alpha.4 必须先补的回归

- [ ] **真实双客户端 Local 回归**：覆盖 `mode=local` 独立 Minecraft 服务端、两个真实客户端自动连接、范围语音、队伍语音、静音、屏蔽听音、断开重连和停服释放 UDP 端口。
- [ ] **真实双客户端 Remote 回归**：确认 standalone voice server、Minecraft 服务端和两个真实客户端在 alpha.3 后不回归。
- [ ] **真实 Open to LAN 回归**：A 开单人世界并 Open to LAN，B 从局域网或虚拟组网加入，确认 endpoint/token 自动下发、范围语音和队伍语音可用。
- [x] **发布验收记录模板**：每个 tag 记录 Minecraft/NeoForge 版本、Java 版本、Local/Remote/LAN 测试结果、已知问题和回滚目标，见 `docs/release-validation-template.md`。
- [x] **Windows 测试 runner 修复**：解决 Unicode 工作目录导致 JUnit worker 找不到测试类的问题，恢复可靠的单元测试执行。

## P1: 双模式联机稳定性

- [x] **Remote 独立语音服务端保持可用**：standalone UDP 鉴权、session、relay、SERVER_STATE 路由继续保留。
- [x] **Local embedded voice server 初步实现**：`mode=local` 时 Minecraft 服务端 JVM 内启动 UDP voice server，停服关闭 socket。
- [x] **Local 状态同步**：Local 模式不再向自己发 UDP `SERVER_STATE`，而是同 JVM 更新 session registry。
- [x] **advertiseHost 自动处理**：`auto` 避免下发 `0.0.0.0`，本机连接使用 loopback，LAN 优先选择 site-local 地址。多网卡仍建议手动配置。
- [x] **Local/Remote/LAN demo 脚本**：已有 `start-local-demo.ps1`、`start-remote-demo.ps1`、`start-lan-demo.ps1`。
- [x] **Local 生命周期加固**：reload 会重启 Local UDP server，stop 会释放旧 server/thread，重复启动会被保护，端口占用或异常关闭会记录到 `/minevoice status` 的 `localError`；仍需真实服停启回归。
- [x] **LAN host 选择诊断**：当 `auto` 无法可靠选择地址时，在日志和 `/minevoice test-endpoint` 提示手动配置 `localVoiceAdvertiseHost`。
- [x] **Remote 错误分流日志**：鉴权失败会区分 token 过期 / 签名错误 / 身份不一致 / token 解析失败；endpoint 配置可用 `/minevoice test-endpoint` 排查。

## P2: 音频链路

- [x] **基础 jitter buffer**：按说话者和 sequence 维护缓冲，支持乱序重排、迟到包丢弃和丢包跳过。
- [x] **codec 抽象**：保留 `VoiceCodec`、`VoiceCodecFactory`、`VoiceAudioFormat`，当前默认实现为 `opus`，`mock-pcm` 作为 fallback。
- [x] **Opus 编解码接入**：使用纯 Java Concentus Opus，避免 native 分发问题；仍需真实双客户端听感和带宽验收。
- [x] **Encoded frame 格式确认**：`VOICE_FRAME` 明确承载 encoded audio frame，服务端只转发、不解码、不混音。
- [x] **Opus fallback**：Opus 初始化失败时回退 `mock-pcm-fallback` / Java Sound 可测试路径。
- [x] **基础带宽统计**：debug 快照显示 codec、UDP send/receive bytes、packet counts、encoded voice bytes 和 frames/sec。
- [x] **带宽对比回归（自动部分）**：`scripts/compare-codec-bandwidth.ps1` 会自动启动 standalone，分别跑 Opus 和 mock-pcm client-sim，输出 UDP/encoded payload 对比。
- [ ] **真实 Opus 听感 / CPU 回归**：用双客户端实测记录 Opus 与裸 PCM/mock 的听感差异和 CPU 占用。
- [x] **语音激活优化**：补 hysteresis、噪声门、触发阈值说明，并区分公共频道和队伍频道的触发配置。
- [x] **降噪 / 回声消除接口**：先做可替换 DSP 接口，不阻塞游戏线程；真实算法后续接入。

## P3: 空间语音

- [x] **基础 pan 修正**：左右声道 pan 使用水平距离归一化，音量仍按 3D 距离衰减。
- [x] **空间 debug**：暴露 speaker、distance、pan、gain、channel、occlusion、backend；sameDimension 当前由服务端路由保证。
- [ ] **空间方向手测**：B 在 A 左/右/前/后和绕圈移动时，确认 pan 不反、变化平滑。
- [x] **OpenAL backend 抽象**：已补 `VoicePlaybackBackend`、`JavaSoundVoicePlaybackBackend`、`OpenAlVoicePlaybackBackend`、listener/source provider；当前默认仍使用 Java Sound。
- [ ] **OpenAL per-speaker source**：每个说话者独立 source，source 跟随玩家位置，listener 跟随相机位置和朝向。
- [ ] **OpenAL 资源释放**：玩家离开、断开、停止说话、关闭世界时释放 source，OpenAL 初始化失败回退 Java Sound。
- [x] **基础遮挡和低通**：客户端按 listener/speaker 视线采样碰撞方块，被遮挡时降低音量并对 PCM 做轻量低通。
- [x] **遮挡性能保护**：遮挡采样只在客户端 tick 的玩家位置刷新中进行，播放线程复用快照结果，不在音频线程扫方块。
- [x] **Sound Physics optional compat skeleton**：只检测安装和预留 backend，不引入硬依赖，不调用不稳定内部 API。

## P4: UI / HUD

- [x] **基础 MineVOICE 面板和设置界面**：按 `O` 打开，支持设备设置、测试输入/输出、静音、屏蔽听音。
- [x] **频道界面重做**：公共/队伍 tab、玩家头像、玩家音量、玩家静音、队伍密码、搜索和滚动列表已具备基础路径。
- [x] **HUD 简化路径**：左下角保持麦克风 / 扬声器 / 闭麦 / 关闭听筒状态提示。
- [x] **名牌旁图标渲染**：把当前 name tag 文字符号改为真正的小图标布局，固定顺序 speaking / muted / deafened / group。
- [x] **队伍 HUD 细化**：可选队伍 HUD 显示成员头像、说话状态、静音角标，左下 HUD 会在按住 `G` 时给出队伍通话反馈；默认关闭，避免遮挡核心画面。
- [ ] **频道页视觉复查**：继续按原版社交界面和原版容器质感微调边框、内嵌列表、搜索框、滚动条和按钮间距。
- [x] **UI 开关补齐**：设置页已暴露显示/隐藏 HUD、说话头像 HUD、队伍 HUD、名牌图标、图标大小、头像位置和调试信息等级；空间 debug 进入 Debug tab 摘要。
- [ ] **设备切换回归**：耳机插拔、默认麦克风切换、默认扬声器切换、设备释放和恢复需要真实 Windows 手测。

## P5: 诊断和腐竹工具

- [x] **管理命令**：实现 `/minevoice status`、`/minevoice debug`、`/minevoice reload`、`/minevoice test-endpoint`。
- [x] **客户端诊断页**：设置页 Debug tab 显示 endpoint、连接状态、codec、playback backend、jitter stats、UDP 速率、voice frames 和 spatial/compat 摘要。
- [x] **服务端诊断日志**：针对 loopback host、模板 host、默认 sharedSecret、Local bind/advertise 端口不一致和 Local UDP 异常给出明确提示。
- [x] **debug 等级整理**：默认 OFF 不刷屏；BASIC 只显示连接关键状态，VERBOSE 继续显示 codec、activation、spatial 等细节。
- [x] **client-sim 压测扩展**：支持 proximity / group、不同距离、codec 参数、带宽统计、丢包和乱序模拟；长时间压测和报告输出后续继续增强。

## P6: 文档和发布

- [x] **自动发布**：推送 `v*` 标签会构建 NeoForge jar、standalone zip、Docker 镜像和 GitHub Pre-release。
- [x] **Docker standalone 示例**：只用于语音服务器，UDP 端口和 `MINEVOICE_SHARED_SECRET` 环境变量明确。
- [x] **README alpha.3 版本链接**：当前公开版本指向 `v0.1.0-alpha.3`。
- [x] **docs/configuration.md 更新**：补 `voiceCodec`、`audioPlaybackBackend`、`spatialBackend`、`enableOcclusion`、`jitterBufferMs`、`enableSpatialDebug`。
- [x] **docs/deployment.md 更新**：补 Local 独立服务端、Open to LAN、Remote、Docker、advertiseHost 常见错误、sharedSecret 安全说明和管理命令。
- [x] **docs/demo.md 更新**：补 Remote/Local/LAN 双客户端脚本、空间方向测试、HUD 状态验证、Opus/jitter/OpenAL debug 查看方式。
- [x] **docs/protocol.md 更新**：说明 VOICE_FRAME payload 是 encoded audio frame，协议版本和 codec negotiation 规则要明确。
- [x] **docs/development.md 更新**：说明 Opus/OpenAL 依赖选择、fallback 策略、DSP 接口、Sound Physics optional compat 边界。
- [x] **docs/roadmap.md 更新**：把下一轮 Alpha 拆成 Opus、OpenAL、遮挡、诊断工具和真实双客户端验收。

## 后续建议 commit 拆分

- `test(client): 补充双客户端回归验证记录`
- `feat(audio): 接入 Opus 编解码链路`
- `feat(audio): 完善语音抖动缓冲统计`
- `feat(audio): 添加 OpenAL 位置音源后端`
- `feat(client): 渲染名牌旁语音状态图标`
- `feat(client): 细化队伍 HUD 状态反馈`
- `feat(server): 添加 MineVOICE 诊断命令`
- `docs(demo): 补充 Alpha 双模式验收流程`
