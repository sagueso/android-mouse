package ve.com.sagues.rafael.android_mouse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {

    private val shift = MouseConfig.MOD_LEFT_SHIFT
    private val altGr = MouseConfig.MOD_RIGHT_ALT

    private fun single(char: Char, layout: HostKeyboardLayout) =
        layout.keystrokesFor(char).single()

    @Test
    fun azertySwapsTheMainLetterPositions() {
        assertEquals(HidKeystroke(0x14), single('a', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x04), single('q', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x1A), single('z', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x1D), single('w', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x33), single('m', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x05), single('b', HostKeyboardLayout.AZERTY))
    }

    @Test
    fun azertyUppercaseUsesShiftOnTheSamePhysicalKey() {
        assertEquals(HidKeystroke(0x14, shift), single('A', HostKeyboardLayout.AZERTY))
    }

    @Test
    fun azertyDigitsRequireShiftAndSymbolsDoNot() {
        assertEquals(HidKeystroke(0x1E, shift), single('1', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x1E), single('&', HostKeyboardLayout.AZERTY))
    }

    @Test
    fun azertyDirectAccentsAreMapped() {
        assertEquals(HidKeystroke(0x1F), single('é', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x24), single('è', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x27), single('à', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x26), single('ç', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x34), single('ù', HostKeyboardLayout.AZERTY))
    }

    @Test
    fun azertyCircumflexUsesDeadKeyThenVowel() {
        assertEquals(
            listOf(HidKeystroke(0x2F), HidKeystroke(0x14)),
            HostKeyboardLayout.AZERTY.keystrokesFor('â')
        )
        assertEquals(
            listOf(HidKeystroke(0x2F), HidKeystroke(0x14, shift)),
            HostKeyboardLayout.AZERTY.keystrokesFor('Â')
        )
    }

    @Test
    fun azertyPunctuationUsesTheLocalPhysicalKeys() {
        assertEquals(HidKeystroke(0x21), single('\'', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x20), single('"', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x10), single(',', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x36), single(';', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x38), single('!', HostKeyboardLayout.AZERTY))
    }

    @Test
    fun azertyAltGrCharactersSetTheRightAltModifier() {
        assertEquals(HidKeystroke(0x27, altGr), single('@', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x08, altGr), single('€', HostKeyboardLayout.AZERTY))
        assertEquals(HidKeystroke(0x2E, altGr), single('}', HostKeyboardLayout.AZERTY))
    }

    @Test
    fun qwertzSwapsYAndZ() {
        assertEquals(HidKeystroke(0x1C), single('z', HostKeyboardLayout.QWERTZ))
        assertEquals(HidKeystroke(0x1D), single('y', HostKeyboardLayout.QWERTZ))
    }

    @Test
    fun qwertzUmlautsAreDirectKeys() {
        assertEquals(HidKeystroke(0x2F), single('ü', HostKeyboardLayout.QWERTZ))
        assertEquals(HidKeystroke(0x33), single('ö', HostKeyboardLayout.QWERTZ))
        assertEquals(HidKeystroke(0x34), single('ä', HostKeyboardLayout.QWERTZ))
        assertEquals(HidKeystroke(0x2F, shift), single('Ü', HostKeyboardLayout.QWERTZ))
    }

    @Test
    fun qwertzSymbolsUseTheGermanPositions() {
        assertEquals(HidKeystroke(0x1E), single('1', HostKeyboardLayout.QWERTZ))
        assertEquals(HidKeystroke(0x1E, shift), single('!', HostKeyboardLayout.QWERTZ))
        assertEquals(HidKeystroke(0x2D), single('ß', HostKeyboardLayout.QWERTZ))
        assertEquals(HidKeystroke(0x30), single('+', HostKeyboardLayout.QWERTZ))
    }

    @Test
    fun qwertyKeepsTheUsMapping() {
        assertEquals(HidKeystroke(0x04), single('a', HostKeyboardLayout.QWERTY))
        assertEquals(HidKeystroke(0x04, shift), single('A', HostKeyboardLayout.QWERTY))
        assertEquals(HidKeystroke(0x1F, shift), single('@', HostKeyboardLayout.QWERTY))
    }

    @Test
    fun unsupportedCharactersAreSkipped() {
        assertTrue(HostKeyboardLayout.QWERTY.keystrokesFor('€').isEmpty())
        assertTrue(HostKeyboardLayout.AZERTY.keystrokesFor('©').isEmpty())
    }
}
