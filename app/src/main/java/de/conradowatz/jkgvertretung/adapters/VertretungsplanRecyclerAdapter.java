package de.conradowatz.jkgvertretung.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.ColorAPI;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.OnlineTag;
import de.conradowatz.jkgvertretung.variables.OnlineTag_Table;
import de.conradowatz.jkgvertretung.variables.Stunde;


public class VertretungsplanRecyclerAdapter extends RecyclerView.Adapter<VertretungsplanRecyclerAdapter.ViewHolder> {

    private static final int VIEWTYPE_HEADER = 1;
    private static final int VIEWTYPE_STUNDENITEM = 2;
    private static final int VIEWTYPE_SILAST = 3;
    private static final int VIEWTYPE_TEXT = 4;
    private OnlineTag onlineTag;
    private int position;
    private boolean noPlan;
    private List<Stunde> stunden;

    public VertretungsplanRecyclerAdapter(int position) {

        this.position = position;

        updateData();
    }

    public void updateData() {

        new AsyncTask<Boolean, Integer, List<Stunde>>() {

            OnlineTag tmpOnlineTag;

            @Override
            protected List<Stunde> doInBackground(Boolean... params) {

                tmpOnlineTag = SQLite.select().from(OnlineTag.class).orderBy(OnlineTag_Table.date, true).offset(position).querySingle();
                if (tmpOnlineTag==null) return null;
                return tmpOnlineTag.getSelectedSortedVertretungList();
            }

            @Override
            protected void onPostExecute(List<Stunde> s) {
                onlineTag = tmpOnlineTag;
                stunden = s;
                noPlan = s==null||s.isEmpty();
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
        } else {  //viewtype == VIEWTYPE_TEXT
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_text, parent, false);
        }

        return new ViewHolder(v, viewType);
    }

    @Override
    public int getItemViewType(int position) {

        if (position == 0) return VIEWTYPE_HEADER;
        else {
            if (noPlan) return VIEWTYPE_TEXT;
            if (position != getItemCount() - 1) return VIEWTYPE_STUNDENITEM;
            else return VIEWTYPE_SILAST;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        int viewType = getItemViewType(position);

        if (viewType == VIEWTYPE_HEADER) {

            holder.datumText.setText(LocalData.makeDateHeadingString(onlineTag.getDate()));
            holder.zeitstempelText.setText(String.format(Locale.GERMANY, "aktualisiert am %s", onlineTag.getZeitStempel()));

        } else if (viewType == VIEWTYPE_TEXT) {

            holder.textText.setText("Keine Vertretung an diesem Tag.");

        } else { //VIEWTYPE_STUNDENITEM

            final Context context = holder.stundeText.getContext();
            holder.kursText.setVisibility(View.GONE);

            final Stunde stunde = stunden.get(position - 1);
            holder.stundeText.setText(String.valueOf(stunde.getStunde()));
            final String fachName = stunde.isFachGeaendert()||stunde.getKurs()==null ? stunde.getFach() : stunde.getKurs().getFachName();
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
            String lehrerName = stunde.isLehrerGeaendert() || stunde.getKurs() == null ? stunde.getLehrer() : stunde.getKurs().getLehrer().getName();
            holder.lehrerText.setText(lehrerName);
            if (fachName.trim().isEmpty() && lehrerName.trim().isEmpty()) {
                holder.fachText.setVisibility(View.GONE);
                holder.lehrerText.setVisibility(View.GONE);
            } else {
                holder.fachText.setVisibility(View.VISIBLE);
                holder.lehrerText.setVisibility(View.VISIBLE);
            }

            holder.eventText.setVisibility(View.GONE);

        }
    }

    @Override
    public int getItemCount() {

        if (stunden==null) return 0;
        return noPlan?2:stunden.size()+1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        TextView datumText;
        TextView zeitstempelText;

        TextView textText;

        TextView kursText;
        TextView stundeText;
        TextView fachText;
        TextView lehrerText;
        TextView raumText;
        TextView infoText;
        TextView eventText;

        public ViewHolder(View itemView, int viewType) {

            super(itemView);
            this.itemView = itemView;

            if (viewType == VIEWTYPE_HEADER) {

                datumText = (TextView) itemView.findViewById(R.id.datumText);
                zeitstempelText = (TextView) itemView.findViewById(R.id.zeitstempelText);
                itemView.findViewById(R.id.stupla_header_view).setBackgroundColor(new ColorAPI(MyApplication.getAppContext()).getAccentColor());

            } else if (viewType == VIEWTYPE_TEXT) {

                textText = (TextView) itemView.findViewById(R.id.text);

            } else {

                kursText = (TextView) itemView.findViewById(R.id.kursText);
                stundeText = (TextView) itemView.findViewById(R.id.stundeText);
                fachText = (TextView) itemView.findViewById(R.id.fachText);
                lehrerText = (TextView) itemView.findViewById(R.id.lehrerText);
                raumText = (TextView) itemView.findViewById(R.id.raumText);
                infoText = (TextView) itemView.findViewById(R.id.infoText);
                eventText = (TextView) itemView.findViewById(R.id.eventText);
            }
        }
    }
}
