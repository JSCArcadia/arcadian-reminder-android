package com.arcadia.wearapp;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Event implements Comparable<Event> {
    @SerializedName("event_id")
    private int eventID;

    private String title;

    @SerializedName("start_date")
    private Date startDate;

    @SerializedName("end_date")
    private Date endDate;

    private String description;

    @SerializedName("group_id")
    private String groupID;

    public Event() {
        this(null);
    }

    public Event(String title) {
        this.title = title;
    }

    public int getEventID() {
        return eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    @Override
    public int compareTo(Event another) {
        return getStartDate().compareTo(another.getStartDate());
    }
}