package com.extracta.app.entity.action

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract

class AndroidEntityActionLauncher(
    private val context: Context,
    private val clipboardHelper: AndroidClipboardHelper = AndroidClipboardHelper(context),
    private val canResolve: (Intent) -> Boolean = { it.resolveActivity(context.packageManager) != null },
    private val startActivity: (Intent) -> Unit = context::startActivity,
) {
    fun launch(action: EntityAction): EntityActionLaunchResult {
        if (action is EntityAction.Copy) {
            clipboardHelper.copy(action.value)
            return EntityActionLaunchResult.Copied
        }

        val intent = action.toIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!canResolve(intent)) {
            return EntityActionLaunchResult.NoHandler
        }
        return try {
            startActivity(intent)
            EntityActionLaunchResult.Launched
        } catch (_: ActivityNotFoundException) {
            EntityActionLaunchResult.NoHandler
        } catch (_: SecurityException) {
            EntityActionLaunchResult.NoHandler
        }
    }
}

enum class EntityActionLaunchResult { Launched, Copied, NoHandler }

fun EntityAction.toIntent(): Intent = when (this) {
    is EntityAction.AddCalendarEvent -> Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
        .putExtra(CalendarContract.Events.TITLE, title)
    is EntityAction.OpenDialer -> Intent(Intent.ACTION_DIAL, Uri.parse(EntityActionUris.dial(phoneNumber)))
    is EntityAction.ComposeEmail -> Intent(Intent.ACTION_SENDTO, Uri.parse(EntityActionUris.email(address)))
    is EntityAction.OpenUrl -> Intent(Intent.ACTION_VIEW, Uri.parse(url))
    is EntityAction.OpenMap -> Intent(Intent.ACTION_VIEW, Uri.parse(EntityActionUris.map(address)))
    is EntityAction.Copy -> error("Copy is handled without an activity intent.")
}
