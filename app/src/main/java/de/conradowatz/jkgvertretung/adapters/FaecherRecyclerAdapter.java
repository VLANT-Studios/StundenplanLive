package de.conradowatz.jkgvertretung.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.LocalData;

public class FaecherRecyclerAdapter extends RecyclerView.Adapter<FaecherRecyclerAdapter.ViewHolder> {

    private Callback callback;

    public FaecherRecyclerAdapter(Callback callback) {

        this.callback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faecher_fach, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        holder.textView.setText(LocalData.getInstance().getFächer().get(position).getName());
        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.onFachClicked(holder.getAdapterPosition());
            }
        });
        holder.textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                callback.onFachLongClicked(holder.getAdapterPosition());
                return true;
            }
        });

        if (position == getItemCount() - 1) holder.divider.setVisibility(View.GONE);
        else holder.divider.setVisibility(View.VISIBLE);

    }

    @Override
    public int getItemCount() {
        return LocalData.getInstance().getFächer().size();
    }

    public interface Callback {

        void onFachClicked(int fachIndex);

        void onFachLongClicked(int fachIndex);
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
