package com.arcadia.wearapp.activities;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;

import com.arcadia.wearapp.BuildConfig;
import com.arcadia.wearapp.R;

public class SettingsActivity extends AppCompatActivity {

    public static String PARSE_VERSION = "1.9.2";
    public static String REALM_VERSION = "0.80.3";

    private ListPreference timezoneList;
    private SwitchPreference timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyPreferenceFragment preferenceFragment = new MyPreferenceFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, preferenceFragment).commit();

        timezoneList = (ListPreference) preferenceFragment.findFragmentPreference("timezoneList");
        timezoneList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setTimezoneListSummary(newValue.toString());
                return true;
            }
        });

        SwitchPreference locallyTimezone = (SwitchPreference) preferenceFragment.findFragmentPreference("locallyTimezone");
        setTimezoneChecked(locallyTimezone.isChecked());
        setTimezoneListSummary(timezoneList.getValue());

        locallyTimezone.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setTimezoneChecked((boolean) newValue);
                return true;
            }
        });

//        timeFormat = (SwitchPreference) preferenceFragment.findFragmentPreference("timeFormat");
//
//        timeFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                setTimeFormatChecked((boolean) newValue);
//                return true;
//            }
//        });
//        if (DateFormat.is24HourFormat(this)) {
//            timeFormat.setChecked(true);
//            setTimeFormatChecked(true);
//        }
        Preference realmVersionPreference = preferenceFragment.findFragmentPreference("realmVersion");
        realmVersionPreference.setSummary(REALM_VERSION);

        Preference parseVersionPreference = preferenceFragment.findFragmentPreference("parseVersion");
        parseVersionPreference.setSummary(PARSE_VERSION);

        Preference appVersionPreference = preferenceFragment.findFragmentPreference("appVersion");
        String versionName = BuildConfig.VERSION_NAME;
        appVersionPreference.setSummary(versionName);
    }

//    private void setTimeFormatChecked(boolean newValue) {
//        if (newValue)
//            timeFormat.setSummary("13:00");
//        else
//            timeFormat.setSummary("01:00 PM");
//    }

    private void setTimezoneListSummary(String value) {
        if (timezoneList != null && value != null) {
            timezoneList.setSummary(value);
        }
    }

    private void setTimezoneChecked(boolean value) {
        if (timezoneList != null)
            if (value)
                timezoneList.setEnabled(false);
            else
                timezoneList.setEnabled(true);
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        public Preference findFragmentPreference(CharSequence key) {
            getFragmentManager().executePendingTransactions();
            return findPreference(key);
        }
    }
}
