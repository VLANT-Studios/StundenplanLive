package de.conradowatz.jkgvertretung.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;

public class ReminderRecyclerAdapter extends RecyclerView.Adapter<ReminderRecyclerAdapter.ViewHolder> {

    private static final int VIEWTYPE_ITEM = 0;
    private static final int VIEWTYPE_ADD = 1;
    private List<Date> reminders;
    private Callback callback;

    public ReminderRecyclerAdapter(List<Date> reminders, Callback callback) {
        this.reminders = reminders;
        this.callback = callback;
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
            holder.datumText.setText(makeDateString(reminders.get(position)));
            holder.uhrzeitText.setText(makeTimeString(reminders.get(position)));
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

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.GERMAN) + ", " + c.get(Calendar.DAY_OF_MONTH) + ". " + c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.GERMAN) + " " + c.get(Calendar.YEAR);
    }

    private String makeTimeString(Date date) {

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return String.format(Locale.GERMANY, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    @Override
    public int getItemViewType(int position) {
        return position == reminders.size() ? VIEWTYPE_ADD : VIEWTYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return reminders.size() + 1;
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
