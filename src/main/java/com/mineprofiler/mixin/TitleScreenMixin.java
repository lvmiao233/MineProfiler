package com.mineprofiler.mixin;

import com.mineprofiler.MineProfilerMod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * TitleScreen的Mixin
 * 自动点击"单人游戏"按钮
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {
    
    // 构造函数必须由于继承Screen
    protected TitleScreenMixin(Text title) {
        super(title);
    }
    
    /**
     * 在TitleScreen初始化完成后自动点击"单人游戏"按钮
     */
    @Inject(method = "init", at = @At("RETURN"))
    private void onInitialized(CallbackInfo ci) {
        // 检查是否已经初始化
        if (this.client == null) return;
        
        // 延迟20 ticks (约1秒)后执行，确保界面已完全加载
        this.client.execute(() -> {
            MineProfilerMod.LOGGER.info("Automatically navigating to Singleplayer screen");
            
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
            
            // 查找并点击单人游戏按钮
            boolean found = false;
            for (ClickableWidget widget : clickableWidgets) {
                String message = widget.getMessage().getString().toLowerCase();
                if (message.contains("singleplayer") || message.contains("单人游戏")) {
                    if (widget instanceof ButtonWidget) {
                        ((ButtonWidget) widget).onPress();
                        found = true;
                        break;
                    }
                }
            }
            
            // 如果没有找到按钮，直接打开单人游戏界面
            if (!found && this.client.currentScreen instanceof TitleScreen) {
                this.client.setScreen(new SelectWorldScreen(this.client.currentScreen));
            }
        });
    }
} 