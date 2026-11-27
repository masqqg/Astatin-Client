package dev.astatin.menu;

import dev.astatin.modules.Module;
import dev.astatin.modules.Setting;
import dev.astatin.modules.Category;

/**
 * All geometry for the ClickGUI lives here. The renderer and the screen both use
 * it, so what is drawn and what is clickable can never drift apart.
 */
public final class MenuLayout {
    public static final float MARGIN = 12f;
    public static final float GAP = 10f;
    public static final float HEADER = 60f;
    public static final float CARD_H = 104f;
    public static final float CARD_GAP = 10f;
    public static final float GRID_PAD = 18f;
    public static final float INSPECTOR_W = 268f;
    public static final float CATEGORY_H = 38f;
    public static final float CATEGORY_GAP = 3f;

    public final float width;
    public final float height;
    public final boolean compact;
    public final boolean mobile;
    public final Frame sidebar;
    public final Frame main;
    public final Frame close;
    public final Frame search;
    public final Frame edit;
    public final Frame grid;
    public final Frame inspector;
    public final Frame profile;
    public final Frame scrollUp;
    public final Frame scrollDown;

    public MenuLayout(float width, float height) {
        this.width = width;
        this.height = height;
        this.compact = width < 820f || height < 380f;
        this.mobile = width < 600f || height < 380f;
        if (mobile) {
            this.main = new Frame(8f, 8f, Math.max(0f, width - 16f), Math.max(0f, height - 16f));
            this.sidebar = new Frame(0f, 0f, 0f, 0f);
            this.profile = sidebar;
            this.close = new Frame(main.right() - 32f, main.y() + 4f, 28f, 28f);
            this.search = new Frame(main.x() + 92f, main.y() + 4f,
                    Math.max(0f, close.x() - main.x() - 100f), 28f);
            this.edit = new Frame(0f, 0f, 0f, 0f);
            this.grid = new Frame(main.x() + 8f, main.y() + 80f,
                    Math.max(0f, main.w() - 16f), Math.max(0f, main.h() - 118f));
            this.inspector = new Frame(main.x() + 8f, main.y() + 40f,
                    Math.max(0f, main.w() - 16f), Math.max(0f, main.h() - 78f));
            this.scrollUp = new Frame(main.centerX() - 36f, main.bottom() - 32f, 32f, 28f);
            this.scrollDown = new Frame(main.centerX() + 4f, main.bottom() - 32f, 32f, 28f);
            return;
        }
        float sideW = compact ? 152f : 186f;
        this.sidebar = new Frame(MARGIN, MARGIN, sideW, height - MARGIN * 2f);
        float mainX = sidebar.right() + GAP;
        this.main = new Frame(mainX, MARGIN, width - mainX - MARGIN, height - MARGIN * 2f);

        float closeSize = 26f;
        this.close = new Frame(main.right() - 18f - closeSize, main.y() + 16f, closeSize, closeSize);
        float searchW = compact ? 152f : 196f;
        this.search = new Frame(close.x() - GAP - searchW, main.y() + 14f, searchW, 30f);
        this.edit = new Frame(search.x() - 8f - 92f, main.y() + 14f, 92f, 30f);

        this.grid = new Frame(main.x() + GRID_PAD, main.y() + 70f,
                main.w() - GRID_PAD * 2f, main.h() - 70f - 16f);
        float inspectorW = compact ? main.w() - 24f : INSPECTOR_W;
        this.inspector = new Frame(main.right() - 12f - inspectorW, main.y() + 64f,
                inspectorW, main.h() - 64f - 12f);
        this.profile = new Frame(sidebar.x() + 10f, sidebar.bottom() - 58f, sidebar.w() - 20f, 46f);
        this.scrollUp = new Frame(0f, 0f, 0f, 0f);
        this.scrollDown = this.scrollUp;
    }

    public int columns() {
        return compact ? 1 : 2;
    }

    public Frame category(int index) {
        if (mobile) {
            float gap = 4f;
            float categoryWidth = Math.max(0f, (main.w() - 16f - gap * (Category.values().length - 1))
                    / Category.values().length);
            return new Frame(main.x() + 8f + index * (categoryWidth + gap), main.y() + 40f,
                    categoryWidth, 32f);
        }
        return new Frame(sidebar.x() + 10f, sidebar.y() + 80f + index * (CATEGORY_H + CATEGORY_GAP),
                sidebar.w() - 20f, CATEGORY_H);
    }

