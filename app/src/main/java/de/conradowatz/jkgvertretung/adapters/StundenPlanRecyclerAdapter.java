package de.conradowatz.jkgvertretung.adapters;

import android.content.Context;
import android.os.AsyncTask;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Event_Table;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Ferien;
import de.conradowatz.jkgvertretung.variables.OnlineTag;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.UnterrichtsZeit;


public class StundenPlanRecyclerAdapter extends RecyclerView.Adapter<StundenPlanRecyclerAdapter.ViewHolder> {

    private static final int VIEWTYPE_HEADER = 1;
    private static final int VIEWTYPE_STUNDENITEM = 2;
    private static final int VIEWTYPE_SILAST = 3;
    private static final int VIEWTYPE_NOOFFLINE = 4;
    private static final int VIEWTYPE_FERIEN = 5;
    private Date date;
    private OnlineTag onlineTag; //can be null
    private Callback callback;
    private boolean noPlan;
    private boolean isFerien;
    private int wochentag;
    private List<UnterrichtsZeit> unterrichtsZeit;
    private List<Stunde> stunden;
    private List<Event> tagEvents = new ArrayList<>();

    public StundenPlanRecyclerAdapter(Date date, Callback callback) {

        this.callback = callback;
        this.date = date;
        wochentag = Utilities.getDayOfWeek(date);

        updateData();
    }

