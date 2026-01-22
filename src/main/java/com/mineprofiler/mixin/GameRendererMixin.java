package com.mineprofiler.mixin;

import com.mineprofiler.MineProfilerMod;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 游戏渲染器的Mixin
 * 测量每帧的渲染耗时（render函数执行时间）
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    // 渲染开始时间（纳秒）
    private static long renderStartTime = 0;
    
    /**
     * 在每帧渲染前注入，记录开始时间
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        renderStartTime = System.nanoTime();
    }
    
    /**
     * 在每帧渲染后注入，计算渲染耗时并更新指标
     */
    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderEnd(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (renderStartTime > 0) {
            double renderTimeMs = (System.nanoTime() - renderStartTime) / 1_000_000.0;
            if (MineProfilerMod.getInstance() != null && 
                MineProfilerMod.getInstance().getMetrics() != null) {
                MineProfilerMod.getInstance().getMetrics().updateFrameTime(renderTimeMs);
            }
        }
    }
}
 