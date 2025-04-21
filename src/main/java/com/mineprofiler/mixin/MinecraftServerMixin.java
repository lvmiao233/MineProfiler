package com.mineprofiler.mixin;

import com.mineprofiler.MineProfilerMod;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MinecraftServer的Mixin
 * 用于获取服务器MSPT数据
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    
    @Shadow
    private float averageTickTime;
    
    /**
     * 在服务器tick结束时收集MSPT数据
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void onServerTick(CallbackInfo ci) {
        if (MineProfilerMod.getInstance() != null && MineProfilerMod.getInstance().getMetrics() != null) {
            // averageTickTime已经是毫秒单位，直接使用
            MineProfilerMod.getInstance().getMetrics().updateMspt(this.averageTickTime);
        }
    }
} 