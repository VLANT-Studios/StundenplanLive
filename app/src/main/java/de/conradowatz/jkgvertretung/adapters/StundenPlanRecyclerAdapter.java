package de.conradowatz.jkgvertretung.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Ferien;
import de.conradowatz.jkgvertretung.variables.StuPlaKlasse;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.Tag;
import de.conradowatz.jkgvertretung.variables.Vertretung;


public class StundenPlanRecyclerAdapter extends RecyclerView.Adapter<StundenPlanRecyclerAdapter.ViewHolder> {

    private static final int MODE_STUNDENPLAN_OFFLINE = 0;
    private static final int MODE_STUNDENPLAN = 1;
    private static final int MODE_VERTRETUNGSPLAN = 2;
    private static final int MODE_ALLGSTUNDENPLAN = 3;
    private static final int VIEWTYPE_HEADER = 1;
    private static final int VIEWTYPE_STUNDENITEM = 2;
    private static final int VIEWTYPE_SILAST = 3;
    private static final int VIEWTYPE_TEXT = 4;
    private Date date;
    private String datumString;
    private String zeitStempelString;
    private List<Stunde> stundenList = null;
    private List<Vertretung> vertretungsList = null;
    private int mode;
    private boolean noPlan;
    private Callback callback;

    private StundenPlanRecyclerAdapter(Tag tag, List<Stunde> stundenList, ArrayList<Vertretung> vertretungsList, int mode, boolean noPlan, Callback callback) {

        this.mode = mode;
        this.stundenList = stundenList;
        this.vertretungsList = vertretungsList;
        this.noPlan = noPlan;
        this.callback = callback;
        this.date = tag.getDatum();

        datumString = tag.getDatumString();
        zeitStempelString = tag.getZeitStempel();
    }

