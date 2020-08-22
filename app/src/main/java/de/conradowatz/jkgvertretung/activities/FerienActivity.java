package de.conradowatz.jkgvertretung.activities;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.events.FerienChangedEvent;
import de.conradowatz.jkgvertretung.tools.ColorAPI;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.variables.Ferien;

public class FerienActivity extends AppCompatActivity {

    private Ferien ferien;
    private long ferienId;

    private boolean isSaveDialog;
    private boolean isDatePickerDialog;
    private boolean isDatePickerStartDate;
    private boolean isDeleteDialog;

    private Toolbar toolbar;
    private EditText nameEdit;
    private TextView startText;
    private TextView endText;

    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ferien);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(new ColorAPI(this).getActionBarColor()));
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);

        nameEdit = (EditText) findViewById(R.id.nameEdit);
        startText = (TextView) findViewById(R.id.startText);
        endText = (TextView) findViewById(R.id.endText);

        if (savedInstanceState != null) {

            ferienId = savedInstanceState.getLong("ferienId", -1);
            ferien = savedInstanceState.getParcelable("ferien");

            if (savedInstanceState.getBoolean("isSaveDialog")) showSaveDialog();
            if (savedInstanceState.getBoolean("isDatePickerDialog"))
                showDatePickerDialog(savedInstanceState.getBoolean("isDatePickerStartDate"));
            if (savedInstanceState.getBoolean("isDeleteDialog")) showDeleteDialog();

        } else {

            ferienId = getIntent().getLongExtra("ferienId", -1);
            if (ferienId > -1) {
                ferien = Ferien.getFerien(ferienId);
            } else {
                Calendar calendar = Calendar.getInstance();
                ferien = new Ferien(calendar.getTime(), calendar.getTime(), "");
            }

        }

        setUpTexts();

    }

    private void setUpTexts() {

        nameEdit.append(ferien.getName());
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

                isDatePickerDialog = false;

                Calendar c = Utilities.getToday();
                c.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                if (isStartDate) {
                    ferien.setStartDate(c.getTime());
                    startText.setText(makeDateString(ferien.getStartDate()));

                    Calendar endCalendar = Utilities.getToday();
                    endCalendar.setTime(ferien.getEndDate());
                    if (Utilities.compareDays(endCalendar, c) < 0) {
                        ferien.setEndDate(c.getTime());
                        endText.setText(makeDateString(ferien.getEndDate()));
                    }
                } else {
                    ferien.setEndDate(c.getTime());
                    endText.setText(makeDateString(ferien.getEndDate()));
                }

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

        SimpleDateFormat dateFormat = new SimpleDateFormat("EE, dd. MMM yyyy", Locale.GERMAN);
        return dateFormat.format(date);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (ferienId == -1) getMenuInflater().inflate(R.menu.menu_save, menu);
        else getMenuInflater().inflate(R.menu.menu_save_delete, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_save) {

            if (saveFerien()) {
                ferien.save();
                eventBus.post(new FerienChangedEvent());
                finish();
            }
            return true;

        } else if (id == R.id.action_delete) {

            showDeleteDialog();
            return true;

        } else if (id == android.R.id.home) {

            showSaveDialog();
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeleteDialog() {

        if (ferienId == -1) return;

        isDeleteDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("'" + ferien.getName() + "' löschen");
        builder.setMessage("Bist du sicher dass du dieses Event löschen möchtest?");
        builder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDeleteDialog = false;

                ferien.delete();
                eventBus.post(new FerienChangedEvent());
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

    private boolean saveFerien() {

        if (ferien.getName().isEmpty()) {
            Toast.makeText(this, "Ferienname darf nicht leer sein!", Toast.LENGTH_SHORT).show();
            return false;
        }
        Calendar heute = Calendar.getInstance();
        if (Utilities.compareDays(ferien.getStartDate(), ferien.getEndDate()) > 0) {
            Toast.makeText(this, "Das Enddatum muss hinter dem Startdatum liegen!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Utilities.compareDays(heute.getTime(), ferien.getEndDate()) > 0) {
            Toast.makeText(this, "Ferien können nicht in der Vergangenheit erstellt werden!", Toast.LENGTH_SHORT).show();
            return false;
        }

        ferien.save();

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

        outState.putLong("ferienInt", ferienId);
        outState.putBoolean("isSaveDialog", isSaveDialog);
        outState.putParcelable("ferien", ferien);

        outState.putBoolean("isDatePickerDialog", isDatePickerDialog);
        outState.putBoolean("isDatePickerStartDate", isDatePickerStartDate);
        outState.putBoolean("isDeleteDialog", isDeleteDialog);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {

        showSaveDialog();
    }
}
