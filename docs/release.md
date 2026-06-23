# 发布说明

## 发布产物

每个 `v*` 标签会自动创建 GitHub Release，并上传以下部署包：

| 文件 | 用途 |
| --- | --- |
| `MineVOICE-NeoForge-1.21.1-<版本>.jar` | 客户端和 Minecraft 服务端共用的 NeoForge 模组。两端必须安装相同版本。 |
| `MineVOICE-Voice-Server-<版本>.zip` | Remote 模式使用的独立语音服务器核心。 |

GitHub 自动显示的 `Source code (zip)` 和 `Source code (tar.gz)` 是源码归档，不是安装文件。

## Alpha 发布前检查

1. 确认目标 Java 为 21、Minecraft 为 1.21.1、NeoForge 为 21.1.233。
2. 确认工作区不含 `.env`、`run/`、日志、崩溃报告、token、cookie、session、API key 或真实 `sharedSecret`。
3. 在 Linux CI 或本地环境构建：

   ```bash
   ./gradlew projects
   ./gradlew :minecraft-neoforge:build :standalone-server:build
   ./gradlew :standalone-server:installDist
   ```

4. 完成一次双客户端联机：连接、范围语音、队伍语音、静音、设备切换和超距不转发。
5. 在 Release 正文记录已验证的环境、已知限制和回滚目标。

## 创建版本

发布前先确保 `master` 已通过 CI，然后创建并推送标签：

```bash
git tag -a v0.1.0-alpha.2 -m "release: v0.1.0-alpha.2"
git push origin v0.1.0-alpha.2
```

GitHub Actions 会构建两个部署包、发布 `ghcr.io/dotredstone/minevoice-server:<版本>` 镜像，并创建 Release。包含 `alpha`、`beta` 或 `rc` 的版本会标记为 Pre-release。

## 回滚

不要覆盖已经被玩家使用的 Release 标签。出现发布问题时，优先发布一个递增的修复版本；只有确认未被使用的错误标签才允许删除或强制移动。
