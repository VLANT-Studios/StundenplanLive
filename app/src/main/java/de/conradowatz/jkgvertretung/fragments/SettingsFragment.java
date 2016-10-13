package de.conradowatz.jkgvertretung.fragments;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.events.FerienChangedEvent;
import de.conradowatz.jkgvertretung.events.KlassenlistUpdatedEvent;
import de.conradowatz.jkgvertretung.events.KursChangedEvent;
import de.conradowatz.jkgvertretung.events.NotenChangedEvent;
import de.conradowatz.jkgvertretung.events.PermissionGrantedEvent;
import de.conradowatz.jkgvertretung.tools.DataVersionCompat;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.tools.Utilities;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.Backup;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static final int REQUEST_CODE_BACKUP_EXPORT = 1;
    public static final int REQUEST_CODE_BACKUP_IMPORT = 2;
    public static final int REQUEST_CODE_BACKUP_DELETE = 3;
    private ListPreference startScreen;
    private ListPreference maxDaysToFetchStart;
    private ListPreference maxDaysToFetchRefresh;
    private ListPreference notificationType;
    private Preference exportBackup;
    private Preference importBackup;
    private Preference deleteBackup;
    private boolean isBackupExportDialog;
    private boolean isBackupImportDialog;
    private boolean isBackupDeleteDialog;

    private EventBus eventBus = EventBus.getDefault();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        eventBus.register(this);

        addPreferencesFromResource(R.xml.settings);

        startScreen = (ListPreference) findPreference("startScreen");
        maxDaysToFetchRefresh = (ListPreference) findPreference("maxDaysToFetchRefresh");
        maxDaysToFetchStart = (ListPreference) findPreference("maxDaysToFetchStart");
        notificationType = (ListPreference) findPreference("notificationType");
        exportBackup = findPreference("exportBackup");
        importBackup = findPreference("importBackup");
        deleteBackup = findPreference("deleteBackup");

        startScreen.setSummary(startScreen.getEntry());
        maxDaysToFetchRefresh.setSummary(maxDaysToFetchRefresh.getEntry());
        maxDaysToFetchStart.setSummary(maxDaysToFetchStart.getEntry());
        notificationType.setSummary(notificationType.getEntry());

        startScreen.setOnPreferenceChangeListener(this);
        maxDaysToFetchRefresh.setOnPreferenceChangeListener(this);
        maxDaysToFetchStart.setOnPreferenceChangeListener(this);
        notificationType.setOnPreferenceChangeListener(this);

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
                String fileDate = f.getName().replaceAll("backup", "").replaceAll(".json", "");
                try {
                    Date date = new SimpleDateFormat(Backup.DATE_FORMAT, Locale.GERMANY).parse(fileDate);
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
            if (f.isFile()) {
                String fileDate = f.getName().replaceAll("backup", "").replaceAll(".json", "");
                try {
                    Date date = new SimpleDateFormat(Backup.DATE_FORMAT, Locale.GERMANY).parse(fileDate);
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

                String json = "";
                Backup backup;
                try {
                    FileInputStream inputStream = new FileInputStream(backupFiles.get(backupIndex));
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder total = new StringBuilder(inputStream.available());
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line);
                    }
                    json = total.toString();

                    Gson gson = Utilities.getDefaultGson();
                    backup = gson.fromJson(json, Backup.class);
                    if (backup == null || backup.getSaveFileVersion() != Backup.latestSaveFileVersion) {
                        backup = DataVersionCompat.createBackupData(json);
                    }
                } catch (Exception e) {

                    e.printStackTrace();
                    backup = DataVersionCompat.createBackupData(json);
                }

                if (backup == null) {
                    Toast.makeText(getActivity(), "Backup Datei inkompatibel!", Toast.LENGTH_SHORT).show();
                    return;
                }

                VertretungsData.setInstance(backup.getVertretungsData());
                //Löscht veraltete Tage
                Calendar heute = Calendar.getInstance();
                for (int i = 0; i < VertretungsData.getInstance().getTagList().size(); i++) {
                    Calendar tagCalendar = Calendar.getInstance();
                    tagCalendar.setTime(VertretungsData.getInstance().getTagList().get(i).getDatum());
                    if (Utilities.compareDays(tagCalendar, heute) < 0) {
                        VertretungsData.getInstance().getTagList().remove(i);
                        i--;
                    }
                }
                VertretungsData.saveDataToFile(getActivity().getApplicationContext());
                LocalData.deleteNotificationAlarms(getActivity().getApplicationContext());
                LocalData.setInstance(backup.getLocalData());
                LocalData.saveToFile(getActivity().getApplicationContext());
                LocalData.recreateNotificationAlarms(getActivity().getApplicationContext());
                PreferenceHelper.setSharedPrefrencesFromBackup(getActivity().getApplicationContext(), backup.getSharedPreferences());

                //Events
                eventBus.post(new EventsChangedEvent());
                eventBus.post(new FaecherUpdateEvent());
                eventBus.post(new FerienChangedEvent());
                eventBus.post(new KlassenlistUpdatedEvent());
                eventBus.post(new KursChangedEvent());
                eventBus.post(new NotenChangedEvent());

                Toast.makeText(getActivity(), "Backup erfolgreich importiert.", Toast.LENGTH_SHORT).show();


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
        dialogBuilder.setMessage("Möchtest du ein Backup deiner Einstellungen speichern?");
        dialogBuilder.setPositiveButton("Backup erstellen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isBackupExportDialog = false;

                if (!isExternalStorageWritable()) return;

                Calendar calendar = Calendar.getInstance();
                String backUpTitle = String.format(Backup.SAVE_FILE_NAME, new SimpleDateFormat(Backup.DATE_FORMAT, Locale.GERMANY).format(calendar.getTime()));

                Gson gson = Utilities.getDefaultGson();
                Backup backup = new Backup(getActivity().getApplicationContext());
                String json = gson.toJson(backup, Backup.class);

                File path = getBackupPath();
                if (path == null) return;
                try {

                    FileOutputStream outputStream = new FileOutputStream(path.getAbsolutePath() + File.separator + backUpTitle);
                    outputStream.write(json.getBytes(Charset.forName("UTF-8")));
                    outputStream.flush();
                    outputStream.close();

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

    private File getBackupPath() {

        Log.d("JKGDEBUG", Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "JKGStundenplanBackup");
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
