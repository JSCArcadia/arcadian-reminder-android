package com.arcadia.wearapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public class WearBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String type = bundle.getString("type");
        switch (type) {
            case WearListenerService.Mobile_List_Path:
                if (((WearApplication) context).getCurrentActivity() != null) {
                    Log.d(this.toString(), ((WearApplication) context).getCurrentActivity().getClass().getName());
                    String json = bundle.getString("json");
                    Gson gson = new GsonBuilder().setDateFormat(DateFormat.DEFAULT).create();
                    Type listType = new TypeToken<Event[]>() {
                    }.getType();
                    Event[] allEvents = gson.fromJson(json, listType);
                    Arrays.sort(allEvents, new Comparator<Event>() {
                        @Override
                        public int compare(Event lEvent, Event rEvent) {
                            Date event1 = lEvent.getStartDate();
                            Date event2 = rEvent.getStartDate();
                            if (event1.after(event2))
                                return 1;
                            else
                                return event1.equals(event2) ? 0 : -1;
                        }
                    });
                    int maxSize = allEvents.length < context.getResources().getInteger(R.integer.max_events_number) ? allEvents.length : context.getResources().getInteger(R.integer.max_events_number);

                    if (((WearApplication) context).events == null)
                        ((WearApplication) context).events = new ArrayList<>();

                    ((WearApplication) context).events.clear();

                    int count = 0;
                    for (Event event : allEvents) {
                        if (event.getStartDate().after(new Date(System.currentTimeMillis())) && count < maxSize) {
                            ((WearApplication) context).events.add(event);
                            count++;
                        }
                    }

                    Activity activity = ((WearApplication) context).getCurrentActivity();
                    if (activity.getClass().equals(WearMainActivity.class)) {
                        ((WearMainActivity) activity).redrawListView();
                    } else if (activity.getClass().equals(WearDescriptionActivity.class)) {
                        int eventID = ((WearDescriptionActivity) activity).getEventID();
                        ((WearDescriptionActivity) activity).setEvent(eventID);
                    }
                    break;
                }
        }
    }
}
