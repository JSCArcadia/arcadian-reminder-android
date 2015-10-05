package com.arcadia.wearapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class WearDescriptionActivity extends Activity {

    public boolean isInflate = false;
    private Event event;
    private TextView nameTextView;
    private TextView dateTextView;
    private TextView descriptionTextView;
    private LinearLayout descriptionLayout;
    private Button openPhoneButton;
    WatchViewStub.OnLayoutInflatedListener layoutInflatedListener = new WatchViewStub.OnLayoutInflatedListener() {

        @Override
        public void onLayoutInflated(WatchViewStub stub) {
            nameTextView = (TextView) stub.findViewById(R.id.name_text);
            dateTextView = (TextView) stub.findViewById(R.id.date_text);
            descriptionTextView = (TextView) stub.findViewById(R.id.description_text);
            descriptionLayout = (LinearLayout) stub.findViewById(R.id.description_layout);
            openPhoneButton = (Button) stub.findViewById(R.id.open_phone_button);

            isInflate = true;

            createUI();
        }
    };
    private int eventID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);
        if (getIntent().getExtras() != null) {
            eventID = getIntent().getExtras().getInt(getString(R.string.intent_event_id_key));
            setEvent(eventID);
            final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
            stub.setOnLayoutInflatedListener(layoutInflatedListener);
        }
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

    private void createUI() {
        if (isInflate) {
            nameTextView.setText(event.getTitle());

            dateTextView.setText(new SimpleDateFormat("EEE d MMM h:mm a", Locale.getDefault()).format(event.getStartDate()));
            if (event.getEndDate() != null && event.getEndDate().after(event.getStartDate()))
                dateTextView.append(String.format(" - %s", new SimpleDateFormat("EEE d MMM h:mm a", Locale.getDefault()).format(event.getEndDate())));

            if (event.getDescription() != null) {
                descriptionTextView.setText(event.getDescription());
                descriptionLayout.setVisibility(View.VISIBLE);
            } else
                descriptionLayout.setVisibility(View.GONE);
            openPhoneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (event != null) {
                        Intent intent = new Intent(WearDescriptionActivity.this, WearListenerService.class);
                        intent.setAction(WearListenerService.Action_Open_Event);
                        intent.putExtra(getString(R.string.intent_event_id_key), event.getEventID());
                        startService(intent);
                    }
                }
            });
        }
    }

    public void setEvent(int eventID) {
        this.event = null;
        for (Event event : ((WearApplication) getApplicationContext()).events) {
            if (event.getEventID() == eventID)
                this.event = event;
        }
        if (this.event == null)
            finish();
        else
            createUI();
    }

    public int getEventID() {
        return eventID;
    }
}
