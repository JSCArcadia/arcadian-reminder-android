package com.arcadia.wearapp.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.arcadia.wearapp.Event;
import com.arcadia.wearapp.R;
import com.arcadia.wearapp.WearApplication;
import com.arcadia.wearapp.activities.WearMainActivity;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WearListenerService extends WearableListenerService {

    public static final String Action_Request_List = "com.arcadia.wearapp.action.RL";
    public static final String Action_Sync = "com.arcadia.wearapp.action.SYNC";
    public static final String Action_Open_Event = "com.arcadia.wearapp.action.OE";
    public static final String Action_Update_Notification = "com.arcadia.wearapp.action.UN";
    public static final String Mobile_List_Path = "/wearapp_send_list";
    public static final String Mobile_Check_Connection = "check_connection";
    public static final String Wear_Request_List = "wearapp_request_list";
    public static final String Wear_Request_Open_Event = "wearapp_open_event";
    public static final String Mobile_Send_List = "mobile_send_list";
    public NotificationCompat.Builder notificationBuilder;
    public NotificationManagerCompat manager;
    private GoogleApiClient googleClient;
    private boolean isRequired = false;
    private String previousNotification;

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

                        if (isRequired) {
                            requireList();
                        }
                        isRequired = false;
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

        manager = NotificationManagerCompat.from(this);

        Intent viewIntent = new Intent(this, WearMainActivity.class);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setLocalOnly(true)
                .setVibrate(new long[]{1000, 1000, 1000})
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Next Event")
                .setContentIntent(viewPendingIntent);
    }

    private void requireList() {
        Intent intent = new Intent(Action_Sync);
        sendBroadcast(intent);
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
                for (Node node : nodes.getNodes()) {
                    int period = getSharedPreferences("periodPreferences", MODE_PRIVATE).getInt("selectedPeriod", 0);
                    byte[] data = new byte[]{(byte) period};
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), Wear_Request_List, data).await();
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
            Wearable.DataApi.addListener(googleClient, WearListenerService.this);

            switch (action) {
                case Action_Request_List:
                    checkDeviceConnection();
                    if (!googleClient.isConnected()) {
                        isRequired = true;
                        googleClient.connect();
                    } else {
                        requireList();
                    }
                    break;
                case Action_Open_Event:
                    int event = intent.getExtras().getInt(getString(R.string.intent_event_id));
                    openEventOnPhone(String.valueOf(event));
                    break;
                case Action_Update_Notification:
                    updateNotification();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void checkDeviceConnection() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra("type", Mobile_Check_Connection);
                intent.putExtra("is_connected", !Wearable.NodeApi.getConnectedNodes(googleClient).await().getNodes().isEmpty());
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        }).start();
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

                checkDeviceConnection();
                updateListView(json);

                break;

            default:
                super.onMessageReceived(messageEvent);
                break;
        }
    }

    private void updateListView(String json) {
        Intent sentIntent = new Intent(Intent.ACTION_SEND);
        sentIntent.putExtra("type", Mobile_List_Path);
        sentIntent.putExtra("json", json);
        LocalBroadcastManager.getInstance(this).sendBroadcast(sentIntent);
    }

    public void updateNotification() {
        String contentText = "";
        Calendar currentDate = Calendar.getInstance();
        if (((WearApplication) getApplicationContext()).events.isEmpty()) {
            contentText = "No events";
        } else {
            for (Event event : ((WearApplication) getApplicationContext()).events) {
                if (currentDate.getTime().before(event.getStartDate())) {
                    contentText = String.format("\t%s\n", event.getTitle());
                    if (event.getStartDate() != null) {
                        contentText += (SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())).format(event.getStartDate());
                    }
                    if (event.getEndDate() != null) {
                        contentText += String.format(" - %s", (SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())).format(event.getEndDate()));
                    }
                    if (event.getDescription() != null && event.getDescription().isEmpty())
                        contentText += String.format("\n%s", event.getDescription());
                    break;
                }
            }
        }
        if (!contentText.equals(previousNotification)) {
            notificationBuilder.setContentText(contentText);
            manager.notify(0, notificationBuilder.build());

            previousNotification = contentText;
        }
    }
}