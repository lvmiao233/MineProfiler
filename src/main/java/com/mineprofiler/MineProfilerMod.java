package com.mineprofiler;

import com.mineprofiler.config.TestConfig;
import com.mineprofiler.metrics.LightweightMetrics;
// 保留导入但不使用
// import com.mineprofiler.world.AutoWorldManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MineProfilerMod implements ClientModInitializer {
    public static final String MOD_ID = "mineprofiler";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static MineProfilerMod INSTANCE;
    private TestConfig config;
    private LightweightMetrics metrics;
    // 保留定义但不使用
    // private AutoWorldManager worldManager;
    private boolean testRunning = false;
    private long testStartTime = 0;
    private long lastFrameTimeUpdate = 0;
    
    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        LOGGER.info("Initializing MineProfiler...");
        
        // 加载配置
        try {
            this.config = TestConfig.load();
            LOGGER.info("Configuration loaded successfully");
        } catch (IOException e) {
            LOGGER.error("Failed to load configuration", e);
            // 创建默认配置
            this.config = TestConfig.createDefault();
            try {
                this.config.save();
                LOGGER.info("Default configuration created");
            } catch (IOException ex) {
                LOGGER.error("Failed to save default configuration", ex);
            }
        }
        
        // 初始化指标收集器
        this.metrics = new LightweightMetrics(this.config);
        
        // 注释掉自动世界管理器的初始化
        // this.worldManager = new AutoWorldManager(this.config);
        
        // 注册Tick事件
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        
        LOGGER.info("MineProfiler initialized successfully (auto features disabled)");
    }
    
    private void onClientTick(MinecraftClient client) {
        // 如果游戏启动，但测试未开始，尝试启动测试
        if (client != null && client.player != null && !testRunning) {
            startTest();
        }
        
        // 如果测试正在运行，检查是否需要结束
        if (testRunning) {
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - testStartTime) / 1000;
            
            // 由于GameRendererMixin被禁用，在这里手动更新FPS和帧时间
            updatePerformanceMetrics(client);
            
            if (elapsedSeconds >= config.getTestDuration()) {
                endTest();
            }
        }
    }
    
    /**
     * 手动更新性能指标
     * 这是GameRendererMixin被禁用后的替代方法
     */
    private void updatePerformanceMetrics(MinecraftClient client) {
        if (metrics == null) return;
        
        // 更新FPS
        metrics.updateFps(client.getCurrentFps());
        
        // 更新帧时间（估算值，每秒更新一次）
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTimeUpdate > 1000) {
            // 估算帧时间 = 1000ms / FPS
            double fps = client.getCurrentFps();
            if (fps > 0) {
                double frameTimeMs = 1000.0 / fps;
                metrics.updateFrameTime(frameTimeMs);
            }
            lastFrameTimeUpdate = currentTime;
        }
    }
    
    public void startTest() {
        if (testRunning) return;
        
        LOGGER.info("Starting performance test...");
        testRunning = true;
        testStartTime = System.currentTimeMillis();
        lastFrameTimeUpdate = System.currentTimeMillis();
        
        // 确保输出目录存在
        File outputDir = new File(config.getOutputDirectory());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // 启动指标收集
        metrics.startCollection();
    }
    
    public void endTest() {
        if (!testRunning) return;
        
        LOGGER.info("Ending performance test...");
        testRunning = false;
        
        // 停止指标收集
        metrics.stopCollection();
        
        // 可选：自动退出游戏
        if (config.isExitAfterTest()) {
            MinecraftClient.getInstance().stop();
        }
    }
    
    public static MineProfilerMod getInstance() {
        return INSTANCE;
    }
    
    public TestConfig getConfig() {
        return config;
    }
    
    public LightweightMetrics getMetrics() {
        return metrics;
    }
    
    public boolean isTestRunning() {
        return testRunning;
    }
} 