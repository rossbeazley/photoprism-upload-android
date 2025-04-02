package ulk.co.rossbeazley.photoprism.upload

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import ulk.co.rossbeazley.photoprism.upload.audit.AuditRepository
import ulk.co.rossbeazley.photoprism.upload.audit.Debug
import java.util.concurrent.TimeUnit

fun scheduleWakeupInCaseOfProcessDeath(appSingleton: Context, auditRepository: AuditRepository) {
    val serviceIntent = Intent(appSingleton, BootBroadcastReceiver::class.java)
    serviceIntent.putExtra("inputExtra", "scheduleWakeupInCaseOfProcessDeath")
    serviceIntent.action = "keepalive"
    val alarmManager = appSingleton.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    val pendingIntentRequestCode = 987

    // maybe cancel the current alarm
    val alarmIntent = PendingIntent.getBroadcast(
        appSingleton,
        pendingIntentRequestCode, serviceIntent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (alarmIntent != null && alarmManager != null) {
        alarmManager.cancel(alarmIntent)
    }

    // schedule an alarm for a couple of hours time
    val pendingIntent = PendingIntent.getBroadcast(
        appSingleton,
        pendingIntentRequestCode, serviceIntent, PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager?.let {
        auditRepository.log(Debug("Setting alarm for "))
        it.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + TimeUnit.HOURS.toMillis(4),
            pendingIntent
        )
    }
}
