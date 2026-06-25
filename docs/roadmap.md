# 路线图

路线图描述发布目标，不等同于完成承诺。具体验收以 [TODO](../TODO.md) 为准。

| 阶段 | 目标 | 核心交付 |
| --- | --- | --- |
| v0.1.0-alpha.1 | 可联机验证骨架 | Remote 独立语音服务器、自动鉴权、设备设置、范围/队伍语音、基础方向感、Release 自动构建 |
| v0.1.0-alpha.2 | 客户端可用性 | 游戏内设置容器样式、输入设备电平测试、测试期间释放并恢复音频设备 |
| 下一轮 Alpha | Local 和网络稳定性 | Local embedded UDP server、Open to LAN、jitter buffer、Local/Remote/LAN demo 脚本、管理命令和基础诊断 |
| Beta 音频 | 可用音频链路 | Opus 编解码验收、丢包隐藏、语音激活优化、DSP 接口、持久化音量控制 |
| Beta 空间 | 可解释空间效果 | OpenAL backend、服务端权威位置、距离衰减平滑、基础遮挡和低通、空间 debug |
| Compat | 可选兼容 | Sound Physics Remastered optional compat skeleton 和回退策略 |
| RC | 运维和发布 | 管理命令、诊断、限流、指标、配置迁移、发布验收记录 |
| v1.0 | 社区可用版本 | 明确人数、网络条件、已知限制和稳定验证结果 |

不属于当前 Alpha 生产承诺：大规模正式服稳定性、录音/回放、服务器保存玩家音频、降噪/AEC、完整声学模拟。
