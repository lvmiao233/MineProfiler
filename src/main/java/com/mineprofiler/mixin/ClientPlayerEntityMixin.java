package com.mineprofiler.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ClientPlayerEntity的Mixin
 * 简化版本 - 玩家控制逻辑已移至主类
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    
    /**
     * 在玩家更新时注入
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void onPlayerTick(CallbackInfo ci) {
        // 玩家控制逻辑已移至主类的ClientTickEvents中处理
    }
} 