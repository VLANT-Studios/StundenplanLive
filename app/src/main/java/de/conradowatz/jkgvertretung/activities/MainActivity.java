package de.conradowatz.jkgvertretung.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.transition.Slide;
import android.transition.Transition;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.greenrobot.eventbus.EventBus;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.events.AnalyticsEventEvent;
import de.conradowatz.jkgvertretung.events.DayUpdatedEvent;
import de.conradowatz.jkgvertretung.events.EventsChangedEvent;
import de.conradowatz.jkgvertretung.events.KlassenlistUpdatedEvent;
import de.conradowatz.jkgvertretung.fragments.EventFragment;
import de.conradowatz.jkgvertretung.fragments.FreieZimmerFragment;
import de.conradowatz.jkgvertretung.fragments.NotenUebersichtFragment;
import de.conradowatz.jkgvertretung.fragments.StundenplanFragment;
import de.conradowatz.jkgvertretung.fragments.TaskFragment;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.tools.VertretungsData;


public class MainActivity extends AppCompatActivity implements TaskFragment.MainCallbacks {

    private Toolbar toolbar;
    private Drawer navigationDrawer;
    private MenuItem refreshItem;
    private long selectedIdentifier;
    private boolean isRefreshing;
    private boolean isInfoDialog;
    private boolean isNoAccessDialog;
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
        taskFragment = (TaskFragment) fm.findFragmentByTag(TaskFragment.TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (taskFragment == null) {
            taskFragment = new TaskFragment();
            fm.beginTransaction().add(taskFragment, TaskFragment.TAG_TASK_FRAGMENT).commit();
        }

        //Falls der SplashScreen einen Login möchte
        if (savedInstanceState == null && getIntent().getBooleanExtra("startLogin", false)) {

            startLoginActivity();
            return;
        }

        if (savedInstanceState != null && VertretungsData.getInstance().isReady() && LocalData.isReady()) {

            //App noch im Speicher, wiederherstellen

            CharSequence title = savedInstanceState.getCharSequence("title");
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
            selectedIdentifier = savedInstanceState.getLong("selectedIdentifier");
            if (selectedIdentifier >= 0) navigationDrawer.setSelection(selectedIdentifier);
            isRefreshing = savedInstanceState.getBoolean("isRefreshing");
            if (isRefreshing) {
                boolean stopRefresh = PreferenceHelper.readBooleanFromPreferences(getApplicationContext(), "stopRefresh", false);
                if (stopRefresh) {
                    isRefreshing = false;
                    PreferenceHelper.saveBooleanToPrefernces(getApplicationContext(), "stopRefresh", false);
                }
            }
            noactiveStartscreen = savedInstanceState.getBoolean("noactiveStartscreen");
            if (noactiveStartscreen) {
                showStartScreen();
                noactiveStartscreen = false;
            }
            isInfoDialog = savedInstanceState.getBoolean("isInfoDialog");
            isNoAccessDialog = savedInstanceState.getBoolean("isNoAccessDialog");

            if (isInfoDialog) showInfoDialog();
            if (isNoAccessDialog) showNoAccesDialog();

        } else {

            if (VertretungsData.getInstance().isReady() && LocalData.isReady()) {

                //Falls alles normal -> starten
                setUp();

            } else {

                //Falls Daten fehlen, SplashScreen zeigen
                Intent splashScreenIntent = new Intent(this, SplashActivity.class);
                splashScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(splashScreenIntent);
                finish();
                overridePendingTransition(0, 0);
            }

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
                        getDefaultDrawerItem().withName("Mein Vertretungsplan").withIcon(R.drawable.ic_person).withIdentifier(2),
                        getDefaultDrawerItem().withName("Termine").withIcon(R.drawable.ic_event).withIdentifier(3),
                        getDefaultDrawerItem().withName("Notenübersicht").withIcon(R.drawable.ic_insert_chart).withIdentifier(4),
                        new DividerDrawerItem(),
                        getDefaultDrawerItem().withName("Allgemeiner Vertretungsplan").withIcon(R.drawable.ic_vertretung).withIdentifier(5),
                        getDefaultDrawerItem().withName("Klassenplan").withIcon(R.drawable.ic_stuplan).withIdentifier(6),
                        getDefaultDrawerItem().withName("Freie Zimmer").withIcon(R.drawable.ic_door).withIdentifier(7),
                        new DividerDrawerItem(),
                        getDefaultDrawerItem().withName("Einstellungen").withIcon(R.drawable.ic_settings).withIdentifier(11),
                        getDefaultDrawerItem().withName("Feedback").withIcon(R.drawable.ic_feedback).withIdentifier(12),
                        getDefaultDrawerItem().withName("Infos").withIcon(R.drawable.ic_info).withIdentifier(13)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {

                        long identifier = drawerItem.getIdentifier();
                        if (identifier < 10) {
                            if (selectedIdentifier != identifier && VertretungsData.getInstance().isReady()) {
                                setFragment(identifier);
                                selectedIdentifier = identifier;
                                return false;
                            }
                        } else {
                            navigationDrawer.setSelection(selectedIdentifier, false);
                            if (identifier == 11) {
                                openSettings();
                                return false;
                            }
                            if (identifier == 12) {
                                openFeedbackPage();
                            }
                            if (identifier == 13) {
                                showInfoDialog();
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
        eventBus.post(new AnalyticsEventEvent("NavDrawer", "Infos"));

        isInfoDialog = true;

        LayoutInflater inflater = getLayoutInflater();
        View scrollView = inflater.inflate(R.layout.dialog_infotext, null);
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
        infoDialogB.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
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
        eventBus.post(new AnalyticsEventEvent("NavDrawer", "Feedback"));


        String url = "http://conradowatz.de/android-apps/jkg-vertretung-support/";
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(getApplicationContext(), R.color.primary));
        builder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
        builder.setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right);
        CustomTabsIntent intent = builder.build();
        intent.launchUrl(this, Uri.parse(url));

    }

    /**
     * öffnet die Einstelungen Activity
     */
    private void openSettings() {

        Intent openSettingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        //Animation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            transitionToActivity(openSettingsIntent, new Slide(Gravity.LEFT));
        } else {
            startActivity(openSettingsIntent);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void transitionToActivity(Intent intent, Transition exitTransition) {

        getWindow().setExitTransition(exitTransition);
        getWindow().setAllowReturnTransitionOverlap(true);
        startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(this).toBundle());
    }

    /**
     * öffnet den Fächer und Ferien Manager
     */
    public void openManager() {

        Intent openManagerIntent = new Intent(getApplicationContext(), ManagerActivity.class);
        //Animation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            transitionToActivity(openManagerIntent, new Slide(Gravity.BOTTOM));
        } else {
            startActivity(openManagerIntent);
        }

    }

    /**
     * Wechselt das angezeigte Fragment
     *
     * @param identifier der Fragment identifier
     */
    private void setFragment(long identifier) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (identifier == 1) {
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_STUNDENPLAN)).commit();
            toolbar.setTitle("Mein Stundenplan");
        } else if (identifier == 2) {
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_VERTRETUNGSPLAN)).commit();
            toolbar.setTitle("Mein Vertretungsplan");
        } else if (identifier == 3) {
            ft.replace(R.id.container, EventFragment.newInstance(EventFragment.MODE_ALLGEMEIN)).commit();
            toolbar.setTitle("Termine");
        } else if (identifier == 4) {
            ft.replace(R.id.container, new NotenUebersichtFragment()).commit();
            toolbar.setTitle("Notenübersicht");
        } else if (identifier == 5) {
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_ALGVERTRETUNGSPLAN)).commit();
            toolbar.setTitle("Allgemeiner Vertretungsplan");
        } else if (identifier == 6) {
            ft.replace(R.id.container, StundenplanFragment.newInstance(StundenplanFragment.MODE_KLASSENPLAN)).commit();
            toolbar.setTitle("Klassenplan");
        } else if (identifier == 7) {
            ft.replace(R.id.container, new FreieZimmerFragment()).commit();
            toolbar.setTitle("Freie Zimmer");
        }
    }

    /**
     * Zeigt das Start Fragment und akrualisiert die VertretungsData wenn in Einstellungen gewünscht
     */
    private void setUp() {

        if (isActive) showStartScreen();
        else noactiveStartscreen = true;

        //Wenn keine Klasse ausgewählt, starte KurswahlActivity
        int klassenIndex = PreferenceHelper.readIntFromPreferences(getApplicationContext(), "meineKlasseInt", -1);
        if (klassenIndex == -1) {

            Intent startKurswahlIntent = new Intent(this, KurswahlActivity.class);
            startActivity(startKurswahlIntent);

        }

        //Refresh wenn in Einstellungen gewollt
        boolean refreshAtStart = PreferenceHelper.readBooleanFromPreferences(getApplicationContext(), "doRefreshAtStart", true);
        if (refreshAtStart) {

            int refreshDays = Integer.valueOf(PreferenceHelper.readStringFromPreferences(getApplicationContext(), "maxDaysToFetchStart", "14"));
            showRefresh();
            taskFragment.downloadAllData(refreshDays);
        }
    }

    /**
     * wechselt zum vom Nutzer als Startscreen festgelegten Fragment
     */
    private void showStartScreen() {

        //zum vom Nutzer gewählten Bildschirm springen
        int startScreenIdentifier = Integer.parseInt(PreferenceHelper.readStringFromPreferences(getApplicationContext(), "startScreen", "1"));
        if (startScreenIdentifier < 1) {
            PreferenceHelper.saveStringToPreferences(getApplicationContext(), "startScreen", "1");
            startScreenIdentifier = 1;
        }
        navigationDrawer.setSelection(startScreenIdentifier, false);
        selectedIdentifier = startScreenIdentifier;
        setFragment(startScreenIdentifier);


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

    @Override
    public void onEventsAdded() {

        eventBus.post(new EventsChangedEvent());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null || requestCode != 1) return;
        String response = data.getStringExtra("ExitCode");
        switch (response) {
            case "Exit":            //Anwendung schließen, falls von LoginActivity gewünscht
                finish();
                break;
            case "LoggedIn":        //LoginActivity hat erfolgreich eingeloggt
                setUp();
                break;
        }

    }

    /**
     * Löscht die Benutzerdaten und initialisert das Laden neu
     */
    private void startLoginActivity() {

        //Benutzerdaten leeren und LoginActivity starten
        PreferenceHelper.saveStringToPreferences(getApplicationContext(), "username", "null");
        PreferenceHelper.saveStringToPreferences(getApplicationContext(), "password", "null");

        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(loginIntent, 1);
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //Noch vorhandenes Item entfernen
        if (refreshItem != null) {
            if (refreshItem.getActionView() != null) {
                refreshItem.getActionView().clearAnimation();
                refreshItem.setActionView(null);
            }
            menu.removeItem(R.id.action_refresh);
        }

        //neues Item erschaffen
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
        } else if (id == R.id.action_edit) {

            openManager();
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
        eventBus.post(new AnalyticsEventEvent("Toolbar", "Refresh"));

        showRefresh();

        //Daten im Hintergrund laden
        int dayCount = Integer.parseInt(PreferenceHelper.readStringFromPreferences(getApplicationContext(), "maxDaysToFetchRefresh", "14"));
        taskFragment.downloadAllData(dayCount);

    }

    /**
     * Wird gecallt wenn downloadAllData beendet wurde
     */
    @Override
    public void onDownloadFinished() {

        LocalData.getInstance().updateCompareDate();
        taskFragment.saveVertretungsDataToFile();

        Toast.makeText(getApplicationContext(), "Daten erfolgreich aktualisiert", Toast.LENGTH_SHORT).show();
        stopRefresh();


    }

    /**
     * Wird gecallt wenn bei downloadAllData keine Verbindung hergestellt werden konnte
     */
    @Override
    public void onNoConnection() {

        Toast.makeText(getApplicationContext(), "Keine Verbindung zum Server!", Toast.LENGTH_LONG).show();
        stopRefresh();
    }

    /**
     * Wird gecallt wenn sich die KlassenList Verändert haben könnte
     */
    @Override
    public void onKlassenListUpdated() {

        //zurücksetzten der Klasse, falls es eine neue Klassenliste gibt
        if (VertretungsData.getInstance().getKlassenList().size() <= PreferenceHelper.readIntFromPreferences(getApplicationContext(), "meineKlasseInt", 0)) {
            PreferenceHelper.saveIntToPreferences(getApplicationContext(), "meineKlasseInt", -1);
        }

        eventBus.post(new KlassenlistUpdatedEvent());
    }

    /**
     * Wird gecallt wenn der Server die Verbindungsabfrage mit 401 ablehnt
     */
    @Override
    public void onNoAccess() {

        showNoAccesDialog();
        stopRefresh();
    }

    private void showNoAccesDialog() {

        isNoAccessDialog = true;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Anmeldefehler");
        dialogBuilder.setMessage("Der Server hat die Anmeldedaten abgelehnt, möchtest du dich neu einloggen?");
        dialogBuilder.setPositiveButton("Neu anmelden", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isNoAccessDialog = false;
                startLoginActivity();
            }
        });
        dialogBuilder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isNoAccessDialog = false;
            }
        });
        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isNoAccessDialog = false;
            }
        });
        dialog.show();
    }

    /**
     * Animiert das Refresh MenuItem
     */
    private void showRefresh() {

        isRefreshing = true;

        if (refreshItem == null) return;

        //Das Refresh Item durch ein ImageView, was sich dreht austauschen
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.refresh_icon, null, false);
        ImageView iv = (ImageView) v.findViewById(R.id.refreshImage);

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
            PreferenceHelper.saveBooleanToPrefernces(getApplicationContext(), "stopRefresh", true);
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

        outState.putLong("selectedIdentifier", selectedIdentifier);
        outState.putCharSequence("title", toolbar.getTitle());
        outState.putBoolean("isInfoDialog", isInfoDialog);
        outState.putBoolean("isNoAccessDialog", isNoAccessDialog);
        outState.putBoolean("noactiveStartscreen", noactiveStartscreen);
        outState.putBoolean("isRefreshing", isRefreshing);

        super.onSaveInstanceState(outState);
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
