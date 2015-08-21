package de.conradowatz.jkgvertretung.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.AdapterView;
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
import de.greenrobot.event.EventBus;


public class MainActivity extends AppCompatActivity {

    private static final String EXTRA_CUSTOM_TABS_SESSION_ID = "android.support.CUSTOM_TABS:session_id";
    private static final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.CUSTOM_TABS:toolbar_color";
    public VertretungsAPI vertretungsAPI;
    private Toolbar toolbar;
    private Drawer navigationDrawer;
    private Menu menu;
    private StundenplanFragment stundenplanFragment;
    private StundenplanFragment vertretungsplanFragment;
    private KurswahlFragment kurswahlFragment;
    private StundenplanFragment allgVertretungsplanFragment;
    private int currentlySelected;
    private int activeFragmentIdentifier;
    private Boolean isRefreshing = false;
    private boolean isActivityActive;
    private boolean showStartScreenInResume = false;
    private boolean isInfoDialog = false;
    private boolean isNoAccesDialog = false;
    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        buildDrawer();
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);


        if (getLastCustomNonConfigurationInstance() != null) {

            //App noch im Speicher, wiederherstellen
            vertretungsAPI = (VertretungsAPI) getLastCustomNonConfigurationInstance();
            CharSequence title = savedInstanceState.getCharSequence("title");
            getSupportActionBar().setTitle(title);
            int selection = savedInstanceState.getInt("selection");
            if (selection >= 0) navigationDrawer.setSelection(selection, false);
            currentlySelected = savedInstanceState.getInt("currentlySelected");
            activeFragmentIdentifier = savedInstanceState.getInt("activeFragmentIdentifier");
            if (isRefreshing == null) isRefreshing = savedInstanceState.getBoolean("isRefreshing");
            isInfoDialog = savedInstanceState.getBoolean("isInfoDialog");
            isNoAccesDialog = savedInstanceState.getBoolean("isNoAccesDialog");

            if (isInfoDialog) showInfoDialog();
            if (isNoAccesDialog) showFetchErrorDialog();

        } else {

            //App starten
            currentlySelected = -1;
            initializeLoadingData();

        }
    }

    //onResume und onPause speichern ob die Activity gerade aktiv ist
    @Override
    protected void onResume() {
        super.onResume();

        isActivityActive = true;

        //Falls der startScreen gezeigt werden soll, weil die Activity vorher inaktiv war
        if (showStartScreenInResume) {
            showStartScreen();
            showStartScreenInResume = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        isActivityActive = false;
    }

    private void buildDrawer() {

        navigationDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withHeader(R.layout.drawer_header)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Mein Stundenplan").withIcon(R.drawable.ic_stuplan).withIconTintingEnabled(true).withIdentifier(1),
                        new PrimaryDrawerItem().withName("Mein Vertretungsplan").withIcon(R.drawable.ic_vertretung).withIconTintingEnabled(true).withIdentifier(2),
                        new PrimaryDrawerItem().withName("Klassen- / Kurswahl").withIcon(R.drawable.ic_check).withIconTintingEnabled(true).withIdentifier(3),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName("Allgemeiner Vertretungsplan").withIcon(R.drawable.ic_vertretung).withIconTintingEnabled(true).withIdentifier(4),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName("Einstellungen").withIcon(R.drawable.ic_settings).withIconTintingEnabled(true).withIdentifier(11),
                        new PrimaryDrawerItem().withName("Feedback").withIcon(R.drawable.ic_feedback).withIconTintingEnabled(true).withIdentifier(12),
                        new PrimaryDrawerItem().withName("Infos").withIcon(R.drawable.ic_info).withIconTintingEnabled(true).withIdentifier(13)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> adapterView, View view, int position, long l, IDrawerItem drawerItem) {
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
        textView.setText(Html.fromHtml(getString(R.string.infoDialog_text)));
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
        intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, getResources().getColor(R.color.primary));
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
     *
     * @param identifier der Fragment identifier
     */
    private void setFragment(int identifier) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (identifier == 1) {
            //if (stundenplanFragment == null) {
            stundenplanFragment = StundenplanFragment.newInstance(StundenplanFragment.MODE_STUNDENPLAN);
            //}
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_STUNDENPLAN)).commit();
            getSupportActionBar().setTitle("Mein Stundenplan");
        } else if (identifier == 2) {
            //if (vertretungsplanFragment == null) {
            vertretungsplanFragment = StundenplanFragment.newInstance(StundenplanFragment.MODE_VERTRETUNGSPLAN);
            //}
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_VERTRETUNGSPLAN)).commit();
            getSupportActionBar().setTitle("Mein Vertretungsplan");
        } else if (identifier == 3) {
            //if (kurswahlFragment == null) {
                kurswahlFragment = new KurswahlFragment();
            //}
            ft.replace(R.id.container, new KurswahlFragment()).commit();
            getSupportActionBar().setTitle("Klassen- / Kurswahl");
        } else if (identifier == 4) {
            //if (allgVertretungsplanFragment == null) {
            allgVertretungsplanFragment = StundenplanFragment.newInstance(StundenplanFragment.MODE_ALGVERTRETUNGSPLAN);
            //}
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_ALGVERTRETUNGSPLAN)).commit();
            getSupportActionBar().setTitle("Allgemeiner Vertretungsplan");
        }

        activeFragmentIdentifier = identifier;
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
            navigationDrawer.setSelection(2, false);
            currentlySelected = 2;
            setFragment(3);

        } else {

            //Ansonsten zum Stundenplan springen
            int startScreen = Integer.parseInt(PreferenceReader.readStringFromPreferences(this, "startScreen", "0"));
            navigationDrawer.setSelection(startScreen, false);
            currentlySelected = startScreen;
            setFragment(startScreen + 1);

        }

    }

    /**
     * Läd die Daten entweder aus dem Speicher oder mit der LoadingActivity
     * verfährt außerdem nach dem vom Nutzer in den Einstellungen festgelegten Regeln zum weiteren Laden
     */
    private void loadData() {

        //Schauen on eine SavedSession im Speicher ist
        final File savedSessionFile = new File(getFilesDir(), VertretungsAPI.SAVE_FILE_NAE);
        if (savedSessionFile.exists()) {

            String username = PreferenceReader.readStringFromPreferences(this, "username", "");
            String password = PreferenceReader.readStringFromPreferences(this, "password", "");

            //Saved Session laden
            final MainActivity context = this;
            VertretungsAPI.createFromFile(
                    this, username, password, new VertretungsAPI.CreateFromFileHandler() {
                        @Override
                        public void onCreated(VertretungsAPI myVertretungsAPI) {

                            //Wenn die Tage nicht mehr aktuell sind, neue laden
                            if (myVertretungsAPI.getTagList().size() == 0) {
                                savedSessionFile.delete();
                                loadData();
                                return;
                            }

                            //wenn es fertig ist, Fragment öffnen
                            vertretungsAPI = myVertretungsAPI;
                            showStartScreen();

                            //Daten aktualisieren aus dem Interwebs
                            boolean doRefresh = PreferenceReader.readBooleanFromPreferences(context, "doRefreshAtStart", true);
                            if (doRefresh) {

                                isRefreshing = true;
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
                            savedSessionFile.delete();
                            loadData();
                        }
                    }
            );

        } else {

            //Daten aus dem Interwebs herunterladen, dazu LoadingActivity starten
            Intent startLoadingIntent = new Intent(this, LoadingActivity.class);
            final int result = 1;
            startActivityForResult(startLoadingIntent, result);

            eventBus.register(this);

        }

    }

    /**
     * Leitet das Anzeigen der Daten ein
     * wird gecalled, wenn die LoadingActivity Daten sendet - z.B. beim ersten Start der App
     */
    public void onEvent(VertretungsAPI event) {

        vertretungsAPI = event;
        eventBus.unregister(this);

        //Falls die Activity noch nicht wieder aktiv ist, kann kein Fragment angezeigt werdem, in diesem Fall wird dies nach onResume getan
        if (isActivityActive) showStartScreen();
        else showStartScreenInResume = true;

        //Daten als Saved Session speichern
        vertretungsAPI.saveToFile(this);

        //mehr Tage im Hintergrund laden
        int dayCount = Integer.parseInt(PreferenceReader.readStringFromPreferences(this, "maxDaysToFetchStart", "14"));
        if (vertretungsAPI.getTagList().size() == 3 && dayCount > 3) {

            //Ladesymbol zeigen
            showRefresh();
            isRefreshing = true;

            downloadTagList(dayCount - 3, 3);
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
        vertretungsAPI.makeTagList(days, skipDays, new VertretungsAPI.GetDaysHandler() {

            @Override
            public void onFinished() {

                //Wenn alle verfügbaren Tage geladen wurden

                //Wenn Tage geladen wurden, diese speichern
                if (vertretungsAPI.getTagList().size() > skipDays)
                    vertretungsAPI.saveToFile(context);

                //Ladesymbol anhalten
                isRefreshing = false;
                stopRefresh();

            }

            @Override
            public void onDayAdded() {

                //Fragments aktualisieren
                informFragmentsDayAdded();
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

                isRefreshing = true;
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
     * Informiert die Fragments, dass ein Tag hinzugefügt wurde
     */
    private void informFragmentsDayAdded() {

        if (stundenplanFragment != null) {
            stundenplanFragment.onDayAdded();
        }
        if (vertretungsplanFragment != null) {
            vertretungsplanFragment.onDayAdded();
        }
        if (allgVertretungsplanFragment != null) {
            allgVertretungsplanFragment.onDayAdded();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null) return;
        String response = data.getStringExtra("ExitCode");
        switch (response) {
            case "Exit":            //Anwendung schließen, falls von Activity gewünscht
                finish();
                break;
            case "LoggedIn":        //Login Activity hat erfolgreich eingeloggt
                loadData();
                break;
            case "ReLog":           //Falls die Loading Activity neu einloggen will
                relog();
                eventBus.unregister(this);
                break;
        }

    }

    /**
     * Löscht die Benutzerdaten und zeigt LoginActivity
     */
    private void relog() {

        //Benutzerdaten leeren und LoginActivity starten
        PreferenceReader.saveStringToPreferences(this, "username", "null");
        PreferenceReader.saveStringToPreferences(this, "password", "null");
        initializeLoadingData();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {

        return vertretungsAPI;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        this.menu = menu; //Menu für alle Methoden verfügbar machen

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

        if (isRefreshing) {
            return;
        }

        //Analytics
        MyApplication analytics = (MyApplication) getApplication();
        analytics.fireEvent("Toolbar", "Refresh");

        showRefresh();
        isRefreshing = true;

        //Daten im Hintergrund laden
        int dayCount = Integer.parseInt(PreferenceReader.readStringFromPreferences(this, "maxDaysToFetchRefresh", "14"));
        redownloadData(dayCount);

    }

    /**
     * Läd alle Daten (klassenList, freieTageList, tagList) neu herunter
     *
     * @param dayCount Anzahl der zu downloadenden Tage
     */
    private void redownloadData(int dayCount) {

        final MainActivity context = this;
        vertretungsAPI.getAllInfo(dayCount, new VertretungsAPI.AsyncVertretungsResponseHandler() {

            @Override
            public void onSuccess() {
                if (vertretungsAPI.getTagList().size() > 0) {

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
            public void onDayAdded() {

                informFragmentsDayAdded();
            }

            @Override
            public void onKlassenListFinished() {

                if (kurswahlFragment != null) {
                    kurswahlFragment.onKlassenListUpdated();
                }
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

                vertretungsAPI.saveToFile(context);
                stopRefresh();
                isRefreshing = false;
            }
        });

    }

    /**
     * Animiert das Refresh MenuItem
     */
    private void showRefresh() {

        if (menu == null) return;
        MenuItem item = menu.findItem(R.id.action_refresh);

        //Das Refresh Item durch ein ImageView, was sich dreht austauschen
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_icon, null);

        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);

        item.setActionView(iv);

    }

    /**
     * Stoppt die Animation des Refresh Menu Items
     */
    private void stopRefresh() {

        if (menu == null) return;
        MenuItem item = menu.findItem(R.id.action_refresh);

        //Das Refresh Item zurücktauschen
        item.getActionView().clearAnimation();
        item.setActionView(null);

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
        outState.putInt("activeFragmentIdentifier", activeFragmentIdentifier);
        outState.putCharSequence("title", toolbar.getTitle());
        outState.putBoolean("isRefreshing", isRefreshing);
        outState.putBoolean("isInfoDialog", isInfoDialog);
        outState.putBoolean("isNoAccesDialog", isNoAccesDialog);
        super.onSaveInstanceState(outState);
    }
}
