package de.conradowatz.jkgvertretung.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.ColorAPI;
import de.conradowatz.jkgvertretung.variables.OnlineTag;

public class FreieZimmerRecyclerAdapter extends RecyclerView.Adapter<FreieZimmerRecyclerAdapter.ViewHolder> {

    private static final int VIEWTYPE_HEADER = 1;
    private static final int VIEWTYPE_ZIMMERITEM = 2;
    private static final int VIEWTYPE_ZILAST = 3;
    private static final int VIEWTYPE_TEXT = 4;
    private String datumString;
    private String zeitStempelString;
    private List<String> stundenTextList;
    private List<String> zimmerTextList;

    public FreieZimmerRecyclerAdapter(OnlineTag onlineTag) {

        Pair<List<String>, List<String>> data = onlineTag.getFreieZimmer();
        this.stundenTextList = data.first;
        this.zimmerTextList = data.second;

        datumString = new SimpleDateFormat("EEEE, dd. MMMM yyyy", Locale.GERMAN).format(onlineTag.getDate());
        zeitStempelString = onlineTag.getZeitStempel();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;

        if (viewType == VIEWTYPE_HEADER) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_header, parent, false);
        } else if (viewType == VIEWTYPE_ZIMMERITEM) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_freiezimmer, parent, false);
        } else if (viewType == VIEWTYPE_ZILAST) {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_freiezimmer_last, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_stundenplan_text, parent, false);
        }

        return new ViewHolder(v, viewType);
    }

    @Override
    public int getItemViewType(int position) {

        if (position == 0) return VIEWTYPE_HEADER;
        else {
            if (stundenTextList==null) return VIEWTYPE_TEXT;
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
            holder.zimmerText.setText(zimmerTextList.get(position - 1));

        }
    }

    @Override
    public int getItemCount() {

        return stundenTextList==null ? 2 : stundenTextList.size()+1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        TextView datumText;
        TextView zeitstempelText;

        TextView stundeText;
        TextView zimmerText;
        TextView infoText;

        public ViewHolder(View itemView, int viewType) {

            super(itemView);
            this.itemView = itemView;

            if (viewType == VIEWTYPE_HEADER) {

                datumText = (TextView) itemView.findViewById(R.id.datumText);
                zeitstempelText = (TextView) itemView.findViewById(R.id.zeitstempelText);
                itemView.findViewById(R.id.stupla_header_view).setBackgroundColor(new ColorAPI(MyApplication.getAppContext()).getAccentColor());
            } else if (viewType == VIEWTYPE_TEXT) {

                infoText = (TextView) itemView.findViewById(R.id.text);
            } else {

                stundeText = (TextView) itemView.findViewById(R.id.stundeText);
                zimmerText = (TextView) itemView.findViewById(R.id.zimmerText);
                infoText = (TextView) itemView.findViewById(R.id.infoText);
            }
        }
    }
}
