package de.conradowatz.jkgvertretung.adapters;


import android.os.AsyncTask;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.ms.square.android.expandabletextview.ExpandableTextView;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Event_Table;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Ferien;
import de.conradowatz.jkgvertretung.variables.Ferien_Table;
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

        updateData();
    }

    public TerminRecyclerAdapter(int mode, Callback callback) {

        setHasStableIds(true);

        this.mode = mode;
        this.callback = callback;
        this.fach = null;

        updateData();
    }

    public void updateData() {

        new AsyncTask<Boolean, Integer, List<Termin>>() {

            boolean[] tmpIsDivider;

            @Override
            protected List<Termin> doInBackground(Boolean... params) {

                List<Termin> terminList = new ArrayList<>();

                if (mode == MODE_EVENTS) { //Fachansicht

                    terminList.addAll(fach.getEventList());

                } else if (mode == MODE_TERMINE) { //Terminansicht

                    List<Ferien> ferien = SQLite.select().from(Ferien.class).orderBy(Ferien_Table.startDate, true).queryList();
                    terminList.addAll(ferien);
                    List<Event> events = SQLite.select().from(Event.class).orderBy(Event_Table.date, true).queryList();
                    terminList.addAll(events);

                    Collections.sort(terminList, new Comparator<Termin>() {
                        @Override
                        public int compare(Termin t1, Termin t2) {
                            return t1.getDate().compareTo(t2.getDate());
                        }
                    });

                } else { //Ferienansicht

                    List<Ferien> ferien = SQLite.select().from(Ferien.class).orderBy(Ferien_Table.startDate, true).queryList();
                    terminList.addAll(ferien);

                }

                //DividerArray erstellen
                int uniqueDays = 0;
                for (int i = 1; i < terminList.size(); i++) {
                    if (Utilities.compareDays(terminList.get(i - 1).getDate(), terminList.get(i).getDate()) != 0)
                        uniqueDays++;
                }
                int size = terminList.size() + uniqueDays + 1;
                if (terminList.size()==0) size = 0;
                if (terminList.size() == 1) size = 2;
                if (size > 0) {
                    tmpIsDivider = new boolean[size];
                    tmpIsDivider[0] = true;
                    tmpIsDivider[1] = false;
                    int dividerCount = 0;
                    for (int i = 2; i < size; i++) {
                        if (Utilities.compareDays(terminList.get(i - dividerCount - 1).getDate(), terminList.get(i - dividerCount - 2).getDate()) != 0) {
                            dividerCount++;
                            tmpIsDivider[i] = true;
                            tmpIsDivider[i + 1] = false;
                            i++;
                        }
                    }
                }
                return terminList;
            }

            @Override
            protected void onPostExecute(List<Termin> t) {
                terminList = t;
                isDivider = tmpIsDivider;
                notifyDataSetChanged();
            }
        }.execute();
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

                int compareNumber = Utilities.compareDays(e.getDate(), cNow.getTime());

                if (compareNumber >= 0)
                    holder.infoText.setText(Utilities.dayDifferenceToString(Utilities.getDayDifference(cNow.getTime(), e.getDate())));
                else holder.infoText.setText("vergangen");


                holder.editButtonText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.onEditEvent(e.getId());
                    }
                });
                holder.deleteButtonText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.onDeleteEvent(e.getId(), holder.getAdapterPosition());
                    }
                });

                if (e.getFach()==null) holder.titleText.setText(e.getName());
                else
                    holder.titleText.setText(String.format(Locale.GERMANY, "%s: %s", e.getFach().getName(), e.getName()));
                holder.descText.setText(e.getDescription());

            } else {

                final Ferien f = (Ferien) termin;

                holder.titleText.setText(f.getName());
                if (Utilities.compareDays(f.getStartDate(), cNow.getTime()) > 0) {
                    holder.infoText.setText(Utilities.dayDifferenceToString(Utilities.getDayDifference(cNow.getTime(), f.getStartDate())));
                } else if (Utilities.compareDays(f.getEndDate(), cNow.getTime()) >= 0) {
                    int dayDiff = Utilities.getDayDifference(cNow.getTime(), f.getEndDate());
                    if (dayDiff == 1)
                        holder.infoText.setText("noch 1 OnlineTag");
                    else
                        holder.infoText.setText(String.format(Locale.GERMANY, "noch %s Tage", dayDiff));
                } else holder.infoText.setText("vergangen");

                holder.descText.setText(f.getDateString());

                holder.deleteButtonText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.onDeleteFerien(f.getId(), holder.getAdapterPosition());
                    }
                });
                holder.editButtonText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.onEditFerien(f.getId());
                    }
                });

            }

        } else holder.datumText.setText(getDateString(termin.getDate()));

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
        else return t.getDate().hashCode();

    }

    @Override
    public int getItemCount() {

        if (terminList==null || terminList.size()==0) return 0;
        if (terminList.size() == 1) return 2;
        int uniqueDays = 0;
        for (int i = 1; i < terminList.size(); i++) {
            if (Utilities.compareDays(terminList.get(i - 1).getDate(), terminList.get(i).getDate()) != 0)
                uniqueDays++;
        }

        return terminList.size() + uniqueDays + 1;
    }

    public interface Callback {

        void onEditEvent(long eventId);

        void onDeleteEvent(long eventId, int recyclerIndex);

        void onEditFerien(long ferienId);

        void onDeleteFerien(long ferienId, int recyclerIndex);
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
