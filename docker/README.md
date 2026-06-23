# Docker 部署

Docker 方向只用于独立语音服务器，不替代 Minecraft 客户端或服务端模组。

## 构建前准备

先在仓库根目录构建安装目录：

```bash
./gradlew :standalone-server:installDist
```

然后构建镜像：

```bash
docker build -f docker/Dockerfile -t minevoice-standalone .
```

或者使用 compose：

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

## 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MINEVOICE_BIND_HOST` | `0.0.0.0` | 容器内监听地址 |
| `MINEVOICE_BIND_PORT` | `24454` | UDP 监听端口 |
| `MINEVOICE_SHARED_SECRET` | `change-me` | MC 服务端和语音服务器共享密钥 |
| `MINEVOICE_MAX_PLAYERS` | `100` | 最大会话数量 |
| `MINEVOICE_ENABLE_BANDWIDTH_STATS` | `true` | 是否启用带宽统计 |
| `MINEVOICE_ENABLE_DEBUG_LOG` | `false` | 是否启用 debug 日志 |

## 注意事项

- 不要把 `sharedSecret` 写进镜像。
- 不要在镜像中包含构建缓存。
- 面板服需要确认 UDP 端口映射，而不是只映射 TCP。
