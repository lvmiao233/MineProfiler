package com.mineprofiler.mixin;

import com.mineprofiler.MineProfilerMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MinecraftClient的Mixin
 * 处理游戏退出时的清理
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    
    /**
     * 在窗口关闭时确保资源被正确释放
     */
    @Inject(method = "stop", at = @At("HEAD"))
    private void onGameStop(CallbackInfo ci) {
        if (MineProfilerMod.getInstance() != null) {
            // 确保玩家控制器已停止
            if (MineProfilerMod.getInstance().getPlayerController() != null) {
                MineProfilerMod.getInstance().getPlayerController().stopMovement();
            }
            
            // 确保性能指标收集器已停止并保存数据
            if (MineProfilerMod.getInstance().getMetrics() != null) {
                MineProfilerMod.getInstance().getMetrics().stopCollection();
            }
            
            MineProfilerMod.LOGGER.info("MineProfiler模组资源已释放");
        }
    }
} 