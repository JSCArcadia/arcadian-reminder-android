package com.arcadia.wearapp.adapters;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.RepeatRule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private static final int TYPE_HEADER = 1;
    private static final int TYPE_ITEM = 0;
    private static final int maxRepeatsCount = 100;
    private Context context;
    private String filter = "";
    private SimpleDateFormat shortDateFormat;
    private SimpleDateFormat longDateFormat;
    private String groupID;
    private View.OnClickListener onClickListener;
    private View.OnLongClickListener onLongClickListener;
    private SparseArray<Section> mSections = new SparseArray<>();
    private List<Event> events = new ArrayList<>();
    private List<Event> allDataSet = new ArrayList<>();
    private Comparator<Event> comparator = new Comparator<Event>() {
        @Override
        public int compare(Event lEvent, Event rEvent) {
            Date event1 = lEvent.getStartDate();
            Date event2 = rEvent.getStartDate();
            if (event1.after(event2))
                return 1;
            else
                return event1.equals(event2) ? 0 : -1;
        }
    };

    public RecyclerViewAdapter(Context context) {
        this.context = context;
        update();
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_list_item, parent, false);
            if (onClickListener != null) {
                v.setOnClickListener(onClickListener);
                if (onLongClickListener != null)
                    v.setOnLongClickListener(onLongClickListener);
            }
            return new ItemViewHolder(v);
        } else if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.events_list_hearder, parent, false);
            return new HeaderViewHolder(v);
        }
        throw new RuntimeException("there is no type that matches the type " + viewType + " + make sure your using types correctly");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {

//            RealmResults<Event> events = getEvents(realm);
            Event event = events.get(sectionedPositionToPosition(position));

            ItemViewHolder viewHolder = (ItemViewHolder) holder;
//            Realm realm = Realm.getInstance(context);
            viewHolder.nameTV.setText(event.getTitle());
            if (event.getStartDate() != null) {
                viewHolder.dateTV.setText(longDateFormat.format(event.getStartDate()));
                if (event.getEndDate() != null && event.getEndDate().after(event.getStartDate()))
                    viewHolder.dateTV.append(String.format(" - %s", longDateFormat.format(event.getEndDate())));
            }
//            realm.close();
            //cast holder to VHItem and set data
        } else if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.headerTV.setText(mSections.get(position).title);
            //cast holder to HeaderViewHolder and set data for header.
        }
    }

    public void addItem(Event item) {
        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(item);
        realm.commitTransaction();
        realm.close();
        update();
    }

    public void deleteItem(int index) {
        Realm realm = Realm.getInstance(context);
        this.events = getEvents();
        realm.beginTransaction();
        events.remove(index);
        realm.commitTransaction();
        realm.close();
        update();
    }

    public Event getItem(int position) {
//        Realm realm = Realm.getInstance(context);
        Event event = getEvents().get(sectionedPositionToPosition(position));
//        realm.close();
        return event;
    }

    @Override
    public int getItemCount() {
//        Realm realm = Realm.getInstance(context);
        int count = events.size();//getEvents(realm).size();
//        realm.close();
        if (count > 0) {
            return count + mSections.size();
        } else
            return 0;
    }

    @Override
    public long getItemId(int position) {
        return isSectionHeaderPosition(position)
                ? Integer.MAX_VALUE - mSections.indexOfKey(position)
                : super.getItemId(sectionedPositionToPosition(position));
    }

    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (constraint == null || constraint.length() == 0) {
                    filter = "";
                } else {
                    filter = constraint.toString();
                }
                return null;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                update();
            }
        };
    }

    @Override
    public int getItemViewType(int position) {
        return isSectionHeaderPosition(position) ? TYPE_HEADER : TYPE_ITEM;
    }

    public int positionToSectionedPosition(int position) {
        int offset = 0;
        for (int i = 0; i < mSections.size(); i++) {
            if (mSections.valueAt(i).firstPosition > position) {
                break;
            }
            ++offset;
        }
        return position + offset;
    }

    public int sectionedPositionToPosition(int sectionedPosition) {
        if (isSectionHeaderPosition(sectionedPosition)) {
            return RecyclerView.NO_POSITION;
        }

        int offset = 0;
        for (int i = 0; i < mSections.size(); i++) {
            if (mSections.valueAt(i).sectionedPosition > sectionedPosition) {
                break;
            }
            --offset;
        }
        return sectionedPosition + offset;
    }

    public boolean isSectionHeaderPosition(int position) {
        return mSections.get(position) != null;
    }

    public void setSections(Section[] sections) {
        mSections.clear();

        Arrays.sort(sections, new Comparator<Section>() {
            @Override
            public int compare(Section o, Section o1) {
                return (o.firstPosition == o1.firstPosition)
                        ? 0
                        : ((o.firstPosition < o1.firstPosition) ? -1 : 1);
            }
        });

        int offset = 0; // offset positions for the headers we're adding
        for (Section section : sections) {
            section.sectionedPosition = section.firstPosition + offset;
            mSections.append(section.sectionedPosition, section);
            ++offset;
        }
        notifyDataSetChanged();
    }

