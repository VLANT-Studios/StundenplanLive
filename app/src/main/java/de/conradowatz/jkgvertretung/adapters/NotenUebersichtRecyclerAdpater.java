package de.conradowatz.jkgvertretung.adapters;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Fach;

public class NotenUebersichtRecyclerAdpater extends RecyclerView.Adapter<NotenUebersichtRecyclerAdpater.ViewHolder> {

    private List<Fach> fachList;
    private DecimalFormat decimalFormat = new DecimalFormat("#0.00");
    private Callback callback;
    private Context context;

    public NotenUebersichtRecyclerAdpater(Callback callback, Context context) {

        this.callback = callback;
        this.context = context;

        calculateItems();
    }

    private void calculateItems() {

        fachList = new ArrayList<>();

        for (Fach f : LocalData.getInstance().getFächer()) {
            Double average = f.getNotenAverage();
            if (average != null) {
                fachList.add(f);
            }
        }

        final boolean isOberstufe = LocalData.isOberstufe(context);
        Collections.sort(fachList, new Comparator<Fach>() {
            @Override
            public int compare(Fach f1, Fach f2) {
                return isOberstufe ? f2.getNotenAverage().compareTo(f1.getNotenAverage()) : f1.getNotenAverage().compareTo(f2.getNotenAverage());
            }
        });

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
        holder.fachValueText.setText(decimalFormat.format(fach.getNotenAverage()));
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onFachClicked(LocalData.getInstance().getFächer().indexOf(fach));
            }
        });
    }

    @Override
    public int getItemCount() {
        return fachList.size();
    }

    public interface Callback {

        void onFachClicked(int fachIndex);
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
