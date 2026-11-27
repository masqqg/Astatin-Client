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
    /** Name shown in the sidebar profile card; filled from the local player. */
    public String playerName = "Player";
    public float scroll;
    public Setting openPicker;
    public float pickerHue;
    public float pickerSat;
    public float pickerVal;
    public boolean pickingHue;
    public boolean pickingSquare;
    public boolean pickingSlider;

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