    private StundenPlanRecyclerAdapter(Date datum, List<Stunde> stundenList, int mode, boolean noPlan, Callback callback) {

        this.mode = mode;
        this.stundenList = stundenList;
        this.noPlan = noPlan;
        this.callback = callback;
        this.date = datum;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(datum);
        String wochenString = (LocalData.getInstance().isAWoche(datum)) ? " (A-Woche)" : " (B-Woche)";
        datumString = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.GERMAN) + ", " + calendar.get(Calendar.DAY_OF_MONTH) + ". " + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMAN) + " " + calendar.get(Calendar.YEAR) + wochenString;
    }

    public static StundenPlanRecyclerAdapter newOnlineStundenplanInstance(Tag tag, int klasseIndex, ArrayList<String> nichtKurse, Callback callback) {

        ArrayList<Stunde> stundenList = new ArrayList<>();
        if (tag.getStuplaKlasseList().size() > klasseIndex) {
            StuPlaKlasse stuPlaKlasse = tag.getStuplaKlasseList().get(klasseIndex);
            for (Stunde stunde : stuPlaKlasse.getStundenList()) {

                if (nichtKurse.contains(stunde.getKurs())
                        || nichtKurse.contains(stunde.getFach())
                        || nichtKurse.contains(stunde.getInfo().split(" ")[0]))
                    continue;

                stundenList.add(stunde);
            }
        }
        return new StundenPlanRecyclerAdapter(tag, stundenList, null, MODE_STUNDENPLAN, stundenList.size() == 0, callback);

    }

    public static StundenPlanRecyclerAdapter newOfflineStundenplanInstance(Date datum, Callback callback) {

        int dayOfWeek = LocalData.getDayOfWeek(datum);
        List<Stunde> stundenList;
        if (VertretungsAPI.isntSchoolDay(datum)) stundenList = new ArrayList<>();
        else
            stundenList = LocalData.getOfflineStundenList(dayOfWeek, LocalData.getInstance().isAWoche(datum));

        return new StundenPlanRecyclerAdapter(datum, stundenList, MODE_STUNDENPLAN_OFFLINE, stundenList.size() == 0, callback);

    }

    public static StundenPlanRecyclerAdapter newKlassenplanplanInstance(Tag tag, int klasseIndex) {

        ArrayList<Stunde> stundenList = new ArrayList<>();
        if (tag.getStuplaKlasseList().size() > klasseIndex) {
            stundenList = tag.getStuplaKlasseList().get(klasseIndex).getStundenList();
        }
        return new StundenPlanRecyclerAdapter(tag, stundenList, null, MODE_STUNDENPLAN, stundenList.size() == 0, null);

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

        return new StundenPlanRecyclerAdapter(tag, null, vertretungsList, MODE_VERTRETUNGSPLAN, vertretungsList.size() == 0, null);

    }

    public static StundenPlanRecyclerAdapter newAllgvertretungsplanInstance(Tag tag) {

        return new StundenPlanRecyclerAdapter(tag, null, tag.getVertretungsList(), MODE_ALLGSTUNDENPLAN, tag.getVertretungsList().size() == 0, null);
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
            if (noPlan) return VIEWTYPE_TEXT; //TODO ersetzen durch offline Daten?
            if (position != getItemCount() - 1) return VIEWTYPE_STUNDENITEM;
            else return VIEWTYPE_SILAST;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        int viewType = getItemViewType(position);

        if (viewType == VIEWTYPE_HEADER) {

            holder.datumText.setText(datumString);
            if (mode == MODE_STUNDENPLAN_OFFLINE)
                holder.zeitstempelText.setText("erstellt aus Offlinedaten");
            else holder.zeitstempelText.setText("aktualisiert am " + zeitStempelString);
        } else if (viewType == VIEWTYPE_TEXT) {

            switch (mode) {
                case MODE_STUNDENPLAN_OFFLINE:
                    if (VertretungsAPI.isntSchoolDay(date)) {
                        String ferienName = "Schulfrei";
                        Calendar cDate = Calendar.getInstance();
                        cDate.setTime(date);
                        for (Ferien f : LocalData.getInstance().getFerien()) {
                            Calendar cStart = Calendar.getInstance();
                            cStart.setTime(f.getStartDate());
                            Calendar cEnd = Calendar.getInstance();
                            cEnd.setTime(f.getEndDate());

                            if (Utilities.compareDays(cDate, cStart) >= 0 && Utilities.compareDays(cDate, cEnd) <= 0) {
                                ferienName = f.getName();
                                break;
                            }
                        }
                        holder.infoText.setText(ferienName);
                    } else
                        holder.infoText.setText("Du hast an diesem Tag keine Offlinestunden Daten hinterlegt. Gehe dazu in den Manager.");
                    break;
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

            if (mode == MODE_STUNDENPLAN || mode == MODE_STUNDENPLAN_OFFLINE) {

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

                try {
                    final int stundenInt = Integer.valueOf(stunde.getStunde()); //1-9 erwartet
                    final String stundenName = stunde.getKurs() != null ? stunde.getKurs() : stunde.getFach();
                    holder.linearLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            boolean isAWoche = LocalData.getInstance().isAWoche(date);
                            int wochenTagInt = LocalData.getDayOfWeek(date); //1-5
                            for (Fach f : LocalData.getInstance().getFächer()) {
                                if (f.getStunden(isAWoche)[wochenTagInt - 1][stundenInt - 1]) {
                                    callback.onFachClicked(f, date);
                                    return;
                                }
                            }
                            callback.onNewStundeClicked(stundenName, isAWoche, wochenTagInt - 1, stundenInt - 1);
                        }
                    });
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }


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
                holder.kursText.setText(String.format("%s:", vertretung.getKlasse()));

            }
        }
    }

    @Override
    public int getItemCount() {

        int itemCount;
        if (mode == MODE_STUNDENPLAN || mode == MODE_STUNDENPLAN_OFFLINE)
            itemCount = stundenList.size();
        else itemCount = vertretungsList.size();
        itemCount++;
        if (itemCount == 1) itemCount++;
        return itemCount;
    }

    public interface Callback {

        void onFachClicked(Fach fach, Date date);

        void onNewStundeClicked(String kursName, boolean aWoche, int tagIndex, int stundeIndex);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        LinearLayout linearLayout;

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

                linearLayout = (LinearLayout) itemView.findViewById(R.id.linearLayout);
                kursText = (TextView) itemView.findViewById(R.id.kursText);
                stundeText = (TextView) itemView.findViewById(R.id.stundeText);
                fachText = (TextView) itemView.findViewById(R.id.fachText);
                raumText = (TextView) itemView.findViewById(R.id.raumText);
                infoText = (TextView) itemView.findViewById(R.id.infoText);
            }
        }
    }
}
