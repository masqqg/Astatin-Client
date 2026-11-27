package dev.astatin.menu;

import dev.astatin.modules.Category;
import dev.astatin.modules.Module;
import dev.astatin.modules.Setting;

public final class MenuState {
    public int mouseX;
    public int mouseY;
    public float dt = 1f / 60f;
    public float time;
    public Category category = Category.HUD;
    public String search = "";
    public boolean searchFocused;
    public Module selected;
    public float scroll;
    public float settingsScroll;
    private float scrollTarget;
    private float settingsScrollTarget;
    public Setting openPicker;
    public float pickerHue;
    public float pickerSat;
    public float pickerVal;
    public boolean pickingHue;
    public boolean pickingSquare;
    public boolean pickingSlider;

    public void resetScroll() {
        scroll = 0f;
        scrollTarget = 0f;
    }

    public void resetSettingsScroll() {
        settingsScroll = 0f;
        settingsScrollTarget = 0f;
    }

    public void scrollBy(float delta, float max, boolean settings, boolean animate) {
        if (settings) {
            settingsScrollTarget = Math.max(0f, Math.min(max, settingsScrollTarget + delta));
            if (!animate) settingsScroll = settingsScrollTarget;
        } else {
            scrollTarget = Math.max(0f, Math.min(max, scrollTarget + delta));
            if (!animate) scroll = scrollTarget;
        }
    }

    public void updateScroll(float dt, boolean animate, float max, float settingsMax) {
        scrollTarget = Math.max(0f, Math.min(max, scrollTarget));
        settingsScrollTarget = Math.max(0f, Math.min(settingsMax, settingsScrollTarget));
        scroll = Math.max(0f, Math.min(max, scroll));
        settingsScroll = Math.max(0f, Math.min(settingsMax, settingsScroll));
        float factor = animate ? 1f - (float) Math.exp(-18f * Math.max(0f, Math.min(.05f, dt))) : 1f;
        scroll += (scrollTarget - scroll) * factor;
        settingsScroll += (settingsScrollTarget - settingsScroll) * factor;
        if (Math.abs(scrollTarget - scroll) < .05f) scroll = scrollTarget;
        if (Math.abs(settingsScrollTarget - settingsScroll) < .05f) settingsScroll = settingsScrollTarget;
    }

    public void openPicker(Setting setting) {
        this.openPicker = setting;
        float[] hsv = dev.astatin.theme.ColorUtil.toHsv(setting.color);
        this.pickerHue = hsv[0];
        this.pickerSat = hsv[1];
        this.pickerVal = hsv[2];
    }

    public void applyPickerColor() {
        if (openPicker != null) {
            openPicker.color = dev.astatin.theme.ColorUtil.hsv(pickerHue, pickerSat, pickerVal);
        }
    }
}
