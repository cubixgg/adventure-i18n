package gg.cubix.adventurei18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagPaletteTest {

    private final TagPalette palette = TagPalette.of(Map.of(
            "error", NamedTextColor.RED,
            "success", NamedTextColor.GREEN));

    @Test
    void resolverKnowsConfiguredTagsOnly() {
        assertTrue(palette.resolver().has("error"));
        assertTrue(palette.resolver().has("success"));
        assertFalse(palette.resolver().has("warning"));
    }

    @Test
    void miniMessageResolvesConfiguredTagToConfiguredColor() {
        Component component = palette.miniMessage().deserialize("<error>Uh oh</error>");

        assertTrue(containsColor(component, NamedTextColor.RED));
    }

    @Test
    void miniMessageStillUnderstandsStandardTags() {
        Component component = palette.miniMessage().deserialize("<bold>Important</bold>");

        assertTrue(containsDecoration(component, TextDecoration.BOLD));
    }

    @Test
    void colorReturnsConfiguredColorForKnownTag() {
        assertEquals(NamedTextColor.RED, palette.color("error"));
        assertEquals(NamedTextColor.GREEN, palette.color("success"));
    }

    @Test
    void colorThrowsForUnknownTag() {
        assertThrows(IllegalArgumentException.class, () -> palette.color("warning"));
    }

    @Test
    void colorThrowsOnNullTag() {
        assertThrows(NullPointerException.class, () -> palette.color(null));
    }

    @Test
    void ofThrowsOnNullTagsMap() {
        assertThrows(NullPointerException.class, () -> TagPalette.of(null));
    }

    @Test
    void ofThrowsOnNullTagName() {
        Map<String, TextColor> withNullKey = new HashMap<>();
        withNullKey.put(null, NamedTextColor.RED);

        assertThrows(NullPointerException.class, () -> TagPalette.of(withNullKey));
    }

    @Test
    void ofThrowsOnNullColor() {
        Map<String, TextColor> withNullColor = new HashMap<>();
        withNullColor.put("error", null);

        assertThrows(NullPointerException.class, () -> TagPalette.of(withNullColor));
    }

    private static boolean containsColor(Component component, TextColor color) {
        if (color.equals(component.color())) {
            return true;
        }
        for (Component child : component.children()) {
            if (containsColor(child, color)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDecoration(Component component, TextDecoration decoration) {
        if (component.hasDecoration(decoration)) {
            return true;
        }
        for (Component child : component.children()) {
            if (containsDecoration(child, decoration)) {
                return true;
            }
        }
        return false;
    }
}
