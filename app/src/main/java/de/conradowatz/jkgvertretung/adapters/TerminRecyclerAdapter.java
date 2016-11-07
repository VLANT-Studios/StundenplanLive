package de.conradowatz.jkgvertretung.adapters;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ms.square.android.expandabletextview.ExpandableTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Ferien;
import de.conradowatz.jkgvertretung.variables.Termin;

public class TerminRecyclerAdapter extends RecyclerView.Adapter<TerminRecyclerAdapter.ViewHolder> {

    public final static int MODE_TERMINE = 0;
    public final static int MODE_EVENTS = 1;
    public final static int MODE_FERIEN = 2;
    private final static int VIEWTYPE_EVENT = 0;
    private final static int VIEWTYPE_DIVIDER = 1;
    private final static int VIEWTYPE_FERIEN = 2;
    private int mode;
    private List<Termin> terminList;
    private Fach fach;
    private boolean[] isDivider;
    private Callback callback;

    public TerminRecyclerAdapter(int mode, Fach fach, Callback callback) {

        setHasStableIds(true);

        this.mode = mode;
        this.callback = callback;
        this.fach = fach;

        sortData();
    }

    public TerminRecyclerAdapter(int mode, Callback callback) {

        setHasStableIds(true);

        this.mode = mode;
        this.callback = callback;
        this.fach = null;

        sortData();
    }

    public void notifyTermineChanged() {

        sortData();
        notifyDataSetChanged();
    }

    private void sortData() {

        terminList = new ArrayList<>();

        if (mode == MODE_EVENTS) { //Fachansicht

            List<Event> eventList = fach.getEvents();
            int fachIndex = LocalData.getInstance().getFächer().indexOf(fach);
            for (Event e : eventList) {
                e.setFachName(fach.getName());
                e.setFachIndex(fachIndex);
                terminList.add(e);
            }

        } else if (mode == MODE_TERMINE) { //Terminansicht

            for (int i = 0; i < LocalData.getInstance().getFächer().size(); i++) {
                Fach f = LocalData.getInstance().getFächer().get(i);
                for (Event e : f.getEvents()) {
                    e.setFachName(f.getName());
                    e.setFachIndex(i);
                    terminList.add(e);
                }
            }
            for (Event e : LocalData.getInstance().getNoFachEvents()) {
                e.setFachIndex(-1);
                e.setFachName("");
                terminList.add(e);
            }

            List<Ferien> ferienList = LocalData.getInstance().getFerien();
            for (Ferien f : ferienList) {
                terminList.add(f);
            }
        } else { //Ferienansicht

            List<Ferien> ferienList = LocalData.getInstance().getFerien();
            for (Ferien f : ferienList) {
                terminList.add(f);
            }

        }

        Collections.sort(terminList, new Comparator<Termin>() {
            @Override
            public int compare(Termin t1, Termin t2) {
                return t1.getDatum().compareTo(t2.getDatum());
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
                if (Utilities.compareDays(terminList.get(i - dividerCount - 1).getDatum(), terminList.get(i - dividerCount - 2).getDatum()) != 0) {
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
        if (viewType == VIEWTYPE_EVENT || viewType == VIEWTYPE_FERIEN)
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_termin, parent, false);
        else
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_daydivider, parent, false);

        return new ViewHolder(v, viewType);
    }

    private Termin getTerminAt(int position) {

        int terminPos;

        if (position == 0 || position == 1) { //Erster Divider
            terminPos = 0;
        } else { //Alle andern durch zählen herausfinden
            int divCount = 0;
            for (int i = 2; i < position; i++) if (isDivider[i]) divCount++;
            terminPos = position - 1 - divCount;
        }

        return terminList.get(terminPos);

    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        Termin termin = getTerminAt(position);

        if (!isDivider[position]) {

            Calendar cNow = Calendar.getInstance();

            if (termin instanceof Event) {

                final Event e = (Event) termin;

                int compareNumber = Utilities.compareDays(e.getDatum(), cNow.getTime());

                if (compareNumber >= 0)
                    holder.infoText.setText(Utilities.dayDifferenceToString(Utilities.getDayDifference(cNow.getTime(), e.getDatum())));
                else holder.infoText.setText("vergangen");


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

                if (e.getFachName().isEmpty()) holder.titleText.setText(e.getTitle());
                else
                    holder.titleText.setText(String.format(Locale.GERMANY, "%s: %s", e.getFachName(), e.getTitle()));
                holder.descText.setText(e.getDescription());

            } else {

                final Ferien f = (Ferien) termin;

                holder.titleText.setText(f.getName());
                if (Utilities.compareDays(f.getStartDate(), cNow.getTime()) > 0) {
                    holder.infoText.setText(Utilities.dayDifferenceToString(Utilities.getDayDifference(cNow.getTime(), f.getStartDate())));
                } else if (Utilities.compareDays(f.getEndDate(), cNow.getTime()) >= 0) {
                    int dayDiff = Utilities.getDayDifference(cNow.getTime(), f.getEndDate());
                    if (dayDiff == 1)
                        holder.infoText.setText("noch 1 Tag");
                    else
                        holder.infoText.setText(String.format(Locale.GERMANY, "noch %s Tage", dayDiff));
                } else holder.infoText.setText("vergangen");

                holder.descText.setText(f.getDateString());

                holder.deleteButtonText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int ferienIndex = LocalData.getInstance().getFerien().indexOf(f);
                        callback.onDeleteFerien(ferienIndex, holder.getAdapterPosition());
                    }
                });
                holder.editButtonText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int ferienIndex = LocalData.getInstance().getFerien().indexOf(f);
                        callback.onEditFerien(ferienIndex);
                    }
                });

            }

        } else holder.datumText.setText(getDateString(termin.getDatum()));

    }

    private String getDateString(Date datum) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMAN);
        return dateFormat.format(datum);
    }

    @Override
    public int getItemViewType(int position) {

        if (isDivider[position]) return VIEWTYPE_DIVIDER;
        else {
            return getTerminAt(position) instanceof Event ? VIEWTYPE_EVENT : VIEWTYPE_FERIEN;
        }
    }

    @Override
    public long getItemId(int position) {

        Termin t = getTerminAt(position);

        if (!isDivider[position]) return t.hashCode();
        else return t.getDatum().hashCode();

    }

    @Override
    public int getItemCount() {

        if (terminList.size() == 0) return 0;
        if (terminList.size() == 1) return 2;
        int uniqueDays = 0;
        for (int i = 1; i < terminList.size(); i++) {
            if (Utilities.compareDays(terminList.get(i - 1).getDatum(), terminList.get(i).getDatum()) != 0)
                uniqueDays++;
        }

        return terminList.size() + uniqueDays + 1;
    }

    public interface Callback {

        void onEditEvent(int fachIndex, int eventIndex);

        void onDeleteEvent(int fachIndex, int eventIndex, int recyclerIndex);

        void onEditFerien(int ferienIndex);

        void onDeleteFerien(int ferienIndex, int recyclerIndex);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView datumText;
        View dividerView;
        TextView titleText;
        ExpandableTextView descText;
        TextView deleteButtonText;
        TextView editButtonText;
        TextView infoText;

        public ViewHolder(View itemView, int viewType) {

            super(itemView);

            if (viewType == VIEWTYPE_EVENT || viewType == VIEWTYPE_FERIEN) {
                dividerView = itemView.findViewById(R.id.dividerView);
                infoText = (TextView) itemView.findViewById(R.id.infoText);
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
