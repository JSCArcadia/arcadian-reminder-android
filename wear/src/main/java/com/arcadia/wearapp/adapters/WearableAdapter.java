package com.arcadia.wearapp.adapters;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.arcadia.wearapp.Event;
import com.arcadia.wearapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WearableAdapter extends WearableListView.Adapter {
    private final LayoutInflater mInflater;
    private final SimpleDateFormat dateFormat;
    private List<Event> dataSet;

    private Comparator<Event> comparator = new Comparator<Event>() {
        @Override
        public int compare(Event lEvent, Event rEvent) {
            Date event1 = lEvent.getStartDate();
            Date event2 = rEvent.getStartDate();
            if (event1.after(event2))
                return 1;
            else
                return 0;
        }
    };

    // Provide a suitable constructor (depends on the kind of dataset)
    public WearableAdapter(Context context, List<Event> dataset) {
        this.mInflater = LayoutInflater.from(context);
        this.dataSet = new ArrayList<>(dataset);

        String longDatePattern = String.format("%s, %s", android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMM dd"), android.text.format.DateFormat.is24HourFormat(context) ? "H:mm" : "h:mm a");
        this.dateFormat = new SimpleDateFormat(longDatePattern, Locale.getDefault());
    }

    // Create new views for list items
    // (invoked by the WearableListView's layout manager)
    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        // Inflate our custom layout for list items
        return new EventViewHolder(mInflater.inflate(R.layout.wear_events_list_item, null));
    }

    // Replace the contents of a list item
    // Instead of creating new views, the list tries to recycle existing ones
    // (invoked by the WearableListView's layout manager)
    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        // retrieve the text view
        Event event = dataSet.get(position);
        EventViewHolder itemHolder = (EventViewHolder) holder;
        itemHolder.titleTextView.setText(event.getTitle());
        if (event.getStartDate() != null) {
            itemHolder.secondaryTextView.setText(dateFormat.format(event.getStartDate()));
            if (event.getEndDate() != null && event.getEndDate().after(event.getStartDate()))
                itemHolder.secondaryTextView.append(String.format(" - %s", dateFormat.format(event.getEndDate())));
        }
        // replace list item's metadata
        holder.itemView.setTag(position);
    }

    // Return the size of your dataset
    // (invoked by the WearableListView's layout manager)
    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public void setList(ArrayList<Event> list) {
        this.dataSet.clear();
        this.dataSet.addAll(list);
        Collections.sort(dataSet, comparator);
        notifyDataSetChanged();
    }

    public Event get(int position) {
        return dataSet.get(position);
    }

    // Provide a reference to the type of views you're using
    public static class EventViewHolder extends WearableListView.ViewHolder {
        private TextView titleTextView;
        private TextView secondaryTextView;

        public EventViewHolder(View itemView) {
            super(itemView);
            // find the text view within the custom item's layout
            titleTextView = (TextView) itemView.findViewById(R.id.title_text);
            secondaryTextView = (TextView) itemView.findViewById(R.id.secondary_text);
        }
    }
}