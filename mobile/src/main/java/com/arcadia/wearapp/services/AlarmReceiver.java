package com.arcadia.wearapp.services;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.activities.MainActivity;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.Reminder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.Realm;

public class AlarmReceiver extends BroadcastReceiver {
    private Context context;
    private SimpleDateFormat dateFormat;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(this.toString(),"New event alarm receive");
        this.context = context;
        this.dateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance(java.text.DateFormat.DEFAULT, Locale.getDefault());
        showNotification(intent.getIntExtra("reminderId", 0));
    }

    private void showNotification(int reminderId) {

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        Realm realm = Realm.getInstance(context);
        Reminder reminder = realm.where(Reminder.class).equalTo("reminderID", reminderId).findFirst();
        if (reminder != null) {
            Event event = realm.where(Event.class).equalTo("eventID", reminder.getEventID()).findFirst();

            if (event == null)
                event = new Event(context.getString(R.string.event_null_name));
            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.setAction(MobileListenerService.Action_Open_Event);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK);
            openIntent.putExtra(context.getString(R.string.intent_event_id_key), event.getEventID());

            PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            String contentText = "";
            if (event.getStartDate() != null) {
                contentText = dateFormat.format(event.getStartDate());
            }
            if (event.getDescription() != null && event.getDescription().isEmpty())
                contentText += String.format("\n%s", event.getDescription());
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_notification_small)
                    .setContentTitle(event.getTitle())
                    .setContentText(contentText)
                    .setAutoCancel(true)
                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                    .setVibrate(new long[]{1000, 1000, 1000})
                    .setLights(Color.CYAN, 1000, 1000)
                    .setContentIntent(viewPendingIntent);
            realm.close();

                // Send the notification to the system.
                notificationManager.notify(reminderId, notificationBuilder.build());
        }
    }
}