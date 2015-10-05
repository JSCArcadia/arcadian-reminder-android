package com.arcadia.wearapp;

import android.app.Activity;
import android.app.Application;

import java.util.ArrayList;

public class WearApplication extends Application {
    public ArrayList<Event> events;
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
