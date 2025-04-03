package com.mineprofiler.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mineprofiler.MineProfilerMod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILENAME = "mineprofiler_config.json";
    
    // 是否启用mod
    private boolean enabled = true;
    
    // 世界配置
    private WorldConfig world = new WorldConfig();
    
    // 测试配置
    private TestParameters test = new TestParameters();
    
    // 性能指标配置
    private MetricsConfig metrics = new MetricsConfig();
    
    public TestConfig() {
    }
    
    public static TestConfig createDefault() {
        return new TestConfig();
    }
    
    public static TestConfig load() throws IOException {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            throw new IOException("Config file does not exist");
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            return GSON.fromJson(reader, TestConfig.class);
        }
    }
    
    public void save() throws IOException {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();
        }
        
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
        }
    }
    
    private static File getConfigFile() {
        String gamePath = System.getProperty("user.dir");
        Path configPath = Paths.get(gamePath, CONFIG_FILENAME);
        return configPath.toFile();
    }
    
    // 世界配置类
    public static class WorldConfig {
        private String seed = "12345";
        private String worldName = "PerfTest";
        private String gameMode = "spectator"; // 默认改为旁观模式
        
        public String getSeed() {
            return seed;
        }
        
        public void setSeed(String seed) {
            this.seed = seed;
        }
        
        public String getWorldName() {
            return worldName;
        }
        
        public void setWorldName(String worldName) {
            this.worldName = worldName;
        }
        
        public String getGameMode() {
            return gameMode;
        }
        
        public void setGameMode(String gameMode) {
            this.gameMode = gameMode;
        }
    }
    
    // 测试参数类
    public static class TestParameters {
        private int duration = 300; // 测试持续时间（秒）
        private String movementType = "straight_line"; // 移动类型: straight_line, random, circular
        private double movementSpeed = 1.0; // 移动速度
        private boolean exitAfterTest = false; // 测试后是否退出游戏
        private boolean useSpectatorMode = true; // 是否使用旁观模式
        private double flyHeight = 70.0; // 飞行高度
        private boolean autoRotateCamera = true; // 是否自动旋转相机
        private double rotationSpeed = 0.5; // 相机旋转速度
        
        public int getDuration() {
            return duration;
        }
        
        public String getMovementType() {
            return movementType;
        }
        
        public double getMovementSpeed() {
            return movementSpeed;
        }
        
        public boolean isExitAfterTest() {
            return exitAfterTest;
        }
        
        public boolean isUseSpectatorMode() {
            return useSpectatorMode;
        }
        
        public double getFlyHeight() {
            return flyHeight;
        }
        
        public boolean isAutoRotateCamera() {
            return autoRotateCamera;
        }
        
        public double getRotationSpeed() {
            return rotationSpeed;
        }
    }
    
    // 性能指标配置类
    public static class MetricsConfig {
        private int sampleInterval = 1; // 采样间隔（秒）
        private String outputDirectory = "./perfdata"; // 输出目录
        private String outputFormat = "csv"; // 输出格式
        
        public int getSampleInterval() {
            return sampleInterval;
        }
        
        public String getOutputDirectory() {
            return outputDirectory;
        }
        
        public String getOutputFormat() {
            return outputFormat;
        }
    }
    
    // Getter方法
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public WorldConfig getWorld() {
        return world;
    }
    
    public TestParameters getTest() {
        return test;
    }
    
    public MetricsConfig getMetrics() {
        return metrics;
    }
    
    // 便捷方法
    public int getTestDuration() {
        return test.getDuration();
    }
    
    public String getOutputDirectory() {
        return metrics.getOutputDirectory();
    }
    
    public boolean isExitAfterTest() {
        return test.isExitAfterTest();
    }
    
    @Override
    public String toString() {
        return "TestConfig{" +
                "enabled=" + enabled +
                ", world=" + world.getWorldName() +
                ", seed=" + world.getSeed() +
                ", gameMode=" + world.getGameMode() +
                '}';
    }
} 