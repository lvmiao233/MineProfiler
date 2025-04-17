# MineProfiler

MineProfiler 是一个轻量级 Minecraft 性能分析模组，作为 SchedEvalBench 调度器评测框架的辅助工具。它旨在提供游戏性能数据，用于评估操作系统进程调度策略对游戏性能的影响。

## 功能特点

- 轻量级设计，对游戏性能影响最小化
- 自动收集游戏性能指标（FPS、帧时间等）
- 导出CSV格式的性能数据，便于后续分析
- 自动化玩家移动，实现可重复的测试路径
- 与SchedEvalBench框架无缝集成
- 支持Minecraft 1.21版本的Fabric模组加载器

## 数据收集

MineProfiler会收集以下性能指标：

- 实时帧率(FPS)
- 帧渲染时间(毫秒)
- 玩家位置坐标(X,Y,Z)
- 已加载的区块数量
- 性能统计摘要(最小/最大/平均FPS和帧时间)

所有数据以CSV格式导出，便于使用pandas等工具进行后续分析。

## 自动化测试

模组实现了简单但有效的自动化测试功能：

- 进入游戏后自动切换到旁观模式
- 执行预定义的移动路径(直线移动+视角旋转)
- 在移动过程中持续收集性能指标
- 数据自动保存至系统临时目录

## 项目结构

```
MineProfiler/
├── src/main/java/com/mineprofiler/
│   ├── MineProfilerMod.java          # 模组主类
│   ├── automation/                   # 自动化移动控制
│   │   ├── SimplePlayerController.java # 玩家移动控制器
│   │   └── AutoWorldManager.java     # 世界管理
│   ├── metrics/                      # 性能指标收集
│   │   └── LightweightMetrics.java   # 轻量级性能收集器
│   ├── mixin/                        # Minecraft核心类修改
│   └── util/                         # 工具类
├── src/main/resources/
│   ├── fabric.mod.json               # 模组元数据
│   ├── mineprofiler.mixins.json      # Mixin配置
│   └── mineprofiler_config.json      # 模组配置
└── build.gradle                      # 构建脚本
```

## 构建与安装

作为SchedEvalBench项目的组件，MineProfiler通常由测试框架自动部署和配置。但如果需要单独构建：

```bash
# 从项目根目录构建
./gradlew build

# 输出位置
build/libs/mineprofiler-1.0.0.jar
```

## 与SchedEvalBench框架集成

在SchedEvalBench项目中，MineProfiler通过Python自动化脚本进行控制：

1. `MinecraftGameplayScene`类自动复制模组到Minecraft实例
2. 启动游戏并使用xdotool执行键盘操作导航到世界
3. 运行预定时长的测试，收集性能数据
4. 解析CSV输出文件，生成性能报告

## 技术细节

- **性能指标收集**: 使用定时器定期采样游戏状态，而非每帧采样，减少对游戏性能的影响
- **数据存储**: 采用简单CSV格式，包括时间戳、FPS、帧时间、玩家位置和加载区块数
- **玩家控制**: 实现简单的直线移动和视角旋转，确保测试路径的一致性和可重复性

## 配置选项

模组使用`mineprofiler_config.json`进行配置，主要选项包括：

```json
{
  "metrics": {
    "sampleInterval": 1,         // 采样间隔(秒)
    "outputDirectory": "./perfdata", // 输出目录
    "outputFormat": "csv"        // 输出格式
  }
}
```

## 限制与约束

- 当前版本针对Minecraft 1.21.x进行了优化
- 仅支持Fabric模组加载器
- 仅实现简单的直线移动+视角旋转
- 不包含图形化界面，专注于数据收集

## 许可证

MineProfiler是SchedEvalBench项目的组成部分，许可证与其保持一致。 