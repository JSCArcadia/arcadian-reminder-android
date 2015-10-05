package com.arcadia.wearapp.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.ParseGroup;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;


public class GroupsAdapter extends ArrayAdapter {

    private RealmResults<ParseGroup> realmList;
    private Context context;
    private ProgressDialog progressDialog;

    public GroupsAdapter(Context context, RealmResults<ParseGroup> realmList) {
        super(context, R.layout.grid_item, realmList);
        this.realmList = realmList;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ParseGroup group = realmList.get(position);
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.grid_item, null, false);
            holder.name = (TextView) convertView.findViewById(R.id.title);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.group_checkbox);
            holder.aboutIcon = (ImageView) convertView.findViewById(R.id.icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (holder != null && group != null) {
            holder.name.setText(group.getTitle());
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.checkBox.setChecked(!holder.checkBox.isChecked());
                }
            });

            holder.checkBox.setTag(group.getGroupID());
            SharedPreferences settings = context.getSharedPreferences("groups", 0);
            Boolean isChecked = settings.getBoolean(group.getGroupID(), false);
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(isChecked);

            holder.aboutIcon.setTag(R.string.group_title, group.getTitle());
            if (group.getDescription() != null)
                holder.aboutIcon.setTag(R.string.group_description, group.getDescription());
            holder.aboutIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String title = v.getTag(R.string.group_title).toString();
                    String description = v.getTag(R.string.group_description).toString();
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(title)
                            .setMessage("(No description)")
                            .setIcon(R.drawable.info)
                            .setCancelable(true)
                            .setNegativeButton("Close",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                    if (description != null)
                        builder.setMessage(description);
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    String groupID = buttonView.getTag().toString();

                    if (isChecked)
                        loadGroup(groupID);
                    else
                        removeGroup(groupID);

                    SharedPreferences.Editor editor = context.getSharedPreferences("groups", 0).edit();
                    editor.putBoolean(groupID, isChecked)
                            .apply();
                }
            });
        }
        return convertView;
    }

    private void showDialog() {
        progressDialog = new ProgressDialog(context, ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Loading");
        progressDialog.setMessage("Please wait");
        progressDialog.show();
    }

    private void loadGroup(final String groupID) {
        showDialog();
        try {
            ParseObject group = ParseQuery.getQuery("Group").get(groupID);
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Event").whereEqualTo("group", group);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> list, ParseException e) {
                    if (e == null) {
                        Realm realm = Realm.getInstance(context);
                        for (ParseObject object : list) {
                            Event event = new Event(object.get("title").toString());
                            // increment index
                            int nextID = (int) (realm.where(Event.class).maximumInt("eventID") + 1);

                            // insert new value
                            event.setEventID(nextID);

                            Date startDate = (Date) object.get("startDate");
                            event.setStartDate(startDate);

                            Date endDate = (Date) object.get("endDate");
                            if (endDate != null)
                                event.setEndDate(endDate);

                            Object description = object.get("description");
                            if (description != null)
                                event.setDescription(description.toString());

                            event.setGroupID(groupID);

                            realm.beginTransaction();
                            realm.copyToRealmOrUpdate(event);
                            realm.commitTransaction();
                        }
                        realm.close();
                    } else {
                        e.printStackTrace();
                    }
                    if (progressDialog != null)
                        progressDialog.dismiss();
                }
            });
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void removeGroup(String groupID) {
        showDialog();
        Realm realm = Realm.getInstance(context);
        RealmResults<Event> results = realm.where(Event.class).equalTo("groupID", groupID).findAll();

        realm.beginTransaction();
        results.clear();
        realm.commitTransaction();

        realm.close();

        if (progressDialog != null)
            progressDialog.dismiss();
    }

    public ParseGroup get(int position) {
        return realmList.get(position);
    }

    class ViewHolder {
        TextView name;
        CheckBox checkBox;
        ImageView aboutIcon;
    }
}