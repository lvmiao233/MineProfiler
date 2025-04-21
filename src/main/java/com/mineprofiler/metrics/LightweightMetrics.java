package com.mineprofiler.metrics;

import com.mineprofiler.MineProfilerMod;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 简化版的轻量级性能指标收集器
 * 收集基本信息并保存到CSV文件
 */
public class LightweightMetrics {
    private static final Logger LOGGER = LogManager.getLogger("LightweightMetrics");
    private final MinecraftClient client;
    
    // 性能指标
    private double currentFps = 0.0;
    private double averageFps = 0.0;
    private double minFps = Double.MAX_VALUE;
    private double maxFps = 0.0;
    
    private double currentFrameTime = 0.0;
    private double averageFrameTime = 0.0;
    private double minFrameTime = Double.MAX_VALUE;
    private double maxFrameTime = 0.0;
    
    // MSPT相关指标
    private float currentMspt = 0.0f;
    private float averageMspt = 0.0f;
    private float minMspt = Float.MAX_VALUE;
    private float maxMspt = 0.0f;
    
    private int frameCount = 0;
    private int msptCount = 0;
    
    // CSV数据导出
    private Timer samplingTimer;
    private BufferedWriter dataWriter;
    private String outputFilename;
    private static final int SAMPLE_INTERVAL_MS = 1000; // 采样间隔，默认1秒
    
    /**
     * 默认构造函数
     */
    public LightweightMetrics() {
        this.client = MinecraftClient.getInstance();
        LOGGER.info("轻量级性能指标收集器已初始化");
    }
    
    /**
     * 更新FPS指标
     */
    public void updateFps(double fps) {
        this.currentFps = fps;
        this.minFps = Math.min(minFps, fps);
        this.maxFps = Math.max(maxFps, fps);
        
        frameCount++;
        // 更新平均值
        double delta = fps - averageFps;
        averageFps += delta / frameCount;
    }
    
    /**
     * 更新帧时间指标
     */
    public void updateFrameTime(double frameTimeMs) {
        this.currentFrameTime = frameTimeMs;
        this.minFrameTime = Math.min(minFrameTime, frameTimeMs);
        this.maxFrameTime = Math.max(maxFrameTime, frameTimeMs);
        
        // 更新平均值
        double delta = frameTimeMs - averageFrameTime;
        averageFrameTime += delta / frameCount;
    }
    
    /**
     * 更新MSPT指标
     */
    public void updateMspt(float mspt) {
        this.currentMspt = mspt;
        this.minMspt = Math.min(minMspt, mspt);
        this.maxMspt = Math.max(maxMspt, mspt);
        
        msptCount++;
        // 更新平均值
        float delta = mspt - averageMspt;
        averageMspt += delta / msptCount;
    }
    
    /**
     * 开始收集性能数据
     */
    public void startCollection() {
        // 创建输出文件
        try {
            setupOutputFile();
        } catch (IOException e) {
            LOGGER.error("无法创建输出文件", e);
            return;
        }
        
        // 写入CSV头
        try {
            dataWriter.write("timestamp,fps,frameTime,mspt,playerX,playerY,playerZ,loadedChunks\n");
            dataWriter.flush();
        } catch (IOException e) {
            LOGGER.error("无法写入CSV头", e);
            closeWriter();
            return;
        }
        
        // 启动计时器进行定期采样
        samplingTimer = new Timer("MetricsSampler");
        samplingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collectAndWriteMetrics();
            }
        }, SAMPLE_INTERVAL_MS, SAMPLE_INTERVAL_MS);
        
        LOGGER.info("已开始收集性能指标，每 " + SAMPLE_INTERVAL_MS/1000 + " 秒采样一次");
    }
    
    /**
     * 停止收集性能数据
     */
    public void stopCollection() {
        if (samplingTimer != null) {
            samplingTimer.cancel();
            samplingTimer = null;
        }
        
        closeWriter();
        LOGGER.info("已停止收集性能指标，数据保存至 " + outputFilename);
        
        // 打印性能报告
        printReport();
    }
    
    /**
     * 设置输出文件
     */
    private void setupOutputFile() throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        
        // 使用Java临时目录属性获取系统临时目录
        String tmpDir = System.getProperty("java.io.tmpdir");
        File outputDir = new File(tmpDir, "game_play");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        outputFilename = outputDir.getPath() + File.separator + "perf_" + timestamp + ".csv";
        dataWriter = new BufferedWriter(new FileWriter(outputFilename));
    }
    
    /**
     * 关闭文件写入器
     */
    private void closeWriter() {
        if (dataWriter != null) {
            try {
                dataWriter.close();
            } catch (IOException e) {
                LOGGER.error("关闭数据写入器时出错", e);
            }
            dataWriter = null;
        }
    }
    
    /**
     * 收集并记录性能指标
     */
    private void collectAndWriteMetrics() {
        if (client == null || client.player == null || dataWriter == null) return;
        
        try {
            // 收集数据
            long timestamp = System.currentTimeMillis();
            double fps = currentFps;
            double frameTime = currentFrameTime;
            float mspt = currentMspt;
            
            // 收集玩家位置信息
            double playerX = client.player.getX();
            double playerY = client.player.getY();
            double playerZ = client.player.getZ();
            
            // 获取已加载区块数量
            int renderDistance = client.options.getViewDistance().getValue();
            int loadedChunks = (2 * renderDistance + 1) * (2 * renderDistance + 1);
            
            // 写入CSV行
            String dataLine = String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d\n",
                    timestamp, fps, frameTime, mspt, playerX, playerY, playerZ, loadedChunks);
            
            dataWriter.write(dataLine);
            dataWriter.flush();
        } catch (Exception e) {
            LOGGER.error("收集或写入性能指标时出错", e);
        }
    }
    
    /**
     * 打印性能报告
     */
    public void printReport() {
        LOGGER.info("===== 性能指标报告 =====");
        LOGGER.info(String.format("平均帧率: %.2f FPS (min: %.2f, max: %.2f)", averageFps, minFps, maxFps));
        LOGGER.info(String.format("平均帧时间: %.2f ms (min: %.2f, max: %.2f)", averageFrameTime, minFrameTime, maxFrameTime));
        LOGGER.info(String.format("平均MSPT: %.2f ms (min: %.2f, max: %.2f)", averageMspt, minMspt, maxMspt));
        LOGGER.info("=======================");
    }
} 