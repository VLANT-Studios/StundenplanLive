package de.conradowatz.jkgvertretung.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
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

        Ferien ferien = LocalData.getInstance().getFerien().get(holder.getAdapterPosition());
        Calendar cNow = Calendar.getInstance();
        Calendar cFbegin = Calendar.getInstance();
        Calendar cFend = Calendar.getInstance();
        cFbegin.setTime(ferien.getStartDate());
        cFend.setTime(ferien.getEndDate());

        holder.nameText.setText(ferien.getName());
        if (Utilities.compareDays(cFbegin, cNow) > 0)
            holder.infoText.setText("in " + Math.abs(cNow.get(Calendar.DAY_OF_YEAR) - cFbegin.get(Calendar.DAY_OF_YEAR)) + " Tag(en)");
        else if (Utilities.compareDays(cFend, cNow) >= 0)
            holder.infoText.setText("noch " + Math.abs(cNow.get(Calendar.DAY_OF_YEAR) - cFend.get(Calendar.DAY_OF_YEAR)) + " Tag(e)");
        else holder.infoText.setText("vergangen");

        holder.dateText.setText("vom " + getDateString(cFbegin.getTime()) + "\n\tbis " + getDateString(cFend.getTime()));

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

    private String getDateString(Date datum) {

        Calendar c = Calendar.getInstance();
        c.setTime(datum); //12. September 2016
        return c.get(Calendar.DAY_OF_MONTH) + ". " + c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMAN) + " " + c.get(Calendar.YEAR);
    }

    @Override
    public int getItemCount() {
        return LocalData.getInstance().getFerien().size();
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
