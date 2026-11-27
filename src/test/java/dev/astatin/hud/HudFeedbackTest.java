package dev.astatin.hud;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class HudFeedbackTest {
    @Test
    void effectCountPreservesTotalWhileOnlyConvertingFourRows() {
        HudData data = new HudData();
        List<String> effects = List.of("first", "second", "third", "fourth", "fifth", "sixth");
        AtomicInteger converted = new AtomicInteger();
        data.setPotions(effects, name -> {
            converted.incrementAndGet();
            return new HudData.PotionInfo(name, "I", 10, 0xFFFFFFFF, 1f);
        });
        assertEquals(6, data.totalPotions());
        assertEquals(4, data.potions.size());
        assertEquals(4, converted.get());
        assertEquals("first", data.potions.get(0).name);
        assertEquals("fourth", data.potions.get(3).name);
        assertEquals("4/6", PotionsElement.countLabel(data));
        assertEquals(136f, new PotionsElement().baseHeight(data));

        data.clearPotions();
        assertTrue(data.potions.isEmpty());
        assertEquals(0, data.totalPotions());
        assertEquals("0", PotionsElement.countLabel(data));
        assertEquals("3", PotionsElement.countLabel(data.sample()));
    }

    @Test
    void cpsHistorySamplesActualClicksAndDoesNotInventValuesDuringGaps() {
        HudData data = new HudData();
        data.cpsLeft = 2;
        data.cpsRight = 1;
        data.recordCps(1000);
        int last = data.cpsHistory.length - 1;
        assertEquals(3f, data.cpsHistory[last]);
        data.cpsLeft = 4;
        data.recordCps(1199);
        assertEquals(3f, data.cpsHistory[last]);
        data.recordCps(1200);
        assertEquals(3f, data.cpsHistory[last - 1]);
        assertEquals(5f, data.cpsHistory[last]);
        data.cpsLeft = 1;
        data.cpsRight = 0;
        data.recordCps(1800);
        assertEquals(0f, data.cpsHistory[last - 1]);
        assertEquals(0f, data.cpsHistory[last - 2]);
        assertEquals(1f, data.cpsHistory[last]);
        data.recordCps(50);
        assertEquals(0f, data.cpsHistory[last - 1]);
        assertEquals(1f, data.cpsHistory[last]);
    }

    @Test
    void keyFeedbackUsesSeparateJumpAndHonorsAnimationSetting() {
        HudData data = new HudData();
        KeyFeedback feedback = new KeyFeedback();
        data.keys[0] = true;
        feedback.update(data.keys, 1f / 60f, false);
        assertEquals(1f, feedback.value(0));
        assertEquals(0f, feedback.value(6));
        data.keys[0] = false;
        data.keys[6] = true;
        feedback.update(data.keys, 1f / 60f, true);
        assertTrue(feedback.value(0) > 0f && feedback.value(0) < 1f);
        assertTrue(feedback.value(6) > 0f && feedback.value(6) < 1f);
        feedback.update(data.keys, 1f / 60f, false);
        assertEquals(0f, feedback.value(0));
        assertEquals(1f, feedback.value(6));
    }

    @Test
    void frameTimeHandlesPauseAndClockReset() {
        HudData data = new HudData();
        data.updateFrameTime(1_000_000_000L);
        data.updateFrameTime(1_016_000_000L);
        assertEquals(.016f, data.frameSeconds, .0001f);
        data.updateFrameTime(5_000_000_000L);
        assertEquals(.05f, data.frameSeconds);
        data.updateFrameTime(10L);
        assertEquals(1f / 60f, data.frameSeconds);
    }
}
