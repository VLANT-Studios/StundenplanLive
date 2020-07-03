package de.conradowatz.jkgvertretung.adapters;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.variables.Erinnerung;

public class ReminderRecyclerAdapter extends RecyclerView.Adapter<ReminderRecyclerAdapter.ViewHolder> {

    private static final int VIEWTYPE_ITEM = 0;
    private static final int VIEWTYPE_ADD = 1;
    private List<Erinnerung> erinnerungen;
    private Callback callback;

    public ReminderRecyclerAdapter(List<Erinnerung> erinnerungen, Callback callback) {
        this.callback = callback;

        updateData(erinnerungen);
    }

    public void updateData(List<Erinnerung> erinnerungen) {

        this.erinnerungen = erinnerungen;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (viewType == VIEWTYPE_ITEM)
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        else
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder_add, parent, false);
        return new ViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        if (getItemViewType(position) == VIEWTYPE_ITEM) {
            holder.datumText.setText(makeDateString(erinnerungen.get(position).getDate()));
            holder.uhrzeitText.setText(makeTimeString(erinnerungen.get(position).getDate()));
            holder.datumText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onDateClicked(holder.getAdapterPosition());
                }
            });
            holder.uhrzeitText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onTimeClicked(holder.getAdapterPosition());
                }
            });
            holder.deleteImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onDeleteClicked(holder.getAdapterPosition());
                }
            });
        } else {
            holder.addText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onAddClicked();
                }
            });
        }

    }

    private String makeDateString(Date date) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("EE, dd. MMM yyyy", Locale.GERMAN);
        return dateFormat.format(date);
    }

    private String makeTimeString(Date date) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.GERMAN);
        return dateFormat.format(date);
    }

    @Override
    public int getItemViewType(int position) {
        return position == erinnerungen.size() ? VIEWTYPE_ADD : VIEWTYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return erinnerungen.size() + 1;
    }

    public interface Callback {

        void onDateClicked(int pos);

        void onTimeClicked(int pos);

        void onDeleteClicked(int pos);

        void onAddClicked();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView datumText;
        TextView uhrzeitText;
        ImageView deleteImage;
        TextView addText;

        public ViewHolder(View itemView, int viewType) {
            super(itemView);

            if (viewType == VIEWTYPE_ITEM) {
                datumText = (TextView) itemView.findViewById(R.id.datumText);
                uhrzeitText = (TextView) itemView.findViewById(R.id.uhrzeitText);
                deleteImage = (ImageView) itemView.findViewById(R.id.deleteImage);
            } else addText = (TextView) itemView.findViewById(R.id.addText);
        }
    }
}
