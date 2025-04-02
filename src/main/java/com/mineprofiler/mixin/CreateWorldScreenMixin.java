package com.mineprofiler.mixin;

import com.mineprofiler.MineProfilerMod;
import com.mineprofiler.config.TestConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * CreateWorldScreen的Mixin
 * 自动设置世界参数并创建世界
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen {
    
    // 构造函数必须由于继承Screen
    protected CreateWorldScreenMixin(Text title) {
        super(title);
    }
    
    /**
     * 在CreateWorldScreen初始化完成后自动设置参数并创建世界
     */
    @Inject(method = "init", at = @At("RETURN"))
    private void onInitialized(CallbackInfo ci) {
        if (this.client == null) return;
        
        // 延迟40 ticks (约2秒)后执行，确保界面已完全加载
        this.client.execute(() -> {
            try {
                // 获取配置
                TestConfig config = MineProfilerMod.getInstance().getConfig();
                String worldName = config.getWorld().getWorldName();
                String seed = config.getWorld().getSeed();
                String gameMode = config.getWorld().getGameMode();
                
                MineProfilerMod.LOGGER.info("Automatically setting up world with name: " + worldName + ", seed: " + seed);
                
                // 通过反射设置世界名称
                for (Field field : CreateWorldScreen.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(this.client.currentScreen);
                    
                    // 设置世界名称
                    if (value instanceof TextFieldWidget) {
                        TextFieldWidget textField = (TextFieldWidget) value;
                        String text = textField.getText();
                        // 检查是否是世界名称输入框
                        if (text != null && (text.isEmpty() || text.equals("World") || text.equals("新的世界"))) {
                            textField.setText(worldName);
                        }
                    }
                    
                    // 设置种子
                    if (field.getName().contains("seed") && value instanceof TextFieldWidget) {
                        TextFieldWidget seedField = (TextFieldWidget) value;
                        seedField.setText(seed);
                    }
                    
                    // 设置游戏模式
                    if (value instanceof CyclingButtonWidget) {
                        CyclingButtonWidget<?> cyclingButton = (CyclingButtonWidget<?>) value;
                        String buttonText = cyclingButton.getMessage().getString().toLowerCase();
                        
                        if (buttonText.contains("game mode") || buttonText.contains("游戏模式")) {
                            // 根据配置的游戏模式设置按钮值
                            GameMode targetMode = GameMode.CREATIVE; // 默认创造模式
                            
                            if (gameMode.equalsIgnoreCase("survival")) {
                                targetMode = GameMode.SURVIVAL;
                            } else if (gameMode.equalsIgnoreCase("adventure")) {
                                targetMode = GameMode.ADVENTURE;
                            } else if (gameMode.equalsIgnoreCase("spectator")) {
                                targetMode = GameMode.SPECTATOR;
                            }
                            
                            // 多次点击，直到达到目标模式
                            for (int i = 0; i < 4; i++) {
                                String currentText = cyclingButton.getMessage().getString().toLowerCase();
                                if ((targetMode == GameMode.CREATIVE && currentText.contains("creative")) ||
                                    (targetMode == GameMode.SURVIVAL && currentText.contains("survival")) ||
                                    (targetMode == GameMode.ADVENTURE && currentText.contains("adventure")) ||
                                    (targetMode == GameMode.SPECTATOR && currentText.contains("spectator"))) {
                                    break;
                                }
                                cyclingButton.onPress();
                            }
                        }
                    }
                }
                
                // 使用反射获取私有字段drawables
                List<ClickableWidget> clickableWidgets = new ArrayList<>();
                try {
                    Field drawablesField = Screen.class.getDeclaredField("drawables");
                    drawablesField.setAccessible(true);
                    Object drawables = drawablesField.get(this);
                    
                    if (drawables instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> elements = (List<Object>) drawables;
                        for (Object element : elements) {
                            if (element instanceof ClickableWidget) {
                                clickableWidgets.add((ClickableWidget) element);
                            }
                        }
                    }
                } catch (Exception e) {
                    MineProfilerMod.LOGGER.error("Failed to get drawables", e);
                }
                
                // 查找并点击"创建"按钮
                ButtonWidget createButton = null;
                for (ClickableWidget widget : clickableWidgets) {
                    String message = widget.getMessage().getString().toLowerCase();
                    if (message.contains("create") || message.contains("done") || 
                        message.contains("创建") || message.contains("完成")) {
                        if (widget instanceof ButtonWidget) {
                            createButton = (ButtonWidget) widget;
                            break;
                        }
                    }
                }
                
                // 再延迟1秒后点击创建按钮
                if (createButton != null) {
                    final ButtonWidget finalCreateButton = createButton;
                    this.client.execute(() -> {
                        MineProfilerMod.LOGGER.info("Creating world: " + worldName);
                        finalCreateButton.onPress();
                    });
                } else {
                    MineProfilerMod.LOGGER.error("Could not find create button");
                }
            } catch (Exception e) {
                MineProfilerMod.LOGGER.error("Failed to set up world creation parameters", e);
            }
        });
    }
} 