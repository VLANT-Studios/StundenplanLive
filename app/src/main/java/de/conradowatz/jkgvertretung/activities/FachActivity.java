package de.conradowatz.jkgvertretung.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import org.greenrobot.eventbus.EventBus;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.FachPagerAdapter;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.VertretungsData;
import de.conradowatz.jkgvertretung.variables.Fach;

public class FachActivity extends AppCompatActivity {

    public static final int TAB_EVENTS = 0;
    public static final int TAB_STUNDEN = 1;
    public static final int TAB_NOTEN = 2;
    private Toolbar toolbar;
    private TabLayout materialTabs;
    private ViewPager viewPager;
    private Fach fach;
    private EventBus eventBus = EventBus.getDefault();
    private boolean isDeleteDialog;
    private boolean isRenameDialog;
    private EditText renameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!VertretungsData.getInstance().isReady() || !LocalData.isReady()) {
            Intent spashIntent = new Intent(this, SplashActivity.class);
            spashIntent.putExtra("intent", getIntent());
            startActivity(spashIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_fach);

        Intent intent = getIntent();
        int tab = TAB_EVENTS;
        if (intent != null) {
            int fachIndex = intent.getIntExtra("fachIndex", 0);
            tab = intent.getIntExtra("tab", TAB_EVENTS);
            fach = LocalData.getInstance().getFächer().get(fachIndex);
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        materialTabs = (TabLayout) findViewById(R.id.materialTabs);
        viewPager = (ViewPager) findViewById(R.id.viewPager);

        toolbar.setTitle(fach.getName());
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("isDeleteDialog")) showDeleteDialog();
            String renameDialogText = savedInstanceState.getString("renameDialogText");
            if (renameDialogText != null) showRenameDialog(renameDialogText);
        }

        setupViewPager(tab);

    }

    public Fach getFach() {
        return fach;
    }

    private void setupViewPager(int tab) {

        FachPagerAdapter adapter = new FachPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        materialTabs.setTabTextColors(ContextCompat.getColor(this, R.color.tabs_unselected), ContextCompat.getColor(this, R.color.white));
        materialTabs.setTabMode(TabLayout.MODE_FIXED);
        materialTabs.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(tab);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_fach, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_delete) {
            showDeleteDialog();
        } else if (id == R.id.action_rename) {
            showRenameDialog(fach.getName());
        } else if (id == android.R.id.home) {

            LocalData.saveToFile(getApplicationContext());
            return false;

        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeleteDialog() {

        isDeleteDialog = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("'" + fach.getName() + "' löschen");
        builder.setMessage("Bist du sicher dass du dieses Fach löschen möchtest?");
        builder.setPositiveButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                LocalData.getInstance().getFächer().remove(fach);
                eventBus.post(new FaecherUpdateEvent());
                LocalData.saveToFile(getApplicationContext());
                finish();
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isDeleteDialog = false;
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isDeleteDialog = false;
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.warn_text));
            }
        });
        dialog.show();

    }

    private void showRenameDialog(String preText) {

        isRenameDialog = true;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Fach umbenennen");
        dialogBuilder.setMessage(String.format("In was soll '%s' umbenannt werden?", fach.getName()));
        final TextInputLayout textInputLayout = (TextInputLayout) getLayoutInflater().inflate(R.layout.dialog_edittext, null);
        renameEditText = textInputLayout.getEditText();

        dialogBuilder.setView(textInputLayout);
        dialogBuilder.setPositiveButton("Umbenennen", null);
        dialogBuilder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isRenameDialog = false;
            }
        });
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                isRenameDialog = false;
            }
        });
        final AlertDialog dialog = dialogBuilder.create();

        if (renameEditText != null) {
            renameEditText.append(preText);
            renameEditText.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        if (renameFach(renameEditText.getText().toString(), textInputLayout))
                            dialog.dismiss();
                    }

                    return false;
                }
            });
        }

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (renameFach(renameEditText.getText().toString(), textInputLayout))
                            dialog.dismiss();
                    }
                });
            }
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

    }

    private boolean renameFach(String fachName, TextInputLayout textInputLayout) {

        fachName = fachName.trim();

        if (!fachName.isEmpty()) {
            for (Fach f : LocalData.getInstance().getFächer()) {
                if (f.getName().equalsIgnoreCase(fachName)) {
                    textInputLayout.setError("Dieses Fach existiert bereits!");
                    return false;
                }
            }

            //Fach umbenennen
            fach.setName(fachName);
            toolbar.setTitle(fachName);
            eventBus.post(new FaecherUpdateEvent());
            LocalData.saveToFile(getApplicationContext());
            return true;

        } else {
            textInputLayout.setError("Der Name darf nicht leer sein!");
            return false;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putBoolean("isDeleteDialog", isDeleteDialog);
        if (isRenameDialog && renameEditText != null)
            outState.putString("renameDialogText", renameEditText.getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        LocalData.saveToFile(getApplicationContext());
    }

}
