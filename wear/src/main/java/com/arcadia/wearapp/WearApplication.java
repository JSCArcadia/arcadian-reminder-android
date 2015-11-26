package com.arcadia.wearapp;

import android.app.Activity;
import android.app.Application;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.ArrayList;

public class WearApplication extends Application {
    public ArrayList<Event> events = new ArrayList<>();
    private Activity mCurrentActivity = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    public void setCurrentActivity(Activity mCurrentActivity) {
        this.mCurrentActivity = mCurrentActivity;
    }
}
