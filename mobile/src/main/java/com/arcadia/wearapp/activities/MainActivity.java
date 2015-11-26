package com.arcadia.wearapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.adapters.RecyclerViewAdapter;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.Reminder;
import com.arcadia.wearapp.realm_objects.RepeatRule;
import com.arcadia.wearapp.services.CalendarContentResolver;
import com.arcadia.wearapp.services.MobileListenerService;
import com.arcadia.wearapp.views.DividerItemDecoration;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.rockerhieu.rvadapter.endless.EndlessRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, DrawerLayout.DrawerListener, EndlessRecyclerViewAdapter.RequestToLoadMoreListener {

    private static final int ADD_EVENT_REQUEST_CODE = 2;
    private static final int EDIT_EVENT_REQUEST_CODE = 1;
    public static final int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 3;
    private RecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private ActionBar actionBar;
    private EndlessRecyclerViewAdapter endlessAdapter;
    private Calendar loadedDate = null;
    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;
    private View headView;
    private boolean hasNextEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_navigation);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Assume thisActivity is the current activity
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR)) {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, MY_PERMISSIONS_REQUEST_READ_CALENDAR);
            }
        }

        this.mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerListener(this);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        headView = navigationView.inflateHeaderView(R.layout.nav_drawer_header);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                menuItem.setChecked(true);
                endlessAdapter.onDataReady(true);
                Realm realm = Realm.getInstance(MainActivity.this);
                switch (menuItem.getItemId()) {
                    case R.id.nav_settings:
                        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        mDrawerLayout.closeDrawers();
                        startActivity(settingsIntent);
                        break;
                    case R.id.group_all:
                        adapter.setGroupID(getString(R.string.all_groups_id));
                        loadedDate = null;
                        endlessAdapter.onDataReady(true);
                        adapter.setDataSet(getNextEvents());
                        break;
                    case R.id.group_local:
                        adapter.setGroupID(null);
                        loadedDate = null;
                        endlessAdapter.onDataReady(true);
                        adapter.setDataSet(getNextEvents());
                        break;
                    case R.id.group_from_calendar:
                        adapter.setGroupID(getString(R.string.calendar_group_id));
                        loadedDate = null;
                        endlessAdapter.onDataReady(true);
                        adapter.setDataSet(getNextEvents());
                        break;
                    case R.id.import_from_calendar:
                        int importCount = 0;

                        CalendarContentResolver resolver = new CalendarContentResolver(MainActivity.this);
                        Set<Event> events = resolver.getCalendarEvents();
                        if (events == null) {
                            Toast.makeText(MainActivity.this, R.string.toast_calendar_permission_denied, Toast.LENGTH_LONG).show();
                        } else if (events.isEmpty()) {
                            Toast.makeText(MainActivity.this, R.string.toast_calendar_no_events, Toast.LENGTH_LONG).show();
                        } else {
                            for (Event event : events) {
                                event.setGroupID(getString(R.string.calendar_group_id));

                                if (realm.where(Event.class).equalTo("title", event.getTitle()).equalTo("startDate", event.getStartDate()).equalTo("endDate", event.getEndDate()).count() == 0) {

                                    Set<Reminder> reminders = resolver.getCalendarReminders(event.getEventID());
                                    // increment index
                                    long nextID = 1;
                                    if (realm.where(Event.class).max("eventID") != null)
                                        nextID += (long) realm.where(Event.class).max("eventID");

                                    // insert new value
                                    event.setEventID((int) nextID);

                                    realm.beginTransaction();
                                    realm.copyToRealmOrUpdate(event);
                                    realm.commitTransaction();

                                    for (Reminder reminder : reminders) {
                                        if (realm.where(Reminder.class).equalTo("eventID", reminder.getEventID()).equalTo("alertOffset", reminder.getAlertOffset()).count() == 0) {
                                            reminder.setEventID((int) nextID);
                                            if (realm.where(Reminder.class).equalTo("reminderID", reminder.getReminderID()).count() > 0) {
                                                long newReminderId = 1;
                                                if (realm.where(Reminder.class).max("reminderID") != null)
                                                    newReminderId += (long) realm.where(Reminder.class).max("reminderID");

                                                reminder.setReminderID((int) newReminderId);
                                            }
                                            realm.beginTransaction();
                                            realm.copyToRealmOrUpdate(reminder);
                                            realm.commitTransaction();
                                        }
                                    }
                                    importCount++;
                                }
                            }

                            Menu menu = navigationView.getMenu();
                            menu.findItem(R.id.group_from_calendar).setVisible(true);
                            invalidateOptionsMenu();

                            if (importCount > 0) {
                                Toast.makeText(MainActivity.this, String.format(getString(R.string.toast_calendar_added), importCount), Toast.LENGTH_LONG).show();
                                adapter.setDataSet(getNextEvents());
                            } else
                                Toast.makeText(MainActivity.this, R.string.toast_calendar_no_new_events, Toast.LENGTH_LONG).show();
                        }
                        break;
                }
                realm.close();
                mDrawerLayout.closeDrawers();
                return true;
            }
        });

        if (MobileListenerService.Action_Open_Event.equals(getIntent().getAction())) {
            if (getIntent().getExtras().containsKey(getString(R.string.intent_event_id_key))) {
                Intent openIntent = new Intent(MainActivity.this, DescriptionActivity.class);
                openIntent.putExtra(getString(R.string.intent_event_id_key), getIntent().getExtras().getInt(getString(R.string.intent_event_id_key)));
                startActivityForResult(openIntent, EDIT_EVENT_REQUEST_CODE);
            }
        }

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new RecyclerViewAdapter(this);
        adapter.setDataSet(getNextEvents());
        adapter.setOnClickListener(this);
        adapter.setOnLongClickListener(this);

        endlessAdapter = new EndlessRecyclerViewAdapter(this, adapter, this);
        recyclerView.setAdapter(endlessAdapter);

        FloatingActionButton button = (FloatingActionButton) findViewById(R.id.floating_add_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DescriptionActivity.class);
                startActivityForResult(intent, ADD_EVENT_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Menu navigationViewMenu = navigationView.getMenu();
        MenuInflater inflater = getMenuInflater();

        navigationViewMenu.clear();
        inflater.inflate(R.menu.drawer_view, navigationViewMenu);

        Realm realm = Realm.getInstance(this);
        if (realm.where(Event.class).equalTo("groupID", getString(R.string.calendar_group_id)).count() > 0) {
            MenuItem item = navigationViewMenu.findItem(R.id.group_from_calendar).setVisible(true);
            if (item != null)
                item.setVisible(true);
        }

        realm.close();
        if (adapter != null) {
            adapter.update();
        }
        return true;
    }

    private List<Event> getNextEvents() {
        List<Event> allEvents = new ArrayList<>();
        Realm realm = Realm.getInstance(this);
        for (int i = 0; i < 7; i++) {
            if (loadedDate == null) {
                loadedDate = Calendar.getInstance();
                if (realm.where(Event.class).minimumDate("startDate") != null)
                    loadedDate.setTime(realm.where(Event.class).minimumDate("startDate"));
                loadedDate.set(Calendar.HOUR, 0);
                loadedDate.set(Calendar.MINUTE, 0);
                loadedDate.set(Calendar.SECOND, 0);
                loadedDate.set(Calendar.MILLISECOND, 0);
            } else
                loadedDate.add(Calendar.DAY_OF_YEAR, 1);

            RealmResults<Event> results = realm.allObjectsSorted(Event.class, "startDate", true);
            if (results.isEmpty())
                return allEvents;
            for (Event event : results) {

                if (getString(R.string.all_groups_id).equals(adapter.getGroupID()) ||
                        (adapter.getGroupID() == null ? event.getGroupID() == null : adapter.getGroupID().equals(event.getGroupID()))) {

                    Calendar startDate = Calendar.getInstance();
                    startDate.setTime(event.getStartDate());

                    if (startDate.get(Calendar.DAY_OF_YEAR) == loadedDate.get(Calendar.DAY_OF_YEAR) && startDate.get(Calendar.YEAR) == loadedDate.get(Calendar.YEAR)) {
                        Calendar endDate = null;
                        if (event.getEndDate() != null) {
                            endDate = Calendar.getInstance();
                            endDate.setTime(event.getEndDate());
                        }

                        allEvents.add(new Event(event.getEventID(), event.getTitle(), startDate.getTime(), endDate == null ? null : endDate.getTime(), event.getDescription(), event.getGroupID()));
                        if (endDate != null && startDate.get(Calendar.DAY_OF_YEAR) == endDate.get(Calendar.DAY_OF_YEAR)) {
                            int days = endDate.get(Calendar.DAY_OF_YEAR) - startDate.get(Calendar.DAY_OF_YEAR);
                            startDate.set(Calendar.HOUR_OF_DAY, 0);
                            startDate.set(Calendar.MINUTE, 0);
                            startDate.set(Calendar.SECOND, 0);
                            for (int d = 0; d < days; d++) {
                                startDate.add(Calendar.DAY_OF_YEAR, 1);
                                allEvents.add(new Event(event.getEventID(), event.getTitle(), startDate.getTime(), endDate.getTime(), event.getDescription(), event.getGroupID()));
                            }
                        }
                    }
                }
            }

            hasNextEvents = realm.where(Event.class).maximumDate("startDate") != null && realm.where(Event.class).maximumDate("startDate").after(loadedDate.getTime());

            RealmResults<RepeatRule> repeatRules = realm.where(RepeatRule.class).findAll();
            for (RepeatRule rule : repeatRules) {
                Event event = realm.where(Event.class).equalTo("eventID", rule.getEventID()).findFirst();
                if ((rule.getEndRepeatDate() == null || !rule.getEndRepeatDate().before(loadedDate.getTime()) && rule.getRepeatPeriod() != 0) &&
                        (getString(R.string.all_groups_id).equals(adapter.getGroupID()) ||
                                (adapter.getGroupID() == null ? event.getGroupID() == null : adapter.getGroupID().equals(event.getGroupID())))) {
                    hasNextEvents = true;

                    Calendar repeatStartDate = Calendar.getInstance();
                    repeatStartDate.setTime(event.getStartDate());
                    Calendar repeatEndDate = null;
                    if (event.getEndDate() != null) {
                        repeatEndDate = Calendar.getInstance();
                        repeatEndDate.setTime(event.getEndDate());
                    }
                    long repeats = 1000;
                    if (rule.getEndRepeatDate() != null)
                        repeats = (rule.getEndRepeatDate().getTime() - event.getStartDate().getTime()) / rule.getRepeatPeriod();
                    for (int r = 0; r < repeats; r++) {
                        repeatStartDate.add(Calendar.MILLISECOND, (int) rule.getRepeatPeriod());
                        if (repeatEndDate != null)
                            repeatEndDate.add(Calendar.MILLISECOND, (int) rule.getRepeatPeriod());
                        if (repeatStartDate.get(Calendar.DAY_OF_YEAR) == loadedDate.get(Calendar.DAY_OF_YEAR) && repeatStartDate.get(Calendar.YEAR) == loadedDate.get(Calendar.YEAR)) {
                            allEvents.add(new Event(event.getEventID(), event.getTitle(), repeatStartDate.getTime(), repeatEndDate == null ? null : repeatEndDate.getTime(), event.getDescription(), event.getGroupID()));
                            break;
                        }
                        if (repeatStartDate.after(loadedDate))
                            break;
                    }
                }
            }
            if (!hasNextEvents) {
                break;
            }
        }
        realm.close();
        return allEvents;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout != null)
                    if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                    } else
                        mDrawerLayout.openDrawer(GravityCompat.START);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ADD_EVENT_REQUEST_CODE:
            case EDIT_EVENT_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    loadedDate = null;
                    endlessAdapter.onDataReady(true);
                    adapter.setDataSet(getNextEvents());
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int position = recyclerView.getChildLayoutPosition(v);
        Intent intent = new Intent(MainActivity.this, DescriptionActivity.class);

        Event event = adapter.getItem(position);

        intent.putExtra(getString(R.string.intent_event_id_key), event.getEventID());
        startActivityForResult(intent, EDIT_EVENT_REQUEST_CODE);
    }

    @Override
    public boolean onLongClick(View v) {
        int position = recyclerView.getChildLayoutPosition(v);
        Event event = adapter.getItem(position);
        if (event.getDescription() != null && !event.getDescription().isEmpty())
            Toast.makeText(MainActivity.this, event.getDescription(), Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        if (headView != null)
            if (actionBar != null) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_back);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_navigation);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {
    }

    @Override
    public void onLoadMoreRequested() {
        new AsyncTask<Void, Void, List<Event>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected List<Event> doInBackground(Void... params) {
                List<Event> events = new ArrayList<>();
                while (hasNextEvents && events.isEmpty()) {
                    events.addAll(getNextEvents());
                }
                return events;
            }

            @Override
            protected void onPostExecute(List<Event> list) {
                for (Event event : list) {
                    adapter.addItem(event);
                }
                endlessAdapter.onDataReady(hasNextEvents);
            }
        }.execute();
    }
}
