package com.arcadia.wearapp.adapters;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.arcadia.wearapp.R;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.views.TimeLineView;

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
import java.util.Map;

import io.realm.Realm;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private static final int TYPE_HEADER = 1;
    private static final int TYPE_ITEM = 0;
    private Context context;
    private String filter = "";
    private SimpleDateFormat longDateFormat;
    private String groupID;
    private View.OnClickListener onClickListener;
    private View.OnLongClickListener onLongClickListener;
    private SparseArray<Section> mSections = new SparseArray<>();
    private List<Event> events = new ArrayList<>();
    private List<Event> dataSet = new ArrayList<>();
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
        this.groupID = context.getString(R.string.all_groups_id);
    }


    public void setGroupID(String groupID) {
        this.groupID = groupID;
        update();
    }

    public String getGroupID(){
        return this.groupID;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public void setDataSet(List<Event> dataSet) {
        this.dataSet = dataSet;
        update();
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
        throw new RuntimeException(String.format("There is no type that matches the type %d. Make sure your using types correctly", viewType));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int[] colors = context.getResources().getIntArray(R.array.material_colors);

        if (holder instanceof ItemViewHolder) {
            Event event = events.get(sectionedPositionToPosition(position));
            ItemViewHolder viewHolder = (ItemViewHolder) holder;
            int positionInSection = 0;
            for (int i = position - 1; i > 0; i--) {
                if (isSectionHeaderPosition(i))
                    break;
                positionInSection++;
            }
            viewHolder.colorView.setBackgroundColor(colors[positionInSection % colors.length]);
            viewHolder.nameTV.setText(event.getTitle());
            if (event.getStartDate() != null) {
                Calendar startDate = Calendar.getInstance();
                startDate.setTime(event.getStartDate());
                if (startDate.get(Calendar.HOUR_OF_DAY) == 0 && startDate.get(Calendar.MINUTE) == 0) {
                    int previousEventPosition = 0;
                    for (int i = position; i >= 0; i--) {
                        if (!isSectionHeaderPosition(i) && events.get(sectionedPositionToPosition(i)).getEventID() == event.getEventID()) {
                            previousEventPosition = sectionedPositionToPosition(i);
                        }
                    }
                    viewHolder.dateTV.setText(longDateFormat.format(events.get(previousEventPosition).getStartDate()));
                } else {
                    viewHolder.dateTV.setText(longDateFormat.format(event.getStartDate()));
                }
                if (event.getEndDate() != null && event.getEndDate().after(event.getStartDate()))
                    viewHolder.dateTV.append(String.format(" - %s", longDateFormat.format(event.getEndDate())));
            }
            //cast holder to VHItem and set data
        } else if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
            headerViewHolder.headerTV.setText(mSections.get(position).title);

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);
            int width = metrics.widthPixels;
            int height = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, metrics));
            float dayInterval = 24 * 60;
            Map<Rect, Integer> rectangles = new ArrayMap<>();

            List<Event> eventsInSection = new ArrayList<>();
            int nextSectionPosition = position;

            for (int i = position + 1; i < positionToSectionedPosition(events.size()); i++) {
                if (isSectionHeaderPosition(i)) {
                    nextSectionPosition = i;
                    break;
                }
            }
            if (position == nextSectionPosition && nextSectionPosition <= position + 2) {
                eventsInSection.add(events.get(sectionedPositionToPosition(position + 1)));
            } else {
                int start = sectionedPositionToPosition(position + 1);
                int end = sectionedPositionToPosition(nextSectionPosition - 1) + 1;
                if (end == -1 || end > events.size())
                    end = events.size();
                eventsInSection = events.subList(start, end);
            }
            for (int i = 0; i < eventsInSection.size(); i++) {
                int hspace = 0;
                Event event = eventsInSection.get(i);
                int color = colors[i % colors.length];

                Calendar startDate = Calendar.getInstance();
                startDate.setTime(event.getStartDate());

                Calendar endDate = Calendar.getInstance();
                if (event.getEndDate() != null) {
                    endDate.setTime(event.getEndDate());
                } else {
                    endDate.setTime(startDate.getTime());
                    endDate.add(Calendar.DAY_OF_YEAR, 1);
                    endDate.set(Calendar.HOUR_OF_DAY, 0);
                    endDate.set(Calendar.MINUTE, 0);
                    endDate.set(Calendar.SECOND, 0);
                }
                if (i > 0 && eventsInSection.get(i - 1) != null) {
                    for (int j = 0; j < i; j++) {
                        Date previousEndDate = eventsInSection.get(j).getEndDate();
                        if (previousEndDate == null || startDate.getTime().before(previousEndDate)) {
                            hspace += height / (eventsInSection.size() + 1);
                        }
                    }
                }

                if (startDate.get(Calendar.YEAR) == endDate.get(Calendar.YEAR) && startDate.get(Calendar.DAY_OF_YEAR) == endDate.get(Calendar.DAY_OF_YEAR)) {
                    rectangles.put(new Rect((int) (width * (startDate.get(Calendar.HOUR_OF_DAY) * 60 + startDate.get(Calendar.MINUTE)) / dayInterval), hspace, (int) (width * (endDate.get(Calendar.HOUR_OF_DAY) * 60 + endDate.get(Calendar.MINUTE)) / dayInterval), height), color);
                } else {
                    rectangles.put(new Rect((int) ((startDate.get(Calendar.HOUR_OF_DAY) * 60 + startDate.get(Calendar.MINUTE)) / dayInterval * width), hspace, width, height), color);
                }
            }
            headerViewHolder.timeLineView.setRectangles(rectangles);
            headerViewHolder.timeLineView.invalidate();
        }
    }

    public void addItem(Event item) {
        dataSet.add(item);
        update();
    }

    public void deleteItem(int index) {
        Realm realm = Realm.getInstance(context);
        realm.beginTransaction();
        dataSet.remove(index);
        realm.commitTransaction();
        realm.close();
        update();
    }

    public Event getItem(int position) {
        return getEvents().get(sectionedPositionToPosition(position));
    }

    @Override
    public int getItemCount() {
        int count = events.size();
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

        // offset positions for the headers we're adding
        int offset = 0;
        for (Section section : sections) {
            section.sectionedPosition = section.firstPosition + offset;
            mSections.append(section.sectionedPosition, section);
            ++offset;
        }
        notifyDataSetChanged();
    }

    public void update() {
        SimpleDateFormat shortDateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String longDatePattern = String.format("%s, %s", android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "E MMM dd"), android.text.format.DateFormat.is24HourFormat(context) ? "H:mm" : "h:mm a");
            this.longDateFormat = new SimpleDateFormat(longDatePattern, Locale.getDefault());
        } else {
            this.longDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        }
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
            if (!have) {
                tempSections.add(new Section(i, shortDateFormat.format(calendar.getTime())));
            }
        }
        Section[] dummy = new Section[tempSections.size()];
        setSections(tempSections.toArray(dummy));
    }

    private List<Event> getEvents() {
        this.events = new ArrayList<>();
        for (Event event : dataSet) {
            if (event.getTitle().toLowerCase().contains(filter.toLowerCase()))
                if (context.getResources().getString(R.string.all_groups_id).equals(groupID)
                        || event.getGroupID() == null && groupID == null
                        || event.getGroupID() != null && event.getGroupID().equals(groupID)) {
                    events.add(event);
                }
        }
        if (!events.isEmpty())
            Collections.sort(events, comparator);
        return events;
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
        private View colorView;
        private TextView nameTV;
        private TextView dateTV;

        public ItemViewHolder(View itemView) {
            super(itemView);
            this.colorView = itemView.findViewById(R.id.recyclerview_item_color);
            this.nameTV = (TextView) itemView.findViewById(R.id.recyclerview_item_primary_text);
            this.dateTV = (TextView) itemView.findViewById(R.id.recyclerview_item_secondary_text);
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView headerTV;
        private TimeLineView timeLineView;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            this.headerTV = (TextView) itemView.findViewById(R.id.recyclerview_header_text);
            this.timeLineView = (TimeLineView) itemView.findViewById(R.id.recyclerview_header_timeline);
        }
    }
}
