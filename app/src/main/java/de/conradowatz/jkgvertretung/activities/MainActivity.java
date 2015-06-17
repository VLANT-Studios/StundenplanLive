package de.conradowatz.jkgvertretung.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

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

    private StundenplanFragment stundenplanFragment;
    private VertretungsplanFragment vertretungsplanFragment;
    private KurswahlFragment kurswahlFragment;
    private AllgVertretungsplanFragment allgVertretungsplanFragment;

    private int currentlySelected;
    public VertretungsAPI vertretungsAPI;

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
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
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
                    }
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

        Intent startLoadingIntent = new Intent(this, LoadingActivity.class);
        final  int result = 1;
        startActivityForResult(startLoadingIntent, result);

        eventBus.register(this);

    }

    //Information von LoadingActivity bekommen
    public void onEvent(VertretungsAPI event) {

        this.vertretungsAPI = event;
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

        //mehr Tage im Hintergrund laden
        if (vertretungsAPI.getTagList().size() == 3) {

            vertretungsAPI.makeTagList(10, 3, new VertretungsAPI.GetDaysHandler() {
                @Override
                public void onFinished() {

                }

                @Override
                public void onDayAdded() {
                    if (stundenplanFragment != null) {
                        stundenplanFragment.onDayAdded();
                    }
                    if (vertretungsplanFragment != null) {
                        vertretungsplanFragment.onDayAdded();
                    }
                }

                @Override
                public void onError(Throwable throwable) {

                }
            });
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
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

        outState.putInt("selection", navigationDrawer.getCurrentSelection());
        outState.putInt("currentlySelected", currentlySelected);
        outState.putCharSequence("title", toolbar.getTitle());
        super.onSaveInstanceState(outState);
    }
}
