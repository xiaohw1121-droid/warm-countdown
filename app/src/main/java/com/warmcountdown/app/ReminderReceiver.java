package com.warmcountdown.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.Notification;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "warm_countdown_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "倒计时提醒", NotificationManager.IMPORTANCE_DEFAULT));
        }
        String title = intent.getStringExtra("title");
        String deadline = intent.getStringExtra("deadline");
        Notification.Builder builder = new Notification.Builder(context)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title == null || title.isEmpty() ? "待办提醒" : title)
            .setContentText(deadline == null ? "" : "Deadline " + deadline)
            .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }
        manager.notify(intent.getStringExtra("id").hashCode(), builder.build());
    }
}
