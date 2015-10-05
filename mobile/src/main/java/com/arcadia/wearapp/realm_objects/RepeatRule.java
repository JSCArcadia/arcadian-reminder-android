package com.arcadia.wearapp.realm_objects;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RepeatRule extends RealmObject {
    @PrimaryKey
    private int ruleID;

    private int eventID;
    private long repeatPeriod;
    private Date endRepeatDate;

    public RepeatRule() {
        this(0);
    }

    public RepeatRule(int eventID) {
        this(0, eventID, 0, null);
    }

    public RepeatRule(int ruleID, int eventID, long repeatPeriod, Date endRepeatDate) {
        this.ruleID = ruleID;
        this.eventID = eventID;
        this.repeatPeriod = repeatPeriod;
        this.endRepeatDate = endRepeatDate;
    }

    public int getRuleID() {
        return ruleID;
    }

    public void setRuleID(int ruleID) {
        this.ruleID = ruleID;
    }

    public int getEventID() {
        return eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;
    }

    public long getRepeatPeriod() {
        return repeatPeriod;
    }

    public void setRepeatPeriod(long repeatPeriod) {
        this.repeatPeriod = repeatPeriod;
    }

    public Date getEndRepeatDate() {
        return endRepeatDate;
    }

    public void setEndRepeatDate(Date endRepeatDate) {
        this.endRepeatDate = endRepeatDate;
    }
}
