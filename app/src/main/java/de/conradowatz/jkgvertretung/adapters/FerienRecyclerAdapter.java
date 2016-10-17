package de.conradowatz.jkgvertretung.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.variables.Ferien;

public class FerienRecyclerAdapter extends RecyclerView.Adapter<FerienRecyclerAdapter.ViewHolder> {

    private Callback callback;

    public FerienRecyclerAdapter(Callback callback) {
        this.callback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ferien, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        Ferien ferien = LocalData.getInstance().getFerien().get(position);
        Calendar cNow = Calendar.getInstance();

        holder.nameText.setText(ferien.getName());
        if (Utilities.compareDays(ferien.getStartDate(), cNow.getTime()) > 0) {
            holder.infoText.setText(Utilities.dayDifferenceToString(Utilities.getDayDifference(cNow.getTime(), ferien.getStartDate())));
        } else if (Utilities.compareDays(ferien.getEndDate(), cNow.getTime()) >= 0) {
            int dayDiff = Utilities.getDayDifference(cNow.getTime(), ferien.getStartDate());
            if (dayDiff == 1)
                holder.infoText.setText("noch 1 Tag");
            else holder.infoText.setText(String.format(Locale.GERMANY, "noch %s Tage", dayDiff));
        } else holder.infoText.setText("vergangen");

        holder.dateText.setText(ferien.getDateString());

        holder.deleteButtonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onDeleteClicked(holder.getAdapterPosition());
            }
        });
        holder.editButtontext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onEditClicked(holder.getAdapterPosition());
            }
        });

    }

    @Override
    public int getItemCount() {
        return LocalData.getInstance().getFerien().size();
    }

    @Override
    public long getItemId(int position) {

        Ferien ferien = LocalData.getInstance().getFerien().get(position);
        return ferien.hashCode();
    }

    public interface Callback {
        void onEditClicked(int pos);

        void onDeleteClicked(int pos);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView nameText;
        TextView infoText;
        TextView dateText;
        TextView editButtontext;
        TextView deleteButtonText;

        public ViewHolder(View itemView) {
            super(itemView);

            nameText = (TextView) itemView.findViewById(R.id.nameText);
            infoText = (TextView) itemView.findViewById(R.id.infoText);
            dateText = (TextView) itemView.findViewById(R.id.dateText);
            editButtontext = (TextView) itemView.findViewById(R.id.editButtonText);
            deleteButtonText = (TextView) itemView.findViewById(R.id.deleteButtonText);
        }
    }
}
