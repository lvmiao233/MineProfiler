# MineProfiler

MineProfiler 是一个轻量级 Minecraft 性能分析模组，用于自动化收集和分析游戏性能数据。它可以帮助评估不同系统配置对游戏性能的影响。

## 功能特点

- 轻量级设计，对游戏性能影响最小化
- 自动收集游戏性能指标（FPS、帧时间等）
- 导出CSV格式的性能数据，便于后续分析
- 自动化玩家移动，实现可重复的测试路径
- 支持Minecraft 1.21版本的Fabric模组加载器

## 数据收集

MineProfiler会收集以下性能指标：

- 实时帧率(FPS)
- 帧时间(毫秒)
- 玩家位置坐标(X,Y,Z)
- 已加载的区块数量

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
│   ├── config/                       # 配置管理
│   │   └── TestConfig.java           # 测试配置
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

使用 Gradle 构建模组：

```bash
# 构建项目
./gradlew build

# 输出位置
build/libs/mineprofiler-1.0.0.jar
```

构建完成后，将生成的 JAR 文件复制到 Minecraft 的 `mods` 目录即可。

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