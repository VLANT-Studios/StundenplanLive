package de.conradowatz.jkgvertretung.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.pnikosis.materialishprogress.ProgressWheel;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;


public class LoginActivity extends AppCompatActivity {

    TextInputLayout usernameInput;
    TextInputLayout passwordInput;
    Button loginButton;
    ProgressWheel loginProgressWheel;
    TextView loginErrorText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        PreferenceReader.saveStringToPreferences(getApplicationContext(), "username", username);
        PreferenceReader.saveStringToPreferences(getApplicationContext(), "password", password);

        //Anwendung schließen
        Intent backToMain = new Intent();
        backToMain.putExtra("ExitCode", "LoggedIn");
        backToMain.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        setResult(RESULT_OK, backToMain);
        finish();
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

        //Anwendung schließen
        Intent backToMain = new Intent();
        backToMain.putExtra("ExitCode", "Exit");
        backToMain.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        setResult(RESULT_OK, backToMain);
        finish();
    }
}
