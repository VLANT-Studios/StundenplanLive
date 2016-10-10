package de.conradowatz.jkgvertretung.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.io.File;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.fragments.TaskFragment;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.tools.VertretungsData;


public class LoginActivity extends AppCompatActivity implements TaskFragment.LoginCallbacks {

    TextInputLayout usernameInput;
    TextInputLayout passwordInput;
    Button loginButton;
    ProgressWheel loginProgressWheel;
    TextView loginErrorText;

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
        usernameInput = (TextInputLayout) findViewById(R.id.usernameInput);
        passwordInput = (TextInputLayout) findViewById(R.id.passwordInput);
        loginButton = (Button) findViewById(R.id.loginButton);
        loginProgressWheel = (ProgressWheel) findViewById(R.id.loginProgressWheel);
        loginErrorText = (TextView) findViewById(R.id.loginErrorText);

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
    }


    /**
     * Überprüft die eingegebenen Benutzerdaten
     */
    private void login() {
        final String username = usernameInput.getEditText().getText().toString();
        final String password = passwordInput.getEditText().getText().toString();

        loginButton.setVisibility(View.INVISIBLE);
        loginProgressWheel.setVisibility(View.VISIBLE);
        loginErrorText.setVisibility(View.INVISIBLE);
        passwordInput.getEditText().setEnabled(false);
        usernameInput.getEditText().setEnabled(false);

        VertretungsAPI.checkLogin(username, password, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                onLoggedIn(username, password);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    onLoginError("Ungültige Daten!");
                } else onLoginError("Fehler bei der Verbindung zum Server");
            }
        });
    }

    private void onLoggedIn(String username, String password) {

        PreferenceHelper.saveStringToPreferences(getApplicationContext(), "username", username);
        PreferenceHelper.saveStringToPreferences(getApplicationContext(), "password", password);

        Toast.makeText(this, "Login erfolgreich", Toast.LENGTH_SHORT).show();

        loadVertretungsData();

    }

    /**
     * Läd die Daten entweder aus dem Speicher oder läd sie runter
     */
    private void loadVertretungsData() {

        //Schauen on eine SavedSession im Speicher ist
        File savedSessionFile = new File(getFilesDir(), VertretungsData.SAVE_FILE_NAME);
        if (savedSessionFile.exists()) {

            //Saved Session laden
            taskFragment.createVertretungsDataFromFile();

        } else {

            taskFragment.downloadAllData(0);

        }

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

        Intent backToMain = new Intent();
        backToMain.putExtra("ExitCode", "LoggedIn");
        backToMain.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        setResult(RESULT_OK, backToMain);
        finish();
        overridePendingTransition(0, 0);

    }

    @Override
    public void onVertretungsDataCreated() {

        startMainActiviy();

    }

    @Override
    public void onVertretungsDataCreateError(Throwable throwable) {

        Log.e("JKGDEBUG", "Fehler beim Laden der VertretungsData aus dem Speicher");
        throwable.printStackTrace();

        File savedSessionFile = new File(getFilesDir(), VertretungsData.SAVE_FILE_NAME);
        if (savedSessionFile.delete()) {
            taskFragment.downloadAllData(0);
        } else {
            finish();
        }

    }

    @Override
    public void onDownloadFinished() {

        taskFragment.saveVertretungsDataToFile();
        startMainActiviy();

    }

    @Override
    public void onDownloadError(Throwable throwable) {

        Log.e("JKGDEBUG", "Unbekannter Fehler beim Download");
        throwable.printStackTrace();

        showDownloadErrorDialog();

    }

    private void showDownloadErrorDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Download Error");
        dialogBuilder.setMessage("Beim Download ist ein unbekannter Fehler aufgetreten.");
        dialogBuilder.setPositiveButton("Erneut versuchen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                taskFragment.downloadAllData(0);
            }
        });
        dialogBuilder.setNegativeButton("Beenden", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                closeApp();
            }
        });
        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                closeApp();
            }
        });
        dialog.show();

    }

    @Override
    public void onNoAccess() {

        onLoginError("Ungültige Daten!");

    }

    @Override
    public void onNoConnection() {

        showNoConnectionDialog();
    }

    private void showNoConnectionDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Keine Internetverbindung");
        dialogBuilder.setMessage("Es konnte keine Verbindung zum Server hergestellt werden.");
        dialogBuilder.setPositiveButton("Erneut versuchen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                taskFragment.downloadAllData(0);
            }
        });
        dialogBuilder.setNegativeButton("Beenden", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                closeApp();
            }
        });
        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                closeApp();
            }
        });
        dialog.show();

    }
}
