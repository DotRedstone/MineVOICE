# 开发说明

## 模块职责

| 模块 | 职责 |
| --- | --- |
| `common` | 协议、配置、鉴权接口、音频接口、空间音效接口、通用工具 |
| `minecraft-neoforge` | Minecraft 客户端和服务端模组骨架 |
| `standalone-server` | 独立语音服务器骨架 |
| `client-sim` | 模拟客户端和后续负载测试 |

## 客户端设置界面入口

当前 NeoForge / Minecraft 版本未确定，因此客户端 UI 先拆成不依赖 MC API 的骨架：

| 类 | 职责 |
| --- | --- |
| `MineVoiceClientUiController` | 打开设置界面、保存设置、关闭当前界面 |
| `MineVoiceSettingsButtonBinding` | 表示游戏内 MineVOICE 设置按钮的点击入口 |
| `KeybindManager` | 预留打开设置界面按键和按键说话绑定 |
| `MineVoiceSettingsScreenModel` | 保存界面字段状态，后续映射到真实 Minecraft `Screen` |
| `ClientSettingsStore` | 读写客户端本地设置 |

后续接入 NeoForge 时，把按钮 `button.minevoice.settings` 的点击事件连接到 `MineVoiceSettingsButtonBinding#click()`，把打开设置界面的快捷键连接到 `KeybindManager#handleOpenSettingsPressed()`。

## 开发环境

- Java 21。
- Gradle 多模块工程。
- Minecraft 1.21.1。
- NeoForge 21.1.233。
- ModDevGradle 2.0.141。

启动开发客户端：

```bash
./gradlew :minecraft-neoforge:runClient
```

进入游戏后有两个入口：

1. Mods 页面选择 MineVOICE，点击 Config。
2. Options -> Controls 里找到 MineVOICE 分类，绑定并按下“打开 MineVOICE 设置”。

## 分支建议

使用短分支名表达变更范围，例如：

```text
codex/init-skeleton
codex/protocol-codec
codex/standalone-udp
```

## TODO 规则

代码 TODO 使用：

```java
// TODO(minevoice): describe the next concrete action.
```

TODO 必须说明后续动作。复杂设计写入 `docs/`。

## 测试建议

- `common` 优先补协议、token、空间距离计算的单元测试。
- `standalone-server` 优先补 UDP 包处理和鉴权测试。
- `client-sim` 用于后续模拟 5 / 10 / 30 个客户端。
- `minecraft-neoforge` 在版本确定后增加集成验证。

## 如何新增协议包

1. 在 `VoicePacketType` 增加包类型。
2. 在 `docs/protocol.md` 说明字段和方向。
3. 在 `VoicePacketCodec` 实现中处理编码和解码。
4. 为兼容性更新 `VoiceProtocolVersion`。
5. 增加最小单元测试或模拟客户端验证。

## 如何新增配置项

1. 先判断配置属于 Local、Remote、Standalone 还是客户端设置。
2. 更新对应 config record。
3. 更新 `docs/configuration.md`。
4. 避免把服务端 secret 下发给客户端。

## 如何做验证

```bash
./gradlew projects
./gradlew :common:build
./gradlew :standalone-server:build
./gradlew :client-sim:build
```

如果本机没有 Gradle wrapper 或 Gradle 命令，至少用 Java 编译器验证标准 Java 模块，并在输出中说明缺口。
