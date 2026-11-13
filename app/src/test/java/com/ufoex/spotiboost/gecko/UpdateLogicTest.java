package com.ufoex.spotiboost.gecko;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdateLogicTest {
    @Test public void versionsCompareNumerically() {
        assertTrue(UpdateLogic.isNewer("0.1.1", "0.1.0"));
        assertTrue(UpdateLogic.isNewer("0.10.0", "0.9.0"));      // not as text
        assertTrue(UpdateLogic.isNewer("1.0.0", "0.99.99"));
        assertTrue(UpdateLogic.isNewer("0.2", "0.1.9"));
        assertFalse(UpdateLogic.isNewer("0.1.0", "0.1.0"));
        assertFalse(UpdateLogic.isNewer("0.1.0", "0.1.0.0"));
        assertFalse(UpdateLogic.isNewer("0.1.0", "0.1.1"));
        assertFalse(UpdateLogic.isNewer("abc", "0.1.0"));
    }

    @Test public void devSuffixIsIgnored() {
        assertTrue(UpdateLogic.isNewer("0.1.1", "0.1.0-dev"));
        assertTrue(UpdateLogic.isNewer("0.1.10", "0.1.9-dev"));
        assertFalse(UpdateLogic.isNewer("0.1.0", "0.1.0-dev"));
    }

    private static final String BODY = "<!-- lang:en -->\n### What's new\n\n- One\n\n<!-- lang:es -->\n### Novedades\n\n- Uno\n\n---\nInstall the APK";

    @Test public void notesPickTheLanguage() {
        assertEquals("### Novedades\n\n- Uno", UpdateLogic.notes(BODY, "es"));
        assertEquals("### What's new\n\n- One", UpdateLogic.notes(BODY, "en"));
    }

    @Test public void unknownLanguageFallsBackToEnglish() {
        assertEquals("### What's new\n\n- One", UpdateLogic.notes(BODY, "fr"));
    }

    @Test public void bodyWithoutBlocksIsUsedWhole() {
        assertEquals("- a\n- b", UpdateLogic.notes("- a\n- b\n\n---\nfooter", "es"));
        assertEquals("solo texto", UpdateLogic.notes("solo texto", "en"));
        assertEquals("", UpdateLogic.notes("", "en"));
    }

    @Test public void githubBoilerplateIsDropped() {
        assertEquals("- a", UpdateLogic.notes("- a\n\n**Full Changelog**: https://x/compare/v1...v2", "en"));
    }

    @Test public void windowsLineEndings() {
        assertEquals("- a\n\n- b", UpdateLogic.notes("<!-- lang:en -->\r\n- a\r\n\r\n- b\r\n", "en"));
    }
}
