package com.extracta.app.entity.action

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class AndroidEntityActionLauncherTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun actionIntentsCarryEncodedValuesAndCalendarExtras() {
        val dial = EntityAction.OpenDialer("+91 98 76").toIntent()
        assertEquals(Intent.ACTION_DIAL, dial.action)
        assertEquals("tel:%2B91%2098%2076", dial.data.toString())

        val email = EntityAction.ComposeEmail("person+scan@example.test").toIntent()
        assertEquals(Intent.ACTION_SENDTO, email.action)
        assertEquals("mailto:person%2Bscan%40example.test", email.data.toString())

        val map = EntityAction.OpenMap("12 Main St., Pune").toIntent()
        assertEquals("geo:0,0?q=12%20Main%20St.%2C%20Pune", map.data.toString())

        val calendar = EntityAction.AddCalendarEvent("8 Oct 2026", 100L, 200L, true).toIntent()
        assertEquals(Intent.ACTION_INSERT, calendar.action)
        assertEquals(CalendarContract.Events.CONTENT_URI, calendar.data)
        assertEquals(100L, calendar.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, -1L))
        assertEquals(200L, calendar.getLongExtra(CalendarContract.EXTRA_EVENT_END_TIME, -1L))
        assertTrue(calendar.getBooleanExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false))
    }

    @Test
    fun installedDeviceHandlersResolveAndNoHandlerIsGraceful() {
        val actions = listOf(
            EntityAction.OpenDialer("1234567890"),
            EntityAction.ComposeEmail("person@example.test"),
            EntityAction.OpenUrl("https://example.test"),
            EntityAction.OpenMap("12 Main Street"),
            EntityAction.AddCalendarEvent("Test event", 100L, 200L, false),
        )
        actions.forEach { action ->
            assertNotNull("No installed handler for ${action.label}", action.toIntent().resolveActivity(context.packageManager))
        }

        var started = false
        val launcher = AndroidEntityActionLauncher(
            context = context,
            canResolve = { false },
            startActivity = { started = true },
        )
        assertEquals(EntityActionLaunchResult.NoHandler, launcher.launch(EntityAction.OpenUrl("unknown://test")))
        assertEquals(false, started)
    }

    @Test
    fun copyingFromInstrumentationThreadUsesTheMainLooper() {
        val launcher = AndroidEntityActionLauncher(context)

        assertEquals(EntityActionLaunchResult.Copied, launcher.launch(EntityAction.Copy("API 26 copy")))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()

        val copiedText = AtomicReference<String>()
        instrumentation.runOnMainSync {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            copiedText.set(clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
        assertEquals("API 26 copy", copiedText.get())
    }
}
