package io.github.syntaxghost404.tappilot.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.store() = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { folder.root.resolve("settings.preferences_pb") },
    )

    @Test
    fun `a fresh install starts with compact controls and haptics on`() = runTest {
        val settings = SettingsRepository(store()).current()
        assertEquals(ControlSize.Compact, settings.controlSize)
        assertTrue(settings.hapticFeedback)
        assertEquals(AppSettings(), settings)
    }

    @Test
    fun `a chosen control size is kept, including the old default`() = runTest {
        val store = store()
        // Written by an earlier version, where Regular was the default.
        store.edit { it[stringPreferencesKey("control_size")] = ControlSize.Regular.name }
        val repository = SettingsRepository(store)
        assertEquals(ControlSize.Regular, repository.current().controlSize)

        repository.setControlSize(ControlSize.Large)
        assertEquals(ControlSize.Large, repository.current().controlSize)
    }

    @Test
    fun `haptic feedback can be turned off and on`() = runTest {
        val repository = SettingsRepository(store())
        repository.setHapticFeedback(false)
        assertFalse(repository.current().hapticFeedback)
        repository.setHapticFeedback(true)
        assertTrue(repository.current().hapticFeedback)
    }
}
