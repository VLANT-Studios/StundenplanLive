package de.conradowatz.jkgvertretung.adapters;


import android.os.AsyncTask;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.variables.Fach;

public class NotenUebersichtRecyclerAdpater extends RecyclerView.Adapter<NotenUebersichtRecyclerAdpater.ViewHolder> {

    private List<Fach> fachList;
    private DecimalFormat decimalFormat = new DecimalFormat("#0.00");
    private Callback callback;

    public NotenUebersichtRecyclerAdpater(Callback callback) {

        this.callback = callback;

        updateData();
    }

    public void updateData() {

        new AsyncTask<Boolean, Integer, List<Fach>>() {
            @Override
            protected List<Fach> doInBackground(Boolean... params) {
                return Fach.getFaecherWithZensuren();
            }

            @Override
            protected void onPostExecute(List<Fach> f) {
                fachList = f;
                notifyDataSetChanged();
            }
        }.execute();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notenuebersicht_fach, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final Fach fach = fachList.get(position);
        holder.fachNameText.setText(fach.getName());
        holder.fachValueText.setText(decimalFormat.format(fach.getZensurenDurchschnitt()));
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onFachClicked(fach.getId());
            }
        });
    }



    @Override
    public int getItemCount() {
        return fachList==null?0:fachList.size();
    }

    public interface Callback {

        void onFachClicked(long fachId);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView fachNameText;
        TextView fachValueText;
        RelativeLayout layout;

        public ViewHolder(View itemView) {
            super(itemView);

            layout = (RelativeLayout) itemView.findViewById(R.id.relativeLayout);
            fachNameText = (TextView) itemView.findViewById(R.id.fachNameText);
            fachValueText = (TextView) itemView.findViewById(R.id.fachValueText);
        }
    }
}