//    public void addSection(int eventID) {
//        Realm realm = Realm.getInstance(context);
//        this.events = getEvents();
//        //events.sort("startDate");
//        Event event = realm.where(Event.class).equalTo("eventID", eventID).findFirst();
//        if (event != null && events.size() > 0) {
//            int position = events.lastIndexOf(event);
//            String title = shortDateFormat.format(event.getStartDate());
//            realm.close();
//
//            Section section = new Section(position, title);
//            Section[] tempList = new Section[mSections.size() + 1];
//
//            for (int i = 0; i < mSections.size(); i++) {
//                tempList[i] = mSections.valueAt(i);
//            }
//            tempList[tempList.length - 1] = section;
//
//            setSections(tempList);
//        }
//    }

    public void update() {
        this.shortDateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String longDatePattern = String.format("%s, %s", android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "E MMM dd"), android.text.format.DateFormat.is24HourFormat(context) ? "H:mm" : "h:mm a");
            this.longDateFormat = new SimpleDateFormat(longDatePattern, Locale.getDefault());
        } else {
            this.longDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        }
//        Realm realm = Realm.getInstance(context);
        allDataSet = getAllDataSet();
        this.events = getEvents();

        List<Section> tempSections = new ArrayList<>();

        for (int i = 0; i < events.size(); i++) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(events.get(i).getStartDate());
            boolean have = false;
            for (int j = 0; j < tempSections.size(); j++) {
                if (tempSections.get(j).title.equals(shortDateFormat.format(calendar.getTime()))) {
                    have = true;
                    break;
                }
            }
            if (!have)
                tempSections.add(new Section(i, shortDateFormat.format(calendar.getTime())));
        }
//        realm.close();
        Section[] dummy = new Section[tempSections.size()];
        setSections(tempSections.toArray(dummy));
    }

    private List<Event> getAllDataSet() {
        List<Event> dataSet = new ArrayList<>();
        Realm realm = Realm.getInstance(context);

//        dataSet.addAll(realm.allObjects(Event.class));
        for (Event event : realm.allObjectsSorted(Event.class, "startDate", true)) {
            dataSet.add(new Event(event.getEventID(), event.getTitle(), event.getStartDate(), event.getEndDate(), event.getDescription(), event.getGroupID()));
            RepeatRule rule = realm.where(RepeatRule.class).equalTo("eventID", event.getEventID()).findFirst();
            if (rule != null) {
                if (rule.getRepeatPeriod() != 0) {
                    Date nextStartDate = event.getStartDate();
                    Date nextEndDate = event.getEndDate();
                    Date maxRepeatDate = new Date();
                    long repeats = 0;
                    if (rule.getEndRepeatDate() == null || realm.where(Event.class).maximumDate("startDate").before(rule.getEndRepeatDate())) {
                        maxRepeatDate.setTime(realm.where(Event.class).maximumDate("startDate").getTime());
                        repeats++;
                    } else {
                        maxRepeatDate.setTime(rule.getEndRepeatDate().getTime());
                    }
                    repeats += ((maxRepeatDate.getTime() - event.getStartDate().getTime()) / rule.getRepeatPeriod());
                    if (repeats > maxRepeatsCount) repeats = maxRepeatsCount;
                    for (int i = 0; i < repeats; i++) {
                        nextStartDate = new Date(nextStartDate.getTime() + rule.getRepeatPeriod());
                        nextEndDate = nextEndDate == null ? null : new Date(nextEndDate.getTime() + rule.getRepeatPeriod());
                        dataSet.add(new Event(event.getEventID(), event.getTitle(), nextStartDate, nextEndDate, event.getDescription(), event.getGroupID()));
                    }
                }
            }
        }
        realm.close();
        return dataSet;
    }

//    private int sectionsBefore(int position) {
//        int count = 0;
//        if (position < getItemCount())
//            for (int i = 0; i < position; i++) {
//                if (isSectionHeaderPosition(i))
//                    count++;
//            }
//        return count > 0 ? count : 0;
//    }

    private List<Event> getEvents() {
//        RealmResults<Event> results = realm.where(Event.class).findAll();
//        RealmQuery<Event> query = realm.where(Event.class);
//
//        query.beginGroup();
//        query.contains("title", filter, false);
//        for (Event event : results) {
//            if (event.getTitle().toLowerCase().contains(filter.toLowerCase())) {
//                query.or().equalTo("eventID", event.getEventID());
//            }
//        }
//        query.endGroup();
//
//        if (groupID != null)
//            query = query.equalTo("groupID", groupID);
//        return query.findAllSorted("startDate");
        this.events = new ArrayList<>();
//        Realm realm = Realm.getInstance(context);
        for (Event event : allDataSet) {
            if (event.getTitle().toLowerCase().contains(filter.toLowerCase()))
                if (groupID == null || groupID.equals(event.getGroupID()))
                    events.add(event);
        }
        if (!events.isEmpty())
            Collections.sort(events, comparator);
//        realm.close();
        return events;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
        update();
    }

    public static class Section {
        int firstPosition;
        int sectionedPosition;
        CharSequence title;

        public Section(int firstPosition, CharSequence title) {

            this.firstPosition = firstPosition;
            this.title = title;
        }

        public CharSequence getTitle() {
            return title;
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTV;
        private TextView dateTV;

        public ItemViewHolder(View itemView) {
            super(itemView);
            this.nameTV = (TextView) itemView.findViewById(R.id.recyclerview_item_primary_text);
            this.dateTV = (TextView) itemView.findViewById(R.id.recyclerview_item_secondary_text);
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView headerTV;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            this.headerTV = (TextView) itemView.findViewById(R.id.recyclerview_header_text);
        }
    }
}
