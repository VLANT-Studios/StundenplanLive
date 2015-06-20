package de.conradowatz.jkgvertretung.activities;

import android.content.Intent;
import android.os.PersistableBundle;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;

import java.io.File;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.variables.Vertretung;
import de.greenrobot.event.EventBus;

public class LoadingActivity extends AppCompatActivity {

    private static final int CONTENT_LOADING = 1;
    private static final int CONTENT_NOCONNECTION = 2;
    private static final int CONTENT_NOACCES = 3;
    private boolean isnoConnection = false;
    private TextView ladeDatenText;
    private TextView errorText;
    private ProgressWheel progressWheel;
    private ProgressBar progressBar;
    private ImageView cloudImage;
    private ImageView logoImage;
    private RelativeLayout loadingLayout;
    private LinearLayout noaccesLayout;
    private Button retryButton;
    private Button changepwButton;

    private String username;
    private String password;

    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        ladeDatenText = (TextView) findViewById(R.id.ladeDatenText);
        errorText = (TextView) findViewById(R.id.errorText);
        progressWheel = (ProgressWheel) findViewById(R.id.progressWheel);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        cloudImage = (ImageView) findViewById(R.id.cloudImage);
        logoImage = (ImageView) findViewById(R.id.logoImage);
        loadingLayout = (RelativeLayout) findViewById(R.id.loadingLayout);
        noaccesLayout = (LinearLayout) findViewById(R.id.noacces_layout);
        retryButton = (Button) findViewById(R.id.retryButton);
        changepwButton = (Button) findViewById(R.id.changepwButton);
        loadingLayout.setOnClickListener(v -> {

            if (!isnoConnection) {
                return;
            }

            contentMode(CONTENT_LOADING);

            isnoConnection = false;
            downloadData();

        });
        retryButton.setOnClickListener(v -> {

            contentMode(CONTENT_LOADING);
            downloadData();
        });
        changepwButton.setOnClickListener(v -> {

            Intent backToMain = new Intent();
            backToMain.putExtra("ExitCode", "ReLog");
            setResult(RESULT_OK, backToMain);
            finish();

        });

        username = PreferenceReader.readStringFromPreferences(this, "username", "");
        password = PreferenceReader.readStringFromPreferences(this, "password", "");

        downloadData();

    }

    private void downloadData() {

        final VertretungsAPI vertretungsAPI = new VertretungsAPI(username, password);

        vertretungsAPI.getAllInfo(new VertretungsAPI.AsyncVertretungsResponseHandler() {
            @Override
            public void onSuccess() {
                if (vertretungsAPI.getTagList().size()>0) {
                    eventBus.post(vertretungsAPI);
                    finish();
                } else {
                    onNoAccess();
                }
            }

            @Override
            public void onProgress(double progress) {
                progressBar.setProgress((int) progress);
            }

            @Override
            public void onNoConnection() {

                contentMode(CONTENT_NOCONNECTION);
                isnoConnection = true;

            }

            @Override
            public void onNoAccess() {

                contentMode(CONTENT_NOACCES);

            }

            @Override
            public void onOtherError(Throwable e) {
                e.printStackTrace();
            }
        });

    }

    private void contentMode(int contentMode) {

        if (contentMode == CONTENT_LOADING) {

            ladeDatenText.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            progressWheel.setVisibility(View.VISIBLE);
            logoImage.setVisibility(View.VISIBLE);
            cloudImage.setVisibility(View.INVISIBLE);
            errorText.setVisibility(View.INVISIBLE);
            noaccesLayout.setVisibility(View.GONE);

        } else if (contentMode == CONTENT_NOCONNECTION) {

            ladeDatenText.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            progressWheel.setVisibility(View.INVISIBLE);
            logoImage.setVisibility(View.INVISIBLE);
            cloudImage.setVisibility(View.VISIBLE);
            errorText.setVisibility(View.VISIBLE);
            noaccesLayout.setVisibility(View.GONE);

        } else if (contentMode == CONTENT_NOACCES) {

            ladeDatenText.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            progressWheel.setVisibility(View.INVISIBLE);
            logoImage.setVisibility(View.INVISIBLE);
            cloudImage.setVisibility(View.INVISIBLE);
            errorText.setVisibility(View.INVISIBLE);
            noaccesLayout.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public void onBackPressed() {

        //Anwendung schließen
        Intent backToMain = new Intent();
        backToMain.putExtra("ExitCode", "Exit");
        setResult(RESULT_OK, backToMain);
        finish();
    }
}
