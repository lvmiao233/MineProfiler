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
 * 简化版本，只用来记录渲染开始时间
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    private static long lastFrameTime = 0;
    
    /**
     * 在每帧渲染前注入
     * 注意：Minecraft 1.21.5中render方法的签名已更改
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        // 记录帧时间，性能指标收集功能已移至主类中实现
        lastFrameTime = System.currentTimeMillis();
    }
} 