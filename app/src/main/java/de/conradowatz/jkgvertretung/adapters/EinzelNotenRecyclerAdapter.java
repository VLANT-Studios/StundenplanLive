package de.conradowatz.jkgvertretung.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

import de.conradowatz.jkgvertretung.R;

public class EinzelNotenRecyclerAdapter extends RecyclerView.Adapter<EinzelNotenRecyclerAdapter.ViewHolder> {

    public static final int NOTEN_TYPE_SONSTIGE = 0;
    public static final int NOTEN_TYPE_KLAUSUREN = 1;
    private static final int ITEM_TYPE_NORMAL = 0;
    private static final int ITEM_TYPE_ADD = 1;
    private List<Integer> notenListe;
    private Context context;
    private Callback callback;
    private int type;

    public EinzelNotenRecyclerAdapter(Context context, List<Integer> notenListe, Callback callback, int type) {

        this.notenListe = notenListe;
        this.context = context;
        this.callback = callback;
        this.type = type;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_einzelnote, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        if (getItemViewType(position) == ITEM_TYPE_NORMAL) {
            holder.button.setText(String.valueOf(notenListe.get(position)));
            holder.button.setBackgroundColor(ContextCompat.getColor(context, R.color.accent));
            holder.button.setOnClickListener(null);
            holder.button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    callback.onNoteLongClick(holder.getAdapterPosition(), type);
                    return false;
                }
            });
        } else {
            holder.button.setText("+");
            holder.button.setBackgroundColor(ContextCompat.getColor(context, R.color.md_green_200));
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onAddClicked(type);
                }
            });
            holder.button.setOnLongClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return notenListe.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == getItemCount() - 1 ? ITEM_TYPE_ADD : ITEM_TYPE_NORMAL);
    }

    public interface Callback {

        void onNoteLongClick(int position, int type);

        void onAddClicked(int type);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        Button button;

        public ViewHolder(View itemView) {
            super(itemView);
            button = (Button) itemView.findViewById(R.id.button);
        }
    }
}
