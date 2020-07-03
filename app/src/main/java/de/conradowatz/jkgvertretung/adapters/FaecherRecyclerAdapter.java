package de.conradowatz.jkgvertretung.adapters;

import android.os.AsyncTask;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.variables.Fach;

public class FaecherRecyclerAdapter extends RecyclerView.Adapter<FaecherRecyclerAdapter.ViewHolder> {

    private Callback callback;
    private List<Fach> faecher;

    public FaecherRecyclerAdapter(Callback callback) {

        setHasStableIds(true);
        updateData();

        this.callback = callback;
    }

    public void updateData() {

        new AsyncTask<Boolean, Integer, List<Fach>>() {
            @Override
            protected List<Fach> doInBackground(Boolean... params) {
                return SQLite.select().from(Fach.class).queryList();
            }

            @Override
            protected void onPostExecute(List<Fach> f) {
                faecher = f;
                notifyDataSetChanged();
            }
        }.execute();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faecher_fach, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        holder.textView.setText(faecher.get(position).getName());
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onFachClicked(faecher.get(holder.getAdapterPosition()).getId());
            }
        });
        holder.textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                callback.onFachLongClicked(faecher.get(holder.getAdapterPosition()).getId());
                return true;
            }
        });

        if (position == getItemCount() - 1) holder.divider.setVisibility(View.GONE);
        else holder.divider.setVisibility(View.VISIBLE);

    }

    @Override
    public int getItemCount() {
        return faecher==null?0:faecher.size();
    }

    @Override
    public long getItemId(int position) {
        return faecher.get(position).getId();
    }

    public interface Callback {

        void onFachClicked(long fachId);

        void onFachLongClicked(long fachId);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;
        View divider;

        public ViewHolder(View itemView) {
            super(itemView);

            textView = (TextView) itemView.findViewById(R.id.textView);
            divider = itemView.findViewById(R.id.divider);
        }
    }
}
