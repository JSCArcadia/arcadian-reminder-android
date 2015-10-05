package com.arcadia.wearapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

public class WearListenerService extends WearableListenerService {

    public static final String Action_Request_List = "com.arcadia.wearapp.action.RL";
    public static final String Action_Sync = "com.arcadia.wearapp.action.SYNC";
    public static final String Action_Open_Event = "com.arcadia.wearapp.action.OE";
    public static final String Mobile_List_Path = "/wearapp_send_list";
    public static final String Wear_Request_List = "wearapp_request_list";
    public static final String Wear_Request_Open_Event = "wearapp_open_event";
    public static final String Mobile_Send_List = "mobile_send_list";

    private GoogleApiClient googleClient;
    private boolean isRequired = false;

    public WearListenerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        googleClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(this.toString(), "onConnected: " + connectionHint);
                        Wearable.DataApi.addListener(googleClient, WearListenerService.this);

                        if (isRequired) {
                            requireList();
                        }
                        isRequired = false;
                        // Now you can use the Data Layer API
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
                .addApi(Wearable.API)
                .build();
        googleClient.connect();
    }

    private void requireList() {
        Intent intent = new Intent(Action_Sync);
        sendBroadcast(intent);
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), Wear_Request_List, null).await();
                    if (result.getStatus().isSuccess()) {
                        Log.d("Listener Service", "Request sent to: " + node.getDisplayName());
                    } else {
                        Log.d("Listener Service", "ERROR: failed to send Request");
                    }
                }
            }
        }).start();
    }

    private void openEventOnPhone(final String event) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), Wear_Request_Open_Event, event.getBytes()).await();
                    if (result.getStatus().isSuccess()) {
                        Log.d("Listener Service", "Request sent to: " + node.getDisplayName());
                    } else {
                        Log.d("Listener Service", "ERROR: failed to send Request");
                    }
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();
            switch (action) {
                case Action_Request_List:
                    if (!googleClient.isConnected()) {
                        isRequired = true;
                        googleClient.connect();
                    } else
                        requireList();
                    break;
                case Action_Open_Event:
                    int event = intent.getExtras().getInt(getString(R.string.intent_event_id_key));
                    openEventOnPhone(String.valueOf(event));
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("Data Changed", "List changed on mobile");
        super.onDataChanged(dataEvents);
        final List<DataEvent> eventList = FreezableUtils.freezeIterable(dataEvents);
        for (DataEvent event : eventList) {
            final Uri uri = event.getDataItem().getUri();
            final String path = uri != null ? uri.getPath() : null;
            if (path != null)
                switch (path) {
                    case Mobile_List_Path:
                        final DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                        // read your values from map:
                        String json = map.getString("json");
//                        String json = "[{\"endDate\":\"Jun 8, 2015 2:32:00 PM\",\"startDate\":\"Jun 8, 2015 2:32:00 PM\",\"title\":\"Item no. 1\",\"eventID\":1},{\"endDate\":\"Jun 7, 2015 2:32:00 PM\",\"startDate\":\"Jun 7, 2015 2:32:00 PM\",\"title\":\"Item no. 2\",\"eventID\":2},{\"endDate\":\"Jun 6, 2015 2:32:00 PM\",\"startDate\":\"Jun 6, 2015 2:32:00 PM\",\"title\":\"Item no. 3\",\"eventID\":3},{\"endDate\":\"Jun 5, 2015 2:32:00 PM\",\"startDate\":\"Jun 5, 2015 2:32:00 PM\",\"title\":\"Item no. 4\",\"eventID\":4},{\"endDate\":\"Jun 4, 2015 2:32:00 PM\",\"startDate\":\"Jun 4, 2015 2:32:00 PM\",\"title\":\"Item no. 5\",\"eventID\":5},{\"endDate\":\"Jun 3, 2015 2:32:00 PM\",\"startDate\":\"Jun 3, 2015 2:32:00 PM\",\"title\":\"Item no. 6\",\"eventID\":6},{\"endDate\":\"Jun 2, 2015 2:32:00 PM\",\"startDate\":\"Jun 2, 2015 2:32:00 PM\",\"title\":\"Item no. 7\",\"eventID\":7}]";
                        updateListView(json);
                        break;
                }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        switch (messageEvent.getPath()) {
            case Mobile_Send_List:
                Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).authority(messageEvent.getSourceNodeId()).path(Mobile_List_Path).build();
                DataApi.DataItemResult result = Wearable.DataApi.getDataItem(googleClient, uri).await();
                DataMapItem item = DataMapItem.fromDataItem(result.getDataItem());
                Log.d(this.toString(), "Mobile sent list");
                String json = item.getDataMap().getString("json");
//                String json = "[{\"endDate\":\"Jun 8, 2015 2:32:00 PM\",\"startDate\":\"Jun 8, 2015 2:32:00 PM\",\"title\":\"Item no. 1\",\"eventID\":1},{\"endDate\":\"Jun 7, 2015 2:32:00 PM\",\"startDate\":\"Jun 7, 2015 2:32:00 PM\",\"title\":\"Item no. 2\",\"eventID\":2},{\"endDate\":\"Jun 6, 2015 2:32:00 PM\",\"startDate\":\"Jun 6, 2015 2:32:00 PM\",\"title\":\"Item no. 3\",\"eventID\":3},{\"endDate\":\"Jun 5, 2015 2:32:00 PM\",\"startDate\":\"Jun 5, 2015 2:32:00 PM\",\"title\":\"Item no. 4\",\"eventID\":4},{\"endDate\":\"Jun 4, 2015 2:32:00 PM\",\"startDate\":\"Jun 4, 2015 2:32:00 PM\",\"title\":\"Item no. 5\",\"eventID\":5},{\"endDate\":\"Jun 3, 2015 2:32:00 PM\",\"startDate\":\"Jun 3, 2015 2:32:00 PM\",\"title\":\"Item no. 6\",\"eventID\":6},{\"endDate\":\"Jun 2, 2015 2:32:00 PM\",\"startDate\":\"Jun 2, 2015 2:32:00 PM\",\"title\":\"Item no. 7\",\"eventID\":7}]";

                updateListView(json);
                break;

            default:
                super.onMessageReceived(messageEvent);
                break;
        }
    }

    public void updateListView(String json) {
        if (json == null)
            return;

        Intent sentIntent = new Intent(Intent.ACTION_SEND);
        sentIntent.putExtra("type", Mobile_List_Path);
        sentIntent.putExtra("json", json);
        LocalBroadcastManager.getInstance(this).sendBroadcast(sentIntent);
    }
}