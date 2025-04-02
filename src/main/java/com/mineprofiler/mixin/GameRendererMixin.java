package com.mineprofiler.mixin;

import com.mineprofiler.MineProfilerMod;
import com.mineprofiler.metrics.LightweightMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * GameRenderer的Mixin
 * 收集FPS和帧时间数据
 * 
 * 注意：该Mixin已在mineprofiler.mixins.json中禁用
 * 由于Minecraft 1.21.5中GameRenderer.render方法签名发生变化
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;
    
    private long frameStartTime = 0;
    private double lastFps = 0;
    
    /**
     * 在帧渲染开始时记录时间
     * 注意：该方法已被禁用，现在通过MineProfilerMod类中的updatePerformanceMetrics方法实现
     */
    // @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(Object renderer, boolean tick, CallbackInfo ci) {
        if (frameStartTime == 0) {
            frameStartTime = System.nanoTime();
            return;
        }
        
        // 计算FPS和帧时间
        if (MineProfilerMod.getInstance() != null && MineProfilerMod.getInstance().isTestRunning()) {
            // 使用Minecraft的FPS计数器
            lastFps = client.getCurrentFps();
            
            // 获取指标收集器并更新FPS
            LightweightMetrics metrics = MineProfilerMod.getInstance().getMetrics();
            if (metrics != null) {
                metrics.updateFps(lastFps);
            }
        }
    }
    
    /**
     * 在帧渲染结束时计算帧时间
     * 注意：该方法已被禁用，现在通过MineProfilerMod类中的updatePerformanceMetrics方法实现
     */
    // @Inject(method = "render", at = @At("RETURN"))
    private void onRenderEnd(Object renderer, boolean tick, CallbackInfo ci) {
        if (frameStartTime == 0) return;
        
        if (MineProfilerMod.getInstance() != null && MineProfilerMod.getInstance().isTestRunning()) {
            long frameEndTime = System.nanoTime();
            long frameTimeNanos = frameEndTime - frameStartTime;
            double frameTimeMs = frameTimeNanos / 1_000_000.0;
            
            // 获取指标收集器并更新帧时间
            LightweightMetrics metrics = MineProfilerMod.getInstance().getMetrics();
            if (metrics != null) {
                metrics.updateFrameTime(frameTimeMs);
            }
            
            // 为下一帧做准备
            frameStartTime = System.nanoTime();
        }
    }
} 