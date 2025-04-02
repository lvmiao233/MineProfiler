package com.mineprofiler.mixin;

import com.mineprofiler.MineProfilerMod;
import com.mineprofiler.metrics.LightweightMetrics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MinecraftClient的Mixin
 * 收集MSPT数据和控制自动测试流程
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    private long tickStartTime = 0;
    private double lastMspt = 0;
    
    /**
     * 在tick开始时记录时间
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        tickStartTime = System.nanoTime();
    }
    
    /**
     * 在tick结束时计算MSPT
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickEnd(CallbackInfo ci) {
        if (tickStartTime == 0) return;
        
        if (MineProfilerMod.getInstance() != null && MineProfilerMod.getInstance().isTestRunning()) {
            long tickEndTime = System.nanoTime();
            long tickTimeNanos = tickEndTime - tickStartTime;
            lastMspt = tickTimeNanos / 1_000_000.0;
            
            // 获取指标收集器并更新MSPT
            LightweightMetrics metrics = MineProfilerMod.getInstance().getMetrics();
            if (metrics != null) {
                metrics.updateMspt(lastMspt);
            }
        }
    }
    
    /**
     * 在窗口关闭时停止测试
     */
    @Inject(method = "stop", at = @At("HEAD"))
    private void onGameStop(CallbackInfo ci) {
        if (MineProfilerMod.getInstance() != null && MineProfilerMod.getInstance().isTestRunning()) {
            MineProfilerMod.getInstance().endTest();
        }
    }
} 