package com.mineprofiler.metrics;

import com.mineprofiler.MineProfilerMod;
import com.mineprofiler.config.TestConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.world.chunk.ChunkManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 轻量级性能指标收集器
 * 收集主要性能指标并记录到文件
 */
public class LightweightMetrics {
    private final TestConfig config;
    private final MinecraftClient client;
    
    // 性能数据
    private volatile double currentFps = 0;
    private volatile double lastMspt = 0;
    private volatile double lastFrameTimeMs = 0;
    
    // 采样计时器
    private Timer samplingTimer;
    private BufferedWriter dataWriter;
    
    // 文件名
    private String outputFilename;
    
    public LightweightMetrics(TestConfig config) {
        this.config = config;
        this.client = MinecraftClient.getInstance();
    }
    
    /**
     * 开始收集性能数据
     */
    public void startCollection() {
        // 创建输出文件
        try {
            setupOutputFile();
        } catch (IOException e) {
            MineProfilerMod.LOGGER.error("Failed to create output file", e);
            return;
        }
        
        // 写入CSV头
        try {
            dataWriter.write("timestamp,fps,mspt,frameTime,playerX,playerY,playerZ,loadedChunks,visibleEntities\n");
            dataWriter.flush();
        } catch (IOException e) {
            MineProfilerMod.LOGGER.error("Failed to write CSV header", e);
            closeWriter();
            return;
        }
        
        // 启动计时器进行定期采样
        int intervalMs = config.getMetrics().getSampleInterval() * 1000;
        samplingTimer = new Timer("MetricsSampler");
        samplingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                collectAndWriteMetrics();
            }
        }, intervalMs, intervalMs);
        
        MineProfilerMod.LOGGER.info("Started metrics collection, sampling every " + 
                config.getMetrics().getSampleInterval() + " seconds");
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
        MineProfilerMod.LOGGER.info("Stopped metrics collection, data saved to " + outputFilename);
    }
    
    /**
     * 设置输出文件
     */
    private void setupOutputFile() throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = dateFormat.format(new Date());
        
        File outputDir = new File(config.getMetrics().getOutputDirectory());
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
                MineProfilerMod.LOGGER.error("Error closing data writer", e);
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
            double fps = currentFps; // 由GameRendererMixin更新
            double mspt = lastMspt; // 由MinecraftClientMixin更新
            double frameTime = lastFrameTimeMs; // 由GameRendererMixin更新
            
            double playerX = client.player.getX();
            double playerY = client.player.getY();
            double playerZ = client.player.getZ();
            
            // 获取已加载区块数量
            int loadedChunks = getLoadedChunkCount();
            
            // 获取可见实体数量
            int visibleEntities = 0;
            if (client.world != null) {
                // Minecraft 1.21.5中getEntityCount方法已更改
                // 手动计数实体
                for (Object entity : client.world.getEntities()) {
                    visibleEntities++;
                }
            }
            
            // 写入CSV行
            String dataLine = String.format("%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d\n",
                    timestamp, fps, mspt, frameTime, playerX, playerY, playerZ, loadedChunks, visibleEntities);
            
            dataWriter.write(dataLine);
            dataWriter.flush();
        } catch (Exception e) {
            MineProfilerMod.LOGGER.error("Error collecting or writing metrics", e);
        }
    }
    
    /**
     * 获取已加载区块数量
     */
    private int getLoadedChunkCount() {
        if (client.world == null) return 0;
        
        try {
            // 尝试直接通过ClientChunkManager获取
            ChunkManager chunkManager = client.world.getChunkManager();
            if (chunkManager instanceof ClientChunkManager) {
                // 尝试多种方法获取已加载区块数量
                
                // 方法1: 使用反射查找getLoadedChunkCount方法
                try {
                    Method getLoadedChunkCountMethod = ClientChunkManager.class.getDeclaredMethod("getLoadedChunkCount");
                    getLoadedChunkCountMethod.setAccessible(true);
                    return (int) getLoadedChunkCountMethod.invoke(chunkManager);
                } catch (Exception ignored) {
                    // 方法不存在，继续尝试其他方法
                }
                
                // 方法2: 使用反射获取chunks字段并计算大小
                try {
                    Field chunksField = ClientChunkManager.class.getDeclaredField("chunks");
                    chunksField.setAccessible(true);
                    Object chunks = chunksField.get(chunkManager);
                    if (chunks instanceof java.util.Map) {
                        return ((java.util.Map<?, ?>) chunks).size();
                    }
                } catch (Exception ignored) {
                    // 字段不存在或类型不匹配，继续尝试
                }
                
                // 方法3: 估算区块数量
                // 通常渲染距离为8-16区块，假设每个方向都是如此
                int renderDistance = client.options.getViewDistance().getValue();
                // 计算一个近似值
                return (2 * renderDistance + 1) * (2 * renderDistance + 1);
            }
        } catch (Exception e) {
            // 如果出现任何异常，使用默认值
            MineProfilerMod.LOGGER.debug("Error getting chunk count", e);
        }
        
        // 默认值：估算已加载区块数量
        int renderDistance = client.options.getViewDistance().getValue();
        return (2 * renderDistance + 1) * (2 * renderDistance + 1);
    }
    
    /**
     * 更新当前FPS值 (由Mixin调用)
     */
    public void updateFps(double fps) {
        this.currentFps = fps;
    }
    
    /**
     * 更新帧时间 (由Mixin调用)
     */
    public void updateFrameTime(double frameTimeMs) {
        this.lastFrameTimeMs = frameTimeMs;
    }
    
    /**
     * 更新MSPT值 (由Mixin调用)
     */
    public void updateMspt(double mspt) {
        this.lastMspt = mspt;
    }
} 