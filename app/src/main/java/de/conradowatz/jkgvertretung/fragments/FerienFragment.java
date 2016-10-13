package de.conradowatz.jkgvertretung.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.ManagerActivity;
import de.conradowatz.jkgvertretung.adapters.FerienRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.FerienChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;

public class FerienFragment extends Fragment implements FerienRecyclerAdapter.Callback {

    private View contentView;

    private boolean isDeleteDialog;
    private int deleteFerienIndex;
    private RecyclerView recyclerView;
    private EventBus eventBus = EventBus.getDefault();

    public FerienFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_ferien, container, false);
        recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("isDeleteDialog"))
                showDeleteDialog(savedInstanceState.getInt("deleteFerienIndex"));
        }

        eventBus.register(this);

        setUpRecycler();

        return contentView;
    }

    private void setUpRecycler() {

        RecyclerView.LayoutManager lManager = new LinearLayoutManager(getActivity());
        FerienRecyclerAdapter adapter = new FerienRecyclerAdapter(this);
        recyclerView.setLayoutManager(lManager);
        recyclerView.setAdapter(adapter);
    }

    @Subscribe
    public void onEvent(FerienChangedEvent event) {
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        eventBus.unregister(this);
        super.onDestroyView();
    }

    @Override
    public void onEditClicked(int pos) {
        ManagerActivity managerActivity = (ManagerActivity) getActivity();
        managerActivity.showNewFerienActivity(pos);
    }

    @Override
    public void onDeleteClicked(int pos) {
        showDeleteDialog(pos);
    }

    private void showDeleteDialog(final int ferienIndex) {

        isDeleteDialog = true;
        deleteFerienIndex = ferienIndex;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Löschen bestätigen");
        dialogBuilder.setMessage("Bist du sicher, dass du '" + LocalData.getInstance().getFerien().get(ferienIndex).getName() + "' löschen willst?");
        dialogBuilder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDeleteDialog = false;

                LocalData.getInstance().getFerien().remove(ferienIndex);
                LocalData.saveToFile(getActivity().getApplicationContext());
                eventBus.post(new FerienChangedEvent());
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDeleteDialog = false;
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isDeleteDialog = false;
            }
        });
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.warn_text));
            }
        });
        dialog.show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isDeleteDialog", isDeleteDialog);
        outState.putInt("deleteFerienIndex", deleteFerienIndex);
        super.onSaveInstanceState(outState);
    }
}
