package de.conradowatz.jkgvertretung.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.events.DaysUpdatedEvent;
import de.conradowatz.jkgvertretung.events.ExitAppEvent;
import de.conradowatz.jkgvertretung.events.FerienChangedEvent;
import de.conradowatz.jkgvertretung.events.KlassenlistUpdatedEvent;
import de.conradowatz.jkgvertretung.fragments.FreieZimmerFragment;
import de.conradowatz.jkgvertretung.fragments.NotenUebersichtFragment;
import de.conradowatz.jkgvertretung.fragments.StundenplanFragment;
import de.conradowatz.jkgvertretung.fragments.TaskFragment;
import de.conradowatz.jkgvertretung.fragments.TerminFragment;
import de.conradowatz.jkgvertretung.tools.ColorAPI;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.variables.Klasse;
import de.conradowatz.jkgvertretung.variables.Lehrer;


public class MainActivity extends AppCompatActivity implements TaskFragment.MainCallbacks {

    private Toolbar toolbar;
    private Drawer navigationDrawer;
    private MenuItem refreshItem;
    private long selectedIdentifier;
    private boolean isRefreshing;
    private boolean isInfoDialog;
    private boolean isNoAccessDialog;
    private boolean isLogOffDialog;
    private boolean isDonateDialog;
    private boolean isActive;
    private boolean noactiveStartscreen;
    private TaskFragment taskFragment;
    private EventBus eventBus = EventBus.getDefault();

    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        adView = (AdView) findViewById(R.id.adView);
        setSupportActionBar(toolbar);

        buildDrawer();
        if (LocalData.isFreeVersion(getApplicationContext()))
            adView.loadAd(new AdRequest.Builder().build());
        else
            adView.setVisibility(View.GONE);

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

        //Falls noch kein Login gesetzt wurde
        if (!LocalData.isLoggedIn()) {

            startLoginActivity();
            return;
        }

