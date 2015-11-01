package de.conradowatz.jkgvertretung.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.io.File;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.fragments.KurswahlFragment;
import de.conradowatz.jkgvertretung.fragments.StundenplanFragment;
import de.conradowatz.jkgvertretung.fragments.TaskFragment;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.DayUpdatedEvent;
import de.conradowatz.jkgvertretung.variables.KlassenlistUpdatedEvent;
import de.greenrobot.event.EventBus;


public class MainActivity extends AppCompatActivity implements TaskFragment.TaskCallbacks {

    private static final String EXTRA_CUSTOM_TABS_SESSION_ID = "android.support.CUSTOM_TABS:session_id";
    private static final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.CUSTOM_TABS:toolbar_color";
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    private Toolbar toolbar;
    private Drawer navigationDrawer;
    private MenuItem refreshItem;
    private int selectedIdentifier;
    private boolean isRefreshing;
    private boolean isInfoDialog;
    private boolean isNoAccesDialog;
    private boolean isActive;
    private boolean noactiveStartscreen;
    private TaskFragment taskFragment;
    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        buildDrawer();
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.settings, false);

        isActive = true;

        FragmentManager fm = getSupportFragmentManager();
        taskFragment = (TaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (taskFragment == null) {
            taskFragment = new TaskFragment();
            fm.beginTransaction().add(taskFragment, TAG_TASK_FRAGMENT).commit();
        }

        if (savedInstanceState != null && (VertretungsData.getInstance().getTagList() != null || getLastCustomNonConfigurationInstance() != null)) {

            //App noch im Speicher, wiederherstellen
            CharSequence title = savedInstanceState.getCharSequence("title");
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
            selectedIdentifier = savedInstanceState.getInt("selectedIdentifier");
            if (selectedIdentifier >= 0) navigationDrawer.setSelection(selectedIdentifier);
            isRefreshing = savedInstanceState.getBoolean("isRefreshing");
            if (isRefreshing) {
                boolean stopRefresh = PreferenceReader.readBooleanFromPreferences(getApplicationContext(), "stopRefresh", false);
                if (stopRefresh) {
                    isRefreshing = false;
                    PreferenceReader.saveBooleanToPrefernces(getApplicationContext(), "stopRefresh", false);
                }
            }
            noactiveStartscreen = savedInstanceState.getBoolean("noactiveStartscreen");
            if (noactiveStartscreen) {
                showStartScreen();
                noactiveStartscreen = false;
            }
            isInfoDialog = savedInstanceState.getBoolean("isInfoDialog");
            isNoAccesDialog = savedInstanceState.getBoolean("isNoAccesDialog");

            if (VertretungsData.getInstance().getTagList() == null)
                VertretungsData.setInstance((VertretungsData) getLastCustomNonConfigurationInstance());

            if (isInfoDialog) showInfoDialog();
            if (isNoAccesDialog) showFetchErrorDialog();

        } else {

            //App starten
            selectedIdentifier = 1;
            initializeLoadingData();

        }
    }

    private PrimaryDrawerItem getDefaultDrawerItem() {

        return new PrimaryDrawerItem().withIconTintingEnabled(true).withSelectedIconColorRes(R.color.primary).withSelectedTextColorRes(R.color.primary);
    }

    private void buildDrawer() {

        navigationDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withHeader(R.layout.drawer_header)
                .addDrawerItems(
                        getDefaultDrawerItem().withName("Mein Stundenplan").withIcon(R.drawable.ic_stuplan).withIdentifier(1),
                        getDefaultDrawerItem().withName("Mein Vertretungsplan").withIcon(R.drawable.ic_vertretung).withIdentifier(2),
                        getDefaultDrawerItem().withName("Klassen- / Kurswahl").withIcon(R.drawable.ic_check).withIdentifier(3),
                        new DividerDrawerItem(),
                        getDefaultDrawerItem().withName("Allgemeiner Vertretungsplan").withIcon(R.drawable.ic_vertretung).withIdentifier(4),
                        getDefaultDrawerItem().withName("Klassenplan").withIcon(R.drawable.ic_stuplan).withIdentifier(5),
                        new DividerDrawerItem(),
                        getDefaultDrawerItem().withName("Einstellungen").withIcon(R.drawable.ic_settings).withIdentifier(11),
                        getDefaultDrawerItem().withName("Feedback").withIcon(R.drawable.ic_feedback).withIdentifier(12),
                        getDefaultDrawerItem().withName("Infos").withIcon(R.drawable.ic_info).withIdentifier(13)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        int identifier = drawerItem.getIdentifier();
                        if (identifier < 10) {
                            if (selectedIdentifier != identifier && VertretungsData.getInstance().getTagList() != null) {
                                setFragment(identifier);
                                selectedIdentifier = identifier;
                                return false;
                            }
                        } else {
                            navigationDrawer.setSelection(selectedIdentifier, false);
                            switch (identifier) {
                                case 11:
                                    openSettings();
                                    return false;
                                case 12:
                                    openFeedbackPage();
                                    break;
                                case 13:
                                    showInfoDialog();
                                    break;
                            }
                        }
                        return true;
                    }
                })
                .build();

    }

    /**
     * Zeigt den Info Dialog
     */
    private void showInfoDialog() {

        //Analytics
        MyApplication analytics = (MyApplication) getApplication();
        analytics.fireEvent("NavDrawer", "Infos");

        isInfoDialog = true;

        LayoutInflater inflater = getLayoutInflater();
        View scrollView = inflater.inflate(R.layout.infotext_dialog, null);
        TextView textView = (TextView) scrollView.findViewById(R.id.textView);
        String infoHtml = getString(R.string.infoDialog_text);
        String versionName = "";
        try {
            versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        infoHtml = String.format(infoHtml, versionName);
        textView.setText(Html.fromHtml(infoHtml));
        textView.setMovementMethod(LinkMovementMethod.getInstance()); //Link klickbar machen

        AlertDialog.Builder infoDialogB = new AlertDialog.Builder(this);
        infoDialogB.setView(scrollView);
        infoDialogB.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                isInfoDialog = false;
            }
        });
        infoDialogB.show();
    }

    /**
     * Öffnet die Feedback Page in einem Chrome Custom Tab
     */
    private void openFeedbackPage() {

        //Analytics
        MyApplication analytics = (MyApplication) getApplication();
        analytics.fireEvent("NavDrawer", "Feedback");


        String url = "http://conradowatz.de/android-apps/jkg-vertretung-support/";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.putExtra(EXTRA_CUSTOM_TABS_SESSION_ID, -1); // -1 or any valid session id returned from newSession() call
        intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, ContextCompat.getColor(getApplicationContext(), R.color.primary));
        startActivity(intent);

    }

    /**
     * öffnet die Einstelungen Activity
     */
    private void openSettings() {

        Intent openSettingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(openSettingsIntent);
        overridePendingTransition(R.anim.slide_in_right, 0);

    }

    /**
     * Wechselt das angezeigte Fragment
     * @param identifier der Fragment identifier
     */
    private void setFragment(int identifier) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (identifier == 1) {
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_STUNDENPLAN)).commit();
            toolbar.setTitle("Mein Stundenplan");
        } else if (identifier == 2) {
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_VERTRETUNGSPLAN)).commit();
            toolbar.setTitle("Mein Vertretungsplan");
        } else if (identifier == 3) {
            ft.replace(R.id.container, new KurswahlFragment()).commit();
            toolbar.setTitle("Klassen- / Kurswahl");
        } else if (identifier == 4) {
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_ALGVERTRETUNGSPLAN)).commit();
            toolbar.setTitle("Allgemeiner Vertretungsplan");
        } else if (identifier == 5) {
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_KLASSENPLAN)).commit();
            toolbar.setTitle("Klassenplan");
        }
    }

    /**
     * wechselt zum vom Nutzer als Startscreen festgelegten Fragment
     */
    private void showStartScreen() {

        boolean keineKlasse = PreferenceReader.readIntFromPreferences(getApplicationContext(), "meineKlasseInt", -1) == -1;
        if (keineKlasse) {

            //Falls noch keine Klasse gewählt ist, zur Klassen-/Kurswahl springen
            navigationDrawer.setSelection(3, false);
            selectedIdentifier = 3;
            setFragment(3);

        } else {

            //Ansonsten zum Stundenplan springen
            int startScreenIdentifier = Integer.parseInt(PreferenceReader.readStringFromPreferences(getApplicationContext(), "startScreen", "1"));
            if (startScreenIdentifier < 1) {
                PreferenceReader.saveStringToPreferences(getApplicationContext(), "startScreen", "1");
                startScreenIdentifier = 1;
            }
            navigationDrawer.setSelection(startScreenIdentifier, false);
            selectedIdentifier = startScreenIdentifier;
            setFragment(startScreenIdentifier);

        }

    }

    /**
     * Leitet das Laden der Daten ein; Prüft ob Benutzerdaten vorhanden sind und öffnet falls nötig die LoginActivity
     */
    private void initializeLoadingData() {

        //Wenn nicht eingeloggt, LoginActivity starten
        if (PreferenceReader.readStringFromPreferences(getApplicationContext(), "username", "null").equals("null")) {
            Intent startLoginIntent = new Intent(getApplicationContext(), LoginActivity.class);
            startLoginIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityForResult(startLoginIntent, 1);
        } else {
            //Ansonsten Daten laden
            loadData();
        }
    }

    /**
     * Läd die Daten entweder aus dem Speicher oder startet die LoadingActivity
     * verfährt außerdem nach den vom Nutzer in den Einstellungen festgelegten Regeln zum weiteren Laden
     */
    private void loadData() {

        //Schauen on eine SavedSession im Speicher ist
        File savedSessionFile = new File(getFilesDir(), VertretungsAPI.SAVE_FILE_NAME);
        if (savedSessionFile.exists()) {

            //Saved Session laden
            taskFragment.createDataFromFile();

        } else {

            //Daten aus dem Interwebs herunterladen, dazu LoadingActivity starten
            Intent startLoadingIntent = new Intent(getApplicationContext(), LoadingActivity.class);
            startLoadingIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityForResult(startLoadingIntent, 1);

        }

    }

    /**
     * Wird aufgerufen wenn VertretungsData erfolgreich aus dem App Speicher erstellt wurde
     */
    @Override
    public void onDataCreated() {

        //wenn es fertig ist, Fragment öffnen
        if (isActive) showStartScreen();
        else noactiveStartscreen = true;

        //Daten aktualisieren aus dem Interwebs
        boolean doRefresh = PreferenceReader.readBooleanFromPreferences(getApplicationContext(), "doRefreshAtStart", true);
        if (doRefresh) {

            showRefresh();
            int dayCount = Integer.parseInt(PreferenceReader.readStringFromPreferences(getApplicationContext(), "maxDaysToFetchStart", "14"));
            taskFragment.downloadAllData(dayCount);
        }

    }

    /**
     * Wird gecallt wenn es einen Fehler beim Laden der VertretungsData aus dem Speicher gab
     * @param throwable der Fehler
     */
    @Override
    public void onDataCreateError(Throwable throwable) {

        //Falls es einnen Fehler gab (z.B. neue App Version nicht mit Saved Session kompatibel), neu herunterladen
        Log.e("JKGDEBUG", "Fehler beim Laden der Daten aus dem Speicher.");
        Log.e("JKGDEBUG", "Message: " + throwable.getMessage());

        File savedSessionFile = new File(getFilesDir(), VertretungsAPI.SAVE_FILE_NAME);
        boolean deleted = savedSessionFile.delete();
        loadData();

        if (!throwable.getMessage().startsWith("Tagliste nicht mehr aktuell"))
            ((MyApplication) getApplication()).fireException(throwable);
    }

    /**
     * Wird gecallt wenn das Laden von einzelnen Tagen (nach dem Beenden der LoadingActivity) erfolgreich beendet wurde
     *
     * @param skipDays wie viele Tage übersprungen wurden
     */
    @Override
    public void onUpdateDaysFinished(int skipDays) {

        //Wenn Tage geladen wurden, diese speichern
        if (VertretungsData.getInstance().getTagList().size() > skipDays)
            taskFragment.saveDataToFile();

        //Ladesymbol anhalten
        stopRefresh();

    }

    /**
     * Informiert die Fragments, dass ein Tag hinzugefügt bzw geupdatet wurde
     *
     * @param position welcher Tag wurde hinzugefügt / geupdatet
     */
    @Override
    public void onDayAdded(int position) {

        eventBus.post(new DayUpdatedEvent(position));

    }

    /**
     * Wird gecallt wenn es einen unbekannten Fehler beim Herunterladen von Daten gab
     *
     * @param throwable der Fehler
     */
    @Override
    public void onDownloadError(Throwable throwable) {

        //Wenn es hier ein Error gibt, hat sich warscheinlich das Online System geändert
        Log.e("JKGDEBUG", "Fehler beim Download oder Verarbeiten der Daten");
        if (throwable == null) return;
        Log.e("JKGDEBUG", "Message: " + throwable.getMessage());

        ((MyApplication) getApplication()).fireException(throwable);

    }

    /**
     * Zeig den NoAcces Fehler-Dialog an
     */
    private void showFetchErrorDialog() {

        isNoAccesDialog = true;

        final AlertDialog alertD = new AlertDialog.Builder(this).create();
        LayoutInflater layoutInflater = getLayoutInflater();
        View promptView = layoutInflater.inflate(R.layout.no_acces_dialog, null);

        Button buttonRelog = (Button) promptView.findViewById(R.id.buttonRelog);
        Button buttonReload = (Button) promptView.findViewById(R.id.buttonReload);
        Button buttonCancel = (Button) promptView.findViewById(R.id.buttonCancel);

        buttonRelog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                relog();
                alertD.cancel();
                isNoAccesDialog = false;
            }
        });
        buttonReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showRefresh();
                int dayCount = Integer.parseInt(PreferenceReader.readStringFromPreferences(getApplicationContext(), "maxDaysToFetch", "14"));
                taskFragment.downloadAllData(dayCount);

                alertD.cancel();
                isNoAccesDialog = false;
            }
        });
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                alertD.cancel();
                isNoAccesDialog = false;
            }
        });

        alertD.setView(promptView);
        alertD.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null || requestCode != 1) return;
        String response = data.getStringExtra("ExitCode");
        switch (response) {
            case "Exit":            //Anwendung schließen, falls von Activity gewünscht
                finish();
                break;
            case "LoggedIn":        //LoginActivity hat erfolgreich eingeloggt
                loggedIn();
                break;
            case "LoadingDone":     //LoadingActivity hat erfolgreich alles geladen
                loadingDone();
                break;
            case "ReLog":           //LoadingActivity fordert neu einloggen
                relog();
                break;
        }

    }

    /**
     * Wird aufgerufen wenn die LoadingActivity beendet wurde und die ersten 3 Tage geladen hat
     */
    private void loadingDone() {

        if (isActive) showStartScreen();
        else noactiveStartscreen = true;

        //Daten als Saved Session speichern
        taskFragment.saveDataToFile();

        //mehr Tage im Hintergrund laden
        int dayCount = Integer.parseInt(PreferenceReader.readStringFromPreferences(getApplicationContext(), "maxDaysToFetchStart", "14"));
        if (VertretungsData.getInstance().getTagList().size() == 3 && dayCount > 3) {

            //Ladesymbol zeigen
            showRefresh();

            //Callback auf onUpdateDaysFinished
            taskFragment.updateDays(dayCount - 3, 3);
        }
    }

    /**
     * Wird aufgerufen wenn die LoginActivity beendet wurde und sich erfolgreich eingeloggt hat
     */
    private void loggedIn() {

        loadData();
    }

    /**
     * Löscht die Benutzerdaten und initialisert das Laden neu
     */
    private void relog() {

        //Benutzerdaten leeren und LoginActivity starten
        PreferenceReader.saveStringToPreferences(getApplicationContext(), "username", "null");
        PreferenceReader.saveStringToPreferences(getApplicationContext(), "password", "null");
        initializeLoadingData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        refreshItem = menu.findItem(R.id.action_refresh); //Menu für alle Methoden verfügbar machen

        if (isRefreshing) { //Falls noch Daten geladen werden, das auch zeigen
            showRefresh();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //Wenn refresh geklickt wurde
        if (id == R.id.action_refresh) {
            refreshClicked();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Wird gecallt, wenn auf refresh geklickt wurde
     */
    private void refreshClicked() {

        if (isRefreshing) return;

        //Analytics
        MyApplication analytics = (MyApplication) getApplication();
        analytics.fireEvent("Toolbar", "Refresh");

        showRefresh();

        //Daten im Hintergrund laden
        int dayCount = Integer.parseInt(PreferenceReader.readStringFromPreferences(getApplicationContext(), "maxDaysToFetchRefresh", "14"));
        taskFragment.downloadAllData(dayCount);

    }

    /**
     * Wird gecallt wenn downloadAllData beendet wurde
     */
    @Override
    public void onRefreshFinished() {

        if (VertretungsData.getInstance().getTagList().size() > 0) {

            Toast.makeText(getApplicationContext(), "Daten erfolgreich aktualisiert", Toast.LENGTH_SHORT).show();
            taskFragment.saveDataToFile();
            stopRefresh();

        } else {

            onNoAccess();
        }

    }

    /**
     * Wird gecallt wenn bei downloadAllData keine Verbindung hergestellt werden konnte
     */
    @Override
    public void onRefreshNoConnection() {

        Toast.makeText(getApplicationContext(), "Keine Verbindung zum Server!", Toast.LENGTH_LONG).show();
        stopRefresh();
    }

    /**
     * Wird gecallt wenn sich die KlassenList Verändert haben könnte
     */
    @Override
    public void onKlassenListUpdated() {

        eventBus.post(new KlassenlistUpdatedEvent());
    }

    /**
     * Wird gecallt wenn der Server die Verbindungsabfrage mit 401 ablehnt
     */
    @Override
    public void onNoAccess() {

        showFetchErrorDialog();
        stopRefresh();
    }

    /**
     * Animiert das Refresh MenuItem
     */
    private void showRefresh() {

        if (refreshItem == null) return;

        isRefreshing = true;

        //Das Refresh Item durch ein ImageView, was sich dreht austauschen
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_icon, null);

        Animation rotation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
        rotation.setRepeatCount(Animation.INFINITE);
        rotation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

                if (!isRefreshing && refreshItem != null && refreshItem.getActionView() != null) {
                    isRefreshing = false;
                    refreshItem.getActionView().clearAnimation();
                    refreshItem.setActionView(null);
                }
            }
        });
        iv.startAnimation(rotation);

        refreshItem.setActionView(iv);
    }

    /**
     * Stoppt die Animation des Refresh Menu Items
     */
    private void stopRefresh() {

        isRefreshing = false;
        if (!isActive)
            PreferenceReader.saveBooleanToPrefernces(getApplicationContext(), "stopRefresh", true);
    }

    @Override
    public void onBackPressed() {

        //Wenn der Drawer noch offen ist, erst ihn schließen, dann beenden
        if (navigationDrawer != null && navigationDrawer.isDrawerOpen()) {
            navigationDrawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt("selectedIdentifier", selectedIdentifier);
        outState.putCharSequence("title", toolbar.getTitle());
        outState.putBoolean("isInfoDialog", isInfoDialog);
        outState.putBoolean("isNoAccesDialog", isNoAccesDialog);
        outState.putBoolean("noactiveStartscreen", noactiveStartscreen);
        outState.putBoolean("isRefreshing", isRefreshing);

        super.onSaveInstanceState(outState);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return VertretungsData.getInstance();
    }

    @Override
    protected void onResume() {

        isActive = true;
        if (noactiveStartscreen) {
            showStartScreen();
            noactiveStartscreen = false;
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        isActive = false;
        super.onPause();
    }
}
