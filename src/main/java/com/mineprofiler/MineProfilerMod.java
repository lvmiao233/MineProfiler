package com.mineprofiler;

import com.mineprofiler.automation.SimplePlayerController;
import com.mineprofiler.metrics.LightweightMetrics;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * 模组主类
 * 简化版本：只实现两个核心功能
 * 1. 进入世界后自动运动
 * 2. 收集并保存性能指标到CSV文件
 */
public class MineProfilerMod implements ClientModInitializer {
    // 模组ID
    public static final String MOD_ID = "mineprofiler";
    // 日志
    public static final Logger LOGGER = LogManager.getLogger("MineProfiler");
    // 单例模式
    private static MineProfilerMod INSTANCE;
    // 玩家控制器
    private SimplePlayerController playerController;
    // 性能指标收集器
    private LightweightMetrics metrics;
    // 游戏刻计数
    private int ticks = 0;
    // 是否已经开始收集指标
    private boolean metricsStarted = false;
    
    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        
        // 设置日志级别为DEBUG
        Configurator.setLevel(LOGGER.getName(), Level.DEBUG);
        
        LOGGER.info("MineProfiler mod 正在初始化...");
        
        // 初始化玩家控制器和性能指标收集器
        this.playerController = new SimplePlayerController();
        this.metrics = new LightweightMetrics();
        
        LOGGER.info("MineProfiler mod 已初始化！");
        LOGGER.info("Minecraft版本: " + FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion());
        
        // 注册客户端Tick事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 客户端每Tick执行一次
            this.ticks++;
            
            // 如果玩家已加载
            if (client != null && client.player != null) {
                // 更新性能指标（每tick都更新）
                if (client.getCurrentFps() > 0) {
                    metrics.updateFps(client.getCurrentFps());
                    metrics.updateFrameTime(1000.0 / client.getCurrentFps());
                }
                
                // 开始收集指标数据（仅执行一次）
                if (!metricsStarted) {
                    LOGGER.info("开始收集性能指标数据...");
                    metrics.startCollection();
                    metricsStarted = true;
                }
                
                // 如果未开始移动，则开始移动
                if (!playerController.isAutoMovementActive()) {
                    LOGGER.info("玩家已加载，启动自动移动");
                    playerController.activateAutoMovement();
                }
                
                // 已激活自动移动，更新玩家控制器
                try {
                    if (playerController.isAutoMovementActive()) {
                        playerController.updatePlayerMovement();
                    }
                } catch (Exception e) {
                    LOGGER.error("更新玩家控制器时出错", e);
                }
            }
        });
    }
    
    /**
     * 获取实例
     * @return MineProfilerMod实例
     */
    public static MineProfilerMod getInstance() {
        return INSTANCE;
    }
    
    /**
     * 获取玩家控制器
     * @return 玩家控制器
     */
    public SimplePlayerController getPlayerController() {
        return playerController;
    }
    
    /**
     * 获取性能指标收集器
     * @return 性能指标收集器
     */
    public LightweightMetrics getMetrics() {
        return metrics;
    }
} 