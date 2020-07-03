package de.conradowatz.jkgvertretung.adapters;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.variables.Fach;

public class NextLessonRecyclerAdapter extends RecyclerView.Adapter<NextLessonRecyclerAdapter.ViewHolder> {

    private List<Date> dateList;
    private Fach fach;
    private Callback callback;

    public NextLessonRecyclerAdapter(Fach fach, Callback callback) {

        this.callback = callback;
        this.fach = fach;
        dateList = new ArrayList<>();

        if (!fach.hasStunden()) return;

        loadMoreDays(0, Utilities.getToday().getTime());

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nextlesson, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateList.get(position));

        holder.dateText.setText(calendar.get(Calendar.DAY_OF_MONTH) + "." + (calendar.get(Calendar.MONTH) + 1) + "." + calendar.get(Calendar.YEAR));
        holder.wochentagText.setText(calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.GERMAN));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onDateClicked(calendar.getTime());
            }
        });

        holder.itemView.post(new Runnable() {
            @Override
            public void run() {
                //Load more if scrolled to End
                if (holder.getAdapterPosition() == dateList.size() - 1) {

                    loadMoreDays(holder.getAdapterPosition(), dateList.get(holder.getAdapterPosition()));

                }
            }
        });
    }

    private void loadMoreDays(int startPosition, Date startDate) {

        dateList.addAll(fach.getUnterrichtDaten(startDate));
        notifyItemRangeInserted(startPosition + 1, startPosition + 21);

    }

    @Override
    public int getItemCount() {
        if (fach.hasStunden()) return dateList.size();
        else return 0;
    }

    public interface Callback {

        void onDateClicked(Date date);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView wochentagText;
        public TextView dateText;
        public View itemView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            wochentagText = (TextView) itemView.findViewById(R.id.wochentagText);
            dateText = (TextView) itemView.findViewById(R.id.dateText);
        }
    }
}
