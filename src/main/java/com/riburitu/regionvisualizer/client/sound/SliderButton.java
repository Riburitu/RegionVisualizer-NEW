package com.riburitu.regionvisualizer.client.sound;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.ChatFormatting;

public class SliderButton extends AbstractSliderButton {
    private final OnValueChange onValueChange;
    private final float minValue;
    private final float maxValue;
    private final boolean showPercentage;
    private final String prefix;
    private final String suffix;
    private final String displayFormat;
    private boolean isDragging = false;
    private long lastUpdateTime = 0;
    private static final long UPDATE_THROTTLE = 50; 

    @FunctionalInterface
    public interface OnValueChange {
        void onChange(SliderButton slider, double value);
    }

    public SliderButton(int x, int y, int width, int height, Component message, double value, OnValueChange onValueChange) {
        this(x, y, width, height, message, value, onValueChange, 0.0f, 1.0f, true, "", "", "default");
    }

    public SliderButton(int x, int y, int width, int height, Component message, double value, 
                       OnValueChange onValueChange, float minValue, float maxValue, 
                       boolean showPercentage, String prefix, String suffix, String displayFormat) {
        super(x, y, width, height, message, normalizeValue(value, minValue, maxValue));
        this.onValueChange = onValueChange;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.showPercentage = showPercentage;
        this.prefix = prefix;
        this.suffix = suffix;
        this.displayFormat = displayFormat != null ? displayFormat : "default";
        
        updateMessage();
    }

    public static SliderButton forPercentage(int x, int y, int width, int height, String label, 
                                           double value, OnValueChange onValueChange) {
        return new SliderButton(x, y, width, height, 
            Component.literal(label + ": " + Math.round(value * 100) + "%"), 
            value, onValueChange, 0.0f, 1.0f, true, label + ": ", "%", "percentage");
    }

    public static SliderButton forSeconds(int x, int y, int width, int height, String label, 
                                        double value, double maxSeconds, OnValueChange onValueChange) {
    	double normalizedValue = value / maxSeconds;
        return new SliderButton(x, y, width, height, 
            Component.literal(label + ": " + String.format("%.1f", value) + "s"), 
            normalizedValue,
            (slider, val) -> onValueChange.onChange(slider, val),  // Cambio aquí: no multiplicar, y renombrar param
            0.0f, (float)maxSeconds, false, label + ": ", "s", "seconds");
    }

    public static SliderButton forRange(int x, int y, int width, int height, String label, 
                                      double value, double min, double max, OnValueChange onValueChange) {
        return new SliderButton(x, y, width, height, 
            Component.literal(label + ": " + String.format("%.2f", value)), 
            value, onValueChange, (float)min, (float)max, false, label + ": ", "", "range");
    }

    public static SliderButton forDecibels(int x, int y, int width, int height, String label,
                                         double value, double minDb, double maxDb, OnValueChange onValueChange) {
        return new SliderButton(x, y, width, height,
            Component.literal(label + ": " + String.format("%.1f", value) + "dB"),
            value, onValueChange, (float)minDb, (float)maxDb, false, label + ": ", "dB", "decibels");
    }

    @Override
    protected void updateMessage() {
        double actualValue = getActualValue();
        String formattedValue = formatValue(actualValue);
        String messageText = prefix + formattedValue + suffix;
        
        Component newMessage;
        
        // Agregar color según el valor si es apropiado
        if (displayFormat.equals("percentage")) {
            if (actualValue > 0.8) {
                newMessage = Component.literal(messageText).withStyle(ChatFormatting.GREEN);
            } else if (actualValue < 0.2) {
                newMessage = Component.literal(messageText).withStyle(ChatFormatting.RED);
            } else {
                newMessage = Component.literal(messageText).withStyle(ChatFormatting.WHITE);
            }
        } else {
            newMessage = Component.literal(messageText);
        }
        
        setMessage(newMessage);
    }

    private String formatValue(double value) {
        switch (displayFormat) {
            case "percentage":
                return String.valueOf(Math.round(value * 100));
            case "seconds":
                return String.format("%.1f", value);
            case "decibels":
                return String.format("%.1f", value);
            case "range":
                if (value == Math.floor(value)) {
                    return String.format("%.0f", value);
                } else {
                    return String.format("%.2f", value);
                }
            default:
                return String.format("%.2f", value);
        }
    }

