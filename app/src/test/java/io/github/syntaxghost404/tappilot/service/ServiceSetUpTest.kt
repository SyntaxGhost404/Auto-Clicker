package io.github.syntaxghost404.tappilot.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.syntaxghost404.tappilot.appGraph
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The app remembers that the service has worked, to tell a turned-off service from a new install. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ServiceSetUpTest {
    @Test
    fun `connecting the service marks it as set up`() {
        val controller = Robolectric.buildService(TapPilotAccessibilityService::class.java).create()
        val service = controller.get()
        val settings = service.appGraph.settings
        assertFalse(runBlocking { settings.current().serviceSetUp })

        try {
            // Called by the system once it has bound the service.
            TapPilotAccessibilityService::class.java.getDeclaredMethod("onServiceConnected")
                .apply { isAccessible = true }
                .invoke(service)
            assertTrue(TapPilotRuntime.isServiceConnected)
            // Read afresh each time: a collector that starts while the write lands can miss it.
            runBlocking {
                withTimeout(10_000) {
                    while (!settings.current().serviceSetUp) delay(20)
                }
            }
        } finally {
            controller.destroy()
        }
        assertFalse(TapPilotRuntime.isServiceConnected)
        assertTrue("set up stays recorded after the service stops", runBlocking { settings.current().serviceSetUp })
    }
}
