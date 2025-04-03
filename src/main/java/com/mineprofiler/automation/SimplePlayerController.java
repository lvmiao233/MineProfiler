package com.mineprofiler.automation;

import com.mineprofiler.MineProfilerMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * 简单的玩家控制器
 * 只实现直线移动+视角旋转
 */
public class SimplePlayerController {
    private final MinecraftClient client;
    
    // 移动开始时间
    private long movementStartTime;
    // 初始位置
    private Vec3d initialPosition;
    // 相机旋转角度
    private float currentRotation = 0.0f;
    // 是否已激活自动移动
    private boolean autoMovementActive = false;
    // 移动速度
    private final double MOVE_SPEED = 0.2;
    // 旋转速度(度/帧)
    private final float ROTATION_SPEED = 0.5f;
    
    /**
     * 默认构造函数
     */
    public SimplePlayerController() {
        this.client = MinecraftClient.getInstance();
        this.movementStartTime = 0;
    }
    
    /**
     * 激活自动移动
     */
    public void activateAutoMovement() {
        this.autoMovementActive = true;
        this.movementStartTime = System.currentTimeMillis();
        MineProfilerMod.LOGGER.info("已激活自动移动");
    }
    
    /**
     * 停止自动移动
     */
    public void deactivateAutoMovement() {
        this.autoMovementActive = false;
        stopMovement();
        MineProfilerMod.LOGGER.info("已停止自动移动");
    }
    
    /**
     * 在客户端tick中调用，控制玩家移动
     */
    public void updatePlayerMovement() {
        ClientPlayerEntity player = client.player;
        if (player == null || !autoMovementActive) return;
        
        // 如果这是第一次移动，记录初始位置
        if (initialPosition == null) {
            initialPosition = player.getPos();
            MineProfilerMod.LOGGER.info("开始自动移动，初始位置: " + initialPosition);
        }
        
        // 执行直线移动
        straightLineMovement(player);
    }
    
    /**
     * 直线移动
     * 同时沿Z轴匀速移动并旋转视角
     */
    private void straightLineMovement(ClientPlayerEntity player) {
        // 方向向量：沿Z轴正方向移动
        Vec3d direction = new Vec3d(0, 0, 1);
        Vec3d movement = direction.multiply(MOVE_SPEED);
        
        // 更新位置
        Vec3d currentPos = player.getPos();
        player.setPosition(currentPos.x + movement.x, currentPos.y, currentPos.z + movement.z);
        
        // 旋转视角
        currentRotation += ROTATION_SPEED;
        if (currentRotation >= 360.0f) currentRotation -= 360.0f;
        player.setYaw(currentRotation);
    }
    
    /**
     * 停止所有移动
     */
    public void stopMovement() {
        ClientPlayerEntity player = client.player;
        if (player != null) {
            MineProfilerMod.LOGGER.info("停止所有移动");
        }
    }
    
    /**
     * 获取自动移动状态
     */
    public boolean isAutoMovementActive() {
        return autoMovementActive;
    }
} 