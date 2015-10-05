package com.arcadia.wearapp;

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

import com.arcadia.wearapp.activities.MainActivity;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.Reminder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.Realm;

public class AlarmReceiver extends BroadcastReceiver {
    private static final int minute = 60 * 1000;    //one minute in milliseconds
    private Context context;
    private SimpleDateFormat dateFormat;
    //public class NotifyService extends IntentService {
//
//    // Name of an intent extra we can use to identify if this service was started to create a notification
//    public static final String INTENT_NOTIFY = "com.atcadia.wearapp.INTENT_NOTIFY";
//    // This is the object that receives interactions from clients
//    private final IBinder mBinder = new ServiceBinder();
//    // The system notification manager
    private NotificationManagerCompat notificationManager;
//}

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        String longDatePattern = String.format("%s, %s", android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "E MMM dd"), android.text.format.DateFormat.is24HourFormat(context) ? "H:mm" : "h:mm a");
        this.dateFormat = new SimpleDateFormat(longDatePattern, Locale.getDefault());

        showNotification(intent.getIntExtra("reminderId", 0));
//        Intent service1 = new Intent(context, MyAlarmService.class);
//        context.startService(service1);
    }

    //
//    public NotifyService() {
//        super(null);
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return mBinder;
//    }
//
//    @Override
//    protected void onHandleIntent(Intent intent) {
////        Log.i("LocalService", "Received start id " + startId + ": " + intent);
//        int reminderId = 0;
//        if (intent.getExtras().containsKey(getString(R.string.intent_reminder_id_key))) {
//            reminderId = intent.getIntExtra(getString(R.string.intent_reminder_id_key), 0);
//        }
//
//        // If this service was started by out AlarmTask intent then we want to show our notification
//        if (intent.getBooleanExtra(INTENT_NOTIFY, false))
//            showNotification(reminderId);
//    }
//
    private void showNotification(int reminderId) {
        boolean justInTime = true;

        notificationManager = NotificationManagerCompat.from(context);

        Realm realm = Realm.getInstance(context);
        Reminder reminder = realm.where(Reminder.class).equalTo("reminderID", reminderId).findFirst();
        if (reminder != null) {
            Event event = realm.where(Event.class).equalTo("eventID", reminder.getEventID()).findFirst();

            if (event == null)
                event = new Event("Unidentified event");
            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.setAction(MobileListenerService.Action_Open_Event);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK);
            openIntent.putExtra(context.getString(R.string.intent_event_id_key), event.getEventID());

            PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            String contentText = "";
            if (event.getStartDate() != null) {
                contentText = dateFormat.format(event.getStartDate());

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

                boolean isLocally = preferences.getBoolean("locallyTimezone", true);
                String timezone = preferences.getString("timezoneList", TimeZone.getDefault().getDisplayName());
                Calendar calendar = Calendar.getInstance();
                if (!isLocally)
                    calendar.setTimeZone(TimeZone.getTimeZone(timezone));
                if ((event.getStartDate().getTime() + reminder.getAlertOffset() * 1000) < (calendar.getTimeInMillis() - minute)) {
                    justInTime = false;
                }
            }
            if (event.getDescription() != null && event.getDescription().isEmpty())
                contentText += String.format("\n%s", event.getDescription());
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(event.getTitle())
                    .setContentText(contentText)
                    .setContentText(contentText)
                    .setAutoCancel(true)
                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                    .setVibrate(new long[]{1000, 1000, 1000})
                    .setLights(Color.CYAN, 1000, 1000)
                    .setContentIntent(viewPendingIntent);
//        realm.beginTransaction();
//        realm.where(Reminder.class).equalTo("reminderID", remindId).findFirst().removeFromRealm();
//        realm.commitTransaction();
            realm.close();

            if (justInTime)
                // Send the notification to the system.
                notificationManager.notify(reminderId, notificationBuilder.build());
        }
    }
//    public class ServiceBinder extends Binder {
//        NotifyService getService() {
//            return NotifyService.this;
//        }
//    }
}