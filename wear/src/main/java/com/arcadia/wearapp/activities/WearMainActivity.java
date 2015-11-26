package com.arcadia.wearapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.WearApplication;
import com.arcadia.wearapp.adapters.WearPagerAdapter;
import com.arcadia.wearapp.services.WearBroadcastReceiver;
import com.arcadia.wearapp.services.WearListenerService;

public class WearMainActivity extends Activity {
    private WearPagerAdapter pagerAdapter;
    private WearBroadcastReceiver broadcastReceiver = new WearBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, WearListenerService.class);
        intent.setAction(WearListenerService.Action_Request_List);
        startService(intent);

        GridViewPager viewPager = (GridViewPager) findViewById(R.id.viewpager);
        pagerAdapter = new WearPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        DotsPageIndicator pageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        pageIndicator.setPager(viewPager);

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, messageFilter);
    }

    @Override
    protected void onResume() {
        ((WearApplication) getApplicationContext()).setCurrentActivity(this);
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

    public void redrawListView() {
        pagerAdapter.notifyDataSetChanged();

        Intent notificationIntent = new Intent(this, WearListenerService.class);
        notificationIntent.setAction(WearListenerService.Action_Update_Notification);
        startService(notificationIntent);
    }

    public void setConnection(boolean isConnected) {
        pagerAdapter.onCheckConnection(isConnected);
    }
}
