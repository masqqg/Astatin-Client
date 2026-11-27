package dev.astatin.menu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuMotionTest {
    @Test
    void scrollEasesTowardTargetAndResetsCleanly() {
        MenuState state = new MenuState();
        MenuLayout layout = new MenuLayout(320, 240);
        float max = layout.maxScroll(8);
        state.scrollBy(58f, max, false, true);
        state.updateScroll(1f / 60f, true, max, 0f);
        assertTrue(state.scroll > 0f && state.scroll < 58f);
        for (int i = 0; i < 100; i++) state.updateScroll(1f / 60f, true, max, 0f);
        assertEquals(58f, state.scroll, .05f);
        state.resetScroll();
        state.updateScroll(1f / 60f, true, max, 0f);
        assertEquals(0f, state.scroll);
    }

    @Test
    void animationsDisabledSnapsAndBothListsClampWhenContentShrinks() {
        MenuState state = new MenuState();
        state.scrollBy(200f, 200f, false, false);
        state.scrollBy(120f, 120f, true, false);
        assertEquals(200f, state.scroll);
        assertEquals(120f, state.settingsScroll);
        state.updateScroll(.016f, false, 40f, 20f);
        assertEquals(40f, state.scroll);
        assertEquals(20f, state.settingsScroll);
        state.resetSettingsScroll();
        assertEquals(0f, state.settingsScroll);
    }
}
