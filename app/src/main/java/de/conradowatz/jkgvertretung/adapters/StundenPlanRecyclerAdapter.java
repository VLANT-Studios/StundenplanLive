package de.conradowatz.jkgvertretung.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.variables.StuPlaKlasse;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.Tag;
import de.conradowatz.jkgvertretung.variables.Vertretung;


public class StundenPlanRecyclerAdapter extends RecyclerView.Adapter<StundenPlanRecyclerAdapter.ViewHolder> {

    public static final int MODE_STUNDENPLAN = 1;
    public static final int MODE_VERTRETUNGSPLAN = 2;
    public static final int MODE_ALLGSTUNDENPLAN = 3;
    private static final int VIEWTYPE_HEADER = 1;
    private static final int VIEWTYPE_STUNDENITEM = 2;
    private static final int VIEWTYPE_SILAST = 3;
    private static final int VIEWTYPE_TEXT = 4;
    private String datumString;
    private String zeitStempelString;
    private ArrayList<Stunde> stundenList = null;
    private ArrayList<Vertretung> vertretungsList = null;
    private int mode;
    private boolean noPlan;

    private StundenPlanRecyclerAdapter(Tag tag, ArrayList<Stunde> stundenList, ArrayList<Vertretung> vertretungsList, int mode, boolean noPlan) {

        this.mode = mode;
        this.stundenList = stundenList;
        this.vertretungsList = vertretungsList;
        this.noPlan = noPlan;

        datumString = tag.getDatumString();
        zeitStempelString = tag.getZeitStempel();
    }

    public static StundenPlanRecyclerAdapter newStundenplanInstance(Tag tag, int klasseIndex, ArrayList<String> nichtKurse) {

        StuPlaKlasse stuPlaKlasse = tag.getStuplaKlasseList().get(klasseIndex);
        ArrayList<Stunde> stundenList = new ArrayList<>();
        for (Stunde stunde : stuPlaKlasse.getStundenList()) {

            if (nichtKurse.contains(stunde.getKurs())
                    || nichtKurse.contains(stunde.getFach())
                    || nichtKurse.contains(stunde.getInfo().split(" ")[0]))
                continue;

            stundenList.add(stunde);
        }

        return new StundenPlanRecyclerAdapter(tag, stundenList, null, MODE_STUNDENPLAN, stundenList.size() == 0);

    }

    public static StundenPlanRecyclerAdapter newKlassenplanplanInstance(Tag tag, int klasseIndex) {

        ArrayList<Stunde> stundenList = tag.getStuplaKlasseList().get(klasseIndex).getStundenList();
        return new StundenPlanRecyclerAdapter(tag, stundenList, null, MODE_STUNDENPLAN, stundenList.size() == 0);

    }

    public static StundenPlanRecyclerAdapter newVertretungsplanInstance(Tag tag, String klassenString, ArrayList<String> nichtKurse) {

        ArrayList<Vertretung> vertretungsList = new ArrayList<>();
        for (Vertretung vertretung : tag.getVertretungsList()) {

            if (!vertretung.getKlasse().contains(klassenString))
                continue;

            boolean goOn = true;
            for (String nichtKursString : nichtKurse) {
                if (vertretung.getKlasse().contains(nichtKursString)) {
                    goOn = false;
                    break;
                }
            }
            if (!goOn) continue;

            vertretungsList.add(vertretung);

        }

        return new StundenPlanRecyclerAdapter(tag, null, vertretungsList, MODE_VERTRETUNGSPLAN, vertretungsList.size() == 0);

    }

