package com.mineprofiler.automation;

import com.mineprofiler.MineProfilerMod;
import com.mineprofiler.config.TestConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.GameMode;
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
    // 初始位置
    private Vec3d initialPosition;
    // 相机旋转角度
    private float currentRotation = 0.0f;
    
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
        
        // 如果这是第一次移动，记录开始时间和初始位置
        if (movementStartTime == 0) {
            movementStartTime = System.currentTimeMillis();
            initialPosition = player.getPos();
            
            // 设置初始高度
            if (config.getTest().isUseSpectatorMode()) {
                double flyHeight = config.getTest().getFlyHeight();
                Vec3d newPos = new Vec3d(initialPosition.x, flyHeight, initialPosition.z);
                player.setPosition(newPos.x, newPos.y, newPos.z);
                MineProfilerMod.LOGGER.info("Setting initial spectator position at height: " + flyHeight);
            }
        }
        
        String movementType = config.getTest().getMovementType();
        double speed = config.getTest().getMovementSpeed();
        
        // 根据游戏模式和移动类型控制移动
        if (client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
            moveInSpectatorMode(player, movementType, speed);
        } else {
            moveInNormalMode(player, movementType, speed);
        }
    }
    
    /**
     * 在旁观模式下控制移动
     */
    private void moveInSpectatorMode(ClientPlayerEntity player, String movementType, double speed) {
        long currentTime = System.currentTimeMillis();
        long elapsedMs = currentTime - movementStartTime;
        
        // 调整速度，因为旁观模式下移动更快
        double adjustedSpeed = speed * 0.05;
        
        if ("straight_line".equals(movementType)) {
            // 直线移动
            Vec3d direction = new Vec3d(0, 0, 1); // 始终向Z轴正方向移动
            Vec3d movement = direction.multiply(adjustedSpeed);
            
            // 使用setVelocity可能在旁观模式下不起作用，所以直接更新位置
            Vec3d currentPos = player.getPos();
            player.setPosition(currentPos.x + movement.x, currentPos.y, currentPos.z + movement.z);
            
            // 可选：旋转视角
            if (config.getTest().isAutoRotateCamera()) {
                // 每秒旋转角度
                float rotationSpeed = (float) config.getTest().getRotationSpeed();
                currentRotation += rotationSpeed;
                if (currentRotation >= 360.0f) currentRotation -= 360.0f;
                player.setYaw(currentRotation);
            }
        } else if ("circular".equals(movementType)) {
            // 圆形移动 - 以初始位置为中心
            // 20秒完成一个圆
            double angleInRadians = (elapsedMs % 20000) / 20000.0 * 2 * Math.PI;
            
            // 圆的半径
            double radius = 20.0 * speed;
            
            // 计算新位置
            double x = initialPosition.x + Math.sin(angleInRadians) * radius;
            double z = initialPosition.z + Math.cos(angleInRadians) * radius;
            
            // 设置位置
            player.setPosition(x, player.getY(), z);
            
            // 更新视角朝向圆心
            float yaw = (float) Math.toDegrees(angleInRadians) + 180.0f;
            player.setYaw(yaw);
            
            // 稍微俯视
            player.setPitch(15.0f);
        } else if ("random".equals(movementType)) {
            // 每5秒改变一次方向，但保持平滑移动
            if (currentTime - movementStartTime > 5000) {
                double randomAngle = Math.random() * 2 * Math.PI;
                lastMovementDirection = new Vec3d(
                        Math.sin(randomAngle) * adjustedSpeed,
                        0,
                        Math.cos(randomAngle) * adjustedSpeed
                );
                movementStartTime = currentTime;
                
                // 根据新方向调整视角
                float yaw = (float) Math.toDegrees(Math.atan2(
                        lastMovementDirection.x,
                        lastMovementDirection.z));
                player.setYaw(yaw);
            }
            
            // 继续移动
            Vec3d currentPos = player.getPos();
            player.setPosition(
                    currentPos.x + lastMovementDirection.x,
                    currentPos.y,
                    currentPos.z + lastMovementDirection.z
            );
        }
    }
    
    /**
     * 在正常模式（创造/生存）下控制移动
     */
    private void moveInNormalMode(ClientPlayerEntity player, String movementType, double speed) {
        long currentTime = System.currentTimeMillis();
        double adjustedSpeed = speed * 0.2; // 调整移动速度
        
        if ("straight_line".equals(movementType)) {
            // 直线移动
            movePlayerInDirection(player, new Vec3d(0, 0, 1), adjustedSpeed);
        } else if ("random".equals(movementType)) {
            // 随机移动 - 每5秒改变一次方向
            if (currentTime - movementStartTime > 5000) {
                // 改变方向
                double randomAngle = Math.random() * 2 * Math.PI;
                lastMovementDirection = new Vec3d(
                        Math.sin(randomAngle),
                        0,
                        Math.cos(randomAngle)
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
            movePlayerInDirection(player, lastMovementDirection, adjustedSpeed);
        } else if ("circular".equals(movementType)) {
            // 圆形移动 - 通过逐渐改变视角实现
            long elapsedMs = currentTime - movementStartTime;
            
            // 20秒完成一个圆
            double angleInRadians = (elapsedMs % 20000) / 20000.0 * 2 * Math.PI;
            
            // 更新玩家视角
            float yaw = (float) Math.toDegrees(angleInRadians);
            player.setYaw(yaw);
            
            // 继续往前走
            movePlayerForward(adjustedSpeed);
        }
    }
    
    /**
     * 控制玩家直接向前移动
     */
    private void movePlayerForward(double speed) {
        // 直接设置速度
        ClientPlayerEntity player = client.player;
        if (player != null) {
            Vec3d lookVec = player.getRotationVector();
            Vec3d movement = lookVec.multiply(speed);
            
            // 保持Y轴速度不变，避免干扰跳跃/下落
            Vec3d currentVelocity = player.getVelocity();
            player.setVelocity(movement.x, currentVelocity.y, movement.z);
        }
    }
    
    /**
     * 控制玩家向指定方向移动
     */
    private void movePlayerInDirection(ClientPlayerEntity player, Vec3d direction, double speed) {
        if (player != null) {
            Vec3d normalizedDir = direction.normalize();
            Vec3d movement = normalizedDir.multiply(speed);
            
            // 保持Y轴速度不变
            Vec3d currentVelocity = player.getVelocity();
            player.setVelocity(movement.x, currentVelocity.y, movement.z);
        }
    }
    
    /**
     * 停止所有移动
     */
    public void stopMovement() {
        ClientPlayerEntity player = client.player;
        if (player != null) {
            if (client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
                // 在旁观模式下，无需停止速度，因为我们直接设置位置
            } else {
                // 在其他模式下，停止速度
                Vec3d currentVelocity = player.getVelocity();
                player.setVelocity(0, currentVelocity.y, 0);
            }
        }
    }
} 