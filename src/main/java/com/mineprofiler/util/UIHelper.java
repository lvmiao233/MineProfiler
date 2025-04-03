package com.mineprofiler.util;

import com.mineprofiler.MineProfilerMod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 界面辅助工具类
 * 用于处理Minecraft界面元素
 */
public class UIHelper {
    
    /**
     * 从Screen实例中获取所有可点击的界面元素
     */
    public static List<ClickableWidget> getAllClickableWidgets(Screen screen) {
        List<ClickableWidget> widgets = new ArrayList<>();
        
        try {
            // 获取Screen类的所有声明字段
            for (Field field : screen.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                
                // 直接检查字段是否为ClickableWidget类型
                try {
                    Object value = field.get(screen);
                    if (value instanceof ClickableWidget) {
                        widgets.add((ClickableWidget) value);
                        MineProfilerMod.LOGGER.info("Found widget directly in field: " + fieldName);
                    }
                } catch (Exception e) {
                    // 忽略访问异常
                }
            }
            
            // 尝试从Screen的实例变量获取
            // 方法1：使用drawables字段
            tryGetWidgetsFrom(screen, "drawables", widgets);
            
            // 方法2：使用children字段
            if (widgets.isEmpty()) {
                tryGetWidgetsFrom(screen, "children", widgets);
            }
            
            // 方法3：尝试获取特定类型的List字段
            for (Field field : Screen.class.getDeclaredFields()) {
                String fieldName = field.getName().toLowerCase();
                if (fieldName.contains("widget") || fieldName.contains("button") || 
                    fieldName.contains("element") || fieldName.contains("component") ||
                    fieldName.contains("control")) {
                    tryGetWidgetsFrom(screen, field.getName(), widgets);
                }
            }
            
            // 方法4：尝试通过方法获取
            for (Method method : Screen.class.getDeclaredMethods()) {
                String methodName = method.getName().toLowerCase();
                if ((methodName.contains("get") && 
                    (methodName.contains("widget") || methodName.contains("button") || 
                     methodName.contains("component") || methodName.contains("element"))) &&
                    method.getParameterCount() == 0) {
                    try {
                        method.setAccessible(true);
                        Object result = method.invoke(screen);
                        if (result instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> elements = (List<Object>) result;
                            for (Object element : elements) {
                                if (element instanceof ClickableWidget) {
                                    widgets.add((ClickableWidget) element);
                                }
                            }
                            MineProfilerMod.LOGGER.info("Found widgets from method: " + method.getName());
                        } else if (result instanceof ClickableWidget) {
                            widgets.add((ClickableWidget) result);
                            MineProfilerMod.LOGGER.info("Found widget from method: " + method.getName());
                        }
                    } catch (Exception e) {
                        // 忽略方法调用异常
                    }
                }
            }
        } catch (Exception e) {
            MineProfilerMod.LOGGER.error("Error in getAllClickableWidgets: " + e.getMessage(), e);
        }
        
        // 输出所有找到的按钮信息以便调试
        MineProfilerMod.LOGGER.info("Found " + widgets.size() + " clickable widgets in screen: " + screen.getClass().getSimpleName());
        
        return widgets;
    }
    
    /**
     * 尝试从指定字段获取界面元素
     */
    private static void tryGetWidgetsFrom(Screen screen, String fieldName, List<ClickableWidget> widgets) {
        try {
            Field field = Screen.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(screen);
            
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> elements = (List<Object>) value;
                int countBefore = widgets.size();
                
                for (Object element : elements) {
                    if (element instanceof ClickableWidget) {
                        widgets.add((ClickableWidget) element);
                    }
                }
                
                int added = widgets.size() - countBefore;
                if (added > 0) {
                    MineProfilerMod.LOGGER.info("Added " + added + " widgets from field: " + fieldName);
                }
            }
        } catch (Exception e) {
            // 忽略这个字段并继续
        }
    }
    
    /**
     * 安全地获取按钮文本
     */
    private static String getButtonText(ClickableWidget widget) {
        try {
            Text message = widget.getMessage();
            if (message != null) {
                return message.getString();
            }
        } catch (Exception e) {
            // 如果getMessage()失败，尝试其他方法
            try {
                // 尝试通过反射获取text或message字段
                for (Field field : widget.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    String fieldName = field.getName().toLowerCase();
                    if (fieldName.contains("text") || fieldName.contains("message") || fieldName.contains("label")) {
                        Object value = field.get(widget);
                        if (value != null) {
                            return value.toString();
                        }
                    }
                }
            } catch (Exception ex) {
                // 忽略反射异常
            }
        }
        
        // 返回空字符串而不是null，避免NullPointerException
        return "";
    }
    
    /**
     * 查找包含指定文本的按钮（不区分大小写）
     */
    public static ButtonWidget findButtonWithText(List<ClickableWidget> widgets, String... textOptions) {
        // 输出所有按钮文本以便调试
        MineProfilerMod.LOGGER.info("---- Button Text Dump ----");
        for (int i = 0; i < widgets.size(); i++) {
            ClickableWidget widget = widgets.get(i);
            String text = getButtonText(widget);
            MineProfilerMod.LOGGER.info(i + ": \"" + text + "\" [" + widget.getClass().getSimpleName() + "]");
        }
        MineProfilerMod.LOGGER.info("-------------------------");
        
        // 查找匹配的按钮（不区分大小写）
        for (ClickableWidget widget : widgets) {
            if (!(widget instanceof ButtonWidget)) continue;
            
            String buttonText = getButtonText(widget).toLowerCase();
            
            // 如果按钮没有文本，尝试检查按钮ID或名称
            if (buttonText.isEmpty()) {
                try {
                    buttonText = widget.toString().toLowerCase();
                } catch (Exception e) {
                    // 忽略任何异常
                }
            }
            
            for (String textOption : textOptions) {
                // 不区分大小写比较
                String lowercaseOption = textOption.toLowerCase();
                if (buttonText.contains(lowercaseOption)) {
                    MineProfilerMod.LOGGER.info("Found button with text containing '" + textOption + "': " + buttonText);
                    return (ButtonWidget) widget;
                }
            }
        }
        
        // 没有找到匹配的按钮，记录用于调试
        MineProfilerMod.LOGGER.warn("No button found with any of the specified texts: " + String.join(", ", textOptions));
        return null;
    }
    
    /**
     * 检查屏幕上是否有包含特定文本的按钮
     */
    public static boolean hasButtonWithText(Screen screen, String... textOptions) {
        return findButtonWithText(getAllClickableWidgets(screen), textOptions) != null;
    }
} 