package com.arcadia.wearapp.adapters;

import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.activities.DescriptionActivity;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.Reminder;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import io.realm.Realm;

public class ReminderView {
    public final static int remind_at_event_time = 0;
    public final static int remind_5_min_before = -300;
    public final static int remind_30_min_before = -1800;
    public final static int remind_1_hour_before = -3600;
    private DescriptionActivity context;
    private TextView timeTextView;
    private TextView dateTextView;
    private ImageButton deleteButton;
    private Spinner spinner;
    private LinearLayout customDateLayout;
    private Reminder reminder;
    private int spinnerWight;
    private boolean editMode = false;
    private LinearLayout reminderItem;
    View.OnClickListener deleteClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            context.removeReminder(ReminderView.this);
        }
    };
    private Calendar calendar;
    private DatePickerDialog.OnDateSetListener reminderDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePickerDialog datePickerDialog, int year, int monthOfYear, int dayOfMonth) {

            calendar.set(year, monthOfYear, dayOfMonth);
            calendar.set(Calendar.SECOND, 0);
            setDate(calendar.getTime());
            context.setChanged(true);
        }
    };
    private TimePickerDialog.OnTimeSetListener reminderTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialPickerLayout radialPickerLayout, int hourOfDay, int minute) {

            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            setDate(calendar.getTime());
            context.setChanged(true);
        }
    };

    public ReminderView(LinearLayout parent, Reminder reminder, final DescriptionActivity activity) {
        this.context = activity;
        this.reminder = reminder;
        this.editMode = activity.getEditMode();
        reminderItem = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.reminder_item, parent, false);
        calendar = Calendar.getInstance();
        if (activity.startDate != null)
            calendar.setTime(activity.startDate.getTime());

        if (reminder.getEventID() != 0) {
            Realm realm = Realm.getInstance(activity);
            Event event = realm.where(Event.class).equalTo("eventID", reminder.getEventID()).findFirst();
//            context.startDate.setTime(event.getStartDate());
            if (event != null) {
                calendar.setTime(event.getStartDate());
                calendar.add(Calendar.SECOND, reminder.getAlertOffset());
            }
        }
        if (reminderItem != null) {
            spinner = (Spinner) reminderItem.findViewById(R.id.reminder_spinner);
            customDateLayout = (LinearLayout) reminderItem.findViewById(R.id.reminder_custom_date_layout);
            dateTextView = (TextView) reminderItem.findViewById(R.id.reminder_custom_date);
            timeTextView = (TextView) reminderItem.findViewById(R.id.reminder_custom_time);
            deleteButton = (ImageButton) reminderItem.findViewById(R.id.delete_spinner_button);

            craftView();
            parent.addView(reminderItem);
        }
    }

    private void craftView() {
        ArrayAdapter<CharSequence> remindSpinnerAdapter = ArrayAdapter.createFromResource(context, R.array.reminder_types_array, android.R.layout.simple_spinner_item);
        remindSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(remindSpinnerAdapter);
        spinnerWight = spinner.getDropDownWidth();
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                context.setChanged(true);
                if (context.startDate != null)
                    calendar.setTime(context.startDate.getTime());
                switch (position) {
                    case 1:
                        calendar.add(Calendar.SECOND, remind_5_min_before);
                        break;
                    case 2:
                        calendar.add(Calendar.SECOND, remind_30_min_before);
                        break;
                    case 3:
                        calendar.add(Calendar.SECOND, remind_1_hour_before);
                        break;
                }
                // "Custom date" is the last item in the remind_types array
                if (position == (context.getResources().getStringArray(R.array.reminder_types_array).length - 1)) {
                    customDateLayout.setVisibility(View.VISIBLE);
                } else {
                    customDateLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        if (reminder != null) {
            int offset = reminder.getAlertOffset();
            switch (offset) {
                case remind_at_event_time:
                    spinner.setSelection(0);
                    break;
                case remind_5_min_before:
                    spinner.setSelection(1);
                    break;
                case remind_30_min_before:
                    spinner.setSelection(2);
                    break;
                case remind_1_hour_before:
                    spinner.setSelection(3);
                    break;
                default:
                    spinner.setSelection(4);
                    dateTextView.setText(context.dateFormat.format(calendar.getTime()));
                    timeTextView.setText(context.timeFormat.format(calendar.getTime()));
                    break;
            }
            setEditMode(editMode);
        }
    }

    private void showDatePicker() {
        calendar.set(Calendar.SECOND, 0);
        DatePickerDialog dpd = DatePickerDialog.newInstance(reminderDateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dpd.setCancelable(true);
        dpd.show(context.getFragmentManager(), context.getResources().getString(R.string.datepicker_tag));
    }

    protected void showTimePicker() {
        calendar.set(Calendar.SECOND, 0);
        boolean is24hFormat = false;
        if (DateFormat.is24HourFormat(context))
            is24hFormat = true;
        TimePickerDialog tpd = TimePickerDialog.newInstance(reminderTimeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24hFormat);
        tpd.setCancelable(true);
        tpd.show(context.getFragmentManager(), context.getResources().getString(R.string.timepicker_tag));
    }

    public Calendar getDate() {
        return calendar;
    }

    public void setDate(Date date) {
//        this.calendar.setTime(date);
        this.dateTextView.setText(context.dateFormat.format(date));
        this.timeTextView.setText(context.timeFormat.format(date));
    }

    public void setEditMode(boolean mode) {
        this.editMode = mode;
        if (editMode) {
            spinner.setDropDownWidth(spinnerWight);

            dateTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text = dateTextView.getText().toString();

                    if (text.isEmpty())
                        calendar.setTime(ReminderView.this.calendar.getTime());
                    else
                        try {
                            Calendar date = Calendar.getInstance();
                            date.setTime(context.dateFormat.parse(text));
                            calendar.set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    showDatePicker();
                }
            });
            timeTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text = timeTextView.getText().toString();

                    if (text.isEmpty())
                        calendar.setTime(ReminderView.this.calendar.getTime());
                    else
                        try {
                            Calendar time = Calendar.getInstance();
                            time.setTime(context.timeFormat.parse(text));
                            calendar.set(Calendar.HOUR, time.get(Calendar.HOUR));
                            calendar.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    showTimePicker();
                }
            });
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(deleteClickListener);
            spinner.setDropDownWidth(spinnerWight);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(deleteClickListener);
        } else {
            spinner.setDropDownWidth(0);
            deleteButton.setVisibility(View.GONE);
        }
    }

    public Reminder getReminder() {
        return this.reminder;
    }

    public View getView() {
        return reminderItem;
    }
}
