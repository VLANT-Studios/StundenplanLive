package de.conradowatz.jkgvertretung.activities;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.events.AnalyticsEventEvent;
import de.conradowatz.jkgvertretung.events.FerienChangedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.Ferien;

public class FerienActivity extends AppCompatActivity {

    private Ferien ferien;
    private int ferienInt;

    private boolean isSaveDialog;
    private boolean isDatePickerDialog;
    private boolean isDatePickerStartDate;

    private Toolbar toolbar;
    private EditText nameEdit;
    private TextView startText;
    private TextView endText;

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

        setContentView(R.layout.activity_ferien);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);

        nameEdit = (EditText) findViewById(R.id.nameEdit);
        startText = (TextView) findViewById(R.id.startText);
        endText = (TextView) findViewById(R.id.endText);

        if (savedInstanceState != null) {

            ferienInt = savedInstanceState.getInt("ferienInt", -1);
            ferien = savedInstanceState.getParcelable("ferien");

            if (savedInstanceState.getBoolean("isSaveDialog")) showSaveDialog();
            if (savedInstanceState.getBoolean("isDatePickerDialog"))
                showDatePickerDialog(savedInstanceState.getBoolean("isDatePickerStartDate"));

        } else {

            ferienInt = getIntent().getIntExtra("ferienInt", -1);
            if (ferienInt > -1) {
                Ferien f = LocalData.getInstance().getFerien().get(ferienInt);
                ferien = new Ferien(f.getStartDate(), f.getEndDate(), f.getName()); //make copy if aborted
            } else {
                Calendar calendar = Calendar.getInstance();
                ferien = new Ferien(calendar.getTime(), calendar.getTime(), "");
            }

        }

        setUpTexts();

    }

    private void setUpTexts() {

        nameEdit.setText(ferien.getName());
        nameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                ferien.setName(editable.toString().trim());
            }
        });
        startText.setText(makeDateString(ferien.getStartDate()));
        endText.setText(makeDateString(ferien.getEndDate()));

        startText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog(true);
            }
        });
        endText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog(false);
            }
        });
    }

    private void showDatePickerDialog(final boolean isStartDate) {

        Calendar c = Calendar.getInstance();
        c.setTime(isStartDate ? ferien.getStartDate() : ferien.getEndDate());

        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                Calendar c = Calendar.getInstance();
                c.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                if (isStartDate) {
                    ferien.setStartDate(c.getTime());
                    startText.setText(makeDateString(ferien.getStartDate()));

                    Calendar endCalendar = Calendar.getInstance();
                    endCalendar.setTime(ferien.getEndDate());
                    if (Utilities.compareDays(endCalendar, c) < 0) {
                        ferien.setEndDate(c.getTime());
                        endText.setText(makeDateString(ferien.getEndDate()));
                    }
                } else {
                    ferien.setEndDate(c.getTime());
                    endText.setText(makeDateString(ferien.getEndDate()));
                }
                isDatePickerDialog = false;
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        isDatePickerDialog = true;
        isDatePickerStartDate = isStartDate;
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isDatePickerDialog = false;
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

        getMenuInflater().inflate(R.menu.menu_kurswahl, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_save) {

            if (saveFerien()) {
                eventBus.post(new FerienChangedEvent());
                LocalData.saveToFile(getApplicationContext());
                finish();
            }
            return true;

        } else if (id == android.R.id.home) {

            showSaveDialog();
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private boolean saveFerien() {

        if (ferien.getName().isEmpty()) {
            Toast.makeText(this, "Ferienname darf nicht leer sein!", Toast.LENGTH_SHORT).show();
            return false;
        }
        Calendar cStart = Calendar.getInstance();
        Calendar cEnd = Calendar.getInstance();
        cStart.setTime(ferien.getStartDate());
        cEnd.setTime(ferien.getEndDate());
        if (Utilities.compareDays(cStart, cEnd) > 0) {
            Toast.makeText(this, "Das Enddatum muss hinter dem Startdatum liegen!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (ferienInt > -1) LocalData.getInstance().getFerien().set(ferienInt, ferien);
        else {
            eventBus.post(new AnalyticsEventEvent("Manager", "Ferien erstellt"));
            LocalData.getInstance().getFerien().add(ferien);
        }
        LocalData.getInstance().sortFerien();
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

        outState.putInt("ferienInt", ferienInt);
        outState.putBoolean("isSaveDialog", isSaveDialog);
        outState.putParcelable("ferien", ferien);

        outState.putBoolean("isDatePickerDialog", isDatePickerDialog);
        outState.putBoolean("isDatePickerStartDate", isDatePickerStartDate);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {

        showSaveDialog();
    }
}
