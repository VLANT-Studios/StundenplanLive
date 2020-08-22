package de.conradowatz.jkgvertretung.fragments;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import de.conradowatz.jkgvertretung.tools.ColorAPI;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.variables.AppDatabase;
import petrov.kristiyan.colorpicker.ColorPicker;

import static android.app.Activity.RESULT_OK;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static final int REQUEST_CODE_BACKUP_EXPORT = 1;
    public static final int REQUEST_CODE_BACKUP_IMPORT = 2;
    public static final int REQUEST_CODE_BACKUP_DELETE = 3;
    private static final int REQUEST_CODE_SELECT_IMAGE = 4;
    public static final int REQUEST_CODE_GET_PICTURE = 5;
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

        PreferenceCategory designCategory = (PreferenceCategory) findPreference("designCategory");
        ListPreference startScreen = (ListPreference) findPreference("startScreen");
        ListPreference maxDaysToFetchRefresh = (ListPreference) findPreference("maxDaysToFetchRefresh");
        ListPreference maxDaysToFetchStart = (ListPreference) findPreference("maxDaysToFetchStart");
        ListPreference notificationType = (ListPreference) findPreference("notificationType");
        ListPreference background = (ListPreference) findPreference("background");
        ListPreference pictureFitMode = (ListPreference) findPreference("pictureFitMode");
        Preference color1 = findPreference("color1");
        Preference color2 = findPreference("color2");
        Preference pickFile = findPreference("pickFile");
        Preference actionBarColor = findPreference("actionBarColor");
        Preference accentColor = findPreference("accentColor");
        Preference exportBackup = findPreference("exportBackup");
        Preference importBackup = findPreference("importBackup");
        Preference deleteBackup = findPreference("deleteBackup");

        startScreen.setSummary(startScreen.getEntry());
        maxDaysToFetchRefresh.setSummary(maxDaysToFetchRefresh.getEntry());
        maxDaysToFetchStart.setSummary(maxDaysToFetchStart.getEntry());
        notificationType.setSummary(notificationType.getEntry());
        background.setSummary(background.getEntry());
        pictureFitMode.setSummary(pictureFitMode.getEntry());

        ColorAPI api = new ColorAPI(getActivity());
        actionBarColor.setSummary(String.format("#%x", api.getActionBarColor()));
        accentColor.setSummary(String.format("#%x", api.getAccentColor()));

        startScreen.setOnPreferenceChangeListener(this);
        maxDaysToFetchRefresh.setOnPreferenceChangeListener(this);
        maxDaysToFetchStart.setOnPreferenceChangeListener(this);
        notificationType.setOnPreferenceChangeListener(this);
        pictureFitMode.setOnPreferenceChangeListener(this);

        if (!background.getValue().equals("4")) {
            designCategory.removePreference(color1);
            designCategory.removePreference(color2);
        }
        if (!background.getValue().equals("5")) {
            designCategory.removePreference(pickFile);
            designCategory.removePreference(pictureFitMode);
        }

        background.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @SuppressWarnings("SuspiciousMethodCalls")
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                designCategory.removePreference(color1);
                designCategory.removePreference(color2);
                designCategory.removePreference(pickFile);
                designCategory.removePreference(pictureFitMode);
                if (o.equals("4")) {
                    designCategory.addPreference(color1);
                    designCategory.addPreference(color2);
                } else if (o.equals("5")) {
                    designCategory.addPreference(pickFile);
                    designCategory.addPreference(pictureFitMode);
                }
                ListPreference listPreference = (ListPreference) preference;
                int position = Arrays.asList(listPreference.getEntryValues()).indexOf(o);
                listPreference.setSummary(listPreference.getEntries()[position]);
                return true;
            }
        });

        color1.setOnPreferenceClickListener(preference -> {
            ColorPicker colorPicker = new ColorPicker(getActivity());
            colorPicker.setTitle("Farbe wählen");
            colorPicker.setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
                @Override
                public void setOnFastChooseColorListener(int position, int color) {
                    preference.setSummary(String.format("#%x", color));
                    PreferenceHelper.saveIntToPreferences(getActivity().getApplicationContext(), "color1", color);
                }

                @Override
                public void onCancel(){
                }
            });
            colorPicker.show();
            return true;
        });

        color2.setOnPreferenceClickListener(preference -> {
            ColorPicker colorPicker = new ColorPicker(getActivity());
            colorPicker.setTitle("Farbe wählen");
            colorPicker.setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
                @Override
                public void setOnFastChooseColorListener(int position, int color) {
                    preference.setSummary(String.format("#%x", color));
                    PreferenceHelper.saveIntToPreferences(getActivity().getApplicationContext(), "color2", color);
                }

                @Override
                public void onCancel(){
                }
            });
            colorPicker.show();
            return true;
        });

        pickFile.setOnPreferenceClickListener(preference -> {
            if (getWritePermission(REQUEST_CODE_GET_PICTURE)) {
                String action;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    action = Intent.ACTION_OPEN_DOCUMENT;
                } else {
                    action = Intent.ACTION_PICK;
                }
                Intent intent = new Intent(action);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
            }
            return true;
        });

        actionBarColor.setOnPreferenceClickListener(preference -> {
            ColorPicker colorPicker = new ColorPicker(getActivity());
            colorPicker.setTitle("Farbe wählen");
            ArrayList<String> colorsHexList = new ArrayList<>();
            for (int col : getActivity().getResources().getIntArray(petrov.kristiyan.colorpicker.R.array.default_colors)) {
                if (col == -14654801)
                    col = Color.rgb(0x4a, 0x8a, 0xba);
                if (col == -740056)
                    col = Color.rgb(0xfd, 0xb7, 0x09);
                colorsHexList.add(String.format("#%x", col).toUpperCase());
            }
            colorPicker.setColors(colorsHexList);
            colorPicker.setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
                @Override
                public void onChooseColor(int position, int color) {
                    if (color != 0) {
                        preference.setSummary(String.format("#%x", color));
                        new ColorAPI(getActivity()).setActionBarColor(color);
                        requestRestart();
                    }
                }

                @Override
                public void onCancel(){
                }
            });
            colorPicker.show();
            return true;
        });

        accentColor.setOnPreferenceClickListener(preference -> {
            ColorPicker colorPicker = new ColorPicker(getActivity());
            colorPicker.setTitle("Farbe wählen");
            ArrayList<String> colorsHexList = new ArrayList<>();
            for (int col : getActivity().getResources().getIntArray(petrov.kristiyan.colorpicker.R.array.default_colors)) {
                if (col == -14654801)
                    col = Color.rgb(0x4a, 0x8a, 0xba);
                if (col == -740056)
                    col = Color.rgb(0xfd, 0xb7, 0x09);
                colorsHexList.add(String.format("#%x", col).toUpperCase());
            }
            colorPicker.setColors(colorsHexList);
            colorPicker.setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
                @Override
                public void onChooseColor(int position, int color) {
                    if (color != 0) {
                        preference.setSummary(String.format("#%x", color));
                        new ColorAPI(getActivity()).setAccentColor(color);
                        requestRestart();
                    }
                }

                @Override
                public void onCancel() {
                }
            });
            colorPicker.show();
            return true;
        });

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

    private void requestRestart() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Um die Farbe zu ändern ist ein Neustart der App erforderlich.\nJetzt App neustarten?")
                .setTitle("Neustart erforderlich")
                .setPositiveButton("Ja", (dialogInterface, i) -> {
                    Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(getActivity().getPackageName());
                    int mPendingIntentId = 1530153;
                    PendingIntent mPendingIntent = PendingIntent.getActivity(getActivity(), mPendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager mgr = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                    System.exit(0);
                })
                .setNegativeButton("Nein", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });
        builder.create().show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK) {
            Uri selectedMediaUri = data.getData();
            if (selectedMediaUri == null) {
                Toast.makeText(getActivity(), "Kein Bild ausgewählt!", Toast.LENGTH_SHORT).show();
            } else if (ContentResolver.SCHEME_CONTENT.equals(selectedMediaUri.getScheme())) {
                PreferenceHelper.saveStringToPreferences(getActivity(), "backgroundPictureURI", selectedMediaUri.toString());
                Toast.makeText(getActivity(), "Bild gespeichert!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Kein gültiges Bild ausgewählt! (Nur .png oder .jpg!)", Toast.LENGTH_SHORT).show();
            }
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
            case REQUEST_CODE_GET_PICTURE:
                String action;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    action = Intent.ACTION_OPEN_DOCUMENT;
                } else {
                    action = Intent.ACTION_PICK;
                }
                Intent intent = new Intent(action);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
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
        } else if (listPreference.getKey().equals("pictureFitMode")) {
            PreferenceHelper.saveStringToPreferences(getActivity(), "pictureFitMode", listPreference.getValue());
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
