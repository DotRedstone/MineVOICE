# MineVOICE

MineVOICE 是一个面向 Minecraft (NeoForge) 的双模式 3D 空间语音基础设施。该项目旨在提供极低延迟、高度沉浸的语音聊天体验，并且在系统架构上进行了深度解耦，使其不仅能作为普通的内置模组运行，还能通过独立的 UDP 服务器支撑超大规模并发。

## 项目特点与核心技术

### 1. 双模式分布式架构 (Dual-Mode Architecture)
本项目在架构设计上严格遵循职责分离与模块化原则，支持两种运行模式：
- **内置服务器模式 (Built-in Mode)**：适用于小型好友局，语音服务与 Minecraft 服务端共用同一个进程。
- **独立服务器模式 (Standalone Mode)**：适用于大型群组服。语音处理被完全抽离到独立的 `standalone-server` 进程中。Minecraft 服务端仅负责玩家连接信息下发（信令），而所有高频的音频数据流（UDP）直接与独立服务器进行点对点交互，彻底解放了 Minecraft 服务端的性能瓶颈。

### 2. 自定义低延迟网络协议栈
完全抛弃了 Minecraft 原生的 TCP 网络（因为其容易受到游戏刻 TPS 波动影响而产生巨大的延迟抖动），自研了一套纯 UDP 的通信协议栈：
- **`VoiceFrame` 帧结构**：轻量级二进制封装。
- **AES-GCM 硬件级加密**：通过加密信令交换生成的 `sessionKey`，保障每帧音频的绝对安全。
- **防重播攻击与抗丢包 (Jitter Buffer)**：内置抗抖动缓冲队列和丢包补偿策略，保障弱网环境下的清晰通话。

### 3. 硬核的 3D 空间声学模拟 (3D Spatial Audio)
不是简单的“左边大点声，右边小点声”，而是一套完整的声学环境模拟：
- **实时声学射线追踪 (Ray-traced Occlusions)**：通过从声源到听者的连线，实时检测并计算方块遮挡物（如墙壁）带来的物理阻尼。
- **水下低通滤波 (Underwater Low-pass Filtering)**：如果玩家潜入水中，系统会自动对音频施加低通滤波，削减高频，并叠加音量衰减，完美还原“耳朵进水”的沉闷听感。
- **HRTF 头部相关传输函数对接**：自动检测环境并拉起底层的 `OpenAL` 引擎，引入 HRTF 算法。利用精确计算的监听器 `Pitch(俯仰角)` 与 `Yaw(偏航角)`，使得玩家甚至能清晰地辨别声音是从“正上方”还是“正下方”传来的。

## 模块结构

- `common`: 核心协议、配置、密码学算法与接口定义。严格隔离对 Minecraft 的依赖。
- `voice-server-core`: 通用的语音服务端业务逻辑，被内置服务端和独立服务端共享。
- `standalone-server`: 独立运行的高性能 UDP 服务器入口。
- `minecraft-neoforge`: 客户端界面、按键绑定、空间音频解算以及内置服务端插件。
- `client-sim`: 压力测试专用的模拟客户端。

## 核心设计模式与解耦 (代码亮点)
在客户端 `minecraft-neoforge/src/main/java/dev/minevoice/neoforge/client/audio` 包下，采用了极其纯粹的软件工程解耦设计：
- **`ClientVoiceAudioPipeline` (协调器)**：管理整体生命周期和依赖注入。
- **`ClientAudioCaptureWorker` (采集器)**：独立线程负责麦克风采集、VAD (语音活动检测) 与 Opus 编码。
- **`ClientAudioPlaybackWorker` (播放器)**：独立线程负责 Jitter Buffer 抗抖动解码，并下发给 OpenAL / JavaSound 后端。

这三大组件彼此完全解耦，相互之间通过事件或队列交互，内部不包含任何网络收发代码（通过回调隔离），完美做到了“单一职责原则 (SRP)”。

## 编译与运行

1. 克隆代码后，在根目录执行编译：
   ```bash
   ./gradlew build
   ```
2. 运行 Minecraft NeoForge 客户端测试：
   ```bash
   ./gradlew :minecraft-neoforge:runClient
   ```
3. 运行独立语音服务器（如果有独立部署需求）：
   ```bash
   ./gradlew :standalone-server:run
   ```
