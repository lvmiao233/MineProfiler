package com.mineprofiler.mixin;

import com.mineprofiler.MineProfilerMod;
import com.mineprofiler.world.AutoWorldManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * SelectWorldScreen的Mixin
 * 自动选择或创建测试世界
 */
@Mixin(SelectWorldScreen.class)
public abstract class SelectWorldScreenMixin extends Screen {
    
    // 1.21.5版本中这个方法可能被重命名或移除
    // @Shadow protected abstract void method_19945(boolean bl); // 这是play按钮的方法
    
    @Shadow private ButtonWidget deleteButton;
    @Shadow private ButtonWidget selectButton;
    @Shadow private ButtonWidget editButton;
    @Shadow private ButtonWidget recreateButton;
    
    // 构造函数必须由于继承Screen
    protected SelectWorldScreenMixin(Text title) {
        super(title);
    }
    
    /**
     * 在SelectWorldScreen初始化完成后自动选择或创建世界
     */
    @Inject(method = "init", at = @At("RETURN"))
    private void onInitialized(CallbackInfo ci) {
        if (this.client == null) return;
        
        // 延迟20 ticks (约1秒)后执行，确保界面已完全加载
        this.client.execute(() -> {
            // 获取世界名称
            String targetWorldName = MineProfilerMod.getInstance().getConfig().getWorld().getWorldName();
            MineProfilerMod.LOGGER.info("Looking for world: " + targetWorldName);
            
            // 获取世界列表
            EntryListWidget<?> levelList = null;
            try {
                // 尝试通过反射获取世界列表
                for (Field field : SelectWorldScreen.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(this.client.currentScreen);
                    if (value instanceof EntryListWidget) {
                        levelList = (EntryListWidget<?>) value;
                        break;
                    }
                }
            } catch (Exception e) {
                MineProfilerMod.LOGGER.error("Failed to get world list", e);
            }
            
            // 如果找到了世界列表
            if (levelList != null) {
                boolean worldFound = false;
                
                // 查找目标世界
                try {
                    // 使用反射来调用受保护的方法
                    Method getEntryCountMethod = EntryListWidget.class.getDeclaredMethod("getEntryCount");
                    Method getEntryMethod = EntryListWidget.class.getDeclaredMethod("getEntry", int.class);
                    
                    // 因为Entry类型是受保护的，我们不直接引用它，而是使用Object类型
                    // 1.21.5中EntryListWidget.Entry类型可能已更改，所以尝试反射查找setSelected方法
                    Method[] methods = EntryListWidget.class.getDeclaredMethods();
                    Method setSelectedMethod = null;
                    for (Method method : methods) {
                        if (method.getName().equals("setSelected") && 
                            method.getParameterCount() == 1 && 
                            !method.getParameterTypes()[0].equals(int.class)) {
                            setSelectedMethod = method;
                            break;
                        }
                    }
                    
                    if (setSelectedMethod == null) {
                        throw new RuntimeException("Could not find setSelected method");
                    }
                    
                    getEntryCountMethod.setAccessible(true);
                    getEntryMethod.setAccessible(true);
                    setSelectedMethod.setAccessible(true);
                    
                    int entryCount = (int) getEntryCountMethod.invoke(levelList);
                    
                    for (int i = 0; i < entryCount; i++) {
                        Object entry = getEntryMethod.invoke(levelList, i);
                        // 通过反射获取LevelSummary
                        Field summaryField = null;
                        
                        // 查找包含level或Level字符串的字段
                        for (Field field : entry.getClass().getDeclaredFields()) {
                            if (field.getName().contains("level") || field.getName().contains("Level")) {
                                field.setAccessible(true);
                                Object fieldValue = field.get(entry);
                                if (fieldValue instanceof LevelSummary) {
                                    summaryField = field;
                                    break;
                                }
                            }
                        }
                        
                        if (summaryField == null) {
                            continue;
                        }
                        
                        Object summary = summaryField.get(entry);
                        
                        if (summary instanceof LevelSummary) {
                            LevelSummary levelSummary = (LevelSummary) summary;
                            if (levelSummary.getName().equals(targetWorldName)) {
                                // 选择该世界
                                setSelectedMethod.invoke(levelList, entry);
                                
                                // 点击加载按钮
                                if (selectButton != null && selectButton.active) {
                                    selectButton.onPress();
                                    worldFound = true;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    MineProfilerMod.LOGGER.error("Failed to select world", e);
                }
                
                // 如果没有找到目标世界，创建新世界
                if (!worldFound) {
                    MineProfilerMod.LOGGER.info("World not found, creating new world");
                    
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
                    
                    // 查找并点击"创建新世界"按钮
                    boolean found = false;
                    for (ClickableWidget widget : clickableWidgets) {
                        String message = widget.getMessage().getString().toLowerCase();
                        if (message.contains("create") || message.contains("new") || 
                            message.contains("创建") || message.contains("新的")) {
                            if (widget instanceof ButtonWidget) {
                                ((ButtonWidget) widget).onPress();
                                found = true;
                                break;
                            }
                        }
                    }
                    
                    // 如果没有找到按钮，尝试直接打开创建世界界面
                    if (!found && this.client.currentScreen instanceof SelectWorldScreen) {
                        openCreateWorldScreen();
                    }
                }
            } else {
                // 如果无法获取世界列表，直接打开创建世界界面
                MineProfilerMod.LOGGER.warn("Could not find world list, directly opening create world screen");
                openCreateWorldScreen();
            }
        });
    }
    
    /**
     * 打开创建世界界面
     * 在Minecraft 1.21.5中，CreateWorldScreen构造函数参数已更改
     */
    private void openCreateWorldScreen() {
        try {
            // 尝试反射获取并调用CreateWorldScreen的创建方法
            Method createMethod = null;
            
            // 查找create方法
            for (Method method : CreateWorldScreen.class.getDeclaredMethods()) {
                if (method.getName().equals("create") || method.getName().contains("create")) {
                    method.setAccessible(true);
                    createMethod = method;
                    break;
                }
            }
            
            if (createMethod != null) {
                // 尝试直接调用静态方法
                Object screen = createMethod.invoke(null, this.client.currentScreen);
                if (screen instanceof Screen) {
                    this.client.setScreen((Screen) screen);
                    return;
                }
            }
            
            // 如果找不到合适的创建方法，尝试直接实例化
            // 尝试获取参数最少的构造函数
            Constructor<?>[] constructors = CreateWorldScreen.class.getDeclaredConstructors();
            Constructor<?> simplestConstructor = null;
            int minParams = Integer.MAX_VALUE;
            
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() < minParams) {
                    minParams = constructor.getParameterCount();
                    simplestConstructor = constructor;
                }
            }
            
            if (simplestConstructor != null) {
                simplestConstructor.setAccessible(true);
                
                // 准备参数
                Object[] params = new Object[minParams];
                Class<?>[] paramTypes = simplestConstructor.getParameterTypes();
                
                for (int i = 0; i < minParams; i++) {
                    Class<?> type = paramTypes[i];
                    
                    if (type.isAssignableFrom(this.client.getClass())) {
                        params[i] = this.client;
                    } else if (type.isAssignableFrom(Screen.class)) {
                        params[i] = this.client.currentScreen;
                    } else if (type.getName().endsWith("GeneratorOptionsHolder")) {
                        // 这里改为使用类名检查，避免直接引用不存在的类
                        // 暂时传null，后续可能需要进一步处理
                        params[i] = null;
                    } else if (type.isAssignableFrom(Optional.class)) {
                        params[i] = Optional.empty();
                    } else if (type.isAssignableFrom(OptionalLong.class)) {
                        params[i] = OptionalLong.empty();
                    } else {
                        // 其他类型就传null
                        params[i] = null;
                    }
                }
                
                Object screen = simplestConstructor.newInstance(params);
                if (screen instanceof Screen) {
                    this.client.setScreen((Screen) screen);
                }
            } else {
                MineProfilerMod.LOGGER.error("Could not find a way to create CreateWorldScreen");
            }
        } catch (Exception e) {
            MineProfilerMod.LOGGER.error("Failed to open create world screen", e);
        }
    }
} 