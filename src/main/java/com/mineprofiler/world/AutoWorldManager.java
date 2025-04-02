package com.mineprofiler.world;

import com.mineprofiler.MineProfilerMod;
import com.mineprofiler.config.TestConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.world.GameMode;
import net.minecraft.world.level.storage.LevelStorage;

/**
 * 自动世界管理器，负责创建和加载测试世界
 * 注意：这个类提供了基本框架，但实际自动创建世界需要通过Mixin实现
 */
public class AutoWorldManager {
    private final TestConfig config;
    private final MinecraftClient client;
    private boolean worldCreationAttempted = false;
    
    public AutoWorldManager(TestConfig config) {
        this.config = config;
        this.client = MinecraftClient.getInstance();
    }
    
    /**
     * 检查指定名称的世界是否存在
     */
    public boolean worldExists(String worldName) {
        try {
            LevelStorage levelStorage = client.getLevelStorage();
            return levelStorage.levelExists(worldName);
        } catch (Exception e) {
            MineProfilerMod.LOGGER.error("Failed to check if world exists", e);
            return false;
        }
    }
    
    /**
     * 尝试自动创建或加载世界
     * 注意：此方法提供基本逻辑框架，实际自动创建需要通过Mixin注入到GUI处理中
     */
    public void tryCreateOrLoadWorld() {
        if (worldCreationAttempted) return;
        
        String worldName = config.getWorld().getWorldName();
        
        // 记录尝试创建世界
        worldCreationAttempted = true;
        
        // 检查当前屏幕
        Screen currentScreen = client.currentScreen;
        
        if (currentScreen instanceof TitleScreen) {
            // 如果在标题屏幕，点击"单人游戏"按钮 - 需要通过Mixin实现
            MineProfilerMod.LOGGER.info("On title screen, attempting to navigate to world selection");
            // 这里需要Mixin来自动点击按钮
        } else if (currentScreen instanceof SelectWorldScreen) {
            // 如果在选择世界屏幕，检查世界是否存在
            if (worldExists(worldName)) {
                // 如果世界存在，加载它 - 需要通过Mixin实现
                MineProfilerMod.LOGGER.info("World exists, attempting to load it");
                // 这里需要Mixin来自动选择并加载世界
            } else {
                // 如果世界不存在，创建新世界 - 需要通过Mixin实现
                MineProfilerMod.LOGGER.info("World does not exist, attempting to create it");
                // 这里需要Mixin来自动点击"创建新世界"按钮
            }
        } else if (currentScreen instanceof CreateWorldScreen) {
            // 如果在创建世界屏幕，设置世界参数并创建 - 需要通过Mixin实现
            MineProfilerMod.LOGGER.info("On create world screen, setting up world parameters");
            // 这里需要Mixin来自动填写世界参数并创建世界
        }
    }
    
    /**
     * 根据配置获取游戏模式
     */
    public GameMode getConfiguredGameMode() {
        String gameMode = config.getWorld().getGameMode().toLowerCase();
        switch (gameMode) {
            case "creative":
                return GameMode.CREATIVE;
            case "survival":
                return GameMode.SURVIVAL;
            case "adventure":
                return GameMode.ADVENTURE;
            case "spectator":
                return GameMode.SPECTATOR;
            default:
                return GameMode.CREATIVE;
        }
    }
} 