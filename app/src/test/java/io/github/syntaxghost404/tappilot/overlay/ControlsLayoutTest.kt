package io.github.syntaxghost404.tappilot.overlay

import io.github.syntaxghost404.tappilot.core.data.ControlSize
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsLayoutTest {

    @Test
    fun `lengths match the floating toolbar`() {
        assertEquals(380f, ControlsLayout.lengthDp(OverlayMode.Multi))
        assertEquals(188f, ControlsLayout.lengthDp(OverlayMode.Single))
        assertEquals(92f, ControlsLayout.COLLAPSED_DP)
    }

    @Test
    fun `a portrait phone keeps every size vertical and unscaled`() {
        for (mode in OverlayMode.entries) {
            for (size in ControlSize.entries) {
                assertEquals(ControlsLayout(vertical = true, scale = size.scale), ControlsLayout.choose(360f, 800f, mode, size))
            }
        }
    }

    @Test
    fun `a landscape phone turns sequence controls horizontal once they are too tall`() {
        // The Redmi 13C sideways: 360 dp tall, so 344 dp of room. Compact is 327 dp and fits.
        assertEquals(ControlsLayout(vertical = true, scale = 0.86f), ControlsLayout.choose(800f, 360f, OverlayMode.Multi, ControlSize.Compact))
        assertEquals(ControlsLayout(vertical = false, scale = 1f), ControlsLayout.choose(800f, 360f, OverlayMode.Multi, ControlSize.Regular))
        assertEquals(ControlsLayout(vertical = false, scale = 1.16f), ControlsLayout.choose(800f, 360f, OverlayMode.Multi, ControlSize.Large))
    }

    @Test
    fun `single point controls fit sideways without turning`() {
        for (size in ControlSize.entries) {
            assertEquals(ControlsLayout(vertical = true, scale = size.scale), ControlsLayout.choose(800f, 360f, OverlayMode.Single, size))
        }
    }

    @Test
    fun `a tablet stays vertical in both orientations`() {
        for (size in ControlSize.entries) {
            assertTrue(ControlsLayout.choose(1280f, 800f, OverlayMode.Multi, size).vertical)
            assertTrue(ControlsLayout.choose(800f, 1280f, OverlayMode.Multi, size).vertical)
        }
    }

    @Test
    fun `the toolbar turns at the exact fitting height`() {
        val exact = 380f + 2 * ControlsLayout.SCREEN_MARGIN_DP
        assertTrue(ControlsLayout.choose(800f, exact, OverlayMode.Multi, ControlSize.Regular).vertical)
        assertFalse(ControlsLayout.choose(800f, exact - 1f, OverlayMode.Multi, ControlSize.Regular).vertical)
    }

    @Test
    fun `a screen too small for either direction scales the controls along its longer side`() {
        // 304 dp of room down and 284 dp across, both short of 380 dp.
        val layout = ControlsLayout.choose(300f, 320f, OverlayMode.Multi, ControlSize.Large)
        assertTrue(layout.vertical)
        assertEquals(304f / 380f, layout.scale, 0.001f)
        assertTrue(ControlsLayout.lengthDp(OverlayMode.Multi) * layout.scale <= 304f)

        val across = ControlsLayout.choose(320f, 300f, OverlayMode.Multi, ControlSize.Large)
        assertFalse(across.vertical)
        assertEquals(304f / 380f, across.scale, 0.001f)
    }

    @Test
    fun `scaling never goes below the minimum`() {
        val layout = ControlsLayout.choose(120f, 120f, OverlayMode.Multi, ControlSize.Regular)
        assertEquals(ControlsLayout.MIN_SCALE, layout.scale)
    }
}