    public Frame card(int index, int total, float scroll) {
        int columns = columns();
        int column = index % columns;
        int row = index / columns;
        float gap = CARD_GAP;
        float cardW = columns == 1 ? grid.w() : (grid.w() - gap) / 2f;
        float cardH = cardHeight();
        float x = grid.x() + column * (cardW + gap);
        float y = grid.y() + row * (cardH + gap) - scroll;
        return new Frame(x, y, cardW, cardH);
    }

    public float cardHeight() {
        return mobile ? 48f : compact ? 96f : CARD_H;
    }

    public float contentHeight(int total) {
        int rows = Math.max(0, (total + columns() - 1) / columns());
        if (rows == 0) {
            return 0f;
        }
        return rows * cardHeight() + (rows - 1) * CARD_GAP;
    }

    public float maxScroll(int total) {
        return Math.max(0f, contentHeight(total) - grid.h());
    }

    public Frame cardToggle(Frame card) {
        if (mobile) {
            return new Frame(card.right() - 44f, card.y() + 10f, 36f, 28f);
        }
        float toggleW = 34f;
        float toggleH = 18f;
        return new Frame(card.right() - 14f - toggleW, card.y() + 16f, toggleW, toggleH);
    }

    public Frame inspectorClose() {
        if (mobile) {
            return new Frame(inspector.right() - 32f, inspector.y() + 4f, 28f, 28f);
        }
        return new Frame(inspector.right() - 16f - 22f, inspector.y() + 14f, 22f, 22f);
    }

    public Frame inspectorToggle() {
        if (mobile) {
            return new Frame(inspector.right() - 44f, inspector.y() + 34f, 36f, 22f);
        }
        return new Frame(inspector.right() - 16f - 34f, inspector.y() + 57f, 34f, 18f);
    }

    public Frame inspectorAction() {
        return new Frame(inspector.x() + 16f, inspector.y() + 56f, inspector.w() - 32f, 30f);
    }

    public float settingsTop() {
        return inspector.y() + (mobile ? 58f : 116f);
    }

    public Frame settingsViewport() {
        return new Frame(inspector.x(), settingsTop(), inspector.w(), Math.max(0f, inspector.bottom() - settingsTop() - 4f));
    }

    public float settingsMaxScroll(Module module, Setting openPicker) {
        float content = 0f;
        for (Setting setting : module.settings) {
            content += settingHeight(module, setting, openPicker);
        }
        return Math.max(0f, content - settingsViewport().h());
    }

    /** Height of a setting row including the picker when it is open. */
    public float settingHeight(Module module, Setting setting, Setting openPicker) {
        if (setting.kind == Setting.Kind.SLIDER) {
            return 46f;
        }
        if (setting.kind == Setting.Kind.COLOR) {
            return 42f + (openPicker == setting ? 110f : 0f);
        }
        return 36f;
    }

    public Frame settingRow(Module module, int index, Setting openPicker) {
        return settingRow(module, index, openPicker, 0f);
    }

    public Frame settingRow(Module module, int index, Setting openPicker, float scroll) {
        float y = settingsTop() - scroll;
        for (int i = 0; i < index && i < module.settings.size(); i++) {
            y += settingHeight(module, module.settings.get(i), openPicker);
        }
        Setting setting = module.settings.get(index);
        return new Frame(inspector.x() + 16f, y, inspector.w() - 32f, settingHeight(module, setting, openPicker));
    }

    public Frame sliderTrack(Frame row) {
        return new Frame(row.x(), row.y() + 27f, row.w(), 4f);
    }

    public Frame colorSwatch(Frame row) {
        return new Frame(row.right() - 22f, row.y() + 7f, 22f, 22f);
    }

    public Frame colorPicker(Frame colorRow) {
        return new Frame(colorRow.x(), colorRow.y() + 44f, colorRow.w(), 100f);
    }

    public Frame pickerSquare(Frame picker) {
        return new Frame(picker.x(), picker.y(), picker.w() - 22f, 74f);
    }

    public Frame pickerHue(Frame picker) {
        return new Frame(picker.right() - 14f, picker.y(), 8f, 74f);
    }

    public Frame pickerHex(Frame picker) {
        return new Frame(picker.x(), picker.y() + 82f, picker.w(), 14f);
    }
}