    public void updateData() {

        new AsyncTask<Boolean, Integer, Boolean>() {

            OnlineTag tmpOnlineTag;
            List<Stunde> tmpStunden;
            List<UnterrichtsZeit> tmpUnterrichtsZeit;
            List<Event> tmpTagEvents;
            boolean tmpNoPlan;
            boolean tmpIsFerien;

            @Override
            protected Boolean doInBackground(Boolean... params) {
                tmpOnlineTag = OnlineTag.getOnlineTag(date);
                tmpTagEvents = SQLite.select().from(Event.class).where(Event_Table.date.eq(date)).and(Event_Table.fach_id.isNull()).queryList();
                if (tmpOnlineTag != null) {
                    tmpStunden = tmpOnlineTag.getSelectedSortedStundenList();
                }
                tmpUnterrichtsZeit = UnterrichtsZeit.getSortedUnterrichtList(wochentag, LocalData.isAWoche(date));
                tmpNoPlan = tmpUnterrichtsZeit.isEmpty() && (tmpOnlineTag==null || tmpStunden.isEmpty());
                tmpIsFerien = Ferien.isFerien(date);
                return true;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                onlineTag = tmpOnlineTag;
                tagEvents = tmpTagEvents;
                stunden = tmpStunden;
                unterrichtsZeit = tmpUnterrichtsZeit;
                noPlan = tmpNoPlan;
                isFerien = tmpIsFerien;
                notifyDataSetChanged();
            }
        }.execute();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;

        if (viewType == VIEWTYPE_HEADER) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_header, parent, false);
        } else if (viewType == VIEWTYPE_STUNDENITEM) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_stunde, parent, false);
        } else if (viewType == VIEWTYPE_SILAST) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_stunde_last, parent, false);
        } else if (viewType == VIEWTYPE_NOOFFLINE) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_nooffline, parent, false);
        } else {  //viewtype == VIEWTYPE_FERIEN
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_ferien, parent, false);
        }

        return new ViewHolder(v, viewType);
    }

    @Override
    public int getItemViewType(int position) {

        if (position == 0) return VIEWTYPE_HEADER;
        else {
            if (isFerien) return VIEWTYPE_FERIEN;
            if (noPlan) return VIEWTYPE_NOOFFLINE;
            if (position != getItemCount() - 1) return VIEWTYPE_STUNDENITEM;
            else return VIEWTYPE_SILAST;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        int viewType = getItemViewType(position);

        if (viewType == VIEWTYPE_HEADER) {

            holder.datumText.setText(LocalData.makeDateHeadingString(date));
            int tagEventSize = tagEvents.size();
            if (tagEventSize>0) {
                holder.tagEventLayout.setVisibility(View.VISIBLE);
                String eventName = tagEvents.get(0).getName();
                if (tagEventSize>1) eventName+="...";
                holder.tagEventText.setText(eventName);
                holder.tagEventLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        callback.onEventClicked(tagEvents.get(0), date);
                    }
                });
            } else {
                holder.tagEventLayout.setVisibility(View.GONE);
                holder.tagEventLayout.setOnClickListener(null);
            }

            if (onlineTag == null)
                holder.zeitstempelText.setText("erstellt aus Offlinedaten");
            else
                holder.zeitstempelText.setText(String.format(Locale.GERMANY, "aktualisiert am %s", onlineTag.getZeitStempel()));

        } else if (viewType == VIEWTYPE_NOOFFLINE) {

            holder.managerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onManagerClicked();
                }
            });

        } else if (viewType == VIEWTYPE_FERIEN) {

            String ferienName = "Schulfrei";
            final Ferien ferien = Ferien.getFerien(date);

            holder.feriennameText.setText(ferienName);
            if (ferien != null) {
                holder.dateText.setVisibility(View.VISIBLE);
                holder.dateText.setText(ferien.getDateString());
                holder.linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.onFerienClicked(ferien.getId());
                    }
                });
            } else holder.dateText.setVisibility(View.GONE);

        } else { //VIEWTYPE_STUNDENITEM

            final Context context = holder.stundeText.getContext();
            holder.kursText.setVisibility(View.GONE);

            UnterrichtsZeit unterricht = null;

            if (onlineTag != null && !stunden.isEmpty()) {

                final Stunde stunde = stunden.get(position - 1);
                holder.stundeText.setText(String.valueOf(stunde.getStunde()));
                final String fachName = stunde.isFachGeaendert()||(stunde.getKurs()==null || stunde.getKurs().getFach()==null) ? stunde.getFach() : stunde.getKurs().getFach().getName();
                holder.fachText.setText(fachName);
                if (stunde.isFachGeaendert())
                    holder.fachText.setTextColor(ContextCompat.getColor(context, R.color.warn_text));
                else
                    holder.fachText.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
                holder.raumText.setText(stunde.getRaum());
                if (stunde.isRaumGeaendert())
                    holder.raumText.setTextColor(ContextCompat.getColor(context, R.color.warn_text));
                else
                    holder.raumText.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
                if (stunde.getInfo().isEmpty())
                    holder.infoText.setVisibility(View.GONE);
                else {
                    holder.infoText.setVisibility(View.VISIBLE);
                    holder.infoText.setText(stunde.getInfo());
                }
                holder.lehrerText.setVisibility(View.VISIBLE);
                String lehrerName = stunde.isLehrerGeaendert() || stunde.getKurs() == null ? stunde.getLehrer() : stunde.getKurs().getLehrer().getName();
                holder.lehrerText.setText(lehrerName);
                if (fachName.trim().isEmpty() && lehrerName.trim().isEmpty()) {
                    holder.fachText.setVisibility(View.GONE);
                    holder.lehrerText.setVisibility(View.GONE);
                } else {
                    holder.fachText.setVisibility(View.VISIBLE);
                    holder.lehrerText.setVisibility(View.VISIBLE);
                }

                if (viewType == VIEWTYPE_SILAST && !onlineTag.getInfotext().isEmpty()) {
                    holder.tagInfoLayout.setVisibility(View.VISIBLE);
                    holder.tagInfoText.setText(onlineTag.getInfotext());
                }

                holder.linearLayout.setOnClickListener(null);
                holder.eventText.setVisibility(View.GONE);

                for (UnterrichtsZeit u : unterrichtsZeit) if (u.getStunde()==stunde.getStunde()) {
                    unterricht = u;
                    break;
                }

                if (unterricht==null) {
                    holder.linearLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            callback.onNewStundeClicked(fachName);
                        }
                    });
                    return;
                }

            } else {

                unterricht = unterrichtsZeit.get(position-1);
                holder.stundeText.setText(String.valueOf(unterricht.getStunde()));
                holder.fachText.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
                holder.fachText.setText(unterricht.getFach().getName());
                holder.infoText.setVisibility(View.GONE);
                holder.raumText.setText("");
                if (unterricht.getFach().getKurs() != null) {
                    holder.lehrerText.setVisibility(View.VISIBLE);
                    holder.lehrerText.setText(unterricht.getFach().getKurs().getLehrer().getName());
                } else
                    holder.lehrerText.setVisibility(View.GONE);
            }

            final List<Event> events = unterricht.getFach().getEventList(date);
            if (!events.isEmpty()) {
                String eventText = events.get(0).getName();
                if (events.size()>1) eventText += "...";
                holder.eventText.setVisibility(View.VISIBLE);
                holder.eventText.setText(eventText);
            } else holder.eventText.setVisibility(View.GONE);

            if (events.isEmpty()) {
                final UnterrichtsZeit finalUnterricht = unterricht;
                holder.linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        callback.onFachClicked(finalUnterricht.getFach(), date);
                    }
                });
            } else {
                holder.linearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        callback.onEventClicked(events.get(0), date);
                    }
                });
            }

        }
    }

    @Override
    public int getItemCount() {

        if (unterrichtsZeit==null) return 0;
        int itemCount = 0;
        if (onlineTag!=null && !stunden.isEmpty())
            itemCount = stunden.size();
        else if (!isFerien) itemCount = unterrichtsZeit.size();
        itemCount++;
        if (itemCount == 1) itemCount++;
        return itemCount;
    }

    public interface Callback {

        void onFachClicked(Fach fach, Date date);

        void onNewStundeClicked(String fachName);

        void onManagerClicked();

        void onFerienClicked(long ferienId);

        void onEventClicked(Event event, Date date);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        TextView datumText;
        TextView zeitstempelText;
        LinearLayout tagEventLayout;
        TextView tagEventText;

        Button managerButton;

        TextView feriennameText;
        TextView dateText;

        LinearLayout linearLayout;
        LinearLayout tagInfoLayout;

        TextView kursText;
        TextView stundeText;
        TextView fachText;
        TextView lehrerText;
        TextView raumText;
        TextView infoText;
        TextView eventText;
        TextView tagInfoText;

        public ViewHolder(View itemView, int viewType) {

            super(itemView);
            this.itemView = itemView;

            if (viewType == VIEWTYPE_HEADER) {

                datumText = (TextView) itemView.findViewById(R.id.datumText);
                zeitstempelText = (TextView) itemView.findViewById(R.id.zeitstempelText);
                tagEventLayout = (LinearLayout) itemView.findViewById(R.id.tagEventLayout);
                tagEventText = (TextView) itemView.findViewById(R.id.tagEventText);

            } else if (viewType == VIEWTYPE_NOOFFLINE) {

                managerButton = (Button) itemView.findViewById(R.id.managerButton);

            } else if (viewType == VIEWTYPE_FERIEN) {

                linearLayout = (LinearLayout) itemView.findViewById(R.id.linearLayout);
                feriennameText = (TextView) itemView.findViewById(R.id.feriennameText);
                dateText = (TextView) itemView.findViewById(R.id.dateText);

            } else {

                linearLayout = (LinearLayout) itemView.findViewById(R.id.linearLayout);
                kursText = (TextView) itemView.findViewById(R.id.kursText);
                stundeText = (TextView) itemView.findViewById(R.id.stundeText);
                fachText = (TextView) itemView.findViewById(R.id.fachText);
                lehrerText = (TextView) itemView.findViewById(R.id.lehrerText);
                raumText = (TextView) itemView.findViewById(R.id.raumText);
                infoText = (TextView) itemView.findViewById(R.id.infoText);
                eventText = (TextView) itemView.findViewById(R.id.eventText);

                if (viewType == VIEWTYPE_SILAST) {
                    tagInfoLayout = (LinearLayout) itemView.findViewById(R.id.tagInfoLayout);
                    tagInfoText = (TextView) itemView.findViewById(R.id.tagInfoText);
                }
            }
        }
    }
}
