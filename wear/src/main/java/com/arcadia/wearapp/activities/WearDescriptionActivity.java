package com.arcadia.wearapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.arcadia.wearapp.Event;
import com.arcadia.wearapp.R;
import com.arcadia.wearapp.WearApplication;
import com.arcadia.wearapp.services.WearListenerService;

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
    private int eventPosition;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);

        String longDatePattern = String.format("%s, %s", android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "E MMM dd"), android.text.format.DateFormat.is24HourFormat(this) ? "H:mm" : "h:mm a");
        this.dateFormat = new SimpleDateFormat(longDatePattern, Locale.getDefault());

        if (getIntent().getExtras() != null) {
            eventPosition = getIntent().getExtras().getInt(getString(R.string.intent_event_position_key));
            setEvent(eventPosition);

            nameTextView = (TextView) findViewById(R.id.name_text);
            dateTextView = (TextView) findViewById(R.id.date_text);
            descriptionTextView = (TextView) findViewById(R.id.description_text);
            descriptionLayout = (LinearLayout) findViewById(R.id.description_layout);
            openPhoneButton = (Button) findViewById(R.id.open_phone_button);

            isInflate = true;

            createUI();
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

    private void createUI() {
        if (isInflate) {
            nameTextView.setText(event.getTitle());

            dateTextView.setText(dateFormat.format(event.getStartDate()));
            if (event.getEndDate() != null && event.getEndDate().after(event.getStartDate()))
                dateTextView.append(String.format(" - %s", dateFormat.format(event.getEndDate())));

            if (event.getDescription() != null && !event.getDescription().isEmpty()) {
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
                        intent.putExtra(getString(R.string.intent_event_id), event.getEventID());
                        startService(intent);
                    }
                }
            });
        }
    }

    public void setEvent(int position) {
        this.event = null;
        if (((WearApplication) getApplicationContext()).events != null) {
            this.event = ((WearApplication) getApplicationContext()).events.get(position);
            if (this.event == null)
                finish();
            else
                createUI();
        }
    }

    public int getEventPosition() {
        return eventPosition;
    }
}
