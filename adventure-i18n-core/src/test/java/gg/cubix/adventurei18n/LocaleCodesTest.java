package gg.cubix.adventurei18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocaleCodesTest {

    @Test
    void parseReturnsLocaleForLanguageAndCountry() {
        assertEquals(Locale.of("en", "US"), LocaleCodes.parse("en_us"));
    }

    @Test
    void parseReturnsLocaleForLanguageOnly() {
        assertEquals(Locale.of("en"), LocaleCodes.parse("en"));
    }

    @Test
    void parseAcceptsHyphenSeparator() {
        assertEquals(Locale.of("no", "NO"), LocaleCodes.parse("no-NO"));
    }

    @Test
    void parseNormalizesCase() {
        assertEquals(Locale.of("no", "NO"), LocaleCodes.parse("NO_no"));
    }

    @Test
    void parseAcceptsNumericRegion() {
        assertEquals(Locale.of("es", "419"), LocaleCodes.parse("es_419"));
    }

    @Test
    void parseReturnsNullForNullInput() {
        assertNull(LocaleCodes.parse(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "e",
            "toolonglanguagecode",
            "en_",
            "en__us",
            "en us",
            "123",
            "en_toolongregioncode1",
    })
    void parseReturnsNullForInvalidFormat(String id) {
        assertNull(LocaleCodes.parse(id));
    }

    @Test
    void idRendersLanguageAndCountryInMinecraftStyle() {
        assertEquals("en_us", LocaleCodes.id(Locale.of("en", "US")));
    }

    @Test
    void idRendersLanguageOnlyWhenNoCountry() {
        assertEquals("en", LocaleCodes.id(Locale.of("en")));
    }

    @Test
    void idThrowsOnNullLocale() {
        assertThrows(NullPointerException.class, () -> LocaleCodes.id(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"en_us", "no_no", "pt_br", "en"})
    void idOfParseRoundTripsCanonicalIds(String id) {
        assertEquals(id, LocaleCodes.id(LocaleCodes.parse(id)));
    }
}
