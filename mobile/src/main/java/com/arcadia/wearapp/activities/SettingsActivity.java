package com.arcadia.wearapp.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.arcadia.wearapp.BuildConfig;
import com.arcadia.wearapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class SettingsActivity extends AppCompatActivity {

    private ListPreference timezoneList;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyPreferenceFragment preferenceFragment = new MyPreferenceFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, preferenceFragment).commit();

        timezoneList = (ListPreference) preferenceFragment.findFragmentPreference("timezoneList");

        SwitchPreference locallyTimezone = (SwitchPreference) preferenceFragment.findFragmentPreference("locallyTimezone");
        setTimezoneChecked(locallyTimezone.isChecked());

        locallyTimezone.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setTimezoneChecked((boolean) newValue);
                return true;
            }
        });

        Preference appVersionPreference = preferenceFragment.findFragmentPreference("appVersion");
        String versionName = BuildConfig.VERSION_NAME;
        appVersionPreference.setSummary(versionName);

        Preference buildPreference = preferenceFragment.findFragmentPreference("build");
        int build = BuildConfig.VERSION_CODE;
        buildPreference.setSummary(String.valueOf(build));

        Preference websitePreference = preferenceFragment.findFragmentPreference("website");
        final String website = getString(R.string.website);
        websitePreference.setSummary(website);
        websitePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("http://%s/", website)));
                List activities = getPackageManager().queryIntentActivities(browseIntent, PackageManager.MATCH_DEFAULT_ONLY);
                if (!activities.isEmpty())
                    startActivity(browseIntent);
                else
                    Toast.makeText(SettingsActivity.this, getString(R.string.toast_settings_app_not_found), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        Preference emailPreference = preferenceFragment.findFragmentPreference("email");
        final String email = getString(R.string.email);
        emailPreference.setSummary(email);
        emailPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent emailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format("mailto:%s", email)));
                List activities = getPackageManager().queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY);
                if (!activities.isEmpty())
                    startActivity(emailIntent);
                else
                    Toast.makeText(SettingsActivity.this, getString(R.string.toast_settings_app_not_found), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
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
