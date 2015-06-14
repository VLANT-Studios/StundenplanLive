package de.conradowatz.jkgvertretung.activities;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.AdapterView;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.ArrayList;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.fragments.KurswahlFragment;
import de.conradowatz.jkgvertretung.fragments.StundenplanFragment;
import de.conradowatz.jkgvertretung.fragments.VertretungsplanFragment;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.greenrobot.event.EventBus;


public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private AccountHeader accountHeader;
    private Drawer navigationDrawer;

    private StundenplanFragment stundenplanFragment;
    private VertretungsplanFragment vertretungsplanFragment;
    private KurswahlFragment kurswahlFragment;

    private int currentlySelected = 0;
    public VertretungsAPI vertretungsAPI;

    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        buildDrawer();


        if (getLastCustomNonConfigurationInstance()!=null) {
            vertretungsAPI = (VertretungsAPI) getLastCustomNonConfigurationInstance();
            CharSequence title = savedInstanceState.getCharSequence("title");
            getSupportActionBar().setTitle(title);
            int selection = savedInstanceState.getInt("selection");
            navigationDrawer.setSelection(selection, false);
            currentlySelected = savedInstanceState.getInt("currentlySelected");
        } else {
            stundenplanFragment = null;
            vertretungsplanFragment = null;
            kurswahlFragment = null;
            startLoadingActivities();
        }
    }

    private void buildDrawer() {

        ProfileDrawerItem profileDrawerItem = new ProfileDrawerItem().withIcon(getResources().getDrawable(R.drawable.logo));

        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withTextColorRes(R.color.primary_text)
                //TODO .withHeaderBackground(R.drawable.timetable)
                .withSelectionListEnabledForSingleProfile(false)
                .addProfiles(
                        profileDrawerItem
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();

        ArrayList<IDrawerItem> footerList = new ArrayList<>();
        footerList.add(new PrimaryDrawerItem().withName("Infos").withIcon(GoogleMaterial.Icon.gmd_info_outline).withIdentifier(-1));

        navigationDrawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(accountHeader)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Mein Stundenplan").withIcon(GoogleMaterial.Icon.gmd_access_time).withIdentifier(1),
                        //new PrimaryDrawerItem().withName("Mein Vertretungsplan").withIcon(GoogleMaterial.Icon.gmd_format_list_numbered).withIdentifier(2),
                        new PrimaryDrawerItem().withName("Klassen- / Kurswahl").withIcon(GoogleMaterial.Icon.gmd_check_box).withIdentifier(3)//,
                        //new DividerDrawerItem(),
                        //new PrimaryDrawerItem().withName("Allgemeiner Vertretungsplan").withIcon(GoogleMaterial.Icon.gmd_format_list_numbered).withIdentifier(4),
                        //new PrimaryDrawerItem().withName("Anderer Stundenplan").withIcon(GoogleMaterial.Icon.gmd_access_time).withIdentifier(5)
                )
                .withStickyFooterDivider(true)
                .withStickyDrawerItems(footerList)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        int identifier = drawerItem.getIdentifier();
                        if (identifier >= 0) {
                            if (currentlySelected != position) {
                                setFragment(identifier);
                                currentlySelected = position;
                                return false;
                            }
                        } else {
                            navigationDrawer.setSelection(currentlySelected, false);
                            switch (identifier) {
                                case -1:
                                    showInfoDialog();
                                    break;
                            }
                        }
                        return true;
                    }
                })
                .build();

    }

    private void setFragment(int position) {

        FragmentManager mFragmentManager = getFragmentManager();
        if (position == 1) {
            if (stundenplanFragment == null) {
                stundenplanFragment = new StundenplanFragment();
            }
            mFragmentManager.beginTransaction().replace(R.id.container, stundenplanFragment).commit();
            getSupportActionBar().setTitle("Meine Stundenplan");
        } else if (position == 2) {
            if (vertretungsplanFragment == null) {
                vertretungsplanFragment = new VertretungsplanFragment();
            }
            mFragmentManager.beginTransaction().replace(R.id.container, vertretungsplanFragment).commit();
            getSupportActionBar().setTitle("Vertretungsplan");
        } else if (position == 3) {
            if (kurswahlFragment == null) {
                kurswahlFragment = new KurswahlFragment();
            }
            mFragmentManager.beginTransaction().replace(R.id.container, kurswahlFragment).commit();
            getSupportActionBar().setTitle("Klassen- / Kurswahl");
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
        startActivity(startLoadingIntent);

        eventBus.register(this);

    }

    //Information von LoadingActivity bekommen
    public void onEvent(VertretungsAPI event) {

        this.vertretungsAPI = event;
        eventBus.unregister(this);

        boolean firstStart = Boolean.valueOf(PreferenceReader.readStringFromPreferences(getApplicationContext(), "firstStart", "true"));
        if (firstStart) {
            //navigationDrawer.setSelection(3, true);
            navigationDrawer.setSelection(2, true);
            navigationDrawer.openDrawer();
            PreferenceReader.saveStringToPreferences(getApplicationContext(), "firstStart", "false");
        } else {
            navigationDrawer.setSelection(0, true);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {

        String response = data.getStringExtra("ExitCode");
        if (response.equals("Exit")) {                  //Anwendung schließen, falls von Activity gewünscht
            finish();
        } else if (response.equals("LoggedIn")) {       //Anzeige von Fragments starten wenn eingeloggt
            loadData();
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
