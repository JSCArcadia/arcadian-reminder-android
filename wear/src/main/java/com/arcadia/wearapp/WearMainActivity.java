package com.arcadia.wearapp;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

public class WearMainActivity extends Activity implements WearableListView.ClickListener {
    private WearableListView listView;
    private TextView emptyListTV;
    private WearableAdapter adapter;
    private WearBroadcastReceiver broadcastReceiver = new WearBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (WearableListView) findViewById(R.id.wearable_list);
        emptyListTV = (TextView) findViewById(R.id.empty_list_textview);

        Log.d("On Create", "Request List");
        Intent intent = new Intent(this, WearListenerService.class);
        intent.setAction(WearListenerService.Action_Request_List);
        startService(intent);

        if (((WearApplication) getApplicationContext()).events == null)
            ((WearApplication) getApplicationContext()).events = new ArrayList<>();
        adapter = new WearableAdapter(this, ((WearApplication) getApplicationContext()).events);
        // Assign an adapter to the list
        listView.setAdapter(adapter);

        // Set a click listener
        listView.setClickListener(this);

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, messageFilter);
    }

    @Override
    protected void onResume() {
        ((WearApplication) getApplicationContext()).setCurrentActivity(this);
        redrawListView();
        super.onResume();
    }

    @Override
    protected void onPause() {
        ((WearApplication) getApplicationContext()).setCurrentActivity(null);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        ((WearApplication) getApplicationContext()).setCurrentActivity(null);
        super.onDestroy();
    }

    @Override
    public void onClick(WearableListView.ViewHolder v) {
        int position = v.getPosition();
        Intent intent = new Intent(WearMainActivity.this, WearDescriptionActivity.class);
        intent.putExtra(getString(R.string.intent_event_id_key), adapter.get(position).getEventID());
        startActivity(intent);
    }

    @Override
    public void onTopEmptyRegionClick() {
        finish();
    }

    public void redrawListView() {
        if (((WearApplication) getApplicationContext()).events.isEmpty()) {
            emptyListTV.setVisibility(View.VISIBLE);
        } else {
            emptyListTV.setVisibility(View.GONE);
            adapter = new WearableAdapter(this, ((WearApplication) getApplicationContext()).events);
            listView.setAdapter(adapter);
//            adapter.setList(((WearApplication) getApplicationContext()).events);
        }
    }

//    public class WearBroadcastReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Bundle bundle = intent.getExtras();
//            String type = bundle.getString("type");
//            switch (type) {
//                case WearListenerService.Mobile_List_Path:
//                    redrawListView(bundle.getString("json"));
////                    Log.d(type, events.toString());
//                    break;
//            }
//        }
//    }
}
