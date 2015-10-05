package com.arcadia.wearapp.realm_objects;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Reminder extends RealmObject {
    @PrimaryKey
    private int reminderID;

    private int eventID;
    private int alertOffset;

    public Reminder() {
        this(0);
    }

    public Reminder(int eventID) {
        this(eventID, 0);
    }

    public Reminder(int eventID, int alertOffset) {
        this.eventID = eventID;
        this.alertOffset = alertOffset;
    }

    public int getAlertOffset() {
        return alertOffset;
    }

    public void setAlertOffset(int alertOffset) {
        this.alertOffset = alertOffset;
    }

    public int getEventID() {
        return eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;
    }

    public int getReminderID() {
        return reminderID;
    }

    public void setReminderID(int reminderID) {
        this.reminderID = reminderID;
    }
}
