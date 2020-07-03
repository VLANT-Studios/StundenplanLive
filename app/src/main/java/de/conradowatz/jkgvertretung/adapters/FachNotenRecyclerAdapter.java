package de.conradowatz.jkgvertretung.adapters;

import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.variables.Fach;
import de.conradowatz.jkgvertretung.variables.Zensur;

public class FachNotenRecyclerAdapter extends RecyclerView.Adapter<FachNotenRecyclerAdapter.ViewHolder> {

    public static final int NOTEN_TYPE_SONSTIGE = 0;
    public static final int NOTEN_TYPE_KLAUSUREN = 1;
    private static final int ITEM_TYPE_NORMAL = 0;
    private static final int ITEM_TYPE_ADD = 1;
    private List<Zensur> notenListe;
    private Fach fach;
    private Callback callback;
    private int type;

    public FachNotenRecyclerAdapter(Fach fach, Callback callback, int type) {

        this.fach = fach;
        this.callback = callback;
        this.type = type;
        updateData();
    }

    public void updateData() {

        if (type==NOTEN_TYPE_SONSTIGE) notenListe = fach.getTests();
        else notenListe = fach.getKlausuren();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_einzelnote, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        Context context = MyApplication.getAppContext();

        if (getItemViewType(position) == ITEM_TYPE_NORMAL) {
            holder.button.setText(String.valueOf(notenListe.get(position).getZensur()));
            holder.button.setBackgroundColor(ContextCompat.getColor(context, R.color.accent));
            holder.button.setOnClickListener(null);
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.onNoteClick(holder.getAdapterPosition(), type);
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

        void onNoteClick(int position, int type);

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
