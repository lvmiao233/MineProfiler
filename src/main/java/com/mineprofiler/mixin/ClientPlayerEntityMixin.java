package com.mineprofiler.mixin;

import com.mineprofiler.MineProfilerMod;
import com.mineprofiler.automation.SimplePlayerController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ClientPlayerEntity的Mixin
 * 实现自动角色移动
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    private SimplePlayerController playerController;
    
    /**
     * 在玩家更新时控制移动
     */
    @Inject(method = "tick", at = @At("RETURN"))
    private void onPlayerTick(CallbackInfo ci) {
        if (MineProfilerMod.getInstance() == null || !MineProfilerMod.getInstance().isTestRunning()) {
            return;
        }
        
        // 懒加载玩家控制器
        if (playerController == null) {
            playerController = new SimplePlayerController(MineProfilerMod.getInstance().getConfig());
        }
        
        // 更新玩家移动
        playerController.updatePlayerMovement();
    }
} 