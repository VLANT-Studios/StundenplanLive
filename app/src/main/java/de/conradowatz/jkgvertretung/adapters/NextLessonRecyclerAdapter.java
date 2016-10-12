package de.conradowatz.jkgvertretung.adapters;

import android.support.v7.widget.RecyclerView;
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
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.variables.Fach;

public class NextLessonRecyclerAdapter extends RecyclerView.Adapter<NextLessonRecyclerAdapter.ViewHolder> {

    private List<Date> dateList;
    private Fach fach;
    private Callback callback;

    public NextLessonRecyclerAdapter(Fach fach, Callback callback) {

        this.callback = callback;
        this.fach = fach;
        dateList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        Date schoolDay = calendar.getTime();
        if (VertretungsAPI.isntSchoolDay(schoolDay))
            schoolDay = VertretungsAPI.nextSchoolDay(schoolDay);
        calendar.setTime(schoolDay);

        if (getItemCount() == 0) return;

        int i = 0;
        while (i < 20) {
            boolean isAWoche = LocalData.getInstance().isAWoche(calendar.getTime());
            int dayOfWeek = LocalData.getDayOfWeek(calendar.getTime()) - 1;
            for (int j = 0; j < 9; j++) {
                if (fach.getStunden(isAWoche)[dayOfWeek][j]) {
                    dateList.add(calendar.getTime());
                    i++;
                    break;
                }
            }
            calendar.setTime(VertretungsAPI.nextSchoolDay(calendar.getTime()));
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nextlesson, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

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
    }

    @Override
    public int getItemCount() {
        if (fach.hasStunden()) return 20;
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
