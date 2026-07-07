package com.warmcountdown.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ReminderScheduler {
    public static void schedule(Context context, AppState.Todo todo) {
        if (todo.reminderAt == null || todo.reminderAt.isEmpty() || todo.done) {
            cancel(context, todo.id);
            return;
        }
        try {
            LocalDateTime reminder = LocalDateTime.parse(todo.reminderAt);
            long millis = reminder.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (millis < System.currentTimeMillis()) return;
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent intent = pendingIntent(context, todo.id, todo.title, todo.deadline, todo.repeat);
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent);
        } catch (Exception ignored) {
        }
    }

    public static void cancel(Context context, String id) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent(context, id, "", "", "none"));
    }

    private static PendingIntent pendingIntent(Context context, String id, String title, String deadline, String repeat) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("id", id);
        intent.putExtra("title", title);
        intent.putExtra("deadline", deadline);
        intent.putExtra("repeat", repeat);
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
