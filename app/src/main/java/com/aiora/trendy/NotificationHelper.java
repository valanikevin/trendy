package com.aiora.trendy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 * Created by HOME on 24-02-2018.
 */

@RequiresApi(api = Build.VERSION_CODES.O)
public class NotificationHelper extends ContextWrapper {

    private NotificationManager notifManager;

    public static final String CHANNEL_ONE_ID = "com.aiora.trendy.ONE";
    public static final String CHANNEL_ONE_NAME = "Quiz Result";

    public NotificationHelper(Context base) {
        super(base);
        createChannels();
    }

    public void createChannels() {

        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                CHANNEL_ONE_NAME, notifManager.IMPORTANCE_HIGH);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.setShowBadge(true);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getManager().createNotificationChannel(notificationChannel);

    }

    public Notification.Builder getNotification1(String title, String body,String message,String answer) {
        Notification.Builder mBuilder = new Notification.Builder(getApplicationContext(), CHANNEL_ONE_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= 21) mBuilder.setVibrate(new long[0]);

        Notification.BigTextStyle bigText = new Notification.BigTextStyle();
        bigText.bigText(message + "\n\n" + body + "\n" + answer);

        mBuilder.setStyle(bigText);

        return mBuilder;
    }

    public void notify(int id, Notification.Builder notification) {
        getManager().notify(id, notification.build());
    }

    private NotificationManager getManager() {
        if (notifManager == null) {
            notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notifManager;
    }

}