        if (savedInstanceState != null) {

            //App noch im Speicher, wiederherstellen

            CharSequence title = savedInstanceState.getCharSequence("title");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(new ColorAPI(this).getActionBarColor()));
            }
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
            isLogOffDialog = savedInstanceState.getBoolean("isLogOffDialog");
            isDonateDialog = savedInstanceState.getBoolean("isDonateDialog");

            if (isInfoDialog) showInfoDialog();
            if (isNoAccessDialog) showNoAccesDialog();
            if (isLogOffDialog) showLogOffDialog();
            if (isDonateDialog) showDonateDialog();

        } else {

            //Falls alles normal -> starten
            if (getSupportActionBar() != null)
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(new ColorAPI(this).getActionBarColor()));
            setUp();
        }

        eventBus.register(this);
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
                        getDefaultDrawerItem().withName("Abmelden").withIcon(R.drawable.ic_exit).withIdentifier(11),
                        new DividerDrawerItem(),
                        getDefaultDrawerItem().withName("Werbung entfernen").withIcon(R.drawable.ic_heart).withIdentifier(12),
                        getDefaultDrawerItem().withName("Einstellungen").withIcon(R.drawable.ic_settings).withIdentifier(13),
                        getDefaultDrawerItem().withName("Feedback").withIcon(R.drawable.ic_feedback).withIdentifier(14),
                        getDefaultDrawerItem().withName("Infos").withIcon(R.drawable.ic_info).withIdentifier(15)


                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {

                        long identifier = drawerItem.getIdentifier();
                        if (identifier < 10) {
                            if (selectedIdentifier != identifier) {
                                setFragment(identifier);
                                selectedIdentifier = identifier;
                                return false;
                            }
                        } else {
                            navigationDrawer.setSelection(selectedIdentifier, false);
                            if (identifier == 11) {
                                showLogOffDialog();
                                return false;
                            }
                            if (identifier == 12) showDonateDialog();
                            if (identifier == 13) {
                                openSettings();
                                return false;
                            }
                            if (identifier == 14) openFeedbackPage();
                            if (identifier == 15) showInfoDialog();
                        }
                        return true;
                    }
                })
                .build();
        if (!LocalData.isFreeVersion(getApplicationContext())) {
            navigationDrawer.removeItem(12);
        }

    }

    private void showDonateDialog() {

        isDonateDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Werbung entfernen / Spenden <3");
        builder.setMessage(R.string.donate_text);
        final MainActivity context = this;
        builder.setPositiveButton("Werbefreie Version", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                isDonateDialog = false;
                Toast.makeText(context, "Danke für die Unterstützung <3", Toast.LENGTH_SHORT).show();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=de.conradowatz.jkgvertretung.pro")));
                } catch (android.content.ActivityNotFoundException e) {
                    //PlayStore not installed
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=de.conradowatz.jkgvertretung.pro")));
                }
            }
        });
        builder.setNegativeButton("Nein danke", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDonateDialog = false;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isDonateDialog = false;
            }
        });
        dialog.show();

    }

    private void showLogOffDialog() {

        isLogOffDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Abmelden");
        builder.setMessage("Bist du sicher, dass du dich von deinem Schulserver abmelden möchtest?\nDu wirst zum Anmeldebildschirm weitergeleitet.");
        builder.setPositiveButton("Abmelden", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isLogOffDialog = false;
                LocalData.setLoggedIn(false);
                SQLite.delete().from(Lehrer.class).async().execute();
                startLoginActivity();
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isLogOffDialog = false;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                isLogOffDialog = false;
            }
        });
        dialog.show();
    }

    /**
     * Zeigt den Info Dialog
     */
    private void showInfoDialog() {

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

        String url = "https://owatz.net/s/appsupport";
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
            ft.replace(R.id.container, TerminFragment.newInstance(TerminFragment.MODE_ALLGEMEIN)).commit();
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

        //Wenn keine Klasse ausgewählt, starte KurswahlActivity
        Klasse selectedKlasse = Klasse.getSelectedKlasse();
        if (selectedKlasse == null) {

            Intent startKurswahlIntent = new Intent(this, KurswahlActivity.class);
            startActivity(startKurswahlIntent);

        }

        if (isActive) showStartScreen();
        else noactiveStartscreen = true;

        //Refresh wenn in Einstellungen gewollt
        boolean refreshAtStart = PreferenceHelper.readBooleanFromPreferences(getApplicationContext(), "doRefreshAtStart", true);
        if (refreshAtStart) {

            int refreshDays = Integer.valueOf(PreferenceHelper.readStringFromPreferences(getApplicationContext(), "maxDaysToFetchStart", "14"));
            showRefresh();
            taskFragment.downloadAllData(refreshDays);
        }
        LocalData.deleteElapsedData();
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
     * Informiert die Fragments, dass die OnlineTage geupdated wurden
     *
     */
    @Override
    public void onDaysUpdated() {

        eventBus.post(new DaysUpdatedEvent());

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
        throwable.printStackTrace();

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

        showRefresh();

        //Daten im Hintergrund laden
        int dayCount = Integer.parseInt(PreferenceHelper.readStringFromPreferences(getApplicationContext(), "maxDaysToFetchRefresh", "14"));
        taskFragment.downloadAllData(dayCount);

        LocalData.deleteElapsedData();

    }

    /**
     * Wird gecallt wenn downloadAllData beendet wurde
     */
    @Override
    public void onDownloadFinished() {

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

        eventBus.post(new KlassenlistUpdatedEvent());
    }

    @Override
    public void onFreieTageAdded() {

        eventBus.post(new FerienChangedEvent());
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

                LocalData.setLoggedIn(false);
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
        outState.putBoolean("isLogOffDialog", isLogOffDialog);
        outState.putBoolean("isDonateDialog", isDonateDialog);
        outState.putBoolean("noactiveStartscreen", noactiveStartscreen);
        outState.putBoolean("isRefreshing", isRefreshing);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        isActive = true;
        if (noactiveStartscreen) {
            showStartScreen();
            noactiveStartscreen = false;
        }
        //if (adView!=null) adView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        isActive = false;
        //if (adView!=null) adView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //if (adView!=null) adView.destroy();
        eventBus.unregister(this);
    }

    @Subscribe
    public void onEvent(ExitAppEvent event) {
        finish();
    }
}
