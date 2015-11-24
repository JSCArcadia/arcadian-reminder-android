package com.arcadia.wearapp.services;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.arcadia.wearapp.Event;
import com.arcadia.wearapp.WearApplication;
import com.arcadia.wearapp.activities.WearDescriptionActivity;
import com.arcadia.wearapp.activities.WearMainActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class WearBroadcastReceiver extends BroadcastReceiver {
    public final String jsonDatePattern = "yyyy-MM-dd'T'HH:mm:ssZ";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();

        Activity activity = ((WearApplication) context).getCurrentActivity();
        if (bundle.containsKey("type")) {
            String type = bundle.getString("type");
            if (type != null)
                switch (type) {
                    case WearListenerService.Mobile_List_Path:
                        String json = bundle.getString("json");
                        if (json != null && !json.isEmpty()) {
                            ((WearApplication) context).events.clear();
                            Gson gson = new GsonBuilder().setDateFormat(jsonDatePattern).create();
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

                            Collections.addAll(((WearApplication) context).events, allEvents);

                            if (activity != null) {
                                if (activity.getClass().equals(WearMainActivity.class)) {
                                    ((WearMainActivity) activity).redrawListView();
                                } else if (activity.getClass().equals(WearDescriptionActivity.class)) {
                                    int position = ((WearDescriptionActivity) activity).getEventPosition();
                                    ((WearDescriptionActivity) activity).setEvent(position);
                                }
                            }
                        } else if (activity != null && activity.getClass().equals(WearMainActivity.class)) {
                            ((WearMainActivity) activity).redrawListView();
                        }
                        break;
                    case WearListenerService.Mobile_Check_Connection:
                        if (activity != null && activity.getClass().equals(WearMainActivity.class)) {
                            ((WearMainActivity) activity).setConnection(intent.getBooleanExtra("is_connected", false));
                        }

                        break;
                }
        }
    }
}