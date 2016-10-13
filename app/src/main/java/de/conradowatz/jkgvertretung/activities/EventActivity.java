package de.conradowatz.jkgvertretung.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
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

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.NextLessonRecyclerAdapter;
import de.conradowatz.jkgvertretung.adapters.ReminderRecyclerAdapter;
import de.conradowatz.jkgvertretung.events.AnalyticsEventEvent;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.Event;
import de.conradowatz.jkgvertretung.variables.Fach;

public class EventActivity extends AppCompatActivity implements ReminderRecyclerAdapter.Callback {

    private Event event;
    private int fachInt;
    private int eventInt;

    private boolean isSaveDialog;
    private boolean isDatePickerDialog;
    private boolean isPickStundeDialog;
    private boolean isDeleteDialog;

    private boolean isReminderTimePickerDialog;
    private int reminderDialogIndex;

    private Toolbar toolbar;
    private EditText nameEdit;
    private TextView datumText;
    private TextView stundenAuswahlText;
    private EditText descEdit;
    private SwitchCompat deleteSwitch;
    private RecyclerView reminderRecycler;

    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!VertretungsData.getInstance().isReady() || !LocalData.isReady()) {
            Intent spashIntent = new Intent(this, SplashActivity.class);
            spashIntent.putExtra("intent", getIntent());
            startActivity(spashIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_event);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);

        nameEdit = (EditText) findViewById(R.id.nameEdit);
        datumText = (TextView) findViewById(R.id.datumText);
        stundenAuswahlText = (TextView) findViewById(R.id.stundenAuswahlText);
        descEdit = (EditText) findViewById(R.id.descEdit);
        deleteSwitch = (SwitchCompat) findViewById(R.id.deleteSwitch);
        reminderRecycler = (RecyclerView) findViewById(R.id.reminderRecycler);

        if (savedInstanceState != null) {

            eventInt = savedInstanceState.getInt("eventInt", -1);
            fachInt = savedInstanceState.getInt("fachInt", -1);
            event = savedInstanceState.getParcelable("event");

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

            eventInt = getIntent().getIntExtra("eventInt", -1);
            fachInt = getIntent().getIntExtra("fachInt", -1);
            if (eventInt > -1) {
                Event e;
                if (fachInt > -1)
                    e = LocalData.getInstance().getFächer().get(fachInt).getEvents().get(eventInt);
                else
                    e = LocalData.getInstance().getNoFachEvents().get(eventInt);
                event = new Event(e.getDatum(), e.getTitle(), e.getDescription(), e.isDeleteWhenElapsed(), e.getFachName(), e.getReminders()); //make copy if aborted
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(getIntent().getLongExtra("date", calendar.getTimeInMillis()));
                event = new Event(calendar.getTime(), "", "", false);
            }

        }

        setUpTexts();
        setUpRecycler();

    }

    private void setUpRecycler() {

        RecyclerView.LayoutManager lManager = new LinearLayoutManager(this);
        ReminderRecyclerAdapter adapter = new ReminderRecyclerAdapter(event.getReminders(), this);
        reminderRecycler.setLayoutManager(lManager);
        reminderRecycler.setAdapter(adapter);
    }

    private void setUpTexts() {

        nameEdit.setText(event.getTitle());
        nameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                event.setTitle(editable.toString().trim());
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

        datumText.setText(makeDateString(event.getDatum()));

        datumText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog(-1);
            }
        });

        if (fachInt == -1 || !LocalData.getInstance().getFächer().get(fachInt).hasStunden())
            stundenAuswahlText.setVisibility(View.GONE);
        else stundenAuswahlText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPickStundeDialog();
            }
        });
    }

    private void showPickStundeDialog() {

        final Fach fach = LocalData.getInstance().getFächer().get(fachInt);

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
        NextLessonRecyclerAdapter adapter = new NextLessonRecyclerAdapter(fach, new NextLessonRecyclerAdapter.Callback() {
            @Override
            public void onDateClicked(Date date) {

                isPickStundeDialog = false;

                event.setDatum(date);
                datumText.setText(makeDateString(event.getDatum()));
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
            c.setTime(event.getReminders().get(reminderInt));
        else
            c.setTime(event.getDatum());

        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {

                isDatePickerDialog = false;

                Calendar c = Calendar.getInstance();
                if (reminderInt > -1)
                    c.setTime(event.getReminders().get(reminderInt));
                else
                    c.setTime(event.getDatum());
                c.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
                if (reminderInt > -1) {
                    event.getReminders().set(reminderInt, c.getTime());
                    reminderRecycler.getAdapter().notifyDataSetChanged();
                } else {
                    event.setDatum(c.getTime());
                    datumText.setText(makeDateString(event.getDatum()));
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
        c.setTime(event.getReminders().get(reminderInt));

        TimePickerDialog dialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int i, int i1) {

                isReminderTimePickerDialog = false;

                Calendar c = Calendar.getInstance();
                c.setTime(event.getReminders().get(reminderInt));
                c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                event.getReminders().set(reminderInt, c.getTime());
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

        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.GERMAN) + ", " + c.get(Calendar.DAY_OF_MONTH) + ". " + c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.GERMAN) + " " + c.get(Calendar.YEAR);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (eventInt == -1) getMenuInflater().inflate(R.menu.menu_save, menu);
        else getMenuInflater().inflate(R.menu.menu_save_delete, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_save) {

            if (saveEvent()) {
                LocalData.saveToFile(getApplicationContext());
                eventBus.post(new EventsChangedEvent());
                finish();
            }
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

        if (eventInt == -1) return;

        isDeleteDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("'" + event.getTitle() + "' löschen");
        builder.setMessage("Bist du sicher dass du dieses Event löschen möchtest?");
        builder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDeleteDialog = false;

                if (fachInt > -1) {
                    Event event = LocalData.getInstance().getFächer().get(fachInt).getEvents().get(eventInt);
                    LocalData.removeEventReminder(getApplicationContext(), event, fachInt, eventInt);
                    LocalData.getInstance().getFächer().get(fachInt).getEvents().remove(eventInt);
                } else {
                    Event event = LocalData.getInstance().getNoFachEvents().get(eventInt);
                    LocalData.removeEventReminder(getApplicationContext(), event, fachInt, eventInt);
                    LocalData.getInstance().getNoFachEvents().remove(eventInt);
                }

                eventBus.post(new EventsChangedEvent());
                LocalData.saveToFile(getApplicationContext());
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

        if (event.getTitle().isEmpty()) {
            Toast.makeText(this, "Das Event braucht eine Bezeichnung!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (fachInt > -1) {
            if (eventInt > -1) {
                LocalData.removeEventReminder(getApplicationContext(), LocalData.getInstance().getFächer().get(fachInt).getEvents().get(eventInt), fachInt, eventInt);
                LocalData.getInstance().getFächer().get(fachInt).getEvents().set(eventInt, event);
            } else {
                eventBus.post(new AnalyticsEventEvent("Manager", "Event erstellt"));
                LocalData.getInstance().getFächer().get(fachInt).getEvents().add(event);
            }
            LocalData.getInstance().getFächer().get(fachInt).sortEvents();
            LocalData.addEventReminder(getApplicationContext(), event, fachInt, LocalData.getInstance().getFächer().get(fachInt).getEvents().indexOf(event));
        } else {
            if (eventInt > -1) {
                LocalData.removeEventReminder(getApplicationContext(), LocalData.getInstance().getNoFachEvents().get(eventInt), fachInt, eventInt);
                LocalData.getInstance().getNoFachEvents().set(eventInt, event);
            } else {
                eventBus.post(new AnalyticsEventEvent("Manager", "Event erstellt"));
                LocalData.getInstance().getNoFachEvents().add(event);
            }
            LocalData.getInstance().sortNoFachEvents();
            LocalData.addEventReminder(getApplicationContext(), event, fachInt, LocalData.getInstance().getNoFachEvents().indexOf(event));
        }

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

        outState.putInt("eventInt", eventInt);
        outState.putInt("fachInt", fachInt);
        outState.putBoolean("isSaveDialog", isSaveDialog);
        outState.putParcelable("event", event);

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
        event.getReminders().remove(pos);
        reminderRecycler.getAdapter().notifyItemRemoved(pos);
    }

    @Override
    public void onAddClicked() {
        Calendar c = Calendar.getInstance();
        event.getReminders().add(c.getTime());
        reminderRecycler.getAdapter().notifyItemInserted(event.getReminders().size() - 1);
    }
}
