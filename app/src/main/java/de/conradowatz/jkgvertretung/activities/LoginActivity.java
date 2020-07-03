package de.conradowatz.jkgvertretung.activities;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import androidx.browser.customtabs.CustomTabsIntent;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import de.conradowatz.jkgvertretung.MyApplication;
import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.fragments.TaskFragment;
import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.variables.Schule;


public class LoginActivity extends AppCompatActivity implements TaskFragment.TaskFragmentCallbacks {

    TextInputLayout usernameInput;
    TextInputLayout passwordInput;
    TextInputLayout additionalInput;
    AppCompatSpinner srcSpinner;
    AppCompatSpinner schulListSpinner;
    Button loginButton;
    ProgressWheel loginProgressWheel;
    TextView loginErrorText;
    LinearLayout noschoolLayout;

    private boolean isChooseAWocheDialog;
    private boolean isHasABWocheDialog;

    private TaskFragment taskFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        taskFragment = (TaskFragment) fm.findFragmentByTag(TaskFragment.TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (taskFragment == null) {
            taskFragment = new TaskFragment();
            fm.beginTransaction().add(taskFragment, TaskFragment.TAG_TASK_FRAGMENT).commit();
        }

        setContentView(R.layout.activity_login);
        srcSpinner = (AppCompatSpinner) findViewById(R.id.srcSpinner);
        schulListSpinner = (AppCompatSpinner) findViewById(R.id.schulListSpinner);
        usernameInput = (TextInputLayout) findViewById(R.id.usernameInput);
        passwordInput = (TextInputLayout) findViewById(R.id.passwordInput);
        additionalInput = (TextInputLayout) findViewById(R.id.additionalInput);
        loginButton = (Button) findViewById(R.id.loginButton);
        loginProgressWheel = (ProgressWheel) findViewById(R.id.loginProgressWheel);
        loginErrorText = (TextView) findViewById(R.id.loginErrorText);
        noschoolLayout = (LinearLayout) findViewById(R.id.noschoolLayout);

        if (savedInstanceState!=null) {
            if (savedInstanceState.getBoolean("isHasABWocheDialog")) showHasABWocheDialog();
            if (savedInstanceState.getBoolean("isChooseAWocheDialog")) showChooseAWocheDialog();
        }

        if (LocalData.isOldLocalData()) {
            showOldLocalDataDialog();
        }

        setUp();
    }

