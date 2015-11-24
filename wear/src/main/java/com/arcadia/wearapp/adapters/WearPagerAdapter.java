package com.arcadia.wearapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.WearApplication;
import com.arcadia.wearapp.activities.WearDescriptionActivity;
import com.arcadia.wearapp.services.WearListenerService;

public class WearPagerAdapter extends GridPagerAdapter {

    private Context context;
    private boolean isConnected = false;

    public WearPagerAdapter(Context context) {
        super();
        this.context = context;
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public int getColumnCount(int i) {
        return 2;
    }

    @Override
    protected Object instantiateItem(ViewGroup viewGroup, int row, int col) {
        View view;
        if (col == 0) {
            setCurrentColumnForRow(0, 0);
            view = LayoutInflater.from(context).inflate(R.layout.wear_list_layout, viewGroup, false);
            WearableListView listView = (WearableListView) view.findViewById(R.id.wearable_list);
            TextView messageTextView = (TextView) view.findViewById(R.id.message_text_view);

            WearableAdapter adapter = new WearableAdapter(context, ((WearApplication) context.getApplicationContext()).events);
            // Assign an adapter to the list
            listView.setAdapter(adapter);
            listView.setGreedyTouchMode(true);
            // Set a click listener
            listView.setClickListener(new WearableListView.ClickListener() {
                @Override
                public void onClick(WearableListView.ViewHolder v) {
                    int position = v.getPosition();
                    Intent intent = new Intent(context, WearDescriptionActivity.class);
                    intent.putExtra(context.getString(R.string.intent_event_position_key), position);
                    context.startActivity(intent);
                }

                @Override
                public void onTopEmptyRegionClick() {
                }
            });

            if (isConnected) {
                if (((WearApplication) context.getApplicationContext()).events.isEmpty()) {
                    messageTextView.setVisibility(View.VISIBLE);
                    listView.setVisibility(View.GONE);
                    messageTextView.setText(context.getString(R.string.no_events_text));
                } else {
                    messageTextView.setVisibility(View.GONE);
                    adapter = new WearableAdapter(context, ((WearApplication) context.getApplicationContext()).events);
                    listView.setVisibility(View.VISIBLE);
                    listView.setAdapter(adapter);
                }
            } else {
                messageTextView.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
                messageTextView.setText(R.string.no_connection_text);

            }
        } else {
            setCurrentColumnForRow(0, 1);
            view = LayoutInflater.from(context).inflate(R.layout.wear_periods_layout, viewGroup, false);

            final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radio_group);
            radioGroup.clearCheck();

            int selectedNum = context.getSharedPreferences("periodPreferences", Context.MODE_PRIVATE).getInt("selectedPeriod", 0);
            ((RadioButton) radioGroup.getChildAt(selectedNum < radioGroup.getChildCount() ? selectedNum : 0)).setChecked(true);

            if (isConnected) {
                radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        RadioButton radioButton = (RadioButton) group.findViewById(checkedId);
                        int position = group.indexOfChild(radioButton) == -1 ? 0 : group.indexOfChild(radioButton);
                        context.getSharedPreferences("periodPreferences", Context.MODE_PRIVATE).edit().putInt("selectedPeriod", position).commit();

                        Intent intent = new Intent(context, WearListenerService.class);
                        intent.setAction(WearListenerService.Action_Request_List);
                        context.startService(intent);
                    }
                });
            } else
                radioGroup.setOnCheckedChangeListener(null);
        }
        viewGroup.addView(view);
        return view;
    }

    @Override
    protected void destroyItem(ViewGroup viewGroup, int i, int i1, Object o) {
        viewGroup.removeView((View) o);
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view.equals(o);
    }

    @Override
    public int getCurrentColumnForRow(int row, int currentColumn) {
        return super.getCurrentColumnForRow(row, currentColumn);
    }

    public void onCheckConnection(boolean isConnect) {
        this.isConnected = isConnect;
        notifyDataSetChanged();
    }
}
