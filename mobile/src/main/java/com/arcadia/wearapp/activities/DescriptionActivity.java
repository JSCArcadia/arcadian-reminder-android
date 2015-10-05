package com.arcadia.wearapp.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
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

import com.arcadia.wearapp.MobileListenerService;
import com.arcadia.wearapp.R;
import com.arcadia.wearapp.adapters.ReminderView;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.Reminder;
import com.arcadia.wearapp.realm_objects.RepeatRule;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    LinearLayout remindersLayout;
    private Calendar endDate = Calendar.getInstance();
    private Calendar repeatDate = Calendar.getInstance();
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
    private TextView remindersLabel;
    private int spinnerWight;
    private int repeatSpinnerWight;
    private TextView endDateTV;
    private TextView startTimeTV;
    private TextView repeatDateTextView;
    private TextView repeatTimeTextView;
    private ImageButton addReminderButton;
    private EditText nameEditText;
    private TextView endTimeTV;
    private boolean userIsInteracting = false;
    TimePickerDialog.OnTimeSetListener endTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialPickerLayout radialPickerLayout, int hourOfDay, int minute) {
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
            if (startDateTV.getText().toString().isEmpty())
                startDateTV.setText(dateFormat.format(startDate.getTime()));
            if (endDateTV.getText().toString().isEmpty() || endTimeTV.getText().toString().isEmpty()) {
                endDate.setTime(startDate.getTime());
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(DescriptionActivity.this);
                if (preferences.getBoolean("timePeriod", false)) {
                    endDate.add(Calendar.HOUR, 1);
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
            endDate.setTime(startDate.getTime());
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
    private TimePickerDialog.OnTimeSetListener repeatTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
            if (repeatDate == null) {
                repeatDate = Calendar.getInstance();
                repeatDate.setTime(startDate.getTime());
            }

            repeatDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
            repeatDate.set(Calendar.MINUTE, minute);
            repeatDate.set(Calendar.SECOND, 0);
            repeatDate.set(Calendar.MILLISECOND, 0);

            repeatTimeTextView.setText(timeFormat.format(repeatDate.getTime()));
            if (repeatDateTextView.getText().toString().isEmpty())
                repeatDateTextView.setText(dateFormat.format(repeatDate.getTime()));
            setChanged(true);
        }
    };
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
            repeatDate.set(Calendar.SECOND, 0);
            repeatDate.set(Calendar.MILLISECOND, 0);

            repeatDateTextView.setText(dateFormat.format(repeatDate.getTime()));
            if (repeatTimeTextView.getText().toString().isEmpty())
                repeatTimeTextView.setText(timeFormat.format(repeatDate.getTime()));
            setChanged(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);
        getIntent().getFlags();

        reminderViews = new ArrayList<>();

        nameEditText = (EditText) findViewById(R.id.name_text);
        clearNameButton = (ImageButton) findViewById(R.id.clear_name_text_button);
        clearStartDateButton = (ImageButton) findViewById(R.id.clear_start_date_button);
        clearEndDateButton = (ImageButton) findViewById(R.id.clear_end_date_button);
        addReminderButton = (ImageButton) findViewById(R.id.add_remind_button);

        startDateTV = (TextView) findViewById(R.id.start_date);
        startTimeTV = (TextView) findViewById(R.id.start_time);

        endDateTV = (TextView) findViewById(R.id.end_date);
        endTimeTV = (TextView) findViewById(R.id.end_time);
        descriptionEditText = (EditText) findViewById(R.id.description_text);

        remindersLabel = (TextView) findViewById(R.id.reminders_text_label);

        repeatSpinner = (Spinner) findViewById(R.id.repeat_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.repeat_rules_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(spinnerAdapter);
        spinnerWight = repeatSpinner.getDropDownWidth();
        if (!editMode) {
            repeatSpinner.setDropDownWidth(0);
        }

        repeatUntilLayout = (LinearLayout) findViewById(R.id.repeat_until_layout);
        repeatDateLayout = (LinearLayout) findViewById(R.id.repeat_until_date_layout);

        repeatUntilSpinner = (Spinner) findViewById(R.id.repeat_until_spinner);
        ArrayAdapter<CharSequence> untilSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.repeat_until_array, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatUntilSpinner.setAdapter(untilSpinnerAdapter);
        repeatUntilSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setChanged(true);
                if (position == 1) {
                    repeatDateLayout.setVisibility(View.VISIBLE);
                } else {
                    repeatDateLayout.setVisibility(View.GONE);
                    repeatDate = null;
                    repeatDateTextView.setText("");
                    repeatTimeTextView.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        repeatSpinnerWight = repeatUntilSpinner.getDropDownWidth();
        if (!editMode) {
            repeatUntilSpinner.setDropDownWidth(0);
        }

        repeatDateTextView = (TextView) findViewById(R.id.repeat_until_date);
        repeatTimeTextView = (TextView) findViewById(R.id.repeat_until_time);

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
                endDate.setTime(event.getEndDate());
                if (startDate != null) {
                    startDateTV.setText(dateFormat.format(startDate.getTime()));
                    startTimeTV.setText(timeFormat.format(startDate.getTime()));
                    if (endDate != null && endDate.after(startDate)) {
                        endDateTV.setText(dateFormat.format(endDate.getTime()));
                        endTimeTV.setText(timeFormat.format(endDate.getTime()));
                    }
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
                    if (repeatRule.getEndRepeatDate().before(new Date((long) getResources().getInteger(R.integer.utc_date_max_value) * 1000))) {
                        repeatUntilSpinner.setSelection(1);

                        repeatDate = Calendar.getInstance();
                        repeatDate.setTime(repeatRule.getEndRepeatDate());

                        repeatDateTextView.setText(dateFormat.format(repeatDate.getTime()));
                        repeatTimeTextView.setText(timeFormat.format(repeatDate.getTime()));

                        repeatDateLayout.setVisibility(View.VISIBLE);
                    } else
                        repeatUntilSpinner.setSelection(0);
                }
                RealmResults<Reminder> reminders = realm.where(Reminder.class).equalTo("eventID", event.getEventID()).findAll();
                if (!reminders.isEmpty()) {
                    for (Reminder reminder : reminders) {
                        addRemindView(reminder);
                    }
                } else
                    remindersLabel.setVisibility(View.GONE);
            }
        } else {
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
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_description, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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

        nameEditText.setFocusableInTouchMode(true);
        nameEditText.setFocusable(true);
        nameEditText.setCursorVisible(true);
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

        startDateTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(startDate, datepicker_type_start);
            }
        });
        startTimeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = startTimeTV.getText().toString();
                Calendar calendar = Calendar.getInstance();
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

        endDateTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(endDate, datepicker_type_end);
            }
        });
        endTimeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();

                calendar.setTime(endDate.getTime());

                showTimePicker(calendar, datepicker_type_end);
            }
        });
        clearEndDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endDateTV.setText("");
                endTimeTV.setText("");
                if (startDate == null)
                    startDate = Calendar.getInstance();
                endDate.setTime(startDate.getTime());
                endDate.set(Calendar.SECOND, 0);
                clearEndDateButton.setVisibility(View.GONE);
                setChanged(true);
            }
        });
        if (!endDateTV.getText().toString().isEmpty())
            clearEndDateButton.setVisibility(View.VISIBLE);

        descriptionEditText.setFocusableInTouchMode(true);
        descriptionEditText.setFocusable(true);
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
            }
        });

        repeatSpinner.setFocusable(true);
        repeatSpinner.setDropDownWidth(spinnerWight);
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

        repeatUntilSpinner.setFocusable(true);
        repeatUntilSpinner.setDropDownWidth(repeatSpinnerWight);

        repeatDateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repeatDate != null)
                    showDatePicker(repeatDate, datepicker_type_repeat);
                else
                    showDatePicker(startDate, datepicker_type_repeat);
            }
        });
        repeatTimeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (repeatDate != null)
                    showTimePicker(repeatDate, datepicker_type_repeat);
                else
                    showTimePicker(startDate, datepicker_type_repeat);
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
        switch (type) {
            case 1:
                repeatTimeMillis = AlarmManager.INTERVAL_DAY;
                break;
            case 2:
                repeatTimeMillis = AlarmManager.INTERVAL_DAY * 7;
                break;
            case 3:
                int currentMonth = startDate.get(Calendar.MONTH);
                if (currentMonth == Calendar.JANUARY || currentMonth == Calendar.MARCH || currentMonth == Calendar.MAY || currentMonth == Calendar.JULY
                        || currentMonth == Calendar.AUGUST || currentMonth == Calendar.OCTOBER || currentMonth == Calendar.DECEMBER) {
                    repeatTimeMillis = AlarmManager.INTERVAL_DAY * 31;
                } else if (currentMonth == Calendar.APRIL || currentMonth == Calendar.JUNE || currentMonth == Calendar.SEPTEMBER
                        || currentMonth == Calendar.NOVEMBER) {
                    repeatTimeMillis = AlarmManager.INTERVAL_DAY * 30;
                } else if (currentMonth == Calendar.FEBRUARY) {
                    if (startDate.get(Calendar.YEAR) % 4 == 0) {
                        repeatTimeMillis = AlarmManager.INTERVAL_DAY * 29;
                    } else {
                        repeatTimeMillis = AlarmManager.INTERVAL_DAY * 28;
                    }
                }
                break;
            case 4:
                if (startDate.get(Calendar.YEAR) % 4 == 0) {
                    repeatTimeMillis = AlarmManager.INTERVAL_DAY * 366;
                } else {
                    repeatTimeMillis = AlarmManager.INTERVAL_DAY * 365;
                }
                break;
            default:
                repeatUntilLayout.setVisibility(View.GONE);
                repeatTimeMillis = 0;
                break;
        }
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

            if (!descriptionEditText.getText().toString().isEmpty()) {
                realm.beginTransaction();
                event.setDescription(descriptionEditText.getText().toString());
                realm.commitTransaction();
            } else {
                realm.beginTransaction();
                event.setDescription("");
                realm.commitTransaction();
            }
            if (startDate != null) {
                if (!isLocally)
                    startDate.setTimeZone(TimeZone.getTimeZone(timezone));
                realm.beginTransaction();
                event.setStartDate(startDate.getTime());
                realm.commitTransaction();
                if (endDate == null) {
                    realm.beginTransaction();
                    event.setEndDate(startDate.getTime());
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
                eventID = (int) (realm.where(Event.class).maximumInt("eventID") + 1);

                realm.beginTransaction();
                // insert new value
                event.setEventID(eventID);
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
                if (repeatDate == null) {
                    repeatRule.setEndRepeatDate(new Date((long) getResources().getInteger(R.integer.utc_date_max_value) * 1000));
                } else
                    repeatRule.setEndRepeatDate(repeatDate.getTime());
                if (repeatRule.getRuleID() == 0) {
                    int newRuleID;
                    RealmQuery<RepeatRule> query = realm.where(RepeatRule.class);
                    if (query.count() > 0)
                        newRuleID = (int) (query.maximumInt("ruleID") + 1);
                    else
                        newRuleID = 1;
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
//            RealmResults<Reminder> reminders = realm.where(Reminder.class).equalTo("eventID", event.getEventID()).findAll();
//            reminders.clear();
            for (ReminderView view : reminderViews) {

                Reminder reminder = view.getReminder();
                if (realm.where(Reminder.class).equalTo("reminderID", reminder.getReminderID()).findFirst() == null) {
                    // increment index
                    int nextID = (int) (realm.where(Reminder.class).maximumInt("reminderID") + 1);
                    // insert new value

                    realm.beginTransaction();
                    reminder.setReminderID(nextID);
                    realm.commitTransaction();
                }
                int offset = (int) (view.getDate().getTimeInMillis() - startDate.getTimeInMillis()) / 1000;
                realm.beginTransaction();
                reminder.setAlertOffset(offset);
                reminder.setEventID(event.getEventID());
                realm.copyToRealmOrUpdate(reminder);
                realm.commitTransaction();
                Calendar c = Calendar.getInstance();
                c.setTime(view.getDate().getTime());
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                if (!isLocally)
                    c.setTimeZone(TimeZone.getTimeZone(timezone));

                Intent intent = new Intent();
                intent.setAction("com.arcadia.wearapp.broadcast");
                intent.putExtra("reminderId", reminder.getReminderID());

                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, reminder.getReminderID(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (repeatTimeMillis != 0)
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), repeatTimeMillis, pendingIntent);
                else
                    alarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
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
            Toast.makeText(this, "Name value cannot be empty!", Toast.LENGTH_SHORT).show();
            return false;
        } else if (startDateTV.getText().toString().isEmpty()) {
            Toast.makeText(this, "Start Date cannot be empty!", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!endDateTV.getText().toString().isEmpty()) {
            if (endDate.before(startDate)) {
                Toast.makeText(this, "End date cannot be before start date!", Toast.LENGTH_SHORT).show();
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
        this.timeFormat = (SimpleDateFormat) DateFormat.getTimeFormat(this);
        this.dateFormat = new SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "E MMM dd"), Locale.getDefault());
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
                .setTitle("Save and exit")
                .setMessage("Do you want save changes?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (invalidateData())
                            saveAndExit();
                        NavUtils.navigateUpFromSameTask(DescriptionActivity.this);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setResult(RESULT_CANCELED);
                Intent intent = NavUtils.getParentActivityIntent(DescriptionActivity.this);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(DescriptionActivity.this, intent);
            }
        }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
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
                .setTitle("Remove event")
                .setMessage("Are you sure want to remove this event?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (invalidateData())
                            removeAndExit();
                        NavUtils.navigateUpFromSameTask(DescriptionActivity.this);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void showTimePicker(Calendar calendar, int type) {
        if (calendar == null)
            calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        TimePickerDialog tpd = null;
        boolean is24hFormat = false;
        if (DateFormat.is24HourFormat(this))
            is24hFormat = true;
        switch (type) {
            case datepicker_type_start:
                tpd = TimePickerDialog.newInstance(startTimeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24hFormat);
                break;
            case datepicker_type_end:
                tpd = TimePickerDialog.newInstance(endTimeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24hFormat);
                break;
            case datepicker_type_repeat:
                tpd = TimePickerDialog.newInstance(repeatTimeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24hFormat);
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