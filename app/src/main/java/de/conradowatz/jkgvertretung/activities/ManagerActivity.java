package de.conradowatz.jkgvertretung.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.transition.Fade;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import org.greenrobot.eventbus.EventBus;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.ManagerPagerAdapter;
import de.conradowatz.jkgvertretung.events.FaecherUpdateEvent;
import de.conradowatz.jkgvertretung.variables.Fach;

public class ManagerActivity extends AppCompatActivity {

    public static final int PAGEPOSITION_FAECHER = 0;
    public static final int PAGEPOSITION_FERIEN = 1;
    private Toolbar toolbar;
    private TabLayout materialTabs;
    private ViewPager viewPager;
    private FloatingActionButton fab;
    private EventBus eventBus = EventBus.getDefault();
    private boolean isNewFachDialog;
    private TextView dialogeditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_manager);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Fade());
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        materialTabs = (TabLayout) findViewById(R.id.materialTabs);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {

            String newFachDialogText = savedInstanceState.getString("newFachDialogText");
            if (newFachDialogText != null) showNewFachDialog(newFachDialogText);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupViewPager();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickedAdd();
            }
        });
    }

    /**
     * Wird gecallt wenn der +-FAB geklickt wurde
     */
    private void clickedAdd() {

        if (viewPager.getCurrentItem() == PAGEPOSITION_FAECHER) {

            showNewFachDialog("");

        } else {
            showFerienActivity(-1);
        }

    }

    private void showFerienActivity(int ferienInt) {

        Intent newFerienActivityIntent = new Intent(getApplicationContext(), FerienActivity.class);
        newFerienActivityIntent.putExtra("ferienInt", ferienInt);
        startActivity(newFerienActivityIntent);
    }

    /**
     * Wird gecallt wenn ein neues Fach angelegt werden soll
     */
    private void showNewFachDialog(String preText) {

        //Dialog
        isNewFachDialog = true;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Fach erstellen");
        dialogBuilder.setMessage("Bitte den Namen des Fachs ein:");
        final TextInputLayout textInputLayout = (TextInputLayout) getLayoutInflater().inflate(R.layout.dialog_edittext, null);
        dialogeditText = textInputLayout.getEditText();

        dialogBuilder.setView(textInputLayout);
        dialogBuilder.setPositiveButton("Erstellen", null);
        dialogBuilder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isNewFachDialog = false;
            }
        });
        final AlertDialog dialog = dialogBuilder.create();

        if (dialogeditText != null) {
            dialogeditText.setText(preText);
            dialogeditText.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        if (newFach(dialogeditText.getText().toString(), textInputLayout)) {
                            dialog.dismiss();
                            isNewFachDialog = false;
                        }
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
                        if (newFach(dialogeditText.getText().toString(), textInputLayout)) {
                            dialog.dismiss();
                            isNewFachDialog = false;
                        }
                    }
                });
            }
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                isNewFachDialog = false;
            }
        });
        dialog.show();

    }

    private boolean newFach(String fachName, TextInputLayout textInputLayout) {

        fachName = fachName.trim();

        if (!fachName.isEmpty()) {

            if (Fach.exists(fachName)) {
                textInputLayout.setError("Dieses Fach existiert bereits!");
                return false;
            }

            //neues Fach hinzufügen
            Fach newFach = new Fach(fachName);
            newFach.save();
            eventBus.post(new FaecherUpdateEvent());

            Intent openFachIntent = new Intent(this, FachActivity.class);
            openFachIntent.putExtra("fachId", newFach.getId());
            startActivity(openFachIntent);

            return true;

        } else {
            textInputLayout.setError("Der Name darf nicht leer sein!");
            return false;
        }

    }

    private void setupViewPager() {

        ManagerPagerAdapter adapter = new ManagerPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        materialTabs.setTabTextColors(ContextCompat.getColor(this, R.color.tabs_unselected), ContextCompat.getColor(this, R.color.white));
        materialTabs.setTabMode(TabLayout.MODE_FIXED);
        materialTabs.setupWithViewPager(viewPager);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) finishAfterTransition();
            else finish();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) finishAfterTransition();
        else finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (isNewFachDialog && dialogeditText != null) {
            outState.putString("newFachDialogText", dialogeditText.getText().toString());
        }

        super.onSaveInstanceState(outState);
    }
}