    private void showOldLocalDataDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alte Daten importiern");
        builder.setMessage("Es wurden Daten aus einer alten Version der App im Speicher gefunden. Sollen diese importiert oder gelöscht werden?");
        final Activity activity = this;
        builder.setPositiveButton("Importieren", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LocalData.importOldLocalData(activity);
            }
        });
        builder.setNegativeButton("Löschen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LocalData.deleteOldLocalData();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                showOldLocalDataDialog();
            }
        });
        dialog.show();
    }

    private void setUp() {

        ArrayAdapter<String> srcSpinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Schule auf Stundenplan24", "URL selbst eingeben", "Schule aus Liste auswählen"});
        srcSpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        srcSpinner.setAdapter(srcSpinnerArrayAdapter);
        srcSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (position<=1) {
                    schulListSpinner.setVisibility(View.GONE);
                    additionalInput.getEditText().setText("");
                    additionalInput.setVisibility(View.VISIBLE);
                    usernameInput.setVisibility(View.VISIBLE);
                    passwordInput.setVisibility(View.VISIBLE);
                    if (position==0) { //Stundenplan24
                        additionalInput.setHint("Schulnummer (8-stellig)");
                        additionalInput.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
                        additionalInput.getEditText().setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
                    } else { //Custom URL
                        additionalInput.setHint("Indiware mobil URL");
                        additionalInput.getEditText().setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
                        additionalInput.getEditText().setFilters(new InputFilter[]{});
                    }
                } else {
                    schulListSpinner.setVisibility(View.VISIBLE);
                    loginButton.setEnabled(true);
                    additionalInput.setVisibility(View.GONE);
                    additionalInput.getEditText().setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        List<String> schulen = new ArrayList<>();
        for(Schule s : LocalData.getSchulen()) schulen.add(s.name);

        ArrayAdapter<String> schulSpinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, schulen);
        schulSpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        schulListSpinner.setAdapter(schulSpinnerArrayAdapter);
        schulListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                Schule schule = LocalData.setSchule(position);
                if (schule.hasAuth) {
                    usernameInput.setVisibility(View.VISIBLE);
                    passwordInput.setVisibility(View.VISIBLE);
                } else {
                    usernameInput.getEditText().setText("");
                    usernameInput.setVisibility(View.GONE);
                    passwordInput.setVisibility(View.GONE);
                    passwordInput.getEditText().setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Wenn Button gedrückt -> einlogen
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        //Wenn ENTER geklickt wird -> einloggen
        passwordInput.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    login();
                }
                return false;
            }
        });

        additionalInput.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (srcSpinner.getSelectedItemPosition()==0) {
                    if (s.length()==8) {
                        loginButton.setEnabled(true);
                        additionalInput.setErrorEnabled(false);
                        LocalData.setStundenplan24(s.toString());
                    } else {
                        additionalInput.setError("Keine 8-stellige Nummer");
                        additionalInput.setErrorEnabled(true);
                        loginButton.setEnabled(false);
                    }
                } else if (srcSpinner.getSelectedItemPosition()==1) {
                    if (Patterns.WEB_URL.matcher(s.toString()).matches()) {
                        loginButton.setEnabled(true);
                        additionalInput.setErrorEnabled(false);
                        LocalData.setSchulUrl(s.toString());
                    } else {
                        additionalInput.setError("Keine gültige URL");
                        additionalInput.setErrorEnabled(true);
                        loginButton.setEnabled(false);
                    }
                }

            }
        });

        noschoolLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoSchoolDialog();
            }
        });
    }

    private void showNoSchoolDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Keine Schule");
        builder.setMessage(R.string.login_noschool_dialog);
        builder.setPositiveButton("Okay", null);
        builder.setNegativeButton("E-Mail senden", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openFeedback();
            }
        });
        builder.show();
    }

    private void openFeedback() {

        String url = "https://owatz.net/s/appsupport";
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(ContextCompat.getColor(getApplicationContext(), R.color.primary));
        builder.setStartAnimations(this, R.anim.slide_in_right, R.anim.slide_out_left);
        builder.setExitAnimations(this, R.anim.slide_in_left, R.anim.slide_out_right);
        CustomTabsIntent intent = builder.build();
        intent.launchUrl(this, Uri.parse(url));
    }


    /**
     * Überprüft die eingegebenen Benutzerdaten
     */
    private void login() {

        String username = usernameInput.getEditText().getText().toString();
        String password = passwordInput.getEditText().getText().toString();

        boolean hasAuth = !username.isEmpty();
        PreferenceHelper.saveBooleanToPrefernces(MyApplication.getAppContext(), "hasAuth", hasAuth);

        loginButton.setVisibility(View.INVISIBLE);
        loginProgressWheel.setVisibility(View.VISIBLE);
        loginErrorText.setVisibility(View.INVISIBLE);
        passwordInput.getEditText().setEnabled(false);
        usernameInput.getEditText().setEnabled(false);

        taskFragment.downloadAllData(0, username, password);
    }

    private void onLoggedIn(String username, String password) {


        PreferenceHelper.saveStringToPreferences(getApplicationContext(), "username", username);
        PreferenceHelper.saveStringToPreferences(getApplicationContext(), "password", password);

        Toast.makeText(this, "Login erfolgreich", Toast.LENGTH_SHORT).show();

        showHasABWocheDialog();

    }

    private void showHasABWocheDialog() {

        isHasABWocheDialog = true;

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("A / B Woche");
        dialogBuilder.setMessage("Hat deine Schule A und B Woche? Wenn du nicht weißt, was das ist, wähle 'Nein'.");
        final Activity activity = this;
        dialogBuilder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isHasABWocheDialog = false;
                LocalData.setHasABWoche(true);
                showChooseAWocheDialog();
            }
        });
        dialogBuilder.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isHasABWocheDialog = false;
                LocalData.setHasABWoche(false);
                startMainActiviy();
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialog.show();
            }
        });
        dialog.show();
    }

    private void showChooseAWocheDialog() {

        isChooseAWocheDialog = true;

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("A / B Woche");
        dialogBuilder.setMessage("Bitte wähle einen Tag aus, bei dem du sicher bist, dass es A-Woche war/ist. Dies kann später in den Einstellungen geändert werden.");
        final Activity activity = this;
        dialogBuilder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                final Calendar calendar = Calendar.getInstance();
                DatePickerDialog dialog = new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {

                        isChooseAWocheDialog = false;

                        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                        LocalData.setCompareDate(calendar.getTime());
                        startMainActiviy();

                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        showChooseAWocheDialog();
                    }
                });
                dialog.show();
            }
        });
        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialog.show();
            }
        });
        dialog.show();
    }

    private void onLoginError(String error) {

        loginButton.setVisibility(View.VISIBLE);
        loginProgressWheel.setVisibility(View.INVISIBLE);
        passwordInput.getEditText().setEnabled(true);
        usernameInput.getEditText().setEnabled(true);
        loginErrorText.setVisibility(View.VISIBLE);
        loginErrorText.setText(error);

    }

    @Override
    public void onBackPressed() {

        closeApp();
    }

    private void closeApp() {

        //Anwendung schließen
        Intent backToMain = new Intent();
        backToMain.putExtra("ExitCode", "Exit");
        backToMain.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        setResult(RESULT_OK, backToMain);
        finish();
        overridePendingTransition(0, 0);

    }

    private void startMainActiviy() {

        LocalData.setLoggedIn(true);
        Intent backToMain = new Intent();
        backToMain.putExtra("ExitCode", "LoggedIn");
        backToMain.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        setResult(RESULT_OK, backToMain);
        finish();
        overridePendingTransition(0, 0);

    }

    @Override
    public void onDownloadFinished() {

        String username = usernameInput.getEditText().getText().toString();
        String password = passwordInput.getEditText().getText().toString();
        onLoggedIn(username, password);

    }

    @Override
    public void onDownloadError(Throwable throwable) {

        Log.e("JKGDEBUG", "Unbekannter Fehler beim Download");
        throwable.printStackTrace();

        onLoginError("Unbekannter Fehler :/");

    }

    @Override
    public void onNoAccess() {

        onLoginError("Ungültige Daten!");

    }

    @Override
    public void onNoConnection() {

        onLoginError("Keine Verbindung zum Server :(");
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {

        outState.putBoolean("isChooseAWocheDialog", isChooseAWocheDialog);
        outState.putBoolean("isHasABWocheDialog", isHasABWocheDialog);
        super.onSaveInstanceState(outState, outPersistentState);
    }
}
