package de.conradowatz.jkgvertretung.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.DatePicker;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.fragments.TaskFragment;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.tools.VertretungsData;

public class SplashActivity extends AppCompatActivity implements TaskFragment.SplashScreenCallbacks {

    private TaskFragment taskFragment;

    private Intent startIntent;

    private boolean isDeleteDialog;
    private boolean isNoConnectionDialog;
    private boolean isDownloadErrorDialog;
    private boolean isChooseAWocheDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        taskFragment = (TaskFragment) fm.findFragmentByTag(TaskFragment.TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (taskFragment == null) {
            taskFragment = new TaskFragment();
            fm.beginTransaction().add(taskFragment, TaskFragment.TAG_TASK_FRAGMENT).commit();
        }

        //läd LocalData in den Speicher
        if (savedInstanceState != null) {

            startIntent = savedInstanceState.getParcelable("startIntent");

            if (savedInstanceState.getBoolean("isDeleteDialog")) showLocalDataDeleteDialog();
            if (savedInstanceState.getBoolean("isNoConnectionDialog")) showNoConnectionDialog();
            if (savedInstanceState.getBoolean("isDownloadErrorDialog")) showDownloadErrorDialog();
            if (savedInstanceState.getBoolean("isChooseAWocheDialog")) showChooseAWocheDialog();

        } else {

            if (getIntent().hasExtra("intent"))
                startIntent = getIntent().getParcelableExtra("intent");
            else startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            loadLocalData();
        }

    }

    /**
     * Läd die LocalData aus dem Spiecher oder erstellt eine neue Datei
     */
    private void loadLocalData() {

        //Schauen ob LocalData im Speicher ist
        File localDataFile = new File(getFilesDir(), LocalData.SAVE_FILE_NAME);
        if (localDataFile.exists()) {

            //Local Data laden
            taskFragment.createLocalDataFromFile();

        } else {

            //neue LocalData erstellen
            LocalData.createNewLocalData();

            showChooseAWocheDialog();

        }
    }

    private void showChooseAWocheDialog() {

        isChooseAWocheDialog = true;

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("A / B Woche");
        dialogBuilder.setMessage("Bitte wähle einen Tag aus, bei dem du sicher bist, dass es A-Woche war/ist.");
        final Activity activity = this;
        dialogBuilder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                final Calendar calendar = Calendar.getInstance();
                DatePickerDialog dialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {

                        isChooseAWocheDialog = false;

                        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                        LocalData.getInstance().setCompareDate(calendar.getTime(), true);
                        taskFragment.saveLocalDataToFile();
                        onLocalDataCreated();

                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        showChooseAWocheDialog();
                    }
                });
                dialog.show();
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialog.show();
            }
        });
        dialog.show();
    }

    /**
     * Wenn Login Daten gespeichert sind -> VertretungsData laden | LoginActivity starten
     */
    @Override
    public void onLocalDataCreated() {

        if (PreferenceHelper.readStringFromPreferences(getApplicationContext(), "username", "null").equals("null"))
            startLoginActivity();
        else
            loadVertretungsData();

    }

    @Override
    public void onLocalDataCreateError(Throwable throwable) {

        Log.e("JKGDEBUG", "Fehler bei der Erstellung der LocalData aus dem Speicher");
        throwable.printStackTrace();

        showLocalDataDeleteDialog();

    }

    private void showLocalDataDeleteDialog() {

        isDeleteDialog = true;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Speicher beschädigt");
        dialogBuilder.setMessage("Die Appdaten sind beschädigt und konnten nicht eingelesen werden. Sollen sie gelöscht werden?");
        dialogBuilder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDeleteDialog = false;

                File localDataFile = new File(getFilesDir(), LocalData.SAVE_FILE_NAME);
                if (localDataFile.delete()) {
                    taskFragment.createLocalDataFromFile();
                } else {
                    finish();
                }
            }
        });
        dialogBuilder.setNegativeButton("Schließen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.warn_text));
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialog.show();
    }

    /**
     * Läd die Daten entweder aus dem Speicher oder läd sie runter
     */
    private void loadVertretungsData() {

        //Schauen on eine SavedSession im Speicher ist
        File savedSessionFile = new File(getFilesDir(), VertretungsData.SAVE_FILE_NAME);
        if (savedSessionFile.exists()) {

            //Saved Session laden
            taskFragment.createVertretungsDataFromFile();

        } else {

            taskFragment.downloadAllData(0);

        }

    }

    /**
     * Öffnet die MainActivity mit dem extra Intent, dass sie den Login starten soll
     */
    private void startLoginActivity() {

        Intent mainLoginIntent = new Intent(this, MainActivity.class);
        mainLoginIntent.putExtra("startLogin", true);
        mainLoginIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(mainLoginIntent);
        finish();
        overridePendingTransition(0, 0);

    }

    @Override
    public void onVertretungsDataCreated() {

        bringBackActivity();

    }

    @Override
    public void onVertretungsDataCreateError(Throwable throwable) {

        Log.e("JKGDEBUG", "Fehler beim Laden der VertretungsData aus dem Speicher");
        throwable.printStackTrace();

        File savedSessionFile = new File(getFilesDir(), VertretungsData.SAVE_FILE_NAME);
        if (savedSessionFile.delete()) {
            taskFragment.downloadAllData(0);
        } else {
            finish();
        }

    }

    /**
     * Öffnet die gewünschte Activity und schließt den SplashScreen
     */
    private void bringBackActivity() {

        startActivity(startIntent);
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onDownloadFinished() {

        taskFragment.saveVertretungsDataToFile();
        bringBackActivity();

    }

    @Override
    public void onDownloadError(Throwable throwable) {

        Log.e("JKGDEBUG", "Unbekannter Fehler beim Download");
        throwable.printStackTrace();

        showDownloadErrorDialog();

    }

    private void showDownloadErrorDialog() {

        isDownloadErrorDialog = true;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Download Error");
        dialogBuilder.setMessage("Beim Download ist ein unbekannter Fehler aufgetreten.");
        dialogBuilder.setPositiveButton("Erneut versuchen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDownloadErrorDialog = false;

                taskFragment.downloadAllData(0);
            }
        });
        dialogBuilder.setNegativeButton("Beenden", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialog.show();

    }

    @Override
    public void onNoAccess() {

        Toast.makeText(this, "Die Logindaten sind ungültig.", Toast.LENGTH_LONG).show();
        startLoginActivity();

    }

    @Override
    public void onNoConnection() {

        showNoConnectionDialog();
    }

    private void showNoConnectionDialog() {

        isNoConnectionDialog = true;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Keine Internetverbindung");
        dialogBuilder.setMessage("Es konnte keine Verbindung zum Server hergestellt werden.");
        dialogBuilder.setPositiveButton("Erneut versuchen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isNoConnectionDialog = false;

                taskFragment.downloadAllData(0);
            }
        });
        dialogBuilder.setNegativeButton("Beenden", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialog.show();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("isDeleteDialog", isDeleteDialog);
        outState.putBoolean("isDownloadErrorDialog", isDownloadErrorDialog);
        outState.putBoolean("isNoConnectionDialog", isNoConnectionDialog);
        outState.putBoolean("isChooseAWocheDialog", isChooseAWocheDialog);
        outState.putParcelable("startIntent", startIntent);
    }
}
