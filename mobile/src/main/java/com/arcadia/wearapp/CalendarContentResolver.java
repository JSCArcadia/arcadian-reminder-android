package com.arcadia.wearapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.Reminder;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class CalendarContentResolver {

    public static final Uri CALENDAR_EVENTS_URI = Uri.parse("content://com.android.calendar/events");
    public static final Uri CALENDAR_REMINDERS_URI = Uri.parse("content://com.android.calendar/reminders");
    private static final String[] EVENT_FIELDS = new String[]{"_id", "title", "description", "allDay", "dtstart", "dtend", "rrule"};
    private static final String[] REMINDER_FIELDS = new String[]{"_id", "event_id", "minutes"};

    ContentResolver contentResolver;
    Set<Event> events = new HashSet<>();

    public CalendarContentResolver(Context ctx) {
        this.contentResolver = ctx.getContentResolver();
    }

    public Set<Event> getCalendarEvents() {
        Cursor cursor = contentResolver.query(CALENDAR_EVENTS_URI, EVENT_FIELDS, null, null, null);

        try {
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    Event event = new Event();

                    int eventId = cursor.getInt(0);
                    String title = cursor.getString(1);
                    String description = cursor.getString(2);
                    Date startDate = new Date(cursor.getLong(4));
                    Date endDate;
                    if (cursor.getString(6) != null) {
                        int curYear;
                        Calendar calendar = Calendar.getInstance();
                        if (startDate.before(calendar.getTime())) {
                            curYear = calendar.get(Calendar.YEAR);
                            calendar.setTime(startDate);
                            calendar.set(Calendar.YEAR, curYear);
                            startDate.setTime(calendar.getTime().getTime());
                        }
                    }
                    startDate.setTime(startDate.getTime());
                    if ("0".endsWith(cursor.getString(3))) {
                        endDate = new Date(cursor.getLong(5));
                    } else {
                        endDate = new Date(startDate.getTime());
                    }
                    Log.d(this.toString(), " " + cursor.getString(6));

                    event.setEventID(eventId);
                    event.setTitle(title);
                    if (description != null)
                        event.setDescription(description);
                    event.setStartDate(startDate);
                    event.setEndDate(endDate);

                    events.add(event);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return events;
    }

    public Set<Reminder> getCalendarReminders(int id) {
        Set<Reminder> reminders = new HashSet<>();
        Cursor cursor = contentResolver.query(CALENDAR_REMINDERS_URI, REMINDER_FIELDS, null, null, null);
        try {
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    int reminderId = cursor.getInt(0);
                    int eventId = cursor.getInt(1);
                    if (id == eventId) {
                        Reminder reminder = new Reminder();
                        int offset = cursor.getInt(2) * 60 * -1;

                        reminder.setReminderID(reminderId);
                        reminder.setEventID(id);
                        reminder.setAlertOffset(offset);

                        reminders.add(reminder);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reminders;
    }
}
