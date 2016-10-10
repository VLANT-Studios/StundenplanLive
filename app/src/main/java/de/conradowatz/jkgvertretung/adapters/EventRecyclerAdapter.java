package de.conradowatz.jkgvertretung.adapters;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ms.square.android.expandabletextview.ExpandableTextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;

public class EventRecyclerAdapter extends RecyclerView.Adapter<EventRecyclerAdapter.ViewHolder> {

    private final static int VIEWTYPE_EVENT = 0;
    private final static int VIEWTYPE_DIVIDER = 1;
    private List<Event> eventList;
    private Fach fach;
    private boolean[] isDivider;
    private Callback callback;

    /**
     * @param fach Nur Events aus diesem Fach anzeugen (null falls alle)
     */
    public EventRecyclerAdapter(Fach fach, Callback callback) {

        this.callback = callback;
        this.fach = fach;

        sortData();

    }

    public void notifyEventsChanged(EventsChangedEvent event) {

        sortData();
        if (event.getType() == EventsChangedEvent.TYPE_CHANGED) {
            notifyDataSetChanged();
        } else if (event.getType() == EventsChangedEvent.TYPE_REMOVED) {
            if (event.isRemoveAbove()) notifyItemRemoved(event.getRecyclerIndex() - 1);
            notifyItemRemoved(event.getRecyclerIndex());
        }
    }

    public boolean isOnlyEventOfThatDay(int recyclerPosition) {

        return getItemViewType(recyclerPosition - 1) == VIEWTYPE_DIVIDER && (recyclerPosition + 1 == getItemCount() || getItemViewType(recyclerPosition + 1) == VIEWTYPE_DIVIDER);
    }

    private void sortData() {

        if (fach != null) {
            eventList = fach.getEvents();
        } else {
            eventList = new ArrayList<>();
            for (int i = 0; i < LocalData.getInstance().getFächer().size(); i++) {
                Fach f = LocalData.getInstance().getFächer().get(i);
                for (Event e : f.getEvents()) {
                    e.setFachName(f.getName());
                    e.setFachIndex(i);
                    eventList.add(e);
                }
            }
            for (Event e : LocalData.getInstance().getNoFachEvents()) {
                e.setFachIndex(-1);
                e.setFachName("");
                eventList.add(e);
            }
        }

        Collections.sort(eventList, new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return e1.getDatum().compareTo(e2.getDatum());
            }
        });

        //DividerArray erstellen
        int size = getItemCount();
        if (size > 0) {
            isDivider = new boolean[size];
            isDivider[0] = true;
            isDivider[1] = false;
            int dividerCount = 0;
            for (int i = 2; i < size; i++) {
                Calendar c1 = Calendar.getInstance();
                Calendar c2 = Calendar.getInstance();
                c1.setTime(eventList.get(i - dividerCount - 1).getDatum());
                c2.setTime(eventList.get(i - dividerCount - 2).getDatum());
                if (Utilities.compareDays(c1, c2) != 0) {
                    dividerCount++;
                    isDivider[i] = true;
                    isDivider[i + 1] = false;
                    i++;
                }
            }
        }
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;
        if (viewType == VIEWTYPE_EVENT)
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item, parent, false);
        else
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_daydivider_item, parent, false);

        return new ViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        int eventPos;

        if (position == 0 || position == 1) { //Erster Divider
            eventPos = 0;
        } else { //Alle andern durch zählen herausfinden
            int divCount = 0;
            for (int i = 2; i < position; i++) if (isDivider[i]) divCount++;
            eventPos = position - 1 - divCount;
        }

        final Event e = eventList.get(eventPos);

        if (!isDivider[position]) {

            Calendar cNow = Calendar.getInstance();
            Calendar cEvent = Calendar.getInstance();
            cEvent.setTime(e.getDatum());

            int compareNumber = Utilities.compareDays(cEvent, cNow);

            if (compareNumber > 0)
                holder.infoText.setText("in " + Math.abs(cNow.get(Calendar.DAY_OF_YEAR) - cEvent.get(Calendar.DAY_OF_YEAR)) + " Tag(en)");
            else if (compareNumber == 0) holder.infoText.setText("heute");
            else holder.infoText.setText("vergangen");

            holder.fachText.setText(e.getFachName());


            final int fachIndex = e.getFachIndex();
            holder.editButtonText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int eventIndex = (fachIndex > -1 ? LocalData.getInstance().getFächer().get(fachIndex).getEvents().indexOf(e) : LocalData.getInstance().getNoFachEvents().indexOf(e));
                    callback.onEditEvent(fachIndex, eventIndex);
                }
            });
            holder.deleteButtonText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int eventIndex = (fachIndex > -1 ? LocalData.getInstance().getFächer().get(fachIndex).getEvents().indexOf(e) : LocalData.getInstance().getNoFachEvents().indexOf(e));
                    callback.onDeleteEvent(fachIndex, eventIndex, holder.getAdapterPosition());
                }
            });

            holder.titleText.setText(e.getTitle());
            holder.descText.setText(e.getDescription());
        } else holder.datumText.setText(getDateString(e.getDatum()));

    }

    private String getDateString(Date datum) {

        Calendar c = Calendar.getInstance();
        c.setTime(datum); //Montag, 12. September 2016
        return c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.GERMAN) + ", " + c.get(Calendar.DAY_OF_MONTH) + ". " + c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMAN) + " " + c.get(Calendar.YEAR);
    }

    @Override
    public int getItemViewType(int position) {

        return isDivider[position] ? VIEWTYPE_DIVIDER : VIEWTYPE_EVENT;
    }

    @Override
    public int getItemCount() {

        if (eventList.size() == 0) return 0;
        if (eventList.size() == 1) return 2;
        int uniqueDays = 0;
        for (int i = 1; i < eventList.size(); i++) {
            Calendar c1 = Calendar.getInstance();
            Calendar c2 = Calendar.getInstance();
            c1.setTime(eventList.get(i - 1).getDatum());
            c2.setTime(eventList.get(i).getDatum());
            if (Utilities.compareDays(c1, c2) != 0) uniqueDays++;
        }

        return eventList.size() + uniqueDays + 1;
    }

    public interface Callback {

        void onEditEvent(int fachIndex, int eventIndex);

        void onDeleteEvent(int fachIndex, int eventIndex, int recyclerIndex);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView fachText;
        TextView datumText;
        View dividerView;
        TextView titleText;
        ExpandableTextView descText;
        TextView deleteButtonText;
        TextView editButtonText;
        TextView infoText;

        public ViewHolder(View itemView, int viewType) {

            super(itemView);

            if (viewType == VIEWTYPE_EVENT) {
                dividerView = itemView.findViewById(R.id.dividerView);
                infoText = (TextView) itemView.findViewById(R.id.infoText);
                fachText = (TextView) itemView.findViewById(R.id.fachText);
                titleText = (TextView) itemView.findViewById(R.id.titleText);
                descText = (ExpandableTextView) itemView.findViewById(R.id.descText);
                deleteButtonText = (TextView) itemView.findViewById(R.id.deleteButtonText);
                editButtonText = (TextView) itemView.findViewById(R.id.editButtonText);
            } else {
                datumText = (TextView) itemView.findViewById(R.id.datumText);
            }
        }
    }
}
