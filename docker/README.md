# Docker 部署

Docker 镜像只用于 standalone voice server，不替代 Minecraft 客户端或服务端模组。

## 构建

先在仓库根目录构建安装目录：

```bash
./gradlew :standalone-server:installDist
```

再构建镜像：

```bash
docker build -f docker/Dockerfile -t minevoice-standalone .
```

或使用 compose：

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

## 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MINEVOICE_BIND_HOST` | `0.0.0.0` | 容器内监听地址 |
| `MINEVOICE_BIND_PORT` | `24454` | UDP 监听端口 |
| `MINEVOICE_SHARED_SECRET` | 无安全默认值 | 必须与 Minecraft 服务端配置一致 |
| `MINEVOICE_MAX_PLAYERS` | `100` | 预留容量参数 |
| `MINEVOICE_PROXIMITY_DISTANCE` | `48` | 范围语音最大距离 |
| `MINEVOICE_ENABLE_BANDWIDTH_STATS` | `true` | 是否启用带宽统计 |
| `MINEVOICE_ENABLE_DEBUG_LOG` | `false` | 是否启用 debug 日志 |

## 注意事项

- 不要把真实 `sharedSecret` 写进镜像或提交到 Git。
- UDP 端口必须用 `/udp` 映射，例如 `"24454:24454/udp"`。
- Minecraft 服务端仍需要安装 NeoForge 模组，并在 Remote 模式下配置玩家可访问的 `remoteVoiceHost`。
