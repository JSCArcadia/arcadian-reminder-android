package com.arcadia.wearapp.services;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.activities.MainActivity;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.RepeatRule;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

public class MobileListenerService extends WearableListenerService {

    public static final String Action_Send_List = "com.arcadia.wearapp.action.SL";
    public static final String Action_Sync = "com.arcadia.wearapp.action.SYNC";
    public static final String Mobile_List_Path = "/wearapp_send_list";
    public static final String Wear_Request_List = "wearapp_request_list";
    public static final String Wear_Request_Open_Event = "wearapp_open_event";
    public static final String Mobile_Send_List = "mobile_send_list";
    public static final String Action_Open_Event = "com.arcadia.wearapp.OPEN";
    private GoogleApiClient googleClient;
    private SimpleDateFormat dateFormat;
    private int period;

    public MobileListenerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //for connect to Android Wear device, android version must be 4.3 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            googleClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle connectionHint) {
                            Log.d(this.toString(), "onConnected: " + connectionHint);
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {
                            Log.d(this.toString(), "onConnectionSuspended: " + cause);
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.d(this.toString(), "onConnectionFailed: " + result);
                        }
                    })
                    .addApiIfAvailable(Wearable.API)
                    .build();
            googleClient.connect();
        }
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
    }

    @Override
    public void onDestroy() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            switch (action) {
                case Action_Send_List:
                    if (googleClient != null && googleClient.isConnected()) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
                                for (Node node : nodes.getNodes()) {
                                    String json = "";
                                    if (intent.getExtras() != null)
                                        json = intent.getExtras().getString(getString(R.string.intent_event_list_key));
                                    PutDataMapRequest putRequest = PutDataMapRequest.create(Mobile_List_Path);

                                    DataMap map = putRequest.getDataMap();
                                    map.putString("json", json);

                                    DataApi.DataItemResult resultData = Wearable.DataApi.putDataItem(googleClient, putRequest.asPutDataRequest()).await();
                                    if (resultData.getStatus().isSuccess()) {
                                        Log.d("Listener Service", "Sent Data to: " + node.getDisplayName());
                                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), Mobile_Send_List, null).await();
                                        if (result.getStatus().isSuccess()) {
                                            Log.d("Listener Service", "Message response sent to: " + node.getDisplayName());
                                        } else {
                                            Log.d("Listener Service", "ERROR: failed to send Message response");
                                        }
                                    } else {
                                        Log.d("Listener Service", "ERROR: failed to send Data");
                                    }
                                }
                            }
                        }).start();
                    }
                    break;
                case Action_Sync:
                    syncEventList(period);
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        switch (messageEvent.getPath()) {
            case Wear_Request_List:
                Log.d(this.toString(), "Wear requesting list");
                if (messageEvent.getData() != null)
                    this.period = (int) messageEvent.getData()[0];
                syncEventList(period);

                break;
            case Wear_Request_Open_Event:
                Log.d(this.toString(), "Wear requesting to open event");

                Intent openIntent = new Intent(this, MainActivity.class);
                openIntent.setAction(MobileListenerService.Action_Open_Event);
                openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK);
                int eventID = Integer.valueOf(new String(messageEvent.getData()));
                openIntent.putExtra(getString(R.string.intent_event_id_key), eventID);
                startActivity(openIntent);
                break;
            default:
                super.onMessageReceived(messageEvent);
                break;
        }
    }

    private void syncEventList(int period) {
        Gson gson = new GsonBuilder()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .setDateFormat(DateFormat.DEFAULT, DateFormat.DEFAULT)
                .create();
        JsonArray jsonArray = new JsonArray();

        Calendar currentDay = Calendar.getInstance();
        currentDay.set(Calendar.HOUR, 0);
        currentDay.set(Calendar.SECOND, 0);
        currentDay.set(Calendar.MILLISECOND, 0);

        Calendar lastDate = Calendar.getInstance();
        lastDate.set(Calendar.HOUR_OF_DAY, 23);
        lastDate.set(Calendar.MINUTE, 59);
        lastDate.set(Calendar.SECOND, 59);

        switch (period) {
            case 1:
                lastDate.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case 2:
                lastDate.add(Calendar.MONTH, 1);
                break;
            case 3:
                lastDate.add(Calendar.YEAR, 1);
                break;
        }

        Realm realm = Realm.getInstance(this);
        RealmResults<Event> allObjects = realm.allObjectsSorted(Event.class, "startDate", true);
        ArrayList<Event> results = new ArrayList<>();

        for (Event event : allObjects) {
            RepeatRule rule = realm.where(RepeatRule.class).equalTo("eventID", event.getEventID()).findFirst();
            Calendar startDate = Calendar.getInstance();
            startDate.setTime(event.getStartDate());
            if (startDate.after(currentDay) && startDate.before(lastDate)) {
                results.add(new Event(event.getEventID(), event.getTitle(), event.getStartDate(), event.getEndDate(), event.getDescription(), event.getGroupID()));
            }
            if (rule != null) {
                Calendar eventEndDate = null;
                if (event.getEndDate() != null) {
                    eventEndDate = Calendar.getInstance();
                    eventEndDate.setTime(event.getEndDate());
                }

                Calendar endRepeatDate = Calendar.getInstance();
                endRepeatDate.setTime(rule.getEndRepeatDate() == null ? lastDate.getTime() : rule.getEndRepeatDate());
                if (endRepeatDate.after(lastDate.getTime()))
                    endRepeatDate.setTime(lastDate.getTime());

                if (rule.getRepeatPeriod() != 0) {
                    long repeats = (endRepeatDate.getTimeInMillis() - (startDate.before(currentDay) ? currentDay.getTimeInMillis() : startDate.getTimeInMillis())) / rule.getRepeatPeriod();    //endRepeatDate.get(Calendar.DAY_OF_YEAR) - startDate.get(Calendar.DAY_OF_YEAR);
                    repeats = repeats < 0 ? 0 : repeats;
                    for (int i = 0; i < repeats; i++) {
                        startDate.add(Calendar.MILLISECOND, (int) rule.getRepeatPeriod());
                        if (eventEndDate != null)
                            eventEndDate.add(Calendar.MILLISECOND, (int) rule.getRepeatPeriod());
                        if (startDate.after(lastDate))
                            break;
                        results.add(new Event(event.getEventID(), event.getTitle(), startDate.getTime(), eventEndDate == null ? null : eventEndDate.getTime(), event.getDescription(), event.getGroupID()));
                    }
                }
            }
        }

        if (!results.isEmpty()) {
            for (Event event : results) {
                jsonArray.add(getJsonFromEvent(event));
            }
        }

        String jsonString = gson.toJson(jsonArray);
        Intent listIntent = new Intent(this, MobileListenerService.class);
        listIntent.putExtra(this.getString(R.string.intent_event_list_key), jsonString);
        listIntent.setAction(MobileListenerService.Action_Send_List);
        startService(listIntent);
    }

    private JsonElement getJsonFromEvent(Event event) {
        JsonObject json = new JsonObject();
        json.addProperty("event_id", event.getEventID());
        json.addProperty("title", event.getTitle());
        json.addProperty("start_date", dateFormat.format(event.getStartDate()));
        if (event.getEndDate() != null)
            json.addProperty("end_date", dateFormat.format(event.getEndDate()));
        if (event.getDescription() != null && !event.getDescription().isEmpty())
            json.addProperty("description", event.getDescription());
        if (event.getGroupID() != null && !event.getGroupID().isEmpty())
            json.addProperty("group_id", event.getGroupID());
        return json;
    }
}