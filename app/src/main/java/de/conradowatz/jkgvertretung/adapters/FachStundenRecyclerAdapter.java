package de.conradowatz.jkgvertretung.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.fragments.FachStundenFragment;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Fach;

public class FachStundenRecyclerAdapter extends RecyclerView.Adapter<FachStundenRecyclerAdapter.ViewHolder> {

    private static final int TYPE_TOP = 0;
    private static final int TYPE_LEFT = 1;
    private static final int TYPE_NORMAL = 2;

    private Context context;
    private Fach fach;
    private boolean[][] stunden;
    private boolean[][] belegt;
    private Callback callback;

    public FachStundenRecyclerAdapter(Context context, Fach fach, int state, Callback callback) {

        this.context = context;
        if (state != FachStundenFragment.STATE_IMMER) {
            stunden = fach.getStunden(state == FachStundenFragment.STATE_AWOCHE);
            belegt = LocalData.getBelegteStunden(fach, state == FachStundenFragment.STATE_AWOCHE);
        } else {
            this.fach = fach;
            calculateStateImmer();
        }

        this.callback = callback;
    }

    public void calculateStateImmer() {

        stunden = new boolean[5][9];
        belegt = new boolean[5][9];
        boolean[][] belegtCalcA = LocalData.getBelegteStunden(fach, true);
        boolean[][] belegtCalcB = LocalData.getBelegteStunden(fach, false);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 9; j++) {
                stunden[i][j] = fach.getaStunden()[i][j] && fach.getbStunden()[i][j];
                belegt[i][j] = belegtCalcA[i][j] || belegtCalcB[i][j];
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view;
        switch (viewType) {
            case TYPE_TOP:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.einzelstunde_label_top_item, parent, false);
                break;
            case TYPE_LEFT:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.einzelstunde_label_left_item, parent, false);
                break;
            default:
                TYPE_NORMAL:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.einzelstunde_item, parent, false);
        }
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        int viewType = getItemViewType(holder.getAdapterPosition());
        if (viewType == TYPE_NORMAL) {
            final int[] i = getItemPosition(holder.getAdapterPosition());
            if (stunden[i[0]][i[1]]) {
                holder.backgroundView.setBackgroundResource(R.drawable.round_shape_yellow);
                holder.imageView.setImageResource(R.drawable.ic_done);
                holder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.onRemoveStunde(i[0], i[1], holder.getAdapterPosition());
                    }
                });
            } else if (belegt[i[0]][i[1]]) {
                holder.backgroundView.setBackgroundResource(R.drawable.round_shape_blue);
                holder.imageView.setImageResource(R.drawable.ic_clear);
                holder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.onReplaceStunde(i[0], i[1], holder.getAdapterPosition());
                    }
                });
            } else {
                holder.backgroundView.setBackgroundResource(R.drawable.round_shape_white);
                holder.imageView.setImageResource(0);
                holder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callback.onNewStunde(i[0], i[1], holder.getAdapterPosition());
                    }
                });
            }
        } else if (viewType == TYPE_LEFT) {
            int stunde = (int) Math.floor(holder.getAdapterPosition() / 6);
            if (stunde > 0) holder.textView.setText(String.valueOf(stunde));
            else holder.textView.setText("");
        } else if (viewType == TYPE_TOP) {
            int tag = holder.getAdapterPosition() % 6 - 1;
            String tagString = "";
            switch (tag) {
                case 0:
                    tagString = "Mo";
                    break;
                case 1:
                    tagString = "Di";
                    break;
                case 2:
                    tagString = "Mi";
                    break;
                case 3:
                    tagString = "Do";
                    break;
                case 4:
                    tagString = "Fr";
                    break;
            }
            holder.textView.setText(tagString);
        }

    }

    @Override
    public int getItemViewType(int position) {

        if (position % 6 == 0) return TYPE_LEFT;
        else if (position <= 5) return TYPE_TOP;
        return TYPE_NORMAL;
    }

    @Override
    public int getItemCount() {
        return 6 * 10;
    }

    private int[] getItemPosition(int position) {

        int[] i = new int[2];
        i[0] = (position % 6) - 1;
        i[1] = (int) Math.floor(position / 6) - 1;
        return i;

    }

    public interface Callback {

        void onNewStunde(int tag, int stunde, int pos);

        void onRemoveStunde(int tag, int stunde, int pos);

        void onReplaceStunde(int tag, int stunde, int pos);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        View backgroundView;
        TextView textView;

        public ViewHolder(View itemView, int viewType) {
            super(itemView);
            if (viewType != TYPE_NORMAL) textView = (TextView) itemView;
            else {
                imageView = (ImageView) itemView.findViewById(R.id.imageView);
                backgroundView = itemView.findViewById(R.id.backgroundView);
            }
        }
    }
}
