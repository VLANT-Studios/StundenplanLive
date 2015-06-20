package de.conradowatz.jkgvertretung.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.io.File;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.fragments.AllgVertretungsplanFragment;
import de.conradowatz.jkgvertretung.fragments.KurswahlFragment;
import de.conradowatz.jkgvertretung.fragments.StundenplanFragment;
import de.conradowatz.jkgvertretung.fragments.VertretungsplanFragment;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.greenrobot.event.EventBus;


public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private Drawer navigationDrawer;
    private Menu menu;

    private StundenplanFragment stundenplanFragment;
    private VertretungsplanFragment vertretungsplanFragment;
    private KurswahlFragment kurswahlFragment;
    private AllgVertretungsplanFragment allgVertretungsplanFragment;

    private int currentlySelected;
    public VertretungsAPI vertretungsAPI;
    private Boolean isRefreshing = false;

    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        buildDrawer();


        if (getLastCustomNonConfigurationInstance() != null) {
            vertretungsAPI = (VertretungsAPI) getLastCustomNonConfigurationInstance();
            CharSequence title = savedInstanceState.getCharSequence("title");
            getSupportActionBar().setTitle(title);
            int selection = savedInstanceState.getInt("selection");
            if (selection>=0) navigationDrawer.setSelection(selection, false);
            currentlySelected = savedInstanceState.getInt("currentlySelected");
            if (isRefreshing==null) isRefreshing = savedInstanceState.getBoolean("isRefreshing");
        } else {
            stundenplanFragment = null;
            vertretungsplanFragment = null;
            kurswahlFragment = null;
            currentlySelected = -1;
            startLoadingActivities();
        }
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
                        new PrimaryDrawerItem().withName("Infos").withIcon(R.drawable.ic_info).withIconTintingEnabled(true).withIdentifier(11)
                )
                .withOnDrawerItemClickListener((parent, view, position, id, drawerItem) -> {
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
                                showInfoDialog();
                                break;
                        }
                    }
                    return true;
                })
                .build();

    }

    private void setFragment(int identifier) {

        FragmentManager mFragmentManager = getSupportFragmentManager();
        if (identifier == 1) {
            if (stundenplanFragment == null) {
                stundenplanFragment = new StundenplanFragment();
            }
            mFragmentManager.beginTransaction().replace(R.id.container, stundenplanFragment).commitAllowingStateLoss();
            getSupportActionBar().setTitle("Mein Stundenplan");
        } else if (identifier == 2) {
            if (vertretungsplanFragment == null) {
                vertretungsplanFragment = new VertretungsplanFragment();
            }
            mFragmentManager.beginTransaction().replace(R.id.container, vertretungsplanFragment).commitAllowingStateLoss();
            getSupportActionBar().setTitle("Mein Vertretungsplan");
        } else if (identifier == 3) {
            if (kurswahlFragment == null) {
                kurswahlFragment = new KurswahlFragment();
            }
            mFragmentManager.beginTransaction().replace(R.id.container, kurswahlFragment).commitAllowingStateLoss();
            getSupportActionBar().setTitle("Klassen- / Kurswahl");
        } else if (identifier == 4) {
            if (allgVertretungsplanFragment == null) {
                allgVertretungsplanFragment = new AllgVertretungsplanFragment();
            }
            mFragmentManager.beginTransaction().replace(R.id.container, allgVertretungsplanFragment).commitAllowingStateLoss();
            getSupportActionBar().setTitle("Allgemeiner Vertretungsplan");
        }
    }

    private void startLoadingActivities() {

        if (PreferenceReader.readStringFromPreferences(this, "username", "null").equals("null")) {
            Intent startLoginIntent = new Intent(this, LoginActivity.class);
            final int result = 1;
            startActivityForResult(startLoginIntent, result);
        } else {
            loadData();
        }
    }

    private void loadData() {

        final File savedSessionFile = new File(getFilesDir(), VertretungsAPI.SAVE_FILE_NAE);
        if (savedSessionFile.exists()) {

            String username = PreferenceReader.readStringFromPreferences(this, "username", "");
            String password = PreferenceReader.readStringFromPreferences(this, "password", "");

            //Load from saved session
            VertretungsAPI.createFromFile(
                    this, username, password, new VertretungsAPI.CreateFromFileHandler() {
                        @Override
                        public void onCreated(VertretungsAPI myVertretungsAPI) {

                            vertretungsAPI = myVertretungsAPI;
                            navigationDrawer.setSelection(0, false);
                            currentlySelected = 0;
                            setFragment(1);

                        }

                        @Override
                        public void onError(Throwable throwable) {

                            Log.e("JKGV", "Error loading data from storage. Redownload it...");
                            throwable.printStackTrace();
                            savedSessionFile.delete();
                            loadData();
                        }
                    }
            );

        } else {

            //Load from the Interwebs

            Intent startLoadingIntent = new Intent(this, LoadingActivity.class);
            final int result = 1;
            startActivityForResult(startLoadingIntent, result);

            eventBus.register(this);

        }

    }

    //Information von LoadingActivity bekommen
    public void onEvent(VertretungsAPI event) {

        vertretungsAPI = event;
        eventBus.unregister(this);

        boolean firstStart = Boolean.valueOf(PreferenceReader.readStringFromPreferences(getApplicationContext(), "firstStart", "true"));
        if (firstStart) {
            navigationDrawer.setSelection(2, false);
            currentlySelected = 2;
            setFragment(3);
            navigationDrawer.openDrawer();
            PreferenceReader.saveStringToPreferences(getApplicationContext(), "firstStart", "false");
        } else {
            navigationDrawer.setSelection(0, false);
            currentlySelected = 0;
            setFragment(1);
        }

        final MainActivity context = this;

        vertretungsAPI.saveToFile(context);

        //mehr Tage im Hintergrund laden
        if (vertretungsAPI.getTagList().size() == 3) {

            showRefresh();
            isRefreshing = true;
            vertretungsAPI.makeTagList(11, 3, new VertretungsAPI.GetDaysHandler() {
                @Override
                public void onFinished() {

                    if (vertretungsAPI.getTagList().size() > 3) vertretungsAPI.saveToFile(context);
                    isRefreshing = false;
                    stopRefresh();

                }

                @Override
                public void onDayAdded() {

                    informFragmentsDayAdded();
                }

                @Override
                public void onError(Throwable throwable) {

                    throwable.printStackTrace();
                }
            });

        }

    }

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

        if (data==null) return;
        String response = data.getStringExtra("ExitCode");
        switch (response) {
            case "Exit":                   //Anwendung schließen, falls von Activity gewünscht
                finish();
                break;
            case "LoggedIn":        //Anzeige von Fragments starten wenn eingeloggt
                loadData();
                break;
            case "ReLog":

                PreferenceReader.saveStringToPreferences(this, "username", "null");
                PreferenceReader.saveStringToPreferences(this, "password", "null");
                eventBus.unregister(this);
                startLoadingActivities();
                break;
        }

    }

    private void showInfoDialog() {

        LayoutInflater inflater = getLayoutInflater();
        View scrollView = inflater.inflate(R.layout.infotext_dialog, null);
        TextView textView = (TextView) scrollView.findViewById(R.id.textView);
        textView.setText(Html.fromHtml(getString(R.string.infoDialog_text)));
        textView.setMovementMethod(LinkMovementMethod.getInstance()); //Link klickbar machen

        AlertDialog.Builder infoDialogB = new AlertDialog.Builder(this);
        infoDialogB.setView(scrollView);
        infoDialogB.setNeutralButton("Okay", null);
        infoDialogB.show();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {

        return vertretungsAPI;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;

        if (isRefreshing) {
            showRefresh();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            refreshClicked(item);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshClicked(final MenuItem item) {

        if (isRefreshing) {
            return;
        }

        showRefresh();
        isRefreshing = true;

        vertretungsAPI.makeTagList(14, 0, new VertretungsAPI.GetDaysHandler() {
            @Override
            public void onFinished() {

                Log.d("SWAG", "finished called");
                stopRefresh();
                isRefreshing = false;

            }

            @Override
            public void onDayAdded() {

                informFragmentsDayAdded();
            }

            @Override
            public void onError(Throwable throwable) {

            }
        });

    }

    private void showRefresh() {

        MenuItem item = menu.findItem(R.id.action_refresh);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_icon, null);

        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);

        item.setActionView(iv);

    }

    private void stopRefresh() {

        MenuItem item = menu.findItem(R.id.action_refresh);

        item.getActionView().clearAnimation();
        item.setActionView(null);

    }

    @Override
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (navigationDrawer != null && navigationDrawer.isDrawerOpen()) {
            navigationDrawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        Log.d("SAVE", "save instance");

        outState.putInt("selection", navigationDrawer.getCurrentSelection());
        outState.putInt("currentlySelected", currentlySelected);
        outState.putCharSequence("title", toolbar.getTitle());
        outState.putBoolean("isRefreshing", isRefreshing);
        super.onSaveInstanceState(outState);
    }
}
