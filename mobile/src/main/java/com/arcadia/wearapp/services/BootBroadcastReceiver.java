package com.arcadia.wearapp.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.Reminder;
import com.arcadia.wearapp.realm_objects.RepeatRule;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmResults;

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Realm realm = Realm.getInstance(context);
        RealmResults<Reminder> reminders = realm.allObjects(Reminder.class);
        for (Reminder reminder : reminders) {
            Intent startIntent = new Intent();
            startIntent.setAction(context.getString(R.string.broadcast_action));
            startIntent.putExtra("reminderId", reminder.getReminderID());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminder.getReminderID(), startIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.cancel(pendingIntent);

            Event event = realm.where(Event.class).equalTo("eventID", reminder.getEventID()).findFirst();
            if (event == null)
                return;

            Date remindDate = new Date(event.getStartDate().getTime() + reminder.getAlertOffset() * 1000);

            RepeatRule repeatRule = realm.where(RepeatRule.class).equalTo("eventID", reminder.getEventID()).findFirst();
            if (repeatRule == null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, remindDate.getTime(), pendingIntent);
            } else {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, remindDate.getTime(), repeatRule.getRepeatPeriod(), pendingIntent);
            }
        }
    }
}
