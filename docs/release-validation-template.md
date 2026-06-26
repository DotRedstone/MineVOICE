# Release 验收记录模板

版本：`v0.1.0-alpha.x`
日期：
验证人：

## 环境

| 项目 | 值 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.233 |
| Java | 21 |
| OS |  |
| MineVOICE commit |  |

## 构建验证

```text
./gradlew projects
./gradlew :common:build
./gradlew :standalone-server:build
./gradlew :client-sim:build
./gradlew :minecraft-neoforge:build
```

结果：

## 双客户端验证

| 场景 | 结果 | 备注 |
| --- | --- | --- |
| Local 独立服务端 | 未测 / 通过 / 失败 |  |
| Remote standalone | 未测 / 通过 / 失败 |  |
| Open to LAN | 未测 / 通过 / 失败 |  |
| 范围语音 | 未测 / 通过 / 失败 |  |
| 队伍语音 | 未测 / 通过 / 失败 |  |
| 静音 / 屏蔽听音 | 未测 / 通过 / 失败 |  |
| 设备切换 | 未测 / 通过 / 失败 |  |
| Opus 听感 | 未测 / 通过 / 失败 |  |

详细步骤见 `docs/manual-regression-checklist.md`。

## 诊断输出

记录 `/minevoice status`、`/minevoice test-endpoint`、设置页 debug snapshot 和 client-sim 带宽输出。

```text

```

## 已知问题

-

## 回滚目标

回滚版本：
回滚原因：
