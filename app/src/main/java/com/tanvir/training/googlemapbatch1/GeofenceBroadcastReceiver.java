package com.tanvir.training.googlemapbatch1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        final List<Geofence> geofenceList = event.getTriggeringGeofences();
        final int transitionType = event.getGeofenceTransition();

        List<String> names = new ArrayList<>();
        for (Geofence g : geofenceList) {
            names.add(g.getRequestId());
        }

        final String fullName = TextUtils.join(",", names);

        sendNotification(context, fullName);

    }

    private void sendNotification(Context context, String fullName) {
        final String CHANNEL_ID = "my_channel";
        NotificationCompat.Builder builder = new NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                .setContentTitle("Arrival Alert!")
                .setContentText("You have entered "+fullName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = "This channel sends todo notification alert";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID,
                            "Geofence", importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int)System.currentTimeMillis(),
                builder.build());
    }
}