    public static StundenPlanRecyclerAdapter newAllgvertretungsplanInstance(Tag tag) {

        return new StundenPlanRecyclerAdapter(tag, null, tag.getVertretungsList(), MODE_ALLGSTUNDENPLAN, tag.getVertretungsList().size() == 0);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;

        if (viewType == VIEWTYPE_HEADER) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.stundenplan_header_item, parent, false);
        } else if (viewType == VIEWTYPE_STUNDENITEM) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.stundenplan_stunde_item, parent, false);
        } else if (viewType == VIEWTYPE_SILAST) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.stundenplan_stunde_lastitem, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.spacedtext_item, parent, false);
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

            holder.datumText.setText(datumString);
            holder.zeitstempelText.setText("aktualisiert am " + zeitStempelString);
        } else if (viewType == VIEWTYPE_TEXT) {

            switch (mode) {
                case MODE_STUNDENPLAN:
                    holder.infoText.setText("Für diesen Tag wurden keine Stunden gefunden.");
                    break;
                case MODE_VERTRETUNGSPLAN:
                    holder.infoText.setText("Für diesen Tag wurde keine Vertretung gefunden. Auf dem allgemeinen Vertretungsplan könnten möglicherweise trotzdem Informationen für dich stehen.");
                    break;
                case MODE_ALLGSTUNDENPLAN:
                    holder.infoText.setText("Für diesen Tag steht kein Vertretungsplan bereit.");
                    break;
            }
        } else {

            Context context = holder.stundeText.getContext();

            if (mode == MODE_STUNDENPLAN) {

                holder.kursText.setVisibility(View.GONE);
                Stunde stunde = stundenList.get(position - 1);
                holder.stundeText.setText(stunde.getStunde());
                holder.fachText.setText(stunde.getFach());
                if (stunde.getFach().trim().isEmpty())
                    holder.fachText.setVisibility(View.GONE);
                else
                    holder.fachText.setVisibility(View.VISIBLE);
                if (stunde.isFachg())
                    holder.fachText.setTextColor(ContextCompat.getColor(context, R.color.warn_text));
                else
                    holder.fachText.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
                holder.raumText.setText(stunde.getRaum());
                if (stunde.isRaumg())
                    holder.raumText.setTextColor(ContextCompat.getColor(context, R.color.warn_text));
                else
                    holder.raumText.setTextColor(ContextCompat.getColor(context, R.color.primary_text));
                if (stunde.getInfo().isEmpty())
                    holder.infoText.setVisibility(View.GONE);
                else
                    holder.infoText.setVisibility(View.VISIBLE);
                holder.infoText.setText(stunde.getInfo());

            } else {

                Vertretung vertretung = vertretungsList.get(position - 1);
                holder.stundeText.setText(vertretung.getStunde());
                holder.fachText.setText(vertretung.getFach());
                if (vertretung.getFach().trim().isEmpty())
                    holder.fachText.setVisibility(View.GONE);
                else
                    holder.fachText.setVisibility(View.VISIBLE);
                holder.raumText.setText(vertretung.getRaum());
                if (vertretung.getInfo().isEmpty())
                    holder.infoText.setVisibility(View.GONE);
                else
                    holder.infoText.setVisibility(View.VISIBLE);
                holder.infoText.setText(vertretung.getInfo());
                holder.kursText.setText(vertretung.getKlasse() + ":");

            }
        }
    }

    @Override
    public int getItemCount() {

        int itemCount;
        if (mode == MODE_STUNDENPLAN) itemCount = stundenList.size() + 1;
        else itemCount = vertretungsList.size() + 1;
        if (itemCount == 1) itemCount++;
        return itemCount;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        TextView datumText;
        TextView zeitstempelText;

        TextView kursText;
        TextView stundeText;
        TextView fachText;
        TextView raumText;
        TextView infoText;

        public ViewHolder(View itemView, int viewType) {

            super(itemView);
            this.itemView = itemView;

            if (viewType == VIEWTYPE_HEADER) {

                datumText = (TextView) itemView.findViewById(R.id.datumText);
                zeitstempelText = (TextView) itemView.findViewById(R.id.zeitstempelText);
            } else if (viewType == VIEWTYPE_TEXT) {

                infoText = (TextView) itemView.findViewById(R.id.text);
            } else {

                kursText = (TextView) itemView.findViewById(R.id.kursText);
                stundeText = (TextView) itemView.findViewById(R.id.stundeText);
                fachText = (TextView) itemView.findViewById(R.id.fachText);
                raumText = (TextView) itemView.findViewById(R.id.raumText);
                infoText = (TextView) itemView.findViewById(R.id.infoText);
            }
        }
    }
}
