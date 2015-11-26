package com.arcadia.wearapp.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.Reminder;
import com.arcadia.wearapp.realm_objects.RepeatRule;
import com.arcadia.wearapp.services.MobileListenerService;
import com.arcadia.wearapp.views.ReminderView;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class DescriptionActivity extends AppCompatActivity {
    public static final int datepicker_type_start = 1;
    public static final int datepicker_type_end = 2;
    public static final int datepicker_type_repeat = 3;
    public Calendar startDate = Calendar.getInstance();
    public SimpleDateFormat timeFormat;
    public SimpleDateFormat dateFormat;
    private LinearLayout remindersLayout;
    private Calendar endDate;
    private Calendar repeatDate;
    private boolean editMode;
    private List<ReminderView> reminderViews;
    private int eventID = -1;
    private long repeatTimeMillis = 0;
    private boolean isChanged = false;
    private Spinner repeatSpinner;
    private TextView startDateTV;
    private LinearLayout repeatUntilLayout;
    private Spinner repeatUntilSpinner;
    private LinearLayout repeatDateLayout;
    private ImageButton clearStartDateButton;
    private ImageButton clearEndDateButton;
    private ImageButton clearNameButton;
    private ImageButton clearDescriptionButton;
    private TextView remindersLabel;
    private TextView endDateTV;
    private TextView startTimeTV;
    private TextView repeatDateTV;
    private ImageButton addReminderButton;
    private EditText nameEditText;
    private TextView endTimeTV;
    private boolean userIsInteracting = false;
    TimePickerDialog.OnTimeSetListener endTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialPickerLayout radialPickerLayout, int hourOfDay, int minute) {
            if (endDate == null) {
                endDate = Calendar.getInstance();
                if (endDateTV.getText().toString().isEmpty())
                    endDate.setTime(startDate.getTime());
            }
            endDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
            endDate.set(Calendar.MINUTE, minute);
            endDate.set(Calendar.SECOND, 0);
            endTimeTV.setText(timeFormat.format(endDate.getTime()));
            if (endDateTV.getText().toString().isEmpty())
                endDateTV.setText(dateFormat.format(endDate.getTime()));
            if (editMode)
                clearEndDateButton.setVisibility(View.VISIBLE);
            setChanged(true);
        }
    };
    DatePickerDialog.OnDateSetListener startDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePickerDialog datePickerDialog, int year, int monthOfYear, int dayOfMonth) {
            startDate.set(year, monthOfYear, dayOfMonth);
            startDate.set(Calendar.SECOND, 0);
            startDate.set(Calendar.MILLISECOND, 0);
            startDateTV.setText(dateFormat.format(startDate.getTime()));
            for (ReminderView reminderView : reminderViews) {
                reminderView.setRemind(reminderView.getRemindPosition());
            }
            if (startTimeTV.getText().toString().isEmpty())
                startTimeTV.setText(timeFormat.format(startDate.getTime()));
            if (editMode) {
                clearStartDateButton.setVisibility(View.VISIBLE);
                setRepeatRule(repeatSpinner.getSelectedItemPosition());
            }
            setChanged(true);
        }
    };
    TimePickerDialog.OnTimeSetListener startTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialPickerLayout radialPickerLayout, int hourOfDay, int minute) {
            startDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
            startDate.set(Calendar.MINUTE, minute);
            startDate.set(Calendar.SECOND, 0);
            startTimeTV.setText(timeFormat.format(startDate.getTime()));
            for (ReminderView reminderView : reminderViews) {
                reminderView.setRemind(reminderView.getRemindPosition());
            }
            if (startDateTV.getText().toString().isEmpty())
                startDateTV.setText(dateFormat.format(startDate.getTime()));
            if (endDateTV.getText().toString().isEmpty() || endTimeTV.getText().toString().isEmpty()) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(DescriptionActivity.this);
                if (preferences.getBoolean("timePeriod", false)) {
                    if (endDate == null)
                        endDate = Calendar.getInstance();
                    endDate.setTime(startDate.getTime());
                    endDate.add(Calendar.HOUR_OF_DAY, 1);
                    endDateTV.setText(dateFormat.format(endDate.getTime()));
                    endTimeTV.setText(timeFormat.format(endDate.getTime()));
                    clearEndDateButton.setVisibility(View.VISIBLE);
                }
            }
            if (editMode) {
                clearStartDateButton.setVisibility(View.VISIBLE);
                setRepeatRule(repeatSpinner.getSelectedItemPosition());
            }
            setChanged(true);
        }
    };
    DatePickerDialog.OnDateSetListener endDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePickerDialog datePickerDialog, int year, int monthOfYear, int dayOfMonth) {
            if (endDate == null) {
                endDate = Calendar.getInstance();
                if (endTimeTV.getText().toString().isEmpty())
                    endDate.setTime(startDate.getTime());
            }
            endDate.set(year, monthOfYear, dayOfMonth);
            endDate.set(Calendar.SECOND, 0);
            endDateTV.setText(dateFormat.format(endDate.getTime()));
            if (endTimeTV.getText().toString().isEmpty())
                endTimeTV.setText(timeFormat.format(endDate.getTime()));
            if (editMode)
                clearEndDateButton.setVisibility(View.VISIBLE);
            setChanged(true);
        }
    };
    private EditText descriptionEditText;
    private DatePickerDialog.OnDateSetListener repeatDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            if (repeatDate == null) {
                repeatDate = Calendar.getInstance();
                repeatDate.setTime(startDate.getTime());
            }
            repeatDate.set(Calendar.YEAR, year);
            repeatDate.set(Calendar.MONTH, monthOfYear);
            repeatDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            repeatDate.set(Calendar.HOUR_OF_DAY, 23);
            repeatDate.set(Calendar.MINUTE, 59);
            repeatDate.set(Calendar.SECOND, 59);

            repeatDateTV.setText(dateFormat.format(repeatDate.getTime()));
            setChanged(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);
        getIntent().getFlags();

        nameEditText = (EditText) findViewById(R.id.name_text);
        clearNameButton = (ImageButton) findViewById(R.id.clear_name_text_button);
        clearDescriptionButton = (ImageButton) findViewById(R.id.clear_description_text_button);
        clearStartDateButton = (ImageButton) findViewById(R.id.clear_start_date_button);
        clearEndDateButton = (ImageButton) findViewById(R.id.clear_end_date_button);
        addReminderButton = (ImageButton) findViewById(R.id.add_remind_button);

        startDateTV = (TextView) findViewById(R.id.start_date);
        startTimeTV = (TextView) findViewById(R.id.start_time);

        endDateTV = (TextView) findViewById(R.id.end_date);
        endTimeTV = (TextView) findViewById(R.id.end_time);
        descriptionEditText = (EditText) findViewById(R.id.description_text);

        remindersLabel = (TextView) findViewById(R.id.reminders_text_label);

        reminderViews = new ArrayList<>();

        repeatSpinner = (Spinner) findViewById(R.id.repeat_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.repeat_rules_array, R.layout.spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(spinnerAdapter);

        repeatUntilLayout = (LinearLayout) findViewById(R.id.repeat_until_layout);
        repeatDateLayout = (LinearLayout) findViewById(R.id.repeat_until_date_layout);

        repeatUntilSpinner = (Spinner) findViewById(R.id.repeat_until_spinner);
        ArrayAdapter<CharSequence> untilSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.repeat_until_array, R.layout.spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatUntilSpinner.setAdapter(untilSpinnerAdapter);
        if (!editMode) {
            repeatSpinner.setEnabled(false);
            repeatSpinner.setBackgroundResource(R.drawable.custom_spinner_drawable);
            repeatUntilSpinner.setEnabled(false);
            repeatUntilSpinner.setBackgroundResource(R.drawable.custom_spinner_drawable);
        }

        repeatDateTV = (TextView) findViewById(R.id.repeat_until_date);

        remindersLayout = (LinearLayout) findViewById(R.id.reminders_list);

        setDateTimeFormat();

        Realm realm = Realm.getInstance(this);
        Event event;
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            eventID = bundle.getInt(getString(R.string.intent_event_id_key));
            event = realm.where(Event.class).equalTo("eventID", eventID).findFirst();
            if (event != null) {
                nameEditText.setText(event.getTitle());
                startDate.setTime(event.getStartDate());
                startDateTV.setText(dateFormat.format(startDate.getTime()));
                startTimeTV.setText(timeFormat.format(startDate.getTime()));
                if (event.getEndDate() != null && event.getEndDate().after(startDate.getTime())) {
                    endDate = Calendar.getInstance();
                    endDate.setTime(event.getEndDate());
                    endDateTV.setText(dateFormat.format(endDate.getTime()));
                    endTimeTV.setText(timeFormat.format(endDate.getTime()));
                } else {
                    endDate = null;
                }
                if (event.getDescription() != null)
                    descriptionEditText.setText(event.getDescription());

                RepeatRule repeatRule = realm.where(RepeatRule.class).equalTo("eventID", event.getEventID()).findFirst();
                if (repeatRule != null && repeatRule.getRepeatPeriod() != 0) {
                    repeatTimeMillis = repeatRule.getRepeatPeriod();
                    if (repeatTimeMillis == AlarmManager.INTERVAL_DAY) {
                        repeatSpinner.setSelection(1);
                    } else if (repeatTimeMillis == AlarmManager.INTERVAL_DAY * 7) {
                        repeatSpinner.setSelection(2);
                    } else if (repeatTimeMillis >= AlarmManager.INTERVAL_DAY * 365) {
                        repeatSpinner.setSelection(4);
                    } else {
                        repeatSpinner.setSelection(3);
                    }
                    repeatUntilLayout.setVisibility(View.VISIBLE);
                    if (repeatRule.getEndRepeatDate() != null) {
                        repeatUntilSpinner.setSelection(1);

                        repeatDate = Calendar.getInstance();
                        repeatDate.setTime(repeatRule.getEndRepeatDate());

                        repeatDateTV.setText(dateFormat.format(repeatDate.getTime()));
                        repeatDateLayout.setVisibility(View.VISIBLE);
                    } else {
                        repeatUntilSpinner.setSelection(0);
                    }
                }
                RealmResults<Reminder> reminders = realm.where(Reminder.class).equalTo("eventID", event.getEventID()).findAll();
                if (!reminders.isEmpty()) {
                    for (Reminder reminder : reminders) {
                        addRemindView(reminder);
                    }
                }
            }
        } else {
            remindersLabel.setVisibility(View.GONE);
            setTitle(getString(R.string.description_activity_new_event_title));
            allowEditMode();
        }
        realm.close();
    }

    private void addRemindView(Reminder reminder) {
        ReminderView reminderView = new ReminderView(remindersLayout, reminder, this);
        reminderViews.add(reminderView);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        userIsInteracting = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_description, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save_button:
                if (invalidateData())
                    saveAndExit();
                return true;
            case R.id.menu_edit_button:
                allowEditMode();
                return true;
            case R.id.menu_remove_button:
                showRemoveDialog();
                return true;
            case R.id.home:
                if (editMode && isChanged)
                    showSaveDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeAndExit() {

        Realm realm = Realm.getInstance(this);
        Event event = realm.where(Event.class).equalTo("eventID", eventID).findFirst();
        if (event != null) {
            RealmResults<RepeatRule> repeatRules = realm.where(RepeatRule.class).equalTo("eventID", eventID).findAll();
            if (repeatRules != null)
                for (RepeatRule rule : repeatRules) {
                    realm.beginTransaction();
                    rule.removeFromRealm();
                    realm.commitTransaction();
                }

            RealmResults<Reminder> reminders = realm.where(Reminder.class).equalTo("eventID", eventID).findAll();
            for (Reminder reminder : reminders) {
                Intent intent = new Intent();
                intent.setAction(getString(R.string.broadcast_action));
                intent.putExtra("reminderId", reminder.getReminderID());
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, reminder.getReminderID(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);

                realm.beginTransaction();
                reminder.removeFromRealm();
                realm.commitTransaction();
            }

            realm.beginTransaction();
            event.removeFromRealm();
            realm.commitTransaction();

            Intent listIntent = new Intent(DescriptionActivity.this, MobileListenerService.class);
            listIntent.setAction(MobileListenerService.Action_Sync);
            startService(listIntent);

            setResult(RESULT_OK);
        } else
            setResult(RESULT_CANCELED);
        realm.close();
        Intent intent = NavUtils.getParentActivityIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        NavUtils.navigateUpTo(this, intent);
    }

    private void allowEditMode() {
        editMode = true;

        nameEditText.setEnabled(true);
        nameEditText.setFocusableInTouchMode(true);
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setChanged(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0)
                    clearNameButton.setVisibility(View.VISIBLE);
                else
                    clearNameButton.setVisibility(View.GONE);
            }
        });
        clearNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameEditText.setText("");
            }
        });
        if (!nameEditText.getText().toString().isEmpty())
            clearNameButton.setVisibility(View.VISIBLE);

        descriptionEditText.setEnabled(true);
        descriptionEditText.setCursorVisible(true);
        descriptionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setChanged(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0)
                    clearDescriptionButton.setVisibility(View.VISIBLE);
                else
                    clearDescriptionButton.setVisibility(View.GONE);
            }
        });
        clearDescriptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                descriptionEditText.setText("");
            }
        });

        startDateTV.setEnabled(true);
        startDateTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(startDate, datepicker_type_start);
            }
        });
        startTimeTV.setEnabled(true);
        startTimeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                if (startDate == null)
                    startDate = Calendar.getInstance();
                calendar.setTime(startDate.getTime());

                showTimePicker(calendar, datepicker_type_start);
            }
        });
        clearStartDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDateTV.setText("");
                startTimeTV.setText("");
                startDate = Calendar.getInstance();
                clearStartDateButton.setVisibility(View.GONE);
                setChanged(true);
            }
        });
        if (!startDateTV.getText().toString().isEmpty())
            clearStartDateButton.setVisibility(View.VISIBLE);

        endDateTV.setEnabled(true);
        endDateTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (endDate != null)
                    showDatePicker(endDate, datepicker_type_end);
                else
                    showDatePicker(startDate, datepicker_type_end);
            }
        });
        endTimeTV.setEnabled(true);
        endTimeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();

                if (endDate != null)
                    calendar.setTime(endDate.getTime());
                else
                    calendar.setTime(startDate.getTime());

                showTimePicker(calendar, datepicker_type_end);
            }
        });
        clearEndDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endDateTV.setText("");
                endTimeTV.setText("");
                endDate = null;
                clearEndDateButton.setVisibility(View.GONE);
                setChanged(true);
            }
        });
        if (!endDateTV.getText().toString().isEmpty())
            clearEndDateButton.setVisibility(View.VISIBLE);

        repeatSpinner.setEnabled(true);
        repeatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setChanged(true);
                setRepeatRule(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        repeatUntilSpinner.setEnabled(true);
        repeatUntilSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setChanged(true);
                if (position == 1) {
                    repeatDateLayout.setVisibility(View.VISIBLE);
                } else {
                    repeatDateLayout.setVisibility(View.GONE);
                    repeatDate = null;
                    repeatDateTV.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        repeatDateTV.setEnabled(true);
        repeatDateTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repeatDate != null)
                    showDatePicker(repeatDate, datepicker_type_repeat);
                else
                    showDatePicker(startDate, datepicker_type_repeat);
            }
        });
        remindersLabel.setVisibility(View.VISIBLE);
        addReminderButton.setVisibility(View.VISIBLE);
        addReminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setChanged(true);
                addRemindView(new Reminder(eventID));
            }
        });

        for (ReminderView view : reminderViews) {
            view.setEditMode(true);
        }

        invalidateOptionsMenu();
    }

    private void setRepeatRule(int type) {
        repeatUntilLayout.setVisibility(View.VISIBLE);
        Calendar nextStartDate = Calendar.getInstance();
        nextStartDate.setTime(startDate.getTime());
        switch (type) {
            case 0:
                repeatUntilLayout.setVisibility(View.GONE);
                break;
            case 1:
                nextStartDate.add(Calendar.DAY_OF_MONTH, 1);
                break;
            case 2:
                nextStartDate.add(Calendar.DAY_OF_MONTH, 7);
                break;
            case 3:
                nextStartDate.add(Calendar.MONTH, 1);
                break;
            case 4:
                nextStartDate.add(Calendar.YEAR, 1);
                break;
        }
        repeatTimeMillis = nextStartDate.getTimeInMillis() - startDate.getTimeInMillis();
    }

    private void saveAndExit() {
        if (isChanged) {
            Realm realm = Realm.getInstance(this);
            Event event = realm.where(Event.class).equalTo("eventID", eventID).findFirst();
            if (event == null)
                event = new Event();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            boolean isLocally = preferences.getBoolean("locallyTimezone", true);
            String timezone = preferences.getString("timezoneList", TimeZone.getDefault().getDisplayName());

            realm.beginTransaction();
            event.setTitle(nameEditText.getText().toString());
            realm.commitTransaction();

            String description = null;
            if (descriptionEditText.getText() != null) {
                description = descriptionEditText.getText().toString();
            }
            realm.beginTransaction();
            event.setDescription(description);
            realm.commitTransaction();

            if (startDate != null) {
                if (!isLocally)
                    startDate.setTimeZone(TimeZone.getTimeZone(timezone));
                realm.beginTransaction();
                event.setStartDate(startDate.getTime());
                realm.commitTransaction();
                if (endDate == null) {
                    realm.beginTransaction();
                    event.setEndDate(null);
                    realm.commitTransaction();
                } else {
                    endDate.setTimeZone(TimeZone.getTimeZone(timezone));
                    realm.beginTransaction();
                    event.setEndDate(endDate.getTime());
                    realm.commitTransaction();
                }
            }

            if (event.getEventID() == 0) {
                // increment index
                long nextID = 1;
                if (realm.where(Event.class).max("eventID") != null)
                    nextID += (long) realm.where(Event.class).max("eventID");

                realm.beginTransaction();
                // insert new value
                event.setEventID((int) nextID);
                realm.commitTransaction();

                realm.beginTransaction();
                realm.copyToRealm(event);
                realm.commitTransaction();
            }

            if (repeatTimeMillis > 0) {
                RepeatRule repeatRule = realm.where(RepeatRule.class).equalTo("eventID", event.getEventID()).findFirst();
                if (repeatRule == null)
                    repeatRule = new RepeatRule(event.getEventID());

                realm.beginTransaction();
                repeatRule.setRepeatPeriod(repeatTimeMillis);
                repeatRule.setEndRepeatDate(repeatDate == null ? null : repeatDate.getTime());
                if (repeatRule.getRuleID() == 0) {
                    int newRuleID = 1;
                    RealmQuery<RepeatRule> query = realm.where(RepeatRule.class);
                    if (query.count() > 0 && (query.max("ruleID") != null))
                        newRuleID += (long) query.max("ruleID");
                    repeatRule.setRuleID(newRuleID);
                }
                realm.copyToRealmOrUpdate(repeatRule);
                realm.commitTransaction();
            } else {
                RealmResults<RepeatRule> repeatRules = realm.where(RepeatRule.class).equalTo("eventID", event.getEventID()).findAll();

                realm.beginTransaction();
                repeatRules.clear();
                realm.commitTransaction();
            }
            for (ReminderView view : reminderViews) {

                Reminder reminder = view.getReminder();
                if (realm.where(Reminder.class).equalTo("reminderID", reminder.getReminderID()).findFirst() == null) {
                    long nextID = 1;
                    if (realm.where(Reminder.class).max("reminderID") != null)
                        nextID += (long) realm.where(Reminder.class).max("reminderID");

                    realm.beginTransaction();
                    reminder.setReminderID((int) nextID);
                    realm.commitTransaction();
                }
                int offset = (int) (view.getDate().getTimeInMillis() - startDate.getTimeInMillis()) / 1000;
                realm.beginTransaction();
                reminder.setAlertOffset(offset);
                reminder.setEventID(event.getEventID());
                realm.copyToRealmOrUpdate(reminder);
                realm.commitTransaction();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(view.getDate().getTime());
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                if (!isLocally)
                    calendar.setTimeZone(TimeZone.getTimeZone(timezone));


                Intent intent = new Intent();
                intent.setAction(getString(R.string.broadcast_action));
                intent.putExtra("reminderId", reminder.getReminderID());

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (repeatTimeMillis != 0) {
                    if (repeatDate != null) {
                        long repeats = (repeatDate.getTimeInMillis() - calendar.getTimeInMillis()) / repeatTimeMillis;
                        for (int r = 1; r <= repeats; r++) {
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, reminder.getReminderID() + r * 100000, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        }
                    }
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, reminder.getReminderID(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), repeatTimeMillis, pendingIntent);
                } else {
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(this, reminder.getReminderID(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }
            realm.close();

            Intent listIntent = new Intent(DescriptionActivity.this, MobileListenerService.class);
            listIntent.setAction(MobileListenerService.Action_Sync);
            startService(listIntent);

            Intent data = new Intent();
            data.putExtra(getString(R.string.intent_event_id_key), eventID);
            setResult(RESULT_OK, data);
        } else
            setResult(RESULT_CANCELED);
        Intent intent = NavUtils.getParentActivityIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        NavUtils.navigateUpTo(this, intent);
    }

    private boolean invalidateData() {
        if (nameEditText.getText().toString().isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_name, Toast.LENGTH_SHORT).show();
            return false;
        } else if (startDateTV.getText().toString().isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_start_date, Toast.LENGTH_SHORT).show();
            return false;
        } else if (!endDateTV.getText().toString().isEmpty()) {
            if (endDate != null && endDate.before(startDate)) {
                Toast.makeText(this, R.string.toast_wrong_end_date, Toast.LENGTH_SHORT).show();
                endDate = null;
                endDateTV.setText("");
                endTimeTV.setText("");
                clearEndDateButton.setVisibility(View.GONE);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setDateTimeFormat();
    }

    @Override
    public void onBackPressed() {
        if (editMode && isChanged)
            showSaveDialog();
        else
            super.onBackPressed();
    }

    public void setDateTimeFormat() {
        this.timeFormat = (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String skeleton = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "E MMM dd");
            this.dateFormat = new SimpleDateFormat(skeleton, Locale.getDefault());
        } else {
            this.dateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        }
    }

    protected void showDatePicker(Calendar calendar, int type) {
        if (calendar == null)
            calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        DatePickerDialog dpd = null;
        switch (type) {
            case datepicker_type_start:
                dpd = DatePickerDialog.newInstance(startDateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                break;
            case datepicker_type_end:
                dpd = DatePickerDialog.newInstance(endDateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                break;
            case datepicker_type_repeat:
                dpd = DatePickerDialog.newInstance(repeatDateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                break;
        }
        if (dpd != null) {
            dpd.setCancelable(true);
            dpd.show(getFragmentManager(), getString(R.string.datepicker_tag));
        }
    }

    private void showSaveDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setTitle(getString(R.string.dialog_exit_title))
                .setMessage(getString(R.string.dialog_exit_message))
                .setPositiveButton(getString(R.string.dialog_positive_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (invalidateData())
                            saveAndExit();
                        NavUtils.navigateUpFromSameTask(DescriptionActivity.this);
                    }
                }).setNegativeButton(getString(R.string.dialog_negative_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setResult(RESULT_CANCELED);
                Intent intent = NavUtils.getParentActivityIntent(DescriptionActivity.this);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(DescriptionActivity.this, intent);
            }
        }).setNeutralButton(getString(R.string.dialog_cancel_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showRemoveDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setTitle(getString(R.string.dialog_remove_title))
                .setMessage(getString(R.string.dialog_remove_message))
                .setPositiveButton(getString(R.string.dialog_positive_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (invalidateData())
                            removeAndExit();
                        NavUtils.navigateUpFromSameTask(DescriptionActivity.this);
                    }
                })
                .setNegativeButton(getString(R.string.dialog_negative_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void showTimePicker(Calendar calendar, int type) {
        calendar.set(Calendar.SECOND, 0);
        TimePickerDialog tpd = null;
        boolean is24hFormat = false;
        if (android.text.format.DateFormat.is24HourFormat(this))
            is24hFormat = true;
        switch (type) {
            case datepicker_type_start:
                tpd = TimePickerDialog.newInstance(startTimeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24hFormat);
                break;
            case datepicker_type_end:
                tpd = TimePickerDialog.newInstance(endTimeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24hFormat);
                break;
        }
        if (tpd != null) {
            tpd.setCancelable(true);
            tpd.show(getFragmentManager(), getString(R.string.timepicker_tag));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (editMode) {
            if (eventID != -1)
                menu.findItem(R.id.menu_remove_button).setVisible(true);
            if (isChanged) {
                menu.findItem(R.id.menu_edit_button).setVisible(false);
                menu.findItem(R.id.menu_save_button).setVisible(true);
            }
        } else {
            menu.findItem(R.id.menu_edit_button).setVisible(true);
            menu.findItem(R.id.menu_save_button).setVisible(false);
            menu.findItem(R.id.menu_remove_button).setVisible(false);
        }
        return true;
    }

    public void setChanged(boolean changed) {
        if (userIsInteracting) {
            this.isChanged = changed;
            invalidateOptionsMenu();
        }
    }

    public void removeReminder(ReminderView view) {
        remindersLayout.removeView(view.getView());
        reminderViews.remove(view);
        Realm realm = Realm.getInstance(this);
        if (view.getReminder().isValid()) {
            realm.beginTransaction();
            view.getReminder().removeFromRealm();
            realm.commitTransaction();
        }
        realm.close();
        setChanged(true);
    }

    public boolean getEditMode() {
        return editMode;
    }
}