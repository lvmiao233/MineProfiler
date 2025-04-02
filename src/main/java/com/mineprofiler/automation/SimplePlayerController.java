package com.mineprofiler.automation;

import com.mineprofiler.MineProfilerMod;
import com.mineprofiler.config.TestConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;

/**
 * 简单的玩家控制器
 * 根据配置控制玩家自动移动
 */
public class SimplePlayerController {
    private final TestConfig config;
    private final MinecraftClient client;
    
    // 上次移动方向
    private Vec3d lastMovementDirection;
    // 移动开始时间
    private long movementStartTime;
    
    public SimplePlayerController(TestConfig config) {
        this.config = config;
        this.client = MinecraftClient.getInstance();
        this.lastMovementDirection = new Vec3d(0, 0, 1); // 默认向前移动
        this.movementStartTime = 0;
    }
    
    /**
     * 在客户端tick中调用，控制玩家移动
     */
    public void updatePlayerMovement() {
        ClientPlayerEntity player = client.player;
        if (player == null || !MineProfilerMod.getInstance().isTestRunning()) return;
        
        // 如果这是第一次移动，记录开始时间
        if (movementStartTime == 0) {
            movementStartTime = System.currentTimeMillis();
        }
        
        String movementType = config.getTest().getMovementType();
        double speed = config.getTest().getMovementSpeed();
        
        if ("straight_line".equals(movementType)) {
            // 直线移动 - 直接控制速度
            movePlayerForward(speed);
        } else if ("random".equals(movementType)) {
            // 随机移动 - 每5秒改变一次方向
            long currentTime = System.currentTimeMillis();
            if (currentTime - movementStartTime > 5000) {
                // 改变方向
                double randomAngle = Math.random() * 2 * Math.PI;
                lastMovementDirection = new Vec3d(
                        Math.sin(randomAngle) * speed,
                        0,
                        Math.cos(randomAngle) * speed
                );
                movementStartTime = currentTime;
                
                // 根据新方向调整视角
                float yaw = (float) Math.toDegrees(Math.atan2(
                        lastMovementDirection.x,
                        lastMovementDirection.z));
                player.setYaw(yaw);
                
                MineProfilerMod.LOGGER.info("Changed movement direction: " + lastMovementDirection);
            }
            
            // 继续往前走
            movePlayerForward(speed);
        } else if ("circular".equals(movementType)) {
            // 圆形移动 - 通过逐渐改变视角实现
            long currentTime = System.currentTimeMillis();
            long elapsedMs = currentTime - movementStartTime;
            
            // 20秒完成一个圆
            double angleInRadians = (elapsedMs % 20000) / 20000.0 * 2 * Math.PI;
            
            // 更新玩家视角
            float yaw = (float) Math.toDegrees(angleInRadians);
            player.setYaw(yaw);
            
            // 继续往前走
            movePlayerForward(speed);
        }
    }
    
    /**
     * 控制玩家直接向前移动
     */
    private void movePlayerForward(double speed) {
        // 直接设置速度，不再依赖KeyBindingMixin
        ClientPlayerEntity player = client.player;
        if (player != null) {
            Vec3d lookVec = player.getRotationVector();
            double adjustedSpeed = speed * 0.2; // 调整移动速度
            Vec3d movement = lookVec.multiply(adjustedSpeed);
            
            // 保持Y轴速度不变，避免干扰跳跃/下落
            Vec3d currentVelocity = player.getVelocity();
            player.setVelocity(movement.x, currentVelocity.y, movement.z);
        }
    }
    
    /**
     * 停止所有移动
     */
    public void stopMovement() {
        // 直接停止玩家移动，不再依赖KeyBindingMixin
        ClientPlayerEntity player = client.player;
        if (player != null) {
            Vec3d currentVelocity = player.getVelocity();
            player.setVelocity(0, currentVelocity.y, 0);
        }
    }
} 