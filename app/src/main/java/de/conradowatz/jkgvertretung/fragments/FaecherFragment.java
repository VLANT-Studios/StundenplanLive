package de.conradowatz.jkgvertretung.fragments;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.FachActivity;
import de.conradowatz.jkgvertretung.activities.KurswahlActivity;
import de.conradowatz.jkgvertretung.adapters.FaecherRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.Fach;

public class FaecherFragment extends Fragment implements FaecherRecyclerAdapter.Callback {

    private static final int MODE_NOFAECHER = 1;
    private static final int MODE_NORMAL = 2;
    private View contentView;
    private RecyclerView recyclerView;
    private LinearLayout nofaecherLayout;
    private Button smartImportButton;
    private Button kurswahlButton;
    private boolean isDeleteDialog;
    private long deleteDialogFachId;
    private boolean isChooseAWocheDialog;
    private int mode;
    private EventBus eventBus = EventBus.getDefault();

    public FaecherFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_faecher, container, false);
        recyclerView = (RecyclerView) contentView.findViewById(R.id.recyclerView);
        nofaecherLayout = (LinearLayout) contentView.findViewById(R.id.nofaecherLayout);
        smartImportButton = (Button) contentView.findViewById(R.id.smartImportButton);
        kurswahlButton = (Button) contentView.findViewById(R.id.kurswahlButton);

        setHasOptionsMenu(true);

        eventBus.register(this);

        setUp();

        if (savedInstanceState != null) {

            if (savedInstanceState.getBoolean("isDeleteDialog"))
                showDeleteDialog(savedInstanceState.getLong("deleteDialogFachId"));
            if (savedInstanceState.getBoolean("isChooseAWocheDialog"))
                showChooseAWocheDialog();
            mode = savedInstanceState.getInt("mode");
        }

        return contentView;
    }

    private void setUp() {

        new AsyncTask<Boolean, Integer, Boolean>() {

            @Override
            protected Boolean doInBackground(Boolean... params) {
                return Fach.hasFaecher();
            }

            @Override
            protected void onPostExecute(Boolean hasFaecher) {

                if (!hasFaecher) {

                    mode = MODE_NOFAECHER;

                    recyclerView.setVisibility(View.GONE);
                    nofaecherLayout.setVisibility(View.VISIBLE);
                    smartImportButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showSmartImportDialog();
                        }
                    });
                    kurswahlButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startKurswahlActivity();
                        }
                    });
                } else {

                    mode = MODE_NORMAL;

                    recyclerView.setVisibility(View.VISIBLE);
                    nofaecherLayout.setVisibility(View.GONE);
                    setUpRecycler();
                }

            }
        }.execute();
    }

    private void setUpRecycler() {

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        FaecherRecyclerAdapter adapter = new FaecherRecyclerAdapter(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

    }

    @Override
    public void onFachClicked(long fachId) {

        startFachActivity(fachId);

    }

    @Override
    public void onFachLongClicked(long fachId) {

        showDeleteDialog(fachId);

    }

    private void showDeleteDialog(final long fachId) {

        isDeleteDialog = true;
        deleteDialogFachId = fachId;

        final Fach fach = Fach.getFach(fachId);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("'" + fach.getName() + "' löschen");
        builder.setMessage("Bist du sicher dass du dieses Fach löschen möchtest?");
        builder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDeleteDialog = false;

                fach.delete();
                eventBus.post(new FaecherUpdateEvent());
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDeleteDialog = false;
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isDeleteDialog = false;
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.warn_text));
            }
        });
        dialog.show();

    }

    private void startFachActivity(long fachId) {

        Intent openFachIntent = new Intent(getContext(), FachActivity.class);
        openFachIntent.putExtra("fachId", fachId);
        startActivity(openFachIntent);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(FaecherUpdateEvent event) {

        if (recyclerView != null) {
            boolean faecherExist = SQLite.select().from(Fach.class).querySingle()!=null;
            if ((mode == MODE_NORMAL && !faecherExist) || (mode == MODE_NOFAECHER && faecherExist))
                setUp();
            else if (recyclerView.getAdapter() != null){
                ((FaecherRecyclerAdapter)recyclerView.getAdapter()).updateData();
            }
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_faecher, menu);
        menu.findItem(R.id.action_abwoche).setVisible(LocalData.hasABWoche());
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_recognizefaecher) {

            showSmartImportDialog();
        } else if (id == R.id.action_kurswahl) {

            startKurswahlActivity();
        } else if (id == R.id.action_abwoche) {

            showChooseAWocheDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void showChooseAWocheDialog() {

        isChooseAWocheDialog = true;

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("A / B Woche");
        dialogBuilder.setMessage("Bitte wähle einen OnlineTag aus, bei dem du sicher bist, dass es A-Woche war/ist.");
        dialogBuilder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                final Calendar calendar = Calendar.getInstance();
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {

                        isChooseAWocheDialog = false;

                        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                        LocalData.setCompareDate(calendar.getTime());

                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void startKurswahlActivity() {

        Intent openKurswahlIntent = new Intent(getActivity(), KurswahlActivity.class);
        startActivity(openKurswahlIntent);

    }

    private void showSmartImportDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Smart Import");
        dialogBuilder.setMessage(R.string.smart_import_desc);
        dialogBuilder.setPositiveButton("Importieren", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                LocalData.smartImport(getActivity());
                eventBus.post(new FaecherUpdateEvent());
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", null);
        dialogBuilder.show();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putBoolean("isDeleteDialog", isDeleteDialog);
        outState.putBoolean("isChooseAWocheDialog", isChooseAWocheDialog);
        outState.putLong("deleteDialogFachId", deleteDialogFachId);
        outState.putInt("mode", mode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        eventBus.unregister(this);
    }
}
