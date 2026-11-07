package io.github.diiafyra.motoman.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.diiafyra.motoman.MainActivity
import io.github.diiafyra.motoman.R
import io.github.diiafyra.motoman.core.domain.maintenance.MaintenanceStatus
import io.github.diiafyra.motoman.core.domain.maintenance.Reminder
import io.github.diiafyra.motoman.ui.format.dueText
import javax.inject.Inject

class AndroidReminderNotifier
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ReminderNotifier {
        private val manager = NotificationManagerCompat.from(context)

        override fun canNotify(): Boolean {
            // Before Android 13 there is no runtime permission: notifications are allowed unless turned off.
            val granted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            val channelOn =
                manager.getNotificationChannelCompat(CHANNEL_ID)?.importance !=
                    NotificationManagerCompat.IMPORTANCE_NONE
            return granted && manager.areNotificationsEnabled() && channelOn
        }

        override fun notify(reminders: List<Reminder>) {
            if (!canNotify()) return
            val openApp =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(
                        context,
                        MainActivity::class.java,
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            reminders.forEach { reminder ->
                val status = reminder.itemStatus.status
                val isOverdue = status is MaintenanceStatus.Overdue
                val titleRes = if (isOverdue) R.string.reminder_title_overdue else R.string.reminder_title_due_soon
                val title = context.getString(titleRes, reminder.itemStatus.item.name)
                val text =
                    context.getString(
                        R.string.reminder_text,
                        reminder.vehicle.name,
                        dueText(context.resources, status),
                    )
                val notification =
                    NotificationCompat
                        .Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_maintenance)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setContentIntent(openApp)
                        .setAutoCancel(true)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .build()
                // One notification per item, so a newer one for the same item replaces the older.
                @Suppress("MissingPermission") // Checked by canNotify() above.
                manager.notify(
                    reminder.itemStatus.item.id
                        .hashCode(),
                    notification,
                )
            }
        }

        companion object {
            const val CHANNEL_ID = "maintenance_reminders"

            fun createChannel(context: Context) {
                NotificationManagerCompat.from(context).createNotificationChannel(
                    NotificationChannelCompat
                        .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                        .setName(context.getString(R.string.reminder_channel_name))
                        .setDescription(context.getString(R.string.reminder_channel_description))
                        .build(),
                )
            }
        }
    }
