package io.github.syntaxghost404.tappilot.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.syntaxghost404.tappilot.service.TapPilotAccessibilityService
import io.github.syntaxghost404.tappilot.service.TapPilotRuntime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/**
 * A service that is switched on gets a few seconds to connect before the app calls it stuck, as the
 * system binds it a moment after the app starts.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ServiceStateTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val seen = mutableListOf<ServiceState>()
    private var service: TapPilotAccessibilityService? = null

    private val now: Long get() = compose.mainClock.currentTime

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        service?.let(TapPilotRuntime::detach)
        ServiceStartup.reset()
    }

    /** Starts the app's clock as if the process started [agoMs] milliseconds ago. */
    private fun processStarted(agoMs: Long = 0) {
        ServiceStartup.reset(clock = { now }, processStart = now - agoMs)
    }

    private fun switchOn(on: Boolean) {
        val ours = ComponentName(context, TapPilotAccessibilityService::class.java).flattenToString()
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, if (on) ours else "")
    }

    private fun show() {
        compose.setContent {
            val state = rememberServiceState()
            if (seen.lastOrNull() != state) seen += state
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun advance(ms: Long) {
        compose.mainClock.advanceTimeBy(ms)
        compose.mainClock.advanceTimeByFrame()
    }

    private val current: ServiceState get() = seen.last()

    @Test
    fun `a service that is on is given time to connect after the app starts`() {
        switchOn(true)
        processStarted()
        show()
        assertEquals(ServiceState.Starting, current)

        advance(ServiceStartup.GRACE_MS - 500)
        assertEquals(ServiceState.Starting, current)

        advance(1_000)
        assertEquals(ServiceState.Stuck, current)
    }

    @Test
    fun `a service that is on connects without ever looking stuck`() {
        switchOn(true)
        processStarted()
        show()
        advance(500)

        service = Robolectric.setupService(TapPilotAccessibilityService::class.java).also(TapPilotRuntime::attach)
        advance(ServiceStartup.GRACE_MS)
        assertEquals(listOf(ServiceState.Starting, ServiceState.Connected), seen)
    }

    @Test
    fun `long after the app started, a service that is on but not connected is stuck at once`() {
        switchOn(true)
        processStarted(agoMs = 60_000)
        show()
        assertEquals(ServiceState.Stuck, current)
        assertFalse("briefly showed $seen", ServiceState.Starting in seen)
    }

    @Test
    fun `a service that is off is off`() {
        switchOn(false)
        processStarted()
        show()
        advance(ServiceStartup.GRACE_MS * 2)
        assertEquals(listOf(ServiceState.Off), seen)
    }

    @Test
    fun `switching the service on starts a new wait`() {
        var time = 60_000L
        ServiceStartup.reset(clock = { time }, processStart = 0)
        ServiceStartup.seen(true)
        assertEquals(0L, ServiceStartup.graceLeftMs())

        ServiceStartup.seen(false)
        time += 10_000
        ServiceStartup.seen(true)
        assertEquals(ServiceStartup.GRACE_MS, ServiceStartup.graceLeftMs())
        // Seeing it on again does not restart the wait.
        time += 1_000
        ServiceStartup.seen(true)
        assertEquals(ServiceStartup.GRACE_MS - 1_000, ServiceStartup.graceLeftMs())
    }
}
