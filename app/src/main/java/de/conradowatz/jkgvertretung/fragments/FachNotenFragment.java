package de.conradowatz.jkgvertretung.fragments;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.adapters.EinzelNotenRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.NotenChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Fach;

public class FachNotenFragment extends Fragment implements EinzelNotenRecyclerAdapter.Callback {

    private View contentView;

    private Fach fach;
    private String klausurenString;

    private RecyclerView sonstigeRecycler;
    private RecyclerView klausurenRecycler;
    private TextView sonstigeText;
    private TextView klausurenText;
    private TextView gesamtText;

    private boolean isNoteAddDialog;
    private boolean isNoteDeleteDialog;
    private int dialogType;
    private int dialogPosition;

    private EventBus eventBus = EventBus.getDefault();

    public FachNotenFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_fach_noten, container, false);
        sonstigeRecycler = (RecyclerView) contentView.findViewById(R.id.sonstigeRecycler);
        klausurenRecycler = (RecyclerView) contentView.findViewById(R.id.klausurenRecycler);
        sonstigeText = (TextView) contentView.findViewById(R.id.sonstigeValueText);
        klausurenText = (TextView) contentView.findViewById(R.id.klausurenValueText);
        gesamtText = (TextView) contentView.findViewById(R.id.gesamtValueText);
        TextView klausurenText = (TextView) contentView.findViewById(R.id.klausurenLabelText);
        TextView klausurenDurchscnittText = (TextView) contentView.findViewById(R.id.klausurenDurchschnittLabelText);

        fach = ((FachActivity) getActivity()).getFach();
        klausurenString = LocalData.isOberstufe(getActivity().getApplicationContext()) ? "Klausuren" : "Klassenarbeiten";
        klausurenText.setText(klausurenString);
        klausurenDurchscnittText.setText("Durchschnitt " + klausurenString);

        if (savedInstanceState != null) {

            isNoteAddDialog = savedInstanceState.getBoolean("isNoteAddDialog");
            isNoteDeleteDialog = savedInstanceState.getBoolean("isNoteDeleteDialog");
            dialogType = savedInstanceState.getInt("dialogType");
            dialogPosition = savedInstanceState.getInt("dialogPosition");
            if (isNoteAddDialog) onAddClicked(dialogType);
            if (isNoteDeleteDialog) onNoteLongClick(dialogPosition, dialogType);
        }

        //setUpRecyclers wird erst gecallt, wenn der RecyclerView ausgelegt ist, damit getWidth funktioniert
        ViewTreeObserver observer = sonstigeRecycler.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                sonstigeRecycler.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                setUpRecyclers();
            }
        });

        calculateAverage();

        return contentView;
    }

    private void setUpRecyclers() {

        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 95, r.getDisplayMetrics());

        int columCount = (int) Math.floor(sonstigeRecycler.getWidth() / px);

        RecyclerView.LayoutManager sLayoutManager = new GridLayoutManager(getActivity(), columCount);
        RecyclerView.LayoutManager kLayoutManager = new GridLayoutManager(getActivity(), columCount);

        sonstigeRecycler.setLayoutManager(sLayoutManager);
        klausurenRecycler.setLayoutManager(kLayoutManager);
        EinzelNotenRecyclerAdapter sAdapter = new EinzelNotenRecyclerAdapter(getActivity().getApplicationContext(), fach.getSonstigeNoten(), this, EinzelNotenRecyclerAdapter.NOTEN_TYPE_SONSTIGE);
        EinzelNotenRecyclerAdapter kAdapter = new EinzelNotenRecyclerAdapter(getActivity().getApplicationContext(), fach.getKlausurenNoten(), this, EinzelNotenRecyclerAdapter.NOTEN_TYPE_KLAUSUREN);
        sonstigeRecycler.setAdapter(sAdapter);
        klausurenRecycler.setAdapter(kAdapter);

    }

    private void calculateAverage() {

        Double sonstigeAverage = fach.getSonstigeAverage();
        Double klausurenAverage = fach.getKlausurenAverage();
        Double gesamtAverage = fach.getNotenAverage();
        sonstigeText.setText(sonstigeAverage == null ? "n.A." : String.valueOf(sonstigeAverage));
        klausurenText.setText(klausurenAverage == null ? "n.A." : String.valueOf(klausurenAverage));
        gesamtText.setText(gesamtAverage == null ? "n.A." : String.valueOf(gesamtAverage));
    }

    @Override
    public void onNoteLongClick(final int position, final int type) {

        isNoteDeleteDialog = true;
        dialogType = type;
        dialogPosition = position;

        int note;
        if (type == EinzelNotenRecyclerAdapter.NOTEN_TYPE_SONSTIGE)
            note = fach.getSonstigeNoten().get(position);
        else note = fach.getKlausurenNoten().get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Note entfernen");
        builder.setMessage("Note " + String.valueOf(note) + " aus '" + (type == EinzelNotenRecyclerAdapter.NOTEN_TYPE_SONSTIGE ? "Sonstige Noten" : klausurenString) + "' entfernen?");
        builder.setPositiveButton("Entfernen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (type == EinzelNotenRecyclerAdapter.NOTEN_TYPE_SONSTIGE) {
                    fach.getSonstigeNoten().remove(position);
                    sonstigeRecycler.getAdapter().notifyItemRemoved(position);
                } else {
                    fach.getKlausurenNoten().remove(position);
                    klausurenRecycler.getAdapter().notifyItemRemoved(position);
                }
                calculateAverage();
                eventBus.post(new NotenChangedEvent());
                isNoteDeleteDialog = false;
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isNoteDeleteDialog = false;
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isNoteDeleteDialog = false;
            }
        });
        builder.show();

    }

    @Override
    public void onAddClicked(final int type) {

        isNoteAddDialog = true;
        dialogType = type;

        final String[] noten;
        if (LocalData.isOberstufe(getActivity().getApplicationContext())) {
            noten = new String[16];
            for (int i = 0; i <= 15; i++) noten[i] = String.valueOf(15 - i);
        } else {
            noten = new String[6];
            for (int i = 1; i <= 6; i++) noten[i - 1] = String.valueOf(i);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Note hinzufügen");
        builder.setItems(noten, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (type == EinzelNotenRecyclerAdapter.NOTEN_TYPE_SONSTIGE) {
                    fach.getSonstigeNoten().add(Integer.valueOf(noten[which]));
                    sonstigeRecycler.getAdapter().notifyItemInserted(fach.getSonstigeNoten().size() - 1);
                } else {
                    fach.getKlausurenNoten().add(Integer.valueOf(noten[which]));
                    klausurenRecycler.getAdapter().notifyItemInserted(fach.getKlausurenNoten().size() - 1);
                }
                calculateAverage();
                eventBus.post(new NotenChangedEvent());
                isNoteAddDialog = false;
            }
        });
        builder.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isNoteAddDialog = false;
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isNoteAddDialog = false;
            }
        });
        builder.show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putBoolean("isNoteAddDialog", isNoteAddDialog);
        outState.putBoolean("isNoteDeleteDialog", isNoteDeleteDialog);
        outState.putInt("dialogType", dialogType);
        outState.putInt("dialogPosition", dialogPosition);

        super.onSaveInstanceState(outState);
    }
}
