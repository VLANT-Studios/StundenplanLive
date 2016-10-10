package de.conradowatz.jkgvertretung.adapters;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.StuPlaKlasse;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.Tag;

public class FreieZimmerRecyclerAdapter extends RecyclerView.Adapter<FreieZimmerRecyclerAdapter.ViewHolder> {

    private static final int VIEWTYPE_HEADER = 1;
    private static final int VIEWTYPE_ZIMMERITEM = 2;
    private static final int VIEWTYPE_ZILAST = 3;
    private static final int VIEWTYPE_TEXT = 4;
    private String datumString;
    private String zeitStempelString;
    private List<List<String>> freieZimmerList = null;
    private List<String> zimmerDisplayTextList;
    private List<String> stundenTextList;
    private int count;
    private boolean noPlan;

    public FreieZimmerRecyclerAdapter(Tag tag) {

        noPlan = true;
        count = 10;

        freieZimmerList = new ArrayList<>();
        zimmerDisplayTextList = new ArrayList<>();
        stundenTextList = new ArrayList<>();

        for (int i = 1; i <= 9; i++)
            freieZimmerList.add(new ArrayList<>(Arrays.asList(LocalData.getAllRooms())));

        for (StuPlaKlasse klasse : tag.getStuplaKlasseList()) {
            ArrayList<Stunde> stundenList = klasse.getStundenList();
            if (stundenList.size() > 0) noPlan = false;
            for (Stunde stunde : stundenList) {
                int stundennr = Integer.valueOf(stunde.getStunde());
                if (0 < stundennr && stundennr < 10) {
                    List<String> freieZimmerStundeList = freieZimmerList.get(stundennr - 1);
                    for (int j = 0; j < freieZimmerStundeList.size(); j++) {
                        String zimmer = freieZimmerStundeList.get(j);
                        if (zimmer.startsWith("%")) zimmer = zimmer.substring(2);
                        if (zimmer.equals(stunde.getRaum())) {
                            freieZimmerStundeList.remove(j);
                            break;
                        }
                    }
                }
            }

        }

        for (int i = 1; i <= 9; i++) {
            List<String> freieZimmerStundeList = freieZimmerList.get(i - 1);
            if (freieZimmerStundeList.size() == 0) {
                zimmerDisplayTextList.add("-");
            } else {
                String freieZimmer = colorString(freieZimmerStundeList.get(0));
                for (int j = 1; j < freieZimmerStundeList.size(); j++) {
                    freieZimmer += ", " + colorString(freieZimmerStundeList.get(j));
                }
                if (zimmerDisplayTextList.size() > 0 && freieZimmer.equals(zimmerDisplayTextList.get(zimmerDisplayTextList.size() - 1))) {
                    count--;
                    stundenTextList.set(stundenTextList.size() - 1, stundenTextList.get(stundenTextList.size() - 1) + "/" + i);
                } else {
                    zimmerDisplayTextList.add(freieZimmer);
                    stundenTextList.add(String.valueOf(i));
                }
            }

        }


        datumString = tag.getDatumString();
        zeitStempelString = tag.getZeitStempel();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;

        if (viewType == VIEWTYPE_HEADER) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.stundenplan_header_item, parent, false);
        } else if (viewType == VIEWTYPE_ZIMMERITEM) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.freiezimmer_item, parent, false);
        } else if (viewType == VIEWTYPE_ZILAST) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.freiezimmer_lastitem, parent, false);
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
            if (position != getItemCount() - 1) return VIEWTYPE_ZIMMERITEM;
            else return VIEWTYPE_ZILAST;
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        int viewType = getItemViewType(position);

        if (viewType == VIEWTYPE_HEADER) {

            holder.datumText.setText(datumString);
            holder.zeitstempelText.setText("aktualisiert am " + zeitStempelString);
        } else if (viewType == VIEWTYPE_TEXT) {

            holder.infoText.setText("Für diesen Tag sind keine Informationen zu freien Zimmern vorhanden.");

        } else {

            holder.stundeText.setText(stundenTextList.get(position - 1));
            holder.zimmerText.setText(Html.fromHtml(zimmerDisplayTextList.get(position - 1)));

        }
    }

    private String colorString(String zimmerCode) {

        if (zimmerCode.startsWith("%"))
            return "<font color=\"#303F9F\">" + zimmerCode.substring(2) + "</font>";
        else return "<font color=\"#388E3C\">" + zimmerCode + "</font>";

    }

    @Override
    public int getItemCount() {

        return noPlan ? 2 : count;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        TextView datumText;
        TextView zeitstempelText;

        TextView stundeText;
        TextView zimmerText;
        TextView infoText;
        TextView colordescText;

        public ViewHolder(View itemView, int viewType) {

            super(itemView);
            this.itemView = itemView;

            if (viewType == VIEWTYPE_HEADER) {

                datumText = (TextView) itemView.findViewById(R.id.datumText);
                zeitstempelText = (TextView) itemView.findViewById(R.id.zeitstempelText);
            } else if (viewType == VIEWTYPE_TEXT) {

                infoText = (TextView) itemView.findViewById(R.id.text);
            } else {

                stundeText = (TextView) itemView.findViewById(R.id.stundeText);
                zimmerText = (TextView) itemView.findViewById(R.id.zimmerText);
                infoText = (TextView) itemView.findViewById(R.id.infoText);
                if (viewType == VIEWTYPE_ZILAST) {
                    colordescText = (TextView) itemView.findViewById(R.id.colordescText);
                    colordescText.setText(Html.fromHtml("<font color=\"#303F9F\">Fachraum</font> - <font color=\"#388E3C\">Unterrichtsraum</font>"));
                }
            }
        }
    }
}
