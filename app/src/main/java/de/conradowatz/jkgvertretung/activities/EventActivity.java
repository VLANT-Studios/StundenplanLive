package de.conradowatz.jkgvertretung.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.NextLessonRecyclerAdapter;
import de.conradowatz.jkgvertretung.adapters.ReminderRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.tools.ColorAPI;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.variables.Erinnerung;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;

public class EventActivity extends AppCompatActivity implements ReminderRecyclerAdapter.Callback {

    private Event event;
    private long eventId;
    private List<Erinnerung> erinnerungen;

    private boolean isSaveDialog;
    private boolean isDatePickerDialog;
    private boolean isPickStundeDialog;
    private boolean isDeleteDialog;

    private boolean isReminderTimePickerDialog;
    private int reminderDialogIndex;

    private Toolbar toolbar;
    private EditText nameEdit;
    private TextView datumText;
    private TextView fachText;
    private TextView stundenAuswahlText;
    private EditText descEdit;
    private SwitchCompat deleteSwitch;
    private RecyclerView reminderRecycler;

    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_event);

        ColorAPI api = new ColorAPI(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(api.getActionBarColor()));
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);

        findViewById(R.id.event_linear_layout).setBackgroundColor(api.getActionBarColor());
        nameEdit = (EditText) findViewById(R.id.nameEdit);
        nameEdit.setBackgroundColor(api.getActionBarColor());
        datumText = (TextView) findViewById(R.id.datumText);
        fachText = (TextView) findViewById(R.id.fachText);
        fachText.setBackgroundColor(api.getActionBarColor());
        stundenAuswahlText = (TextView) findViewById(R.id.stundenAuswahlText);
        descEdit = (EditText) findViewById(R.id.descEdit);
        deleteSwitch = (SwitchCompat) findViewById(R.id.deleteSwitch);
        reminderRecycler = (RecyclerView) findViewById(R.id.reminderRecycler);

        if (savedInstanceState != null) {

            eventId = savedInstanceState.getLong("eventId", -1);
            event = savedInstanceState.getParcelable("event");

            erinnerungen = new ArrayList<>();
            for (String s : savedInstanceState.getStringArrayList("erinnerungen")) {
                erinnerungen.add(new Erinnerung(new Date(Long.valueOf(s)), event));
            }

            if (savedInstanceState.getBoolean("isSaveDialog")) showSaveDialog();
            if (savedInstanceState.getBoolean("isDatePickerDialog")) showDatePickerDialog(-1);
            int reminderDialogIndex = savedInstanceState.getInt("reminderDialogIndex");
            if (savedInstanceState.getBoolean("isReminderDatePickerDialog"))
                showDatePickerDialog(reminderDialogIndex);
            if (savedInstanceState.getBoolean("isReminderTimePickerDialog"))
                showTimePickerDialog(reminderDialogIndex);
            if (savedInstanceState.getBoolean("isPickStundeDialog")) showPickStundeDialog();
            if (savedInstanceState.getBoolean("isDeleteDialog")) showDeleteDialog();

        } else {

            eventId = getIntent().getLongExtra("eventId", (long) -1);
            if (eventId > -1) {
                event = Event.getEvent(eventId);
                erinnerungen = event.getErinnerungen();
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(getIntent().getLongExtra("date", Utilities.getToday().getTimeInMillis()));
                Fach fach = Fach.getFach(getIntent().getLongExtra("fachId", -1));
                event = new Event();
                event.setName("");
                event.setDescription("");
                event.setDate(calendar.getTime());
                event.setFach(fach);
                erinnerungen = new ArrayList<>();
            }

        }

        setUpTexts();
        setUpRecycler();

    }

    private void setUpRecycler() {

        RecyclerView.LayoutManager lManager = new LinearLayoutManager(this);
        ReminderRecyclerAdapter adapter = new ReminderRecyclerAdapter(erinnerungen, this);
        reminderRecycler.setLayoutManager(lManager);
        reminderRecycler.setAdapter(adapter);
    }

    private void setUpTexts() {

        nameEdit.append(event.getName());
        nameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                event.setName(editable.toString().trim());
            }
        });

        descEdit.setText(event.getDescription());
        descEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                event.setDescription(editable.toString().trim());
            }
        });

        deleteSwitch.setChecked(event.isDeleteWhenElapsed());
        deleteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                event.setDeleteWhenElapsed(b);
            }
        });

        datumText.setText(makeDateString(event.getDate()));

        datumText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog(-1);
            }
        });


        if (event.getFach() == null) {

            stundenAuswahlText.setVisibility(View.GONE);
            fachText.setVisibility(View.GONE);

        } else {

            fachText.setText(String.format(Locale.GERMANY, "%s:", event.getFach().getName()));

            if (event.getFach().getUnterrichtsZeiten().isEmpty()) stundenAuswahlText.setVisibility(View.GONE);
            else stundenAuswahlText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPickStundeDialog();
                }
            });
        }
    }

    private void showPickStundeDialog() {

        isPickStundeDialog = true;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Datum auswählen");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nextlesson, null, false);
        RecyclerView recyclerView = (RecyclerView) dialogView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dialogBuilder.setView(dialogView);
        dialogBuilder.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isPickStundeDialog = false;
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        NextLessonRecyclerAdapter adapter = new NextLessonRecyclerAdapter(event.getFach(), new NextLessonRecyclerAdapter.Callback() {
            @Override
            public void onDateClicked(Date date) {

                isPickStundeDialog = false;

                event.setDate(date);
                datumText.setText(makeDateString(event.getDate()));
                dialog.cancel();
            }
        });
        recyclerView.setAdapter(adapter);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isPickStundeDialog = false;
            }
        });
        dialog.show();
    }

    /**
     * @param reminderInt index des Reminders, -1 bei Abgabedatum
     */
    private void showDatePickerDialog(final int reminderInt) {

        Calendar c = Calendar.getInstance();
        if (reminderInt > -1)
            c.setTime(erinnerungen.get(reminderInt).getDate());
        else
            c.setTime(event.getDate());

        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {

                isDatePickerDialog = false;

                Calendar c = Utilities.getToday();
                if (reminderInt > -1)
                    c.setTime(erinnerungen.get(reminderInt).getDate());
                else
                    c.setTime(event.getDate());
                c.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                if (reminderInt > -1) {
                    erinnerungen.get(reminderInt).setDate(c.getTime());
                    ((ReminderRecyclerAdapter)reminderRecycler.getAdapter()).updateData(erinnerungen);
                    reminderRecycler.getAdapter().notifyDataSetChanged();
                } else {
                    event.setDate(c.getTime());
                    datumText.setText(makeDateString(event.getDate()));
                }

            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        isDatePickerDialog = true;
        reminderDialogIndex = reminderInt;
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isDatePickerDialog = false;
            }
        });
        dialog.show();

    }

    /**
     * @param reminderInt index des Reminders
     */
    private void showTimePickerDialog(final int reminderInt) {

        Calendar c = Calendar.getInstance();
        c.setTime(erinnerungen.get(reminderInt).getDate());

        TimePickerDialog dialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int i, int i1) {

                isReminderTimePickerDialog = false;

                Calendar c = Calendar.getInstance();
                c.setTime(erinnerungen.get(reminderInt).getDate());
                c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                erinnerungen.get(reminderInt).setDate(c.getTime());
                ((ReminderRecyclerAdapter)reminderRecycler.getAdapter()).updateData(erinnerungen);
                reminderRecycler.getAdapter().notifyDataSetChanged();

            }
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
        isReminderTimePickerDialog = true;
        reminderDialogIndex = reminderInt;
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isReminderTimePickerDialog = false;
            }
        });
        dialog.show();

    }

    private String makeDateString(Date date) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("EE, dd. MMM yyyy", Locale.GERMAN);
        return dateFormat.format(date);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (eventId == -1) getMenuInflater().inflate(R.menu.menu_save, menu);
        else getMenuInflater().inflate(R.menu.menu_save_delete, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_save) {

            if (saveEvent()) finish();
            return true;

        } else if (id == R.id.action_delete) {

            showDeleteDialog();

        } else if (id == android.R.id.home) {

            showSaveDialog();
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeleteDialog() {

        if (eventId == -1) return;

        isDeleteDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("'" + event.getName() + "' löschen");
        builder.setMessage("Bist du sicher dass du dieses Event löschen möchtest?");
        builder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDeleteDialog = false;

                LocalData.removeEventReminder(getApplicationContext(), event);
                event.delete();
                eventBus.post(new EventsChangedEvent());
                finish();

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
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.warn_text));
            }
        });
        dialog.show();

    }

    private boolean saveEvent() {

        if (event.getName().isEmpty()) {
            Toast.makeText(this, "Das Event braucht eine Bezeichnung!", Toast.LENGTH_SHORT).show();
            return false;
        }

        //Alte Erinnerungen löschen
        event.deleteErinnerungen();
        if (eventId != -1) LocalData.removeEventReminder(getApplicationContext(), event);

        event.save();
        //neue Erinnerungen speichern
        for (Erinnerung e : erinnerungen) e.save();
        LocalData.addEventReminder(getApplicationContext(), event);

        eventBus.post(new EventsChangedEvent());
        return true;

    }

    private void showSaveDialog() {

        isSaveDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Änderungen verwerfen");
        builder.setMessage("Bist du sicher, dass du die Änderungen nicht speichern möchtest?");
        builder.setPositiveButton("Verwerfen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isSaveDialog = false;
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isSaveDialog = false;
            }
        });
        builder.show();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putLong("eventId", eventId);
        outState.putBoolean("isSaveDialog", isSaveDialog);
        outState.putParcelable("event", event);

        ArrayList<String> erinnerungenStr = new ArrayList<>();
        for (Erinnerung e : erinnerungen) {
            erinnerungenStr.add(String.valueOf(e.getDate().getTime()));
        }
        outState.putStringArrayList("erinnerungen", erinnerungenStr);

        outState.putBoolean("isDatePickerDialog", isDatePickerDialog);
        outState.putBoolean("isReminderTimePickerDialog", isReminderTimePickerDialog);
        outState.getBoolean("isPickStundeDialog", isPickStundeDialog);
        outState.getBoolean("isDeleteDialog", isDeleteDialog);
        outState.putInt("reminderDialogIndex", reminderDialogIndex);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {

        showSaveDialog();
    }

    @Override
    public void onDateClicked(int pos) {
        showDatePickerDialog(pos);
    }

    @Override
    public void onTimeClicked(int pos) {
        showTimePickerDialog(pos);
    }

    @Override
    public void onDeleteClicked(int pos) {
        erinnerungen.remove(pos);
        ((ReminderRecyclerAdapter)reminderRecycler.getAdapter()).updateData(erinnerungen);
        reminderRecycler.getAdapter().notifyItemRemoved(pos);
    }

    @Override
    public void onAddClicked() {
        Calendar c = Calendar.getInstance();
        erinnerungen.add(new Erinnerung(c.getTime(), event));
        ((ReminderRecyclerAdapter)reminderRecycler.getAdapter()).updateData(erinnerungen);
        reminderRecycler.getAdapter().notifyItemInserted(erinnerungen.size() - 1);
    }
}
