package de.conradowatz.jkgvertretung.fragments;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import android.widget.Toast;

import com.raizlabs.android.dbflow.config.FlowManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.events.ExitAppEvent;
import de.conradowatz.jkgvertretung.events.PermissionGrantedEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.variables.AppDatabase;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static final int REQUEST_CODE_BACKUP_EXPORT = 1;
    public static final int REQUEST_CODE_BACKUP_IMPORT = 2;
    public static final int REQUEST_CODE_BACKUP_DELETE = 3;
    public static final String SAVE_FILE_NAME = "backup%s.db";
    public static final String DATE_FORMAT = "dd-MM-yyyy_HH-mm-ss";
    private boolean isBackupExportDialog;
    private boolean isBackupImportDialog;
    private boolean isBackupDeleteDialog;

    private EventBus eventBus = EventBus.getDefault();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        eventBus.register(this);

        addPreferencesFromResource(R.xml.settings);

        ListPreference startScreen = (ListPreference) findPreference("startScreen");
        ListPreference maxDaysToFetchRefresh = (ListPreference) findPreference("maxDaysToFetchRefresh");
        ListPreference maxDaysToFetchStart = (ListPreference) findPreference("maxDaysToFetchStart");
        ListPreference notificationType = (ListPreference) findPreference("notificationType");
        ListPreference backgroundStuPlan = (ListPreference) findPreference("background");
        Preference exportBackup = findPreference("exportBackup");
        Preference importBackup = findPreference("importBackup");
        Preference deleteBackup = findPreference("deleteBackup");

        startScreen.setSummary(startScreen.getEntry());
        maxDaysToFetchRefresh.setSummary(maxDaysToFetchRefresh.getEntry());
        maxDaysToFetchStart.setSummary(maxDaysToFetchStart.getEntry());
        notificationType.setSummary(notificationType.getEntry());
        backgroundStuPlan.setSummary(backgroundStuPlan.getEntry());

        startScreen.setOnPreferenceChangeListener(this);
        maxDaysToFetchRefresh.setOnPreferenceChangeListener(this);
        maxDaysToFetchStart.setOnPreferenceChangeListener(this);
        notificationType.setOnPreferenceChangeListener(this);
        backgroundStuPlan.setOnPreferenceChangeListener(this);

        exportBackup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (getWritePermission(REQUEST_CODE_BACKUP_EXPORT)) showExportBackupDialog();
                return true;
            }
        });
        importBackup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (getWritePermission(REQUEST_CODE_BACKUP_IMPORT)) showImportBackupDialog();
                return true;
            }
        });
        deleteBackup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (getWritePermission(REQUEST_CODE_BACKUP_DELETE)) showDeleteBackupDialog();
                return true;
            }
        });

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("isBackupExportDialog")) showExportBackupDialog();
            if (savedInstanceState.getBoolean("isBackupImportDialog")) showImportBackupDialog();
            if (savedInstanceState.getBoolean("isBackupDeleteDialog")) showDeleteBackupDialog();
        }
    }

    @Subscribe
    public void onEvent(PermissionGrantedEvent event) {
        int requestCode = event.getRequestCode();
        switch (requestCode) {
            case REQUEST_CODE_BACKUP_EXPORT:
                showExportBackupDialog();
                break;
            case REQUEST_CODE_BACKUP_IMPORT:
                showImportBackupDialog();
                break;
            case REQUEST_CODE_BACKUP_DELETE:
                showDeleteBackupDialog();
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        eventBus.unregister(this);
    }

    private void showDeleteBackupDialog() {

        if (!isExternalStorageWritable()) return;

        isBackupDeleteDialog = true;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Backup importieren");

        File path = getBackupPath();
        if (path == null) return;
        File[] allFiles = path.listFiles();
        final List<File> backupFiles = new ArrayList<>();
        List<String> displayNames = new ArrayList<>();
        for (File f : allFiles) {
            if (f.isFile()) {
                String fileDate = f.getName().replaceAll("backup", "").replaceAll(".db", "");
                try {
                    Date date = new SimpleDateFormat(DATE_FORMAT, Locale.GERMANY).parse(fileDate);
                    backupFiles.add(f);
                    displayNames.add("Backup vom " + new SimpleDateFormat("dd.MM.yy, HH:mm:ss", Locale.GERMANY).format(date));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        String[] displayNamesArray = new String[displayNames.size()];
        displayNamesArray = displayNames.toArray(displayNamesArray);
        dialogBuilder.setItems(displayNamesArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int backupIndex) {

                isBackupDeleteDialog = false;

                if (!backupFiles.get(backupIndex).delete()) {
                    Toast.makeText(getActivity(), "Fehler beim löschen.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Backup erfolgreich gelöscht.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialogBuilder.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isBackupDeleteDialog = false;
            }
        });
        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isBackupDeleteDialog = false;
            }
        });
        dialog.show();

    }

    private void showImportBackupDialog() {

        if (!isExternalStorageWritable()) return;

        isBackupImportDialog = true;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Backup importieren");

        File path = getBackupPath();
        if (path == null) return;
        File[] allFiles = path.listFiles();
        final List<File> backupFiles = new ArrayList<>();
        List<String> displayNames = new ArrayList<>();
        for (File f : allFiles) {
            if (f.isFile() && f.getName().endsWith(".db")) {
                String fileDate = f.getName().replaceAll("backup", "").replaceAll(".db", "");
                try {
                    Date date = new SimpleDateFormat(DATE_FORMAT, Locale.GERMANY).parse(fileDate);
                    backupFiles.add(f);
                    displayNames.add("Backup vom " + new SimpleDateFormat("dd.MM.yy, HH:mm:ss", Locale.GERMANY).format(date));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        String[] displayNamesArray = new String[displayNames.size()];
        displayNamesArray = displayNames.toArray(displayNamesArray);
        dialogBuilder.setItems(displayNamesArray, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int backupIndex) {

                isBackupImportDialog = false;

                try {
                    File backupFile = backupFiles.get(backupIndex);
                    File database = new File(FlowManager.getContext().getDatabasePath(AppDatabase.NAME).getAbsolutePath()+".db");
                    copyFile(backupFile, database);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                Toast.makeText(getActivity(), "Neustart erforderlich.", Toast.LENGTH_SHORT).show();

                eventBus.post(new ExitAppEvent());
                getActivity().finish();


            }
        });
        dialogBuilder.setNeutralButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isBackupImportDialog = false;
            }
        });
        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isBackupImportDialog = false;
            }
        });
        dialog.show();

    }

    private void showExportBackupDialog() {

        isBackupExportDialog = true;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle("Backup exportieren");
        dialogBuilder.setMessage("Möchtest du ein Backup deiner Fächer und Termine speichern?");
        dialogBuilder.setPositiveButton("Backup erstellen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isBackupExportDialog = false;

                if (!isExternalStorageWritable()) return;

                Calendar calendar = Calendar.getInstance();
                String backUpTitle = String.format(SAVE_FILE_NAME, new SimpleDateFormat(DATE_FORMAT, Locale.GERMANY).format(calendar.getTime()));

                File path = getBackupPath();
                if (path == null) return;
                try {

                    File backupDest = new File(path.getAbsolutePath() + File.separator + backUpTitle);
                    File database = new File(FlowManager.getContext().getDatabasePath(AppDatabase.NAME).getAbsolutePath()+".db");
                    copyFile(database, backupDest);

                    Toast.makeText(getActivity(), "Backup erfolgreich exportiert.", Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    Toast.makeText(getActivity(), "Unbekannter Fehler!", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isBackupExportDialog = false;
            }
        });
        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isBackupExportDialog = false;
            }
        });
        dialog.show();

    }

    private void copyFile(File src, File dest) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dest);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private File getBackupPath() {

        File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "JKGStundenplanBackup");
        path.mkdirs();
        return path;
    }

    public boolean isExternalStorageWritable() {

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        Toast.makeText(getActivity(), "Der externe Speicher ist nicht verfügbar.", Toast.LENGTH_SHORT).show();
        return false;
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        ListPreference listPreference = (ListPreference) preference;
        int position = Arrays.asList(listPreference.getEntryValues()).indexOf(newValue);
        listPreference.setSummary(listPreference.getEntries()[position]);

        if (preference.getKey().equals("notificationType")) {
            LocalData.deleteNotificationAlarms(getActivity().getApplicationContext());
            LocalData.recreateNotificationAlarms(getActivity().getApplicationContext());
        }
        return true;
    }

    private boolean getWritePermission(int requestCode) {

        int permissionCheck = ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) return true;
        else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            return false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putBoolean("isBackupExportDialog", isBackupExportDialog);
        outState.putBoolean("isBackupImportDialog", isBackupImportDialog);
        outState.putBoolean("isBackupDeleteDialog", isBackupDeleteDialog);

        super.onSaveInstanceState(outState);
    }
}
