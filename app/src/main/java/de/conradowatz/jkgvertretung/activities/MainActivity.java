package de.conradowatz.jkgvertretung.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.DayUpdatedEvent;
import de.conradowatz.jkgvertretung.variables.KlassenlistUpdatedEvent;
import de.greenrobot.event.EventBus;


public class MainActivity extends AppCompatActivity {

    private static final String EXTRA_CUSTOM_TABS_SESSION_ID = "android.support.CUSTOM_TABS:session_id";
    private static final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.CUSTOM_TABS:toolbar_color";
    private Toolbar toolbar;
    private Drawer navigationDrawer;
    private MenuItem refreshItem;
    private int currentlySelected;
    private Boolean isRefreshing;
    private boolean isInfoDialog;
    private boolean isNoAccesDialog;

    private boolean isActive;
    private boolean noactiveStartscreen;

    private String username;
    private String password;

    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        buildDrawer();
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        isActive = true;

        if (savedInstanceState != null && (VertretungsData.getsInstance().getTagList() != null || getLastCustomNonConfigurationInstance() != null)) {

            //App noch im Speicher, wiederherstellen
            CharSequence title = savedInstanceState.getCharSequence("title");
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
            int selection = savedInstanceState.getInt("selection");
            if (selection >= 0) navigationDrawer.setSelection(selection, false);
            currentlySelected = savedInstanceState.getInt("currentlySelected");
            if (isRefreshing == null) isRefreshing = savedInstanceState.getBoolean("isRefreshing");
            noactiveStartscreen = savedInstanceState.getBoolean("noactiveStartscreen");
            if (noactiveStartscreen) {
                showStartScreen();
                noactiveStartscreen = false;
            }
            isInfoDialog = savedInstanceState.getBoolean("isInfoDialog");
            isNoAccesDialog = savedInstanceState.getBoolean("isNoAccesDialog");

            if (VertretungsData.getsInstance().getTagList() == null)
                VertretungsData.setInstance((VertretungsData) getLastCustomNonConfigurationInstance());

            username = savedInstanceState.getString("username");
            password = savedInstanceState.getString("password");

            if (isInfoDialog) showInfoDialog();
            if (isNoAccesDialog) showFetchErrorDialog();

        } else {

            //App starten
            currentlySelected = -1;
            isRefreshing = false;
            PreferenceReader.saveBooleanToPrefernces(this, "stopRefreshing", false);
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
                            if (currentlySelected != position) {
                                setFragment(identifier);
                                currentlySelected = position;
                                return false;
                            }
                        } else {
                            navigationDrawer.setSelection(currentlySelected, false);
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
        intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, ContextCompat.getColor(this, R.color.primary));
        startActivity(intent);

    }

    /**
     * öffnet die Einstelungen Activity
     */
    private void openSettings() {

        Intent openSettingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(openSettingsIntent);

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
        }
    }

    /**
     * Leitet das Laden der Daten ein; Prüft ob Benutzerdaten vorhanden sind und öffnet falls nötig die LoginActivity
     */
    private void initializeLoadingData() {

        //Wenn nicht eingeloggt, LoginActivity starten
        if (PreferenceReader.readStringFromPreferences(this, "username", "null").equals("null")) {
            Intent startLoginIntent = new Intent(this, LoginActivity.class);
            final int result = 1;
            startActivityForResult(startLoginIntent, result);
        } else {
            //Ansonsten Daten laden
            username = PreferenceReader.readStringFromPreferences(this, "username", "");
            password = PreferenceReader.readStringFromPreferences(this, "password", "");
            loadData();
        }
    }

    /**
     * wechselt zum vom Nutzer als Startscreen festgelegten Fragment
     */
    private void showStartScreen() {

        boolean keineKlasse = PreferenceReader.readIntFromPreferences(this, "meineKlasseInt", -1) == -1;
        if (keineKlasse) {

            //Falls noch keine Klasse gewählt ist, zur Klassen-/Kurswahl springen
            navigationDrawer.setSelection(3, false);
            currentlySelected = 3;
            setFragment(3);

        } else {

            //Ansonsten zum Stundenplan springen
            int startScreen = Integer.parseInt(PreferenceReader.readStringFromPreferences(this, "startScreen", "1"));
            navigationDrawer.setSelection(startScreen, false);
            currentlySelected = startScreen;
            setFragment(startScreen);

        }

    }

    /**
     * Wird aufgerufen wenn die LoadingActivity beendet wurde und die ersten 3 tage geladen hat
     */
    private void loadingDone() {

        showStartScreen();

        //Daten als Saved Session speichern
        VertretungsAPI.saveDataToFile(this);

        //mehr Tage im Hintergrund laden
        int dayCount = Integer.parseInt(PreferenceReader.readStringFromPreferences(this, "maxDaysToFetchStart", "14"));
        if (VertretungsData.getsInstance().getTagList().size() == 3 && dayCount > 3) {

            //Ladesymbol zeigen
            showRefresh();

            downloadTagList(dayCount - 3, 3);
        }
    }

    /**
     * Wird aufgerufen wenn die LoginActivity beendet wurde und sich erfolgreich eingeloggt hat
     */
    private void loggedIn() {

        username = PreferenceReader.readStringFromPreferences(this, "username", "");
        password = PreferenceReader.readStringFromPreferences(this, "password", "");
        loadData();
    }

    /**
     * Läd die Daten entweder aus dem Speicher oder startet die LoadingActivity
     * verfährt außerdem nach den vom Nutzer in den Einstellungen festgelegten Regeln zum weiteren Laden
     */
    private void loadData() {

        //Schauen on eine SavedSession im Speicher ist
        final File savedSessionFile = new File(getFilesDir(), VertretungsAPI.SAVE_FILE_NAE);
        if (savedSessionFile.exists()) {

            //Saved Session laden
            final MainActivity context = this;
            VertretungsAPI.createDataFromFile(
                    this, new VertretungsAPI.CreateDataFromFileListener() {
                        @Override
                        public void onCreated() {

                            //Wenn die Tage nicht mehr aktuell sind, neue laden
                            if (VertretungsData.getsInstance().getTagList().size() == 0) {
                                boolean deleted = savedSessionFile.delete();
                                loadData();
                                return;
                            }

                            //wenn es fertig ist, Fragment öffnen
                            if (isActive) showStartScreen();
                            else noactiveStartscreen = true;

                            //Daten aktualisieren aus dem Interwebs
                            boolean doRefresh = PreferenceReader.readBooleanFromPreferences(context, "doRefreshAtStart", true);
                            if (doRefresh) {

                                showRefresh();
                                int dayCount = Integer.parseInt(PreferenceReader.readStringFromPreferences(context, "maxDaysToFetchStart", "14"));
                                redownloadData(dayCount);
                            }

                        }

                        @Override
                        public void onError(Throwable throwable) {

                            //Falls es einnen Fehler gab (z.B. neue App Version nicht mit Saved Session kompatibel), neu herunterladen
                            Log.e("JKGDEBUG", "Error loading data from storage. Redownload it...");
                            throwable.printStackTrace();
                            boolean deleted = savedSessionFile.delete();
                            loadData();
                        }
                    }
            );

        } else {

            //Daten aus dem Interwebs herunterladen, dazu LoadingActivity starten
            Intent startLoadingIntent = new Intent(this, LoadingActivity.class);
            final int result = 1;
            startActivityForResult(startLoadingIntent, result);

        }

    }

    /**
     * Läd Tage neu herunter
     *
     * @param days     Anzahl der Tage
     * @param skipDays Wieviele tage es (ab heute) überspringen soll
     */
    private void downloadTagList(int days, final int skipDays) {

        final MainActivity context = this;
        new VertretungsAPI(username, password).downloadDays(days, skipDays, new VertretungsAPI.DownloadDaysListener() {

            @Override
            public void onFinished() {

                //Wenn Tage geladen wurden, diese speichern
                if (VertretungsData.getsInstance().getTagList().size() > skipDays)
                    VertretungsAPI.saveDataToFile(context);

                //Ladesymbol anhalten
                stopRefresh();

            }

            @Override
            public void onDayAdded(int position) {

                //Fragments aktualisieren
                informFragmentsDayAdded(position);
            }

            @Override
            public void onError(Throwable throwable) {

                //Wenn es hier ein Error gibt, hat sich warscheinlich das Online System geändert
                Log.e("JKGDEBUG", "Error bei der Verarbeitung der Daten");
                throwable.printStackTrace();
            }
        });

    }

    /**
     * Zeig den NoAcces Fehler-Dialog an
     */
    private void showFetchErrorDialog() {

        isNoAccesDialog = true;

        final MainActivity context = this;
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
                int dayCount = Integer.parseInt(PreferenceReader.readStringFromPreferences(context, "maxDaysToFetch", "14"));
                redownloadData(dayCount);

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

    /**
     * Informiert die Fragments, dass ein Tag hinzugefügt bzw geupdatet wurde
     * @param position welcher Tag wurde hinzugefügt / geupdatet
     */
    private void informFragmentsDayAdded(int position) {

        eventBus.post(new DayUpdatedEvent(position));

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null) return;
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
     * Löscht die Benutzerdaten und initialisert das Laden neu
     */
    private void relog() {

        //Benutzerdaten leeren und LoginActivity starten
        PreferenceReader.saveStringToPreferences(this, "username", "null");
        PreferenceReader.saveStringToPreferences(this, "password", "null");
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
        int dayCount = Integer.parseInt(PreferenceReader.readStringFromPreferences(this, "maxDaysToFetchRefresh", "14"));
        redownloadData(dayCount);

    }

    /**
     * Läd alle Daten (klassenList, freieTageList, tagList) neu herunter
     * @param dayCount Anzahl der zu downloadenden Tage
     */
    private void redownloadData(int dayCount) {

        final MainActivity context = this;
        new VertretungsAPI(username, password).getAllInfo(dayCount, new VertretungsAPI.AllInfoResponseListener() {

            @Override
            public void onSuccess() {
                if (VertretungsData.getsInstance().getTagList().size() > 0) {

                    Toast.makeText(context, "Daten erfolgreich aktualisiert", Toast.LENGTH_SHORT).show();
                    onFinished();

                } else {

                    onNoAccess();
                }
            }

            @Override
            public void onNoConnection() {

                Toast.makeText(context, "Keine Verbindung zum Server!", Toast.LENGTH_LONG).show();
                onFinished();

            }

            @Override
            public void onDayAdded(int position) {

                informFragmentsDayAdded(position);
            }

            @Override
            public void onKlassenListFinished() {

                eventBus.post(new KlassenlistUpdatedEvent());
            }

            @Override
            public void onNoAccess() {

                showFetchErrorDialog();
                onFinished();

            }

            @Override
            public void onOtherError(Throwable throwable) {

                //Wenn es hier ein Error gibt, hat sich warscheinlich das Online System geändert
                Log.e("JKGDEBUG", "Error bei der Verarbeitung der Daten");
                throwable.printStackTrace();
            }

            private void onFinished() {

                VertretungsAPI.saveDataToFile(context);
                stopRefresh();
            }
        });

    }

    /**
     * Animiert das Refresh MenuItem
     */
    private void showRefresh() {

        if (refreshItem == null) return;
        boolean stopRefreshing = PreferenceReader.readBooleanFromPreferences(getApplicationContext(), "stopRefreshing", true);
        if (stopRefreshing) return;

        isRefreshing = true;
        PreferenceReader.saveBooleanToPrefernces(this, "stopRefreshing", false);

        //Das Refresh Item durch ein ImageView, was sich dreht austauschen
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_icon, null);

        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
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

                boolean stopRefreshing = PreferenceReader.readBooleanFromPreferences(getApplicationContext(), "stopRefreshing", true);
                if (stopRefreshing) {
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

        PreferenceReader.saveBooleanToPrefernces(this, "stopRefreshing", true);
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

        outState.putInt("selection", navigationDrawer.getCurrentSelection());
        outState.putInt("currentlySelected", currentlySelected);
        outState.putCharSequence("title", toolbar.getTitle());
        outState.putBoolean("isInfoDialog", isInfoDialog);
        outState.putBoolean("isNoAccesDialog", isNoAccesDialog);
        outState.putBoolean("noactiveStartscreen", noactiveStartscreen);
        outState.putString("username", username);
        outState.putString("password", password);
        outState.putBoolean("isRefreshing", isRefreshing);

        super.onSaveInstanceState(outState);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return VertretungsData.getsInstance();
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