    @Override
    protected void applyValue() {
        long currentTime = System.currentTimeMillis();
        
        // Throttling para evitar demasiadas llamadas durante el arrastre
        if (isDragging && currentTime - lastUpdateTime < UPDATE_THROTTLE) {
            return;
        }
        
        lastUpdateTime = currentTime;
        
        if (onValueChange != null) {
            double actualValue = getActualValue();
            
            try {
                onValueChange.onChange(this, actualValue);
                updateMessage();
            } catch (Exception e) {
                System.err.println("[SliderButton] Error en callback: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        isDragging = true;
        boolean result = super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return result;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging) {
            isDragging = false;
            if (onValueChange != null) {
                onValueChange.onChange(this, getActualValue());
                updateMessage();
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private static double normalizeValue(double value, float minValue, float maxValue) {
        if (minValue == 0.0f && maxValue == 1.0f) {
            return Mth.clamp(value, 0.0, 1.0);
        }
        return Mth.clamp((value - minValue) / (maxValue - minValue), 0.0, 1.0);
    }

    private double getActualValue() {
        if (minValue != 0.0f || maxValue != 1.0f) {
            return minValue + this.value * (maxValue - minValue);
        }
        return this.value;
    }

    // Métodos públicos para control externo
    public void setValue(double value) {
        if (minValue != 0.0f || maxValue != 1.0f) {
            this.value = Mth.clamp((value - minValue) / (maxValue - minValue), 0.0, 1.0);
        } else {
            this.value = Mth.clamp(value, 0.0, 1.0);
        }
        updateMessage();
    }

    public double getValue() {
        return getActualValue();
    }

    public double getRawValue() {
        return this.value;
    }

    public boolean isWorking() {
        return onValueChange != null;
    }

    public void debugInfo() {
        System.out.println("[SliderButton] " + prefix.trim() + ":");
        System.out.println("  - Raw Value: " + this.value);
        System.out.println("  - Actual Value: " + getActualValue());
        System.out.println("  - Range: " + minValue + " - " + maxValue);
        System.out.println("  - Format: " + displayFormat);
        System.out.println("  - Working: " + isWorking());
        System.out.println("  - Dragging: " + isDragging);
    }

    public void setEnabled(boolean enabled) {
        this.active = enabled;
    }

    public String getDisplayValue() {
        return formatValue(getActualValue());
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public void renderToolTip(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.isHovered()) {
            String tooltipText = createTooltipText();
            if (!tooltipText.isEmpty()) {
                guiGraphics.renderTooltip(
                    net.minecraft.client.Minecraft.getInstance().font,
                    Component.literal(tooltipText),
                    mouseX,
                    mouseY
                );
            }
        }
    }

    private String createTooltipText() {
        double actualValue = getActualValue();
        
        switch (displayFormat) {
            case "percentage":
                return "Valor: " + Math.round(actualValue * 100) + "% (Rango: 0% - 100%)";
            case "seconds":
                return "Valor: " + String.format("%.1f", actualValue) + "s (Rango: 0.0s - " + 
                       String.format("%.1f", maxValue) + "s)";
            case "decibels":
                return "Valor: " + String.format("%.1f", actualValue) + "dB (Rango: " + 
                       String.format("%.1f", minValue) + "dB - " + String.format("%.1f", maxValue) + "dB)";
            case "range":
                return "Valor: " + formatValue(actualValue) + " (Rango: " + 
                       String.format("%.2f", minValue) + " - " + String.format("%.2f", maxValue) + ")";
            default:
                return "Valor actual: " + formatValue(actualValue);
        }
    }

    // Métodos estáticos de utilidad para crear sliders específicos para música
    public static SliderButton forVolume(int x, int y, int width, int height, String label, 
                                        double currentVolume, OnValueChange onValueChange) {
        SliderButton slider = forPercentage(x, y, width, height, label, currentVolume, onValueChange);
        return slider;
    }

    public static SliderButton forFadeTime(int x, int y, int width, int height, String label, 
                                          double currentTime, double maxTime, OnValueChange onValueChange) {
        return forSeconds(x, y, width, height, label, currentTime, maxTime, onValueChange);
    }

    public static SliderButton forAudioGain(int x, int y, int width, int height, String label,
                                           double currentGain, OnValueChange onValueChange) {
        return forDecibels(x, y, width, height, label, currentGain, -40.0, 20.0, onValueChange);
    }

    // Método para crear slider con steps discretos
    public static SliderButton forSteps(int x, int y, int width, int height, String label,
                                       int currentStep, int maxSteps, OnValueChange onValueChange) {
        double normalizedValue = (double) currentStep / maxSteps;
        
        return new SliderButton(x, y, width, height,
            Component.literal(label + ": " + currentStep + "/" + maxSteps),
            normalizedValue,
            (slider, value) -> {
                int step = (int) Math.round(value * maxSteps);
                onValueChange.onChange(slider, step);
            },
            0.0f, maxSteps, false, label + ": ", "/" + maxSteps, "steps");
    }

    // Método para crear slider logarítmico (útil para frecuencias)
    public static SliderButton forLogarithmic(int x, int y, int width, int height, String label,
                                             double currentValue, double minValue, double maxValue,
                                             OnValueChange onValueChange) {
        // Convertir valor actual a escala logarítmica
        double logMin = Math.log10(minValue);
        double logMax = Math.log10(maxValue);
        double logCurrent = Math.log10(currentValue);
        double normalizedValue = (logCurrent - logMin) / (logMax - logMin);
        
        return new SliderButton(x, y, width, height,
            Component.literal(label + ": " + String.format("%.0f", currentValue)),
            normalizedValue,
            (slider, value) -> {
                // Convertir de vuelta de escala logarítmica
                double logValue = logMin + value * (logMax - logMin);
                double actualValue = Math.pow(10, logValue);
                onValueChange.onChange(slider, actualValue);
            },
            (float) minValue, (float) maxValue, false, label + ": ", "", "logarithmic");
    }

    // Método para animación suave de valores
    public void animateToValue(double targetValue, long durationMs) {
        double startValue = getActualValue();
        double diff = targetValue - startValue;
        long startTime = System.currentTimeMillis();
        
        new Thread(() -> {
            while (System.currentTimeMillis() - startTime < durationMs) {
                long elapsed = System.currentTimeMillis() - startTime;
                double progress = (double) elapsed / durationMs;
                progress = Math.min(1.0, progress);
                
                // Función de easing suave
                double easedProgress = easeInOutCubic(progress);
                double currentValue = startValue + diff * easedProgress;
                
                // Actualizar en el hilo principal de Minecraft
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    setValue(currentValue);
                });
                
                try {
                    Thread.sleep(16); // ~60 FPS
                } catch (InterruptedException e) {
                    break;
                }
                
                if (progress >= 1.0) break;
            }
            
            // Asegurar valor final exacto
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                setValue(targetValue);
            });
        }).start();
    }

    private static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    // Validación de rangos mejorada
    public boolean isValueInValidRange(double value) {
        return value >= minValue && value <= maxValue;
    }

    public void clampValueToRange() {
        double currentValue = getActualValue();
        double clampedValue = Mth.clamp(currentValue, minValue, maxValue);
        if (currentValue != clampedValue) {
            setValue(clampedValue);
            System.out.println("[SliderButton] Valor ajustado a rango válido: " + clampedValue);
        }
    }

    // Método para resetear a valor por defecto
    public void resetToDefault() {
        double defaultValue = getDefaultValue();
        setValue(defaultValue);
        if (onValueChange != null) {
            onValueChange.onChange(this, defaultValue);
        }
    }

    private double getDefaultValue() {
        switch (displayFormat) {
            case "percentage":
                return 1.0; // 100% por defecto
            case "seconds":
                return maxValue * 0.5; // Mitad del rango por defecto
            case "decibels":
                return 0.0; // 0dB por defecto
            case "range":
                return (minValue + maxValue) * 0.5; // Valor medio por defecto
            default:
                return 0.5; // 50% por defecto
        }
    }

    // Método para obtener información de configuración
    public String getConfigInfo() {
        return String.format("SliderButton[%s]: %.3f (%.1f%%) [%.2f-%.2f] format=%s",
            prefix.trim(), getActualValue(), getRawValue() * 100, 
            minValue, maxValue, displayFormat);
    }

    // Métodos de accesibilidad mejorados
    public net.minecraft.network.chat.Component getNarration() {
        return Component.literal(prefix + formatValue(getActualValue()) + suffix + 
            ". Usar flechas izquierda y derecha para ajustar.");
    }

    // Soporte para teclado mejorado
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Flecha izquierda: disminuir
        if (keyCode == 263) { // LEFT_ARROW
            double step = (modifiers & 2) != 0 ? 0.01 : 0.05; // Ctrl para pasos más pequeños
            double newValue = getRawValue() - step;
            this.value = Mth.clamp(newValue, 0.0, 1.0);
            applyValue();
            return true;
        }
        
        // Flecha derecha: aumentar
        if (keyCode == 262) { // RIGHT_ARROW
            double step = (modifiers & 2) != 0 ? 0.01 : 0.05; // Ctrl para pasos más pequeños
            double newValue = getRawValue() + step;
            this.value = Mth.clamp(newValue, 0.0, 1.0);
            applyValue();
            return true;
        }
        
        // Home: mínimo
        if (keyCode == 268) { // HOME
            this.value = 0.0;
            applyValue();
            return true;
        }
        
        // End: máximo
        if (keyCode == 269) { // END
            this.value = 1.0;
            applyValue();
            return true;
        }
        
        // R: resetear a valor por defecto
        if (keyCode == 82 && (modifiers & 2) != 0) { // Ctrl+R
            resetToDefault();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // Método para obtener texto de ayuda
    public String getHelpText() {
        return "Controles: Arrastrar ratón, Flechas ←/→ (Ctrl para precisión), Home/End para extremos, Ctrl+R para resetear";
    }

    // Estado de validación
    public boolean isValidState() {
        return onValueChange != null && 
               minValue <= maxValue && 
               !Double.isNaN(this.value) && 
               !Double.isInfinite(this.value);
    }

    // Logging mejorado para debug
    public void logState() {
        System.out.println("[SliderButton] === ESTADO COMPLETO ===");
        System.out.println("  Prefix: " + prefix);
        System.out.println("  Suffix: " + suffix);
        System.out.println("  Display Format: " + displayFormat);
        System.out.println("  Raw Value: " + this.value);
        System.out.println("  Actual Value: " + getActualValue());
        System.out.println("  Formatted: " + formatValue(getActualValue()));
        System.out.println("  Range: [" + minValue + ", " + maxValue + "]");
        System.out.println("  Valid State: " + isValidState());
        System.out.println("  Active: " + this.active);
        System.out.println("  Dragging: " + isDragging);
        System.out.println("  Has Callback: " + (onValueChange != null));
        System.out.println("=== FIN ESTADO ===");
    }
